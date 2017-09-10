package nxt.db.quicksync;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Output;
import nxt.Constants;
import nxt.Nxt;
import nxt.db.h2.H2Dbs;
import nxt.db.mariadb.MariadbDbs;
import nxt.db.sql.Db;
import nxt.db.store.Dbs;
import nxt.util.LoggerConfigurator;
import org.reflections.ReflectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLDecoder;
import java.sql.*;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.zip.GZIPOutputStream;

/**
 * Creates a binary dump from a sql database. The resulting dump is database-agnostic and should work with any
 * driver.
 * The files on the pojo-package reflect the database structure of h2 and mysql. Reflection is used to read the
 * attributes of those objects which are named just like the database columns.
 * With this information select statements are created which are then used to create instances of the classes in pojo
 * which are then written to the dump.
 * by BraindeadOne (BURST-BJSX-4C6A-UH35-F4Q3A)
 */
public class CreateBinDump {
    private static final Logger logger = LoggerFactory.getLogger(CreateBinDump.class.getSimpleName());
    private static Dbs dbs;

    public static void main(String[] args) {
        try {
            long startTime = System.currentTimeMillis();

            LoggerConfigurator.init();

            String dbUrl;
            if (Constants.isTestnet) {
                dbUrl = Nxt.getStringProperty("nxt.testDbUrl");
            } else {
                dbUrl = Nxt.getStringProperty("nxt.dbUrl");
            }

            if (Db.getDatabaseType() == Db.TYPE.H2) {
                logger.warn("Creating a dump from a h2 database will probably take multiple hours." +
                        " Consider switching to mariadb for much better performance");
                Thread.sleep(20000);
            }
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
            Db.init();
            dump(args[0]);
        } catch (Exception e) {
            logger.error("Error", e);
        }
    }

    public static void dump(String filename) throws IOException, URISyntaxException, ClassNotFoundException, SQLException, IllegalAccessException, InstantiationException {
        long start = System.currentTimeMillis();
        Kryo kryo = new Kryo();
        try (Output output = new Output(new GZIPOutputStream(new FileOutputStream(filename)))) {

            output.writeString(BinDumps.MAGIC);
            output.write(BinDumps.VERSION);
            output.writeString(Nxt.VERSION);

            int fetchSize;
            switch (Db.getDatabaseType()) {
                case H2:
                    // h2 works best with a ridiculous fetch size
                    fetchSize = 1000000;
                    break;

                default:
                case MARIADB:
                    fetchSize = 100000;
            }


            try (Connection con = Db.getConnection()) {
                Db.beginTransaction();

                Statement stmt = con.createStatement();
                ResultSet rs = stmt.executeQuery("select max(height) from block;");
                rs.next();
                int height = rs.getInt(1);
                output.write(height);
                rs.close();

                List<String> classes = getClassNamesFromPackage("nxt.db.quicksync.pojo");

                for (String classname : classes) {
                    Class clazz = Class.forName("nxt.db.quicksync.pojo." + classname);
                    StringBuilder sb = new StringBuilder("select ");
                    List<Field> fields = new ArrayList<>();
                    boolean hasDbId = false;
                    for (Field field : ReflectionUtils.getAllFields(clazz)) {
                        fields.add(field);
                        String fieldname = field.getName();
                        sb.append(fieldname).append(",");
                        if ("db_Id".equals(fieldname))
                            hasDbId = true;
                    }
                    // Remove last ,
                    sb.deleteCharAt(sb.lastIndexOf(","));
                    sb.append(" from ");
                    sb.append(clazz.getSimpleName().toLowerCase());
                    if (hasDbId)
                        sb.append(" order by db_id");
                    sb.append(" limit ?,").append(fetchSize).append(";");


                    String sql = sb.toString();
                    logger.debug(sql);
                    kryo.writeClass(output, clazz);
                    rs = con.createStatement().executeQuery("select count(1) from " + classname);
                    rs.next();
                    long rows = rs.getLong(1);
                    output.writeLong(rows);
                    long records = 0;
                    PreparedStatement ps = con.prepareStatement(sql);

                    while (records < rows) {
                        ps.setLong(1, records);

                        rs = ps.executeQuery();
                        Object data = clazz.newInstance();
                        while (rs.next()) {
                            records++;
                            if (records % 1000 == 0)
                                logger.info(classname + ": " + records + " / " + rows);
                            int i = 1;


                            for (Field field : fields) {
                                Class fieldType = field.getType();
                                field.setAccessible(true);
                                Object value;


                                if (fieldType.equals(String.class)) {
                                    value = rs.getString(i);
                                } else if (fieldType.equals(Long.class)) {
                                    if (rs.getObject(i) == null)
                                        value = null;
                                    else
                                        value = rs.getLong(i);
                                } else if (fieldType.equals(long.class)) {
                                    value = rs.getLong(i);
                                } else if (fieldType.isArray()) {
                                    // Byte array?
                                    if (fieldType.getComponentType().equals(byte.class)) {
                                        // Not sure if this works across drivers
                                        value = rs.getBytes(i);
                                    } else {
                                        logger.error("Unhandled array type for" + field.getName() + ": " + fieldType);
                                        value = rs.getObject(i);
                                    }

                                } else {
                                    logger.error("Unhandled field type for" + field.getName() + ": " + fieldType);
                                    value = rs.getObject(i);
                                }
                                field.set(data, value);
                                i++;
                            }

                            kryo.writeObject(output, data);
                        }

                        rs.close();


                    }
                    ps.close();
                    output.flush();
                    logger.info(classname + ": " + rows + " / " + rows);

                }
                Db.endTransaction();
            }

        }
        logger.info("Dump created in " + ((System.currentTimeMillis() - start) / 1000) + "seconds");
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
            File[] folderfiles = folder.listFiles();
            String entryName;
            for (File actual : folderfiles) {
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
