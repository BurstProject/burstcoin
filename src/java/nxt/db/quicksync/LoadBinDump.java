package nxt.db.quicksync;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import nxt.Constants;
import nxt.Nxt;
import nxt.db.h2.H2Dbs;
import nxt.db.mariadb.MariadbDbs;
import nxt.db.sql.Db;
import nxt.db.store.Dbs;
import nxt.util.LoggerConfigurator;
import org.apache.commons.lang.StringUtils;
import org.reflections.ReflectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLDecoder;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.zip.GZIPInputStream;


public class LoadBinDump {
    private static final Logger logger = LoggerFactory.getLogger(LoadBinDump.class.getSimpleName());
    private static Dbs dbs;

    public static void main(String[] args) {
        try {
            LoggerConfigurator.init();

            Db.init();
            switch (Db.getDatabaseType()) {
                case MARIADB:
                    logger.info("Using mariadb Backend");
                    dbs = new MariadbDbs();
                    break;
                case H2:
                    logger.info("Using h2 Backend");
                    dbs = new H2Dbs();
                    break;
                default:
                    throw new RuntimeException("Error initializing wallet: Unknown database type");
            }
            load(args[0]);
        } catch (Exception e) {
            logger.error("Error", e);
        }
    }

    public static void load(String filename) throws
            IOException, URISyntaxException, ClassNotFoundException, SQLException, IllegalAccessException, InstantiationException {

        Kryo kryo = new Kryo();
        long start = System.currentTimeMillis();
        try (Input input = new Input(new GZIPInputStream(new FileInputStream(filename)))) {
            try (Connection con = Db.getConnection()) {
                Db.beginTransaction();
                dbs.disableForeignKeyChecks(con);

                Object o = null;
                Class clazz = null;
                Statement stmt = con.createStatement();

                while (!input.eof() && (clazz = kryo.readClass(input).getType()) != null) {
                    long rows = input.readLong();
                    stmt.executeUpdate("truncate table " + clazz.getSimpleName().toLowerCase() + ";");


                    StringBuilder sb = new StringBuilder("insert into ");
                    sb.append(clazz.getSimpleName().toLowerCase());
                    sb.append(" (");
                    List<Field> fields = new ArrayList<>();

                    for (Field field : ReflectionUtils.getAllFields(clazz)) {
                        fields.add(field);
                        sb.append(field.getName()).append(",");

                    }
                    // Remove last ,
                    sb.deleteCharAt(sb.lastIndexOf(","));
                    sb.append(") VALUES ( ");
                    sb.append(StringUtils.repeat("?", ",", fields.size()));
                    sb.append(")");
                    String sql = sb.toString();
                    logger.debug(sql);
                    PreparedStatement ps = con.prepareStatement(sql);

                    for (long l = 0; l < rows; l++) {
                        o = kryo.readObject(input, clazz);
                        int i = 0;
                        for (Field field : fields) {
                            i++;
                            Class fieldType = field.getType();
                            field.setAccessible(true);

                            if (fieldType.equals(String.class)) {
                                ps.setString(i, (String) field.get(o));

                            } else if (fieldType.equals(Long.class)) {
                                ps.setLong(i, (Long) field.get(o));
                            } else if (fieldType.equals(long.class)) {
                                ps.setLong(i, field.getLong(o));
                            } else if (fieldType.isArray()) {
                                // Byte array?

                                if (fieldType.getComponentType().equals(byte.class)) {
                                    ps.setBytes(i, (byte[]) field.get(o));

                                } else {
                                    logger.error("Unhandled field type for"+field.getName() + ": " + fieldType);
                                }

                            } else {
                                logger.error("Unhandled field type for"+field.getName() + ": " + fieldType);
                            }
                        }
                        ps.addBatch();
                        ps.clearParameters();
                        if (l % 100 == 0) {
                           logger.info(clazz.getSimpleName()+": "+l + " / " + rows );
                            ps.executeBatch();
                        }
                    }
                    ps.executeBatch();
                    logger.info(clazz.getSimpleName()+": "+rows + " / " + rows );
                }

                dbs.enableForeignKeyChecks(con);
                Db.commitTransaction();
                Db.endTransaction();
                logger.info("Dump loaded in " + ((System.currentTimeMillis() - start) / 1000) + "seconds");
            }

        }

    }


    public static List<String> getClassNamesFromPackage(String packageName) throws IOException, URISyntaxException {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        URL packageURL;
        ArrayList<String> names = new ArrayList<String>();


        packageName = packageName.replace(".", "/");
        packageURL = classLoader.getResource(packageName);

        if (packageURL.getProtocol().equals("jar")) {
            String jarFileName;
            JarFile jf;
            Enumeration<JarEntry> jarEntries;
            String entryName;

            // build jar file name, then loop through zipped entries
            jarFileName = URLDecoder.decode(packageURL.getFile(), "UTF-8");
            jarFileName = jarFileName.substring(5, jarFileName.indexOf("!"));
            jf = new JarFile(jarFileName);
            jarEntries = jf.entries();
            while (jarEntries.hasMoreElements()) {
                entryName = jarEntries.nextElement().getName();
                if (entryName.startsWith(packageName) && entryName.length() > packageName.length() + 5) {
                    entryName = entryName.substring(packageName.length(), entryName.lastIndexOf('.'));
                    names.add(entryName);
                }
            }

            // loop through files in classpath
        } else {
            URI uri = new URI(packageURL.toString());
            File folder = new File(uri.getPath());
            // won't work with path which contains blank (%20)
            // File folder = new File(packageURL.getFile());
            File[] contenuti = folder.listFiles();
            String entryName;
            for (File actual : contenuti) {
                entryName = actual.getName();
                if (entryName.contains(".")) {
                    entryName = entryName.substring(0, entryName.lastIndexOf('.'));
                    names.add(entryName);
                }
            }
        }
        return names;
    }
}
