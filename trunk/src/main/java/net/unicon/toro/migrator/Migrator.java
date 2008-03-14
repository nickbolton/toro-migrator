package net.unicon.toro.migrator;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.Map.Entry;

import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.SQLExec;
import org.apache.tools.ant.types.Path;
import org.dom4j.Attribute;
import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.Node;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.SAXReader;
import org.dom4j.io.XMLWriter;


public class Migrator {

    private File extractDir;
    private File installDir;
    private boolean windows = false;
    private Properties tokens = new Properties();
    private boolean keepAlm = false;

    private String jdbcDriver = null;
    private String jdbcHostname = null;
    private String jdbcPort = null;
    private String jdbcUsername = null;
    private String jdbcPassword = null;
    private String jdbcDatabaseName = null;
    private String jdbcUrl = null;
    private String sqlfile = null;
    private String jdbcClasspath = null;

    public Migrator(String extractDir, String academusInstallDir) {
        this.extractDir = new File(extractDir);
        this.installDir = new File(academusInstallDir);
    }

    public void execute() throws Exception {
        windows = System.getProperty("os.name").toLowerCase().indexOf("windows") >= 0;
        generateInstallerConfig();
        backupTomcat();
        deployNewTomcat();
        initializeDbSettings();
        migratePortletConfigurations();
        migrateBlojsom();
        migrateChannelConfigurations();
        migratePortalConfigurations();
        migrateApache();
        retokenizeTomcat();
        dbMigrations();
        checkForMissingWebApplications();
        setFileOwnershipPermissions();
    }

    private void checkForMissingWebApplications() throws Exception {
        System.out.println("Checking for other deployed web applications...");
        File oldWebappsDir = new File(installDir, "unicon/Academus/portal-tomcat-a.old/webapps");
        File[] webapps = oldWebappsDir.listFiles(new NonAcademusWebappFilenameFilter());
        boolean found = false;
        for (int i=0; i<webapps.length; i++) {
            String webappName = webapps[i].getName();
            File newWebXml = new File(installDir, "unicon/Academus/portal-tomcat-a/webapps/"+webappName+"/WEB-INF/web.xml");
            if (!newWebXml.exists()) {
                found = true;
                System.out.println("Found web application '"+webappName+"'.");
                Utils.copyDir(webapps[i], new File(installDir, "unicon/Academus/portal-tomcat-a/webapps/"+webappName), false);
            }
        }
        if (found) {
            System.out.println();
            System.out.println("These web applications may require manual migration to the new tomcat. " +
                "This tool simply copied the web application to the new tomcat instance. " +
                "If it depends on common or shared libraries these will need to be copied to the new tomcat.");
            System.out.println("The old tomcat was moved to portal-tomcat-a.old.");
        }
    }

    private void setFileOwnershipPermissions() throws Exception {
        System.out.println("Setting file permissions...");
        String cmd = null;
        if (windows) {

        } else {
            cmd = "chown -R nobody:nobody " +
                new File(installDir, "unicon/Academus").getAbsolutePath() + " " +
                new File(installDir, "unicon/tools").getAbsolutePath();
        }
        Utils.invokeCommand(cmd, null);

        if (windows) {

        } else {
            cmd = "chmod -R a+x " +
                new File(installDir, "unicon/Academus/portal-tomcat-a/bin").getAbsolutePath();
        }
        Utils.invokeCommand(cmd, null);
    }

    private void retokenizeTomcat() throws Exception {
        FileInputStream is = null;
        try {
            is = new FileInputStream(new File(extractDir, "ant.install.properties"));
            tokens.load(is);
        } finally {
            if (is != null) {is.close(); is = null;}
        }

        System.out.println("Replacing settings from the previous install...");

        System.out.println("Tokens:\n" + tokens);

        String[] files = {
            "unicon/Academus/portal-tomcat-a/conf/Standalone/localhost/portal.xml",
            "unicon/Academus/portal-tomcat-a/conf/Standalone/localhost/toro-blojsom.xml",
            "unicon/Academus/portal-tomcat-a/conf/Standalone/localhost/toro-briefcase-portlet.xml",
            "unicon/Academus/portal-tomcat-a/conf/Standalone/localhost/toro-gateway-portlet.xml",
            "unicon/Academus/portal-tomcat-a/conf/Standalone/localhost/toro-messaging-portlet.xml",
            "unicon/Academus/portal-tomcat-a/conf/Standalone/localhost/toro-permissions-portlet.xml",
            "unicon/Academus/portal-tomcat-a/conf/Standalone/localhost/toro-web-content-portlet.xml",
            "unicon/Academus/portal-tomcat-a/webapps/portal/WEB-INF/classes/properties/ldap.xml",
            "unicon/Academus/portal-tomcat-a/webapps/portal/WEB-INF/classes/properties/personDirectory.xml",
            "unicon/Academus/portal-tomcat-a/webapps/portal/WEB-INF/classes/properties/ToroPersonDirs.xml",
            "unicon/Academus/portal-tomcat-a/webapps/toro-blojsom/WEB-INF/default/blog.properties",
            "unicon/Academus/portal-tomcat-a/webapps/toro-blojsom/WEB-INF/web.xml",
            "unicon/Academus/portal-tomcat-a/webapps/toro-gateway-portlet/WEB-INF/classes/config/blog-default.xml",
            "unicon/Academus/portal-tomcat-a/webapps/toro-gateway-portlet/WEB-INF/classes/config/blog-EXAMPLE.xml",
            "unicon/Academus/portal-tomcat-a/webapps/toro-portlets-common/WEB-INF/web.xml",
            "unicon/Academus/portal-tomcat-a/webapps/toro-briefcase-portlet/WEB-INF/classes/log4j.properties",
            "unicon/Academus/portal-tomcat-a/webapps/toro-gateway-portlet/WEB-INF/classes/log4j.properties",
            "unicon/Academus/portal-tomcat-a/webapps/toro-messaging-portlet/WEB-INF/classes/log4j.properties",
            "unicon/Academus/portal-tomcat-a/webapps/toro-permissions-portlet/WEB-INF/classes/log4j.properties",
            "unicon/Academus/portal-tomcat-a/webapps/toro-portlets-common/WEB-INF/classes/log4j.properties",
            "unicon/Academus/portal-tomcat-a/webapps/toro-web-content-portlet/WEB-INF/classes/log4j.properties",
        };

        for (int i=0; i<files.length; i++) {
            replaceTokens(new File(installDir, files[i]));
        }
    }

    private String replaceTokensInLine(String line) throws Exception {
        Iterator itr = tokens.entrySet().iterator();
        while (itr.hasNext()) {
            Entry e = (Entry)itr.next();
            String exp = "INSTALLER_TOKEN"+e.getKey().toString();
            line = line.replaceAll(exp, e.getValue().toString());
        }

        return line;
    }

    private void replaceTokens(File f) throws Exception {
        if (!f.exists()) {
            throw new RuntimeException("File does not exist: " + f.getAbsolutePath());
        }

        PrintWriter out = null;
        BufferedReader in = null;
        try {
            in = new BufferedReader(new FileReader(f));
            StringWriter sw = new StringWriter();
            out = new PrintWriter(sw);
            String line;
            while ((line = in.readLine()) != null) {
                out.println(replaceTokensInLine(line));
            }
            in.close(); in = null;
            out = new PrintWriter(new FileWriter(f));
            out.print(sw.toString());
        } finally {
            if (in != null) {in.close(); in = null;}
            if (out != null) {out.close(); out = null;}
        }
    }

    private String findPostgresLib(String path) {
        File dir = new File(path, "common/lib");
        File[] files = dir.listFiles();

        for (int i=0; i<files.length; i++) {
            if (files[i].getName().indexOf("postgres") == 0) {
                return files[i].getAbsolutePath();
            } else if (files[i].getName().indexOf("pg74") == 0) {
                return files[i].getAbsolutePath();
            }
        }

        dir = new File(path, "webapps/portal/WEB-INF/lib");
        files = dir.listFiles();
        for (int i=0; i<files.length; i++) {
            if (files[i].getName().indexOf("postgres") == 0) {
                return files[i].getAbsolutePath();
            } else if (files[i].getName().indexOf("pg74") == 0) {
                return files[i].getAbsolutePath();
            }
        }

        throw new RuntimeException("Failed to find postgres lib in path: " + path);
    }

    private String findOracleLib(String path) {
        File dir = new File(path);
        File[] files = dir.listFiles();

        for (int i=0; i<files.length; i++) {
            if (files[i].getName().indexOf("ojdbc14") == 0) {
                return files[i].getAbsolutePath();
            }
        }

        dir = new File(path, "webapps/portal/WEB-INF/lib");
        files = dir.listFiles();
        for (int i=0; i<files.length; i++) {
            if (files[i].getName().indexOf("ojdbc14") == 0) {
                return files[i].getAbsolutePath();
            }
        }

        throw new RuntimeException("Failed to find oracle lib in path: " + path);
    }

    private String findJtdsLib(String path) {
        File dir = new File(path);
        File[] files = dir.listFiles();

        for (int i=0; i<files.length; i++) {
            if (files[i].getName().indexOf("jtds") == 0) {
                return files[i].getAbsolutePath();
            }
        }

        dir = new File(path, "webapps/portal/WEB-INF/lib");
        files = dir.listFiles();
        for (int i=0; i<files.length; i++) {
            if (files[i].getName().indexOf("jtds") == 0) {
                return files[i].getAbsolutePath();
            }
        }

        throw new RuntimeException("Failed to find jtds lib in path: " + path);
    }

    private String findJConnectLib(String path) {
        File dir = new File(path);
        File[] files = dir.listFiles();

        for (int i=0; i<files.length; i++) {
            if (files[i].getName().indexOf("jsqlconnect") == 0) {
                return files[i].getAbsolutePath();
            }
        }

        dir = new File(path, "webapps/portal/WEB-INF/lib");
        files = dir.listFiles();
        for (int i=0; i<files.length; i++) {
            if (files[i].getName().indexOf("jsqlconnect") == 0) {
                return files[i].getAbsolutePath();
            }
        }

        throw new RuntimeException("Failed to find jconnect lib in path: " + path);
    }

    private String getJdbcClasspath() {
        StringBuffer sb = new StringBuffer();
        sb.append(System.getProperty("java.class.path"));
        sb.append(System.getProperty("path.separator"));
        sb.append(getLibsForClasspath(new File(installDir, "/unicon/Academus/portal-tomcat-a/common/lib")));
        sb.append(System.getProperty("path.separator"));
        sb.append(getLibsForClasspath(new File(installDir, "/unicon/Academus/portal-tomcat-a/shared/lib")));
        sb.append(System.getProperty("path.separator"));
        sb.append(getLibsForClasspath(new File(installDir, "/unicon/Academus/portal-tomcat-a/webapps/portal/WEB-INF/lib")));
        sb.append(System.getProperty("path.separator"));
        sb.append(new File(installDir, "/unicon/Academus/portal-tomcat-a/webapps/portal/WEB-INF/classes").getAbsolutePath());
        return sb.toString();
    }

    private String getLibsForClasspath(File libDir) {
        if (!libDir.exists()) {
            throw new RuntimeException("Lib dir does not exist: " + libDir.getAbsolutePath());
        }
        File[] libs = libDir.listFiles();

        StringBuffer sb = new StringBuffer();
        for (int i=0; libs!=null && i<libs.length; i++) {
            if (i>0) {
                sb.append(System.getProperty("path.separator"));
            }
            sb.append(libs[i].getAbsolutePath());
        }
        return sb.toString();
    }

    private boolean dbAlreadyMigrated(String jdbcDriver, String jdbcUrl,
        String jdbcUsername, String jdbcPassword) throws Exception {

        File jvmExe = Utils.getJavaExec(installDir);
        String command = jvmExe.getAbsolutePath() + " -classpath " + getJdbcClasspath() +
            " net.unicon.toro.migrator.CheckDbMigration " +
            jdbcDriver + " " + jdbcUrl + " " + jdbcUsername + " " +jdbcPassword;

        int rc = Utils.invokeCommandWithRC(command, null);
        if (rc != 0 && rc != 1) {
            throw new RuntimeException("CheckDbMigration failed!");
        }
        return rc == 0;
    }

    private void initializeDbSettings() throws Exception {
        Properties p = new Properties();
        FileInputStream is = null;
        try {
            is = new FileInputStream(new File(extractDir, "ant.install.properties"));
            p.load(is);

            jdbcDriver = p.getProperty("jdbc.driver");
            jdbcHostname = p.getProperty("jdbc.hostname");
            jdbcPort = p.getProperty("jdbc.port");
            jdbcUsername = p.getProperty("jdbc.user");
            jdbcPassword = p.getProperty("jdbc.password");
            jdbcDatabaseName = p.getProperty("jdbc.database.name");

            if (jdbcDriver.toLowerCase().indexOf("postgres")>=0) {
                jdbcUrl = "jdbc:postgresql://"+jdbcHostname+":"+jdbcPort+"/"+jdbcDatabaseName;
                sqlfile = extractDir.getAbsolutePath()+"/sphinx48-to-toro-db-migration-postgres.sql";

                String jdbcDriverLib = findPostgresLib(installDir.getAbsolutePath()+"/unicon/Academus/portal-tomcat-a");
                new File(jdbcDriverLib).delete();
                jdbcDriverLib = findPostgresLib(installDir.getAbsolutePath()+"/unicon/Academus/portal-tomcat-a.old");
                File driverFile = new File(jdbcDriverLib);
                jdbcClasspath = installDir.getAbsolutePath()+"/unicon/Academus/portal-tomcat-a/common/lib/"+driverFile.getName();
                Utils.copyFile(jdbcDriverLib, jdbcClasspath);

            } else if (jdbcDriver.toLowerCase().indexOf("oracle")>=0) {
                jdbcUrl = "jdbc:oracle:thin:@"+jdbcHostname+":"+jdbcPort+":"+jdbcDatabaseName;
                sqlfile = extractDir.getAbsolutePath()+"/sphinx48-to-toro-db-migration-oracle.sql";

                String jdbcDriverLib = findOracleLib(installDir.getAbsolutePath()+"/unicon/Academus/portal-tomcat-a");
                new File(jdbcDriverLib).delete();
                jdbcDriverLib = findOracleLib(installDir.getAbsolutePath()+"/unicon/Academus/portal-tomcat-a.old");
                File driverFile = new File(jdbcDriverLib);
                jdbcClasspath = installDir.getAbsolutePath()+"/unicon/Academus/portal-tomcat-a/common/lib/"+driverFile.getName();
                Utils.copyFile(jdbcDriverLib, jdbcClasspath);

            } else if (jdbcDriver.toLowerCase().indexOf("jtds")>=0) {
                jdbcUrl = "jdbc:jtds:sqlserver://"+jdbcHostname+":"+jdbcPort+";DatabaseName="+jdbcDatabaseName;
                sqlfile = extractDir.getAbsolutePath()+"/sphinx48-to-toro-db-migration-sqlserver.sql";

                String jdbcDriverLib = findJtdsLib(installDir.getAbsolutePath()+"/unicon/Academus/portal-tomcat-a");
                new File(jdbcDriverLib).delete();
                jdbcDriverLib = findJtdsLib(installDir.getAbsolutePath()+"/unicon/Academus/portal-tomcat-a.old");
                File driverFile = new File(jdbcDriverLib);
                jdbcClasspath = installDir.getAbsolutePath()+"/unicon/Academus/portal-tomcat-a/common/lib/"+driverFile.getName();
                Utils.copyFile(jdbcDriverLib, jdbcClasspath);

            } else if (jdbcDriver.toLowerCase().indexOf("jnetdirect")>=0) {
                jdbcUrl = "jdbc:JSQLConnect://"+jdbcHostname+":"+jdbcPort+"/databaseName="+jdbcDatabaseName;
                sqlfile = extractDir.getAbsolutePath()+"/sphinx48-to-toro-db-migration-sqlserver.sql";

                String jdbcDriverLib = findJConnectLib(installDir.getAbsolutePath()+"/unicon/Academus/portal-tomcat-a");
                new File(jdbcDriverLib).delete();
                jdbcDriverLib = findJConnectLib(installDir.getAbsolutePath()+"/unicon/Academus/portal-tomcat-a.old");
                File driverFile = new File(jdbcDriverLib);
                jdbcClasspath = installDir.getAbsolutePath()+"/unicon/Academus/portal-tomcat-a/common/lib/"+driverFile.getName();
                Utils.copyFile(jdbcDriverLib, jdbcClasspath);

            }
        } finally {
            if (is != null) { is.close(); is = null;}
        }
    }

    private void dbMigrations() throws Exception {
        if (!dbAlreadyMigrated(jdbcDriver, jdbcUrl, jdbcUsername, jdbcPassword)) {
            if (jdbcUrl != null) {
                System.out.println("Migrating database...");
                SQLExec sqlTask = new SQLExec();
                sqlTask.setClasspath(new Path(null, jdbcClasspath));
                sqlTask.setDriver(jdbcDriver);
                sqlTask.setPassword(jdbcPassword);
                sqlTask.setUrl(jdbcUrl);
                sqlTask.setUserid(jdbcUsername);
                sqlTask.setSrc(new File(sqlfile));
                sqlTask.setProject(new Project());
                sqlTask.execute();

                String url = extractDir.toURL().toExternalForm();
                if (url.endsWith("/")) {
                    url = url.substring(0, url.length()-1);
                }
                String args = url+"/import/migrate.crn" + " " +
                    extractDir.getAbsolutePath()+"/import" + " " +
                    "org.jasig.portal.FilePatternPhrase.USE_DEFAULT_VALUE";
                File jvmExe = Utils.getJavaExec(installDir);
                String command = jvmExe.getAbsolutePath() + " -classpath " + getJdbcClasspath() +
                    " net.unicon.toro.migrator.RunCernunnos ";

                Utils.invokeCommand(command + args, null);

                args = url+"/import/import.crn" + " " +
                    extractDir.getAbsolutePath()+"/import" + " " +
                    "org.jasig.portal.FilePatternPhrase.USE_DEFAULT_VALUE";


                Utils.invokeCommand(command + args, null);
            }
        } else {
            System.out.println("Database already migrated.");
        }
    }

    private void appendToFile(String f, String s) throws Exception {
        PrintWriter out = null;
        try {
            out = new PrintWriter(new FileWriter(f, true));
            out.println(s);
        } finally {
            if (out != null) {out.close(); out = null;}
        }
    }

    private String findPortalRedirect(String contents) throws Exception {
        String[] split = contents.split("\n");

        String target = "Redirect /portal/WEB-INF";
        for (int i=0; i<split.length; i++) {
            if (split[i].contains(target) && !split[i].trim().startsWith("#")) {
                String[] toks = split[i].split(" ");
                String portalRedirect = toks[toks.length-1].trim();
                if (!portalRedirect.startsWith("http") || !portalRedirect.endsWith("portal")) {
                    throw new RuntimeException("Found Redirect /portal/WEB-INF in httpd.conf, but it was invalid: " + portalRedirect);
                }
                return portalRedirect;
            }
        }

        throw new RuntimeException("Failed to find Redirect /portal/WEB-INF in httpd.conf");
    }

    private void migrateApache() throws Exception {
        File httpdConf = new File(installDir, "unicon/Academus/portal-apache/config/httpd.conf");

        net.unicon.toro.installer.tools.Utils.instance().
            backupFile(httpdConf, true);

        String contents = Utils.readFile(httpdConf);
        String portalRedirect = findPortalRedirect(contents);

        StringBuffer addedMounts = new StringBuffer();
        addedMounts.append("  JkMount /toro-blojsom/blog/* worker1\n");
        addedMounts.append("  JkMount /toro-blojsom/xmlrpc/* worker1\n");
        addedMounts.append("  JkMount /toro-blojsom/commentapi/* worker1\n");
        addedMounts.append("  JkMount /toro-blojsom/atomapi/* worker1\n");
        addedMounts.append("  JkMount /toro-portlets-common/services/* worker1\n");
        addedMounts.append("  JkMount /toro-portlets-common/download worker1\n");
        addedMounts.append("  JkMount /toro-portlets-common/downloadService worker1\n");
        addedMounts.append("  JkMount /toro-portlets-common/spellcheck worker1\n");
        addedMounts.append("  JkMount /toro-messaging-portlet/services/* worker1\n");
        addedMounts.append("  JkMount /toro-briefcase-portlet/rendering/jsp/*.jsp worker1\n");
        addedMounts.append("  JkMount /toro-gateway-portlet/rendering/jsp/*.jsp worker1\n");
        addedMounts.append("  JkMount /toro-messaging-portlet/rendering/jsp/*.jsp worker1\n");
        addedMounts.append("  JkMount /toro-permissions-portlet/rendering/jsp/*.jsp worker1\n");
        addedMounts.append("  JkMount /toro-web-content-portlet/rendering/jsp/*.jsp worker1\n");

        StringBuffer addedRedirects = new StringBuffer();
        addedRedirects.append("        Redirect /toro-briefcase-portlet/WEB-INF ").append(portalRedirect).append('\n');
        addedRedirects.append("        Redirect /toro-gateway-portlet/WEB-INF ").append(portalRedirect).append('\n');
        addedRedirects.append("        Redirect /toro-messaging-portlet/WEB-INF ").append(portalRedirect).append('\n');
        addedRedirects.append("        Redirect /toro-permissions-portlet/WEB-INF ").append(portalRedirect).append('\n');
        addedRedirects.append("        Redirect /toro-portlets-common/WEB-INF ").append(portalRedirect).append('\n');
        addedRedirects.append("        Redirect /toro-web-content-portlet/WEB-INF ").append(portalRedirect).append('\n');
        addedRedirects.append("        Redirect /toro-blojsom/WEB-INF ").append(portalRedirect).append('\n');

        contents = Utils.replaceAll(contents, "^\\s*JkMount\\s+/AcademusApps/download\\s+worker1\\s*$", addedMounts.toString());
        contents = Utils.replaceAll(contents, "^\\s*JkMount /AcademusApps.*$", "");
        contents = Utils.replaceAll(contents, "^\\s*JkMount /blojsom/.*$", "");
        contents = Utils.replaceAll(contents, "^\\s*Redirect\\s+/AcademusApps/WEB-INF\\s+"+portalRedirect+"\\s*$", addedRedirects.toString());
        contents = Utils.replaceAll(contents, "^\\s*Redirect /blojsom/.*$", "");


        Utils.writeFile(httpdConf, contents);
    }

    private void migratePortalConfigurations() throws Exception {
        System.out.println("Migrating uPortal settings...");
        String oldDir = installDir.getAbsolutePath()+"/unicon/Academus/portal-tomcat-a.old/webapps/portal/WEB-INF/classes/properties";
        String newDir = installDir.getAbsolutePath()+"/unicon/Academus/portal-tomcat-a/webapps/portal/WEB-INF/classes/properties";

        String[] propertyFiles = {
            "/EntityPropertyRegistry.xml",
            "/Logger.properties",
            "/browser.mappings",
            "/drop_group_entity.xml",
            "/global_dictionary.properties",
            "/global_dictionary_ja_JP.properties",
            "/media.properties",
            "/mime.properties",
            "/portal.properties",
            "/proxyportlet.xml",
            "/rdbm.properties",
            "/resin.conf",
            "/sample-jaas.conf",
            "/security.properties",
            "/serializer.properties",
            "/services.xml",
            "/uPortal.xml",
            "/worker.properties",
            "/PermissionsManagerRegistry.xml",
        };
        for (int i=0; i<propertyFiles.length; i++) {
            Utils.copyFile(oldDir+propertyFiles[i], newDir+propertyFiles[i]);
        }

        String[] jspHtmlFiles = {
            "changepassword.jsp",
            "cscr.jsp",
            "hidden.html",
            "index.html",
            "index_option2.html",
            "index_option3.html",
            "main.html",
        };
        for (int i=0; i<jspHtmlFiles.length; i++) {
            Utils.copyFile(installDir.getAbsolutePath()+"/unicon/Academus/portal-tomcat-a.old/webapps/portal/"+jspHtmlFiles[i],
                installDir.getAbsolutePath()+"/unicon/Academus/portal-tomcat-a/webapps/portal/"+jspHtmlFiles[i]);
        }

        List destDirs = new ArrayList();
        destDirs.add(new File(newDir, "groups"));
        migrateFiles(new File(oldDir, "groups"), null, destDirs);
        destDirs.clear();
        destDirs.add(new File(newDir, "container"));
        migrateFiles(new File(oldDir, "container"), null, destDirs);

        Utils.copyFile(installDir+"/unicon/Academus/portal-tomcat-a.old/webapps/portal/WEB-INF/classes/log4j.properties",
            installDir+"/unicon/Academus/portal-tomcat-a/webapps/portal/WEB-INF/classes/log4j.properties");

        appendToFile(newDir+"/browser.mappings", "\n# Google probing bot\nGooglebot/2.*=1");

        mergeConfiguration("merge-portal.xml", "unicon/Academus/portal-tomcat-a/webapps/portal/WEB-INF/classes/properties");

        if (keepAlm) {
            revertToAlm();
        }
    }

    private void migrateChannelConfigurations() throws Exception {
        System.out.println("Migrating channel settings...");
        String oldDir = installDir + "/unicon/Academus/portal-tomcat-a.old/webapps/portal/WEB-INF/classes/properties/";
        String newDir = installDir + "/unicon/Academus/portal-tomcat-a/webapps/portal/WEB-INF/classes/properties/";

        String[] files = {
            "AcademusImportService.properties",
            "jms-content.properties",
            "jms_cache.properties",
            "academus-apps-factory.properties",
            "rad.properties",
            "academus-apps-lms.properties",
            "rmi.properties",
            "academus-apps.properties",
            "session-policy",
            "academus-lms.properties",
            "unicon-service.properties",
            "academus-portal.properties",
            "velocity.properties",
            "common-factory.properties",
            "version.properties",
            "factoryImpl.properties"};

        for (int i=0; i<files.length; i++) {
            Utils.copyFile(oldDir+files[i], newDir+files[i]);
        }
    }

    private void migrateBlojsom() throws Exception {
        File blojsomWebInf = new File(installDir, "unicon/Academus/portal-tomcat-a/webapps/toro-blojsom/WEB-INF");
        File[] blogs = blojsomWebInf.listFiles(new BlogFileFilter());
        File f = null;
        for (int i=0; i<blogs.length; i++) {
            f = new File(blogs[i], "blog.properties");
            String blogName = blogs[i].getName();
            Properties p = new Properties();
            FileInputStream is = null;
            try {
                is = new FileInputStream(f);
                p.load(is);
                String blogBaseUrl = p.getProperty("blog-base-url");
                if (blogBaseUrl != null) {
                    String blogBaseUrlServerPort = blogBaseUrl.replaceAll("blojsom*.", "");

                    String blogProperties = Utils.readFile(f);
                    blogProperties = Utils.replaceAll(blogProperties, "blog-base-url", "blog-base-url="+blogBaseUrlServerPort+"toro-blojsom/");
                    blogProperties = Utils.replaceAll(blogProperties, "blog-url", "blog-url="+blogBaseUrlServerPort+"toro-blojsom/blog/"+blogName+"/");

                    Utils.writeFile(f, blogProperties);
                }

            } catch (Exception e) {
                if (f != null) {
                    System.out.println("Failed migrating file: " + f.getAbsolutePath());
                }
                throw e;
            } finally {
                if (is != null) {is.close(); is=null;}
            }
        }


    }

    private void migratePortletConfigurations() throws Exception {
        System.out.println("Migrating portlet settings...");
        File oldDir = new File(installDir, "unicon/Academus/portal-tomcat-a.old/webapps/AcademusApps/WEB-INF/classes/config");
        String newTomcat = "unicon/Academus/portal-tomcat-a";
        List destDirs = new ArrayList();
        destDirs.add(new File(installDir, newTomcat+"/webapps/toro-briefcase-portlet/WEB-INF/classes/config"));
        migrateFiles(oldDir, "briefcase", destDirs);

        destDirs.clear();
        destDirs.add(new File(installDir, newTomcat+"/webapps/toro-web-content-portlet/WEB-INF/classes/config"));
        migrateFiles(oldDir, "content", destDirs);

        destDirs.clear();
        destDirs.add(new File(installDir, newTomcat+"/webapps/toro-messaging-portlet/WEB-INF/classes/config"));
        destDirs.add(new File(installDir, newTomcat+"/webapps/toro-portlets-common/WEB-INF/classes/config"));
        migrateFiles(oldDir, "downloadservice.properties", destDirs);

        destDirs.clear();
        destDirs.add(new File(installDir, newTomcat+"/webapps/toro-briefcase-portlet/WEB-INF/classes/config"));
        destDirs.add(new File(installDir, newTomcat+"/webapps/toro-gateway-portlet/WEB-INF/classes/config"));
        destDirs.add(new File(installDir, newTomcat+"/webapps/toro-portlets-common/WEB-INF/classes/config"));
        migrateFiles(oldDir, "encryption.xml", destDirs);

        /*
        destDirs.clear();
        destDirs.add(new File(installDir, newTomcat+"/webapps/toro-gateway-portlet/WEB-INF/classes/config"));
        migrateFiles(oldDir, "gateway", destDirs);
        migrateFiles(oldDir, "blog", destDirs);
        File blogs = new File(oldDir, "blogs");
        if (blogs.exists()) {
            migrateFiles(blogs, "gateway", destDirs);
        }
        */

        destDirs.clear();
        destDirs.add(new File(installDir, newTomcat+"/webapps/toro-messaging-portlet/WEB-INF/classes/config"));
        destDirs.add(new File(installDir, newTomcat+"/webapps/toro-portlets-common/WEB-INF/classes/config"));
        migrateFiles(oldDir, "messaging", destDirs);

        destDirs.clear();
        destDirs.add(new File(installDir, newTomcat+"/webapps/toro-permissions-portlet/WEB-INF/classes/config"));
        migrateFiles(oldDir, "permissions", destDirs);

        destDirs.clear();
        destDirs.add(new File(installDir, newTomcat+"/webapps/toro-web-content-portlet/WEB-INF/classes/config"));
        migrateFiles(oldDir, "xhtmlfilter.properties", destDirs);

        Utils.copyDir(new File(installDir, "unicon/Academus/portal-tomcat-a.old/webapps/blojsom"), new File(installDir, newTomcat+"/webapps/toro-blojsom"));

        migratePortletWebXml();

        // migrate permissions portlet settings
        URL url = new URL("file:"+new File(installDir, "unicon/Academus/portal-tomcat-a.old/webapps/AcademusApps/WEB-INF/classes/config/permissions-portlet.xml").getAbsolutePath());
        SAXReader reader = new SAXReader();
        Document document = reader.read(url);
        List importSrcAttributes = document.selectNodes("//*[name()='portlet-access']/*[name()='import']/@src");
        for (Iterator iter = importSrcAttributes.iterator(); iter.hasNext(); ) {
            Attribute a = (Attribute)iter.next();
            String configFile = a.getText();
            File f = new File(installDir, "unicon/Academus/portal-tomcat-a.old/webapps/AcademusApps/WEB-INF/classes"+configFile);
            if (!f.exists()) {
                throw new RuntimeException("Permissions Portlet config file does not exist: " + f.getAbsolutePath());
            }
            Utils.copyFile(f, new File(installDir, newTomcat+"/webapps/toro-permissions-portlet/WEB-INF/classes"+configFile));
        }

    }

    private String getGatewayServletTemplate() {
        StringBuffer sb = new StringBuffer();
        sb.append("    <servlet>\n");
        sb.append("        <servlet-name>%PORTLET_NAME%</servlet-name>\n");
        sb.append("        <display-name>%PORTLET_NAME% Wrapper</display-name>\n");
        sb.append("        <description>Automated generated Portlet Wrapper</description>\n");
        sb.append("        <servlet-class>org.jasig.portal.container.PortletServlet</servlet-class>\n");
        sb.append("        <init-param>\n");
        sb.append("            <param-name>portlet-class</param-name>\n");
        sb.append("            <param-value>net.unicon.academus.apps.gateway.GatewayPortlet</param-value>\n");
        sb.append("        </init-param>\n");
        sb.append("        <init-param>\n");
        sb.append("            <param-name>portlet-guid</param-name>\n");
        sb.append("            <param-value>toro-gateway-portlet.%PORTLET_NAME%</param-value>\n");
        sb.append("        </init-param>\n");
        sb.append("    </servlet>\n");
        return sb.toString();
    }

    private String getGatewayServletMappingTemplate() {
        StringBuffer sb = new StringBuffer();
        sb.append("    <servlet-mapping>\n");
        sb.append("        <servlet-name>%PORTLET_NAME%</servlet-name>\n");
        sb.append("        <url-pattern>/%PORTLET_NAME%/*</url-pattern>\n");
        sb.append("    </servlet-mapping>\n");
        return sb.toString();
    }

    private void migratePortletWebXml() throws Exception {
        SAXReader reader = new SAXReader();
        URL url = new URL("file:"+new File(installDir, "unicon/Academus/portal-tomcat-a.old/webapps/AcademusApps/WEB-INF/web.xml").getAbsolutePath());
        URL portletUrl = new URL("file:"+new File(installDir, "unicon/Academus/portal-tomcat-a.old/webapps/AcademusApps/WEB-INF/portlet.xml").getAbsolutePath());
        Document document = reader.read(url);
        Document portletDoc = reader.read(portletUrl);
        List servletNodes = document.selectNodes("//param-value[text()='net.unicon.academus.apps.gateway.GatewayPortlet']");

        StringBuffer servletsBuffer = new StringBuffer();
        StringBuffer servletMappingsBuffer = new StringBuffer();

        StringBuffer portletsBuffer = new StringBuffer();

        for (Iterator iter = servletNodes.iterator(); iter.hasNext(); ) {
            Node n = (Node)iter.next();
            Element servletNode = n.getParent().getParent();
            String servletName = servletNode.selectSingleNode("servlet-name").getText();
            String portletGuid = servletNode.selectSingleNode("init-param/param-name[text()='portlet-guid']/../param-value").getText();
            System.out.println("Migrating gateway portlet: " + servletName + "/" + portletGuid);

            servletsBuffer.append(getGatewayServletTemplate().replaceAll("%PORTLET_NAME%", servletName));
            servletMappingsBuffer.append(getGatewayServletMappingTemplate().replaceAll("%PORTLET_NAME%", servletName));

            migratePortletGuid(portletGuid, "toro-gateway-portlet."+servletName);

            List portletNameNodes = portletDoc.selectNodes("//*");
            Element portletNode = (Element)portletDoc.selectSingleNode("//*[name()='portlet-name' and text()='"+servletName+"']");
            if (portletNode != null) {
                portletNode = portletNode.getParent();
                String xpath = "*[name()='init-param']/*[name()='name' and text()='configPath']/../*[name()='value']";
                Node configPathNode = portletNode.selectSingleNode(xpath);
                if (configPathNode == null) {
                    StringWriter sw = new StringWriter();
                    XMLWriter xmlWriter = new XMLWriter( sw, OutputFormat.createPrettyPrint() );
                    xmlWriter.write( portletNode );
                }
                String configFile = configPathNode.getText();
                File f = new File(installDir, "unicon/Academus/portal-tomcat-a/webapps/toro-gateway-portlet"+configFile);
                Utils.copyFile(new File(installDir, "unicon/Academus/portal-tomcat-a.old/webapps/AcademusApps"+configFile), f);

                if (f.exists()) {
                    String contents = Utils.readFile(f);
                    contents = contents.replaceAll("/blojsom/", "/toro-blojsom/");
                    Utils.writeFile(f, contents);
                }
                StringWriter sw = new StringWriter();
                XMLWriter xmlWriter = new XMLWriter( sw, OutputFormat.createPrettyPrint() );
                xmlWriter.write( portletNode );
                portletsBuffer.append(sw.toString());
            }
        }

        File f = new File(installDir, "unicon/Academus/portal-tomcat-a/webapps/toro-gateway-portlet/WEB-INF/web.xml");
        String webXml = Utils.readFile(f);
        webXml = webXml.replaceAll("%SERVLET_LIST%", servletsBuffer.toString());
        webXml = webXml.replaceAll("%SERVLET_MAPPING_LIST%", servletMappingsBuffer.toString());
        Utils.writeFile(f, webXml);

        f = new File(installDir, "unicon/Academus/portal-tomcat-a/webapps/toro-gateway-portlet/WEB-INF/portlet.xml");
        String portletXml = Utils.readFile(f);
        portletXml = portletXml.replaceAll("%PORTLETS%", portletsBuffer.toString());
        Utils.writeFile(f, portletXml);
    }

    private void migratePortletGuid(String oldGuid, String newGuid) throws Exception {
        File jvmExe = Utils.getJavaExec(installDir);
        String command = jvmExe.getAbsolutePath() + " -classpath " + getJdbcClasspath() +
            " net.unicon.toro.migrator.MigratePortletGuids " +
            jdbcDriver + " " + jdbcUrl + " " + jdbcUsername + " " +jdbcPassword +
            " " + oldGuid + " " + newGuid;

        int rc = Utils.invokeCommandWithRC(command, null);
        if (rc != 0) {
            throw new RuntimeException("CheckDbMigration failed!");
        }
    }

    private void migrateFiles(File dir, String prefix, List destDirs) throws Exception {
        File[] files = dir.listFiles();
        for (int i=0; i<files.length; i++) {
            if (files[i].isFile() && (prefix == null || files[i].getName().startsWith(prefix))) {
                for (int j=0; j<destDirs.size(); j++) {
                    Utils.copyFile(files[i], new File((File)destDirs.get(j), files[i].getName()));
                }
            }
        }
    }

    private void deployNewTomcat() throws Exception {
        System.out.println("Installing Tomcat 5.5.x ...");

        String filename = "portal-tomcat-a.zip";
        File zipFile = new File(extractDir, filename);
        if (!zipFile.exists()) {
            throw new RuntimeException("Missing tomcat: " + zipFile.getAbsolutePath());
        }
        File dir = new File(installDir, "unicon/Academus");
        if (!dir.exists()) {
            throw new RuntimeException("Academus directory missing: " + dir.getAbsolutePath());
        }

        System.out.println("Extracting " + filename + " to " + dir.getAbsolutePath() + " ...");
        Utils.extract(zipFile, dir);

        File oldServerXml = new File(installDir, "unicon/Academus/portal-tomcat-a.old/conf/server.xml");
        File newServerXml = new File(installDir, "unicon/Academus/portal-tomcat-a/conf/server.xml");
        Utils.copyFile(oldServerXml, newServerXml);

        mergeConfiguration("merge-server.xml", "unicon/Academus/portal-tomcat-a/conf");

        Utils.copyDir(new File(installDir, "unicon/Academus/portal-tomcat-a.old/webapps/portal/resources"),
            new File(installDir, "unicon/Academus/portal-tomcat-a/webapps/portal/resources"));
        Utils.copyDir(new File(installDir, "unicon/Academus/portal-tomcat-a.old/webapps/portal/html/help"),
            new File(installDir, "unicon/Academus/portal-tomcat-a/webapps/portal/html/help"));
        Utils.copyDir(new File(installDir, "unicon/Academus/portal-tomcat-a.old/webapps/portal/WEB-INF/classes/net/unicon/portal/layout"),
            new File(installDir, "unicon/Academus/portal-tomcat-a/webapps/portal/WEB-INF/classes/net/unicon/portal/layout"));
        Utils.copyDir(new File(installDir, "unicon/Academus/portal-tomcat-a.old/webapps/portal/media/org/jasig/portal/layout/AL_TabColumn"),
            new File(installDir, "unicon/Academus/portal-tomcat-a/webapps/portal/media/org/jasig/portal/layout/AL_TabColumn"));
        Utils.copyDir(new File(installDir, "unicon/Academus/portal-tomcat-a.old/webapps/portal/media/net/unicon/academusTheme/academus"),
            new File(installDir, "unicon/Academus/portal-tomcat-a/webapps/portal/media/net/unicon/academusTheme/academus"));
        Utils.copyDir(new File(installDir, "unicon/Academus/portal-tomcat-a.old/webapps/portal/WEB-INF/classes/org/jasig/portal/layout/AL_TabColumn"),
            new File(installDir, "unicon/Academus/portal-tomcat-a/webapps/portal/WEB-INF/classes/org/jasig/portal/layout/AL_TabColumn"));

    }

    private void revertToAlm() throws Exception {
        mergeConfiguration("merge-alm.xml", "unicon/Academus/portal-tomcat-a/webapps/portal/WEB-INF/classes/properties");
    }

    private void mergeConfiguration(String mergeFile, String baseDir) throws Exception {
        File jvmExe = Utils.getJavaExec(installDir);
        String command = jvmExe.getAbsolutePath() + " -classpath " +
            getJdbcClasspath() + " net.unicon.toro.installer.tools.MergeConfiguration " +
            extractDir.getAbsolutePath()+"/"+mergeFile+" "+installDir.getAbsolutePath()+"/"+baseDir;

        Utils.invokeCommand(command, null);

    }

    private void deployNewTomcatOLD() throws Exception {
        System.out.println("Installing Tomcat 5.5.x ...");

        String filename = "apache-tomcat-5.5.26.zip";
        File zipFile = new File(extractDir, filename);
        if (!zipFile.exists()) {
            throw new RuntimeException("Missing tomcat: " + zipFile.getAbsolutePath());
        }
        File dir = new File(installDir, "unicon/Academus");
        if (!dir.exists()) {
            throw new RuntimeException("Academus directory missing: " + dir.getAbsolutePath());
        }

        System.out.println("Extracting " + filename + " to " + dir.getAbsolutePath() + " ...");
        Utils.extract(zipFile, dir);

        File oldServerXml = new File(installDir, "unicon/Academus/portal-tomcat-a.old/conf/server.xml");
        File newServerXml = new File(installDir, "unicon/Academus/portal-tomcat-a/conf/server.xml");
        Utils.copyFile(oldServerXml, newServerXml);

        mergeConfiguration("merge-server.xml", "unicon/Academus/portal-tomcat-a/conf");
    }

    private void generateInstallerConfig() throws Exception {
        System.out.println("Generating Installer Config...");
        File dir = new File(installDir, "unicon/Academus");
        if (!dir.exists()) {
            throw new RuntimeException("Missing Academus directory: " + dir.getAbsolutePath());
        }
        new BuildToroInstallerConfiguration(dir, extractDir).execute();
    }

    private void deployNewJdk() throws Exception {
        System.out.println("Installing JDK 5.0 ...");
        String os = System.getProperty("os.name");
        String filename = "jdk-1.5.zip";
        /*
        if (os.toLowerCase().contains("linux")) {
            filename = "jdk-1.5-linux.zip";
        } else if (os.toLowerCase().contains("windows")) {
            filename = "jdk-1.5-windows.zip";
        } else if (os.toLowerCase().contains("sunos")) {
            filename = "jdk-1.5-solaris.zip";
        } else {
            throw new RuntimeException("OS not supported: " + os);
        }
        */

        File zipFile = new File(extractDir, "jdk-1.5.zip");
        if (!zipFile.exists()) {
            throw new RuntimeException("Missing jdk: " + zipFile.getAbsolutePath());
        }
        File toolsDir = new File(installDir, "unicon/tools");
        if (!toolsDir.exists()) {
            throw new RuntimeException("Tools directory missing: " + toolsDir.getAbsolutePath());
        }

        System.out.println("Extracting " + filename + " to " + toolsDir.getAbsolutePath() + " ...");
        Utils.extract(zipFile, toolsDir);

        File oldCacertsFile = new File(toolsDir, "j2sdk.old/jre/lib/security/cacerts");
        if (!oldCacertsFile.exists()) {
            throw new RuntimeException("Missing cacerts file: " + oldCacertsFile.getAbsolutePath());
        }

        File cacertsFile = new File(toolsDir, "j2sdk/jre/lib/security/cacerts");
        Utils.copyFile(oldCacertsFile, cacertsFile);

        File binDir = new File(toolsDir, "j2sdk/bin");
        File[] binFiles = binDir.listFiles();
        for (int i=0; i<binFiles.length; i++) {
            if (windows) {
                setExecutableWindows(binFiles[i]);
            } else {
                setExecutableUnix(binFiles[i]);
            }
        }
    }

    private void setExecutableUnix(File f) throws Exception {
        Runtime rt = Runtime.getRuntime();
        Process p = rt.exec("chmod a+x " + f.getAbsolutePath());
        int rc = p.waitFor();
        if (rc != 0) {
            throw new RuntimeException("chmod a+x " + f.getAbsolutePath() + " failed: " + rc);
        }
    }

    private void setExecutableWindows(File f) throws Exception {
        Runtime rt = Runtime.getRuntime();
        Process p = rt.exec("chmod a+x " + f.getAbsolutePath());
        int rc = p.waitFor();
        if (rc != 0) {
            throw new RuntimeException("chmod a+x " + f.getAbsolutePath() + " failed: " + rc);
        }
    }

    private void backupJdk() {
        File jdkDir = new File(installDir, "unicon/tools/j2sdk");
        if (!jdkDir.exists()) {
            throw new RuntimeException("JDK directory doesn not exist. Aborting..: " + jdkDir.getAbsolutePath());
        }
        File backup = new File(installDir, "unicon/tools/j2sdk.old");
        if (backup.exists()) return;

        boolean success = jdkDir.renameTo(backup);
        if (!success) {
            throw new RuntimeException("Failed backing up jdk dir: " + jdkDir.getAbsolutePath());
        }
    }

    private void backupTomcat() {

        File tomcatDir = new File(installDir, "unicon/Academus/portal-tomcat-a");
        System.out.println("Backing up Tomcat to: " +tomcatDir.getAbsolutePath()+".old" );

        if (!tomcatDir.exists()) {
            throw new RuntimeException("Tomcat directory doesn not exist. Aborting..: " + tomcatDir.getAbsolutePath());
        }
        File backup = new File(installDir, "unicon/Academus/portal-tomcat-a.old");
        if (backup.exists()) return;

        boolean success = tomcatDir.renameTo(backup);
        if (!success) {
            throw new RuntimeException("Failed backing up tomcat dir: " + tomcatDir.getAbsolutePath());
        }
    }



    /**
     * @param args
     */
    public static void main(String[] args) {
        System.setErr(System.out);
        try {
            if (args.length < 2) {
                System.out.println("Usage Migrator <extract dir> <academus install dir>");
                System.exit(1);
            }
            new Migrator(args[0], args[1]).execute();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    private static class NonAcademusWebappFilenameFilter implements FileFilter {
        public boolean accept(File f) {
            String webappName = f.getName();
            if (webappName.startsWith("Academus") ||
                webappName.equals("blojsom")      ||
                webappName.equals("portal")) return false;

            File webxml = new File(f, "WEB-INF/web.xml");
            return webxml.exists();
        }
    }

    private static class BlogFileFilter implements FileFilter {
        public boolean accept(File f) {
            File blogProperties = new File(f, "blog.properties");
            return blogProperties.exists();
        }
    }
}
