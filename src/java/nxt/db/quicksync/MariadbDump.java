package nxt.db.quicksync;

import nxt.Constants;
import nxt.Nxt;
import nxt.db.sql.Db;
import nxt.util.LoggerConfigurator;
import org.reflections.ReflectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLDecoder;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;


public class MariadbDump {
    private static final Logger logger = LoggerFactory.getLogger(MariadbDump.class.getSimpleName());

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

            Db.init();
            dump();
        } catch (Exception e) {
            logger.error("Error", e);
        }
    }

    public static void dump() throws IOException, URISyntaxException, ClassNotFoundException, SQLException {

        List<String> classes = getClassNamesFromPackage("nxt.db.quicksync.pojo");
        for (String classname : classes) {
            Class clazz = Class.forName("nxt.db.quicksync.pojo." + classname);
            StringBuilder sb = new StringBuilder("select ");
            List<Field> fields = new ArrayList<>();
            for (Field field : ReflectionUtils.getAllFields(clazz)) {
                fields.add(field);
                sb.append(field.getName()).append(",");
            }
            // Remove last ,
            sb.deleteCharAt(sb.lastIndexOf(","));
            sb.append(" from ");
            sb.append(clazz.getSimpleName().toLowerCase());
            sb.append(";");
            String sql = sb.toString();
            System.out.println(sql);

            try (Connection con = Db.getConnection()) {
                Db.beginTransaction();
                con.setReadOnly(true);
                PreparedStatement ps = con.prepareStatement(sql);
                ResultSet rs = ps.executeQuery();
                while (rs.next()) {
                    int i=1;
                    for (Field field : fields) {
                        Class fieldType = field.getType();

                        if (fieldType.equals(String.class)) {
//                            System.err.println("S: "+ rs.getString(i));
                        } else if (fieldType.equals(Long.class) || fieldType.equals(long.class)) {
//                            System.err.println("L: "+rs.getLong(i));
                        } else {
                            System.err.println(field.getName() + ": " + fieldType);
                        }

                        i++;
                    }

                }
                Db.endTransaction();
                System.err.println("X");
            }

        }
    }

    public static List<String> getClassNamesFromPackage(String packageName) throws IOException, URISyntaxException {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        URL packageURL;
        ArrayList<String> names = new ArrayList<String>();
        ;

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
            System.out.println(">" + jarFileName);
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
