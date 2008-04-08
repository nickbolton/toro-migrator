package net.unicon.toro.migrator;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;
import java.util.Map.Entry;

import org.dom4j.Document;
import org.dom4j.Node;
import org.dom4j.io.SAXReader;

public class BuildToroInstallerConfiguration {

    private File academusDir;
    private File saveDir;
    private boolean sphinx;

    public BuildToroInstallerConfiguration(File academusDir, File saveDir, boolean sphinx) {
        this.academusDir = academusDir;
        this.saveDir = saveDir;
        this.sphinx = sphinx;
    }

    private String getProperty(String relativeFile, String propertyName) throws Exception {
        File f = new File(academusDir, relativeFile);
        if (!f.exists()) {
            throw new RuntimeException("Property file does not exist: " + f.getAbsolutePath());
        }
        Properties p = new Properties();
        FileInputStream is = null;

        try {
            is = new FileInputStream(f);
            p.load(is);
            return p.getProperty(propertyName);
        } finally {
            if (is != null) {is.close(); is = null;}
        }
    }

    private String getXmlText(String relativeFile, String xpath) throws Exception {
        File f = new File(academusDir, relativeFile);
        if (!f.exists()) {
            throw new RuntimeException("Xml file does not exist: " + f.getAbsolutePath());
        }

        String value = null;
        SAXReader reader = new SAXReader();
        Document document = reader.read(f);
        Node node = document.selectSingleNode(xpath);

        if (node != null) {
            value = node.getText();
        }

        if (value == null) {
            value = "";
        }
        return value;
    }

    private String getHostname(String url) {
        String hostname = url.split("/")[2];
        int pos = hostname.indexOf(':');
        if (pos >= 0) {
            return hostname.substring(0, pos);
        }
        return hostname;
    }

    private String getPort(String url) {
        String hostname = url.split(":")[1].substring(2);
        int pos = hostname.indexOf(':');
        if (pos < 0) {
            return "";
        }

        String port = hostname.substring(pos);
        pos = port.indexOf('/');
        return port.substring(0, pos);
    }

    private String getJdbcHostname(String url) {
        String oraclePrefix = "jdbc:oracle:thin:@";
        if (url.indexOf("://") >= 0) {
            return getHostname(url);
        } else if (url.indexOf(oraclePrefix) >= 0) {
            return url.substring(oraclePrefix.length()).split(":")[0];
        }
        throw new RuntimeException("Failed to parse jdbc hostname from url: " + url);
    }

    private String getJdbcID(String url) {
        String oraclePrefix = "jdbc:oracle:thin:@";
        if (url.startsWith("jdbc:postgresql")) {
            return url.split("/")[3];
        } else if (url.startsWith("jtds:sqlserver")) {
            return url.split("/")[3];
        } else if (url.startsWith("jdbc:JSQLConnect")) {
            return url.split("/")[3];
        } else if (url.indexOf(oraclePrefix) >= 0) {
            return url.split(":")[5];
        }
        throw new RuntimeException("Failed to parse jdbc ID from url: " + url);
    }

    private String getJdbcPort(String url) {
        String oraclePrefix = "jdbc:oracle:thin:@";
        String port = null;
        if (url.indexOf("://") >= 0) {
            String hostPort = url.split("/")[2];
            int pos = hostPort.indexOf(':');
            if (pos >= 0) {
                port = hostPort.substring(pos+1);
            }
        } else if (url.indexOf(oraclePrefix)  >= 0) {
            port = url.split(":")[4];
        }

        if (port == null) {
            if (url.startsWith("jdbc:postgresql")) {
                port = "5432";
            } else if (url.startsWith("jtds:sqlserver")) {
                port = "1433";
            }
        }

        if (port == null) {
            throw new RuntimeException("Failed to parse jdbc port from url: " + url);
        }

        return port;
    }

    private String fixBackslashes(String s) {
        return s.replaceAll("\\\\", "/");
    }

    public void execute() throws Exception {
        InputStream is = null;
        Properties p = new Properties();
        try {
            is = BuildToroInstallerConfiguration.class.getResourceAsStream("build.properties.sample");
            p.load(is);
        } finally {
            if (is != null) {is.close(); is = null;}
        }

        p.setProperty("basedir", fixBackslashes(System.getProperty("user.dir")));
        p.setProperty("tomcat.home", fixBackslashes(academusDir.getAbsolutePath()+"/portal-tomcat-a"));
        p.setProperty("toro.data.home", fixBackslashes(academusDir.getAbsolutePath()+"/portal-data"));
        p.setProperty("tomcat.host.conf", fixBackslashes(p.getProperty("tomcat.home")+"/conf/Standalone/localhost"));
        p.setProperty("portal.webapp.home", fixBackslashes(p.getProperty("tomcat.home")+"/webapps/portal"));

        p.setProperty("cms.active", fixBackslashes(getProperty(
            "portal-tomcat-a/webapps/portal/WEB-INF/classes/properties/academus-apps-lms.properties",
            "net.unicon.academus.lmsEnabled")));

        p.setProperty("multibox.configuration", fixBackslashes(getProperty(
            "portal-tomcat-a/webapps/portal/WEB-INF/classes/properties/academus-apps-lms.properties",
            "net.unicon.portal.Academus.multipleBoxConfig")));

        String url = getProperty("portal-tomcat-a/webapps/portal/WEB-INF/classes/properties/academus-portal.properties",
            "net.unicon.portal.common.service.notification.NotificationServiceMercuryImpl.service_location");
        p.setProperty("portal.server.http.protocol", fixBackslashes(url.split(":")[0]));

        p.setProperty("portal.server.hostname", fixBackslashes(getHostname(url)));
        p.setProperty("portal.server.port", fixBackslashes(getPort(url)));
        p.setProperty("server.base.url", fixBackslashes(p.getProperty("portal.server.http.protocol")+"://"+p.getProperty("portal.server.hostname")+p.getProperty("portal.server.port")));
        p.setProperty("portal.base.url", fixBackslashes(p.getProperty("server.base.url")+"/portal"));

        p.setProperty("jms.server.hostname", fixBackslashes(getProperty(
            "portal-tomcat-a/webapps/portal/WEB-INF/classes/properties/jms-content.properties",
            "net.unicon.academus.delivery.academus.jms.ProviderURL").split(":")[0]));

        p.setProperty("ldap.server.hostname", fixBackslashes(getProperty(
            "portal-tomcat-a/webapps/portal/WEB-INF/classes/properties/ldap.properties",
            "ldap.host")));
        p.setProperty("ldap.server.port", fixBackslashes(getProperty(
            "portal-tomcat-a/webapps/portal/WEB-INF/classes/properties/ldap.properties",
            "ldap.port")));
        p.setProperty("ldap.server.managerdn", fixBackslashes(getProperty(
            "portal-tomcat-a/webapps/portal/WEB-INF/classes/properties/ldap.properties",
            "ldap.managerDN")));
        p.setProperty("ldap.server.manager.password", fixBackslashes(getProperty(
            "portal-tomcat-a/webapps/portal/WEB-INF/classes/properties/ldap.properties",
            "ldap.managerPW")));
        p.setProperty("ldap.server.basedn", fixBackslashes(getProperty(
            "portal-tomcat-a/webapps/portal/WEB-INF/classes/properties/ldap.properties",
            "ldap.baseDN")));
        p.setProperty("ldap.server.uid", fixBackslashes(getProperty(
            "portal-tomcat-a/webapps/portal/WEB-INF/classes/properties/ldap.properties",
            "ldap.uidAttribute")));
        String ssl = getProperty(
            "portal-tomcat-a/webapps/portal/WEB-INF/classes/properties/ldap.properties",
        "ldap.protocol");

        p.setProperty("ldap.protocol.checkbox", (ssl != null ? "true" : "false"));
        p.setProperty("ldap.protocol", (ssl != null ? ssl : ""));

        p.setProperty("smtp.server.hostname", fixBackslashes(getXmlText(
            "portal-tomcat-a/webapps/AcademusApps/WEB-INF/classes/config/messaging-portlet.xml",
            "/messaging/message-factory[@id='system']/account/host")));
        p.setProperty("smtp.server.port", fixBackslashes(getXmlText(
            "portal-tomcat-a/webapps/AcademusApps/WEB-INF/classes/config/messaging-portlet.xml",
            "/messaging/message-factory[@id='system']/account/port")));
        p.setProperty("smtp.server.username", fixBackslashes(getXmlText(
            "portal-tomcat-a/webapps/AcademusApps/WEB-INF/classes/config/messaging-portlet.xml",
            "/messaging/message-factory[@id='system']/account/username")));
        p.setProperty("smtp.server.password", fixBackslashes(getXmlText(
            "portal-tomcat-a/webapps/AcademusApps/WEB-INF/classes/config/messaging-portlet.xml",
            "/messaging/message-factory[@id='system']/account/password")));

        p.setProperty("reddot.url", fixBackslashes(getXmlText(
            "portal-tomcat-a/webapps/AcademusApps/WEB-INF/classes/config/content-portlet.xml",
            "/web-content/wcms-sso/access-broker[@handle='content']/entry/target/sso-entry[@handle='wcms']/target[@handle='login']/url")));
        p.setProperty("reddot.method", fixBackslashes(getXmlText(
            "portal-tomcat-a/webapps/AcademusApps/WEB-INF/classes/config/content-portlet.xml",
            "/web-content/wcms-sso/access-broker[@handle='content']/entry/target/sso-entry[@handle='wcms']/target[@handle='login']/method")));
        String contentPath = getXmlText(
            "portal-tomcat-a/webapps/AcademusApps/WEB-INF/classes/config/content-portlet.xml",
            "/web-content/access-broker[@handle='content']/entry/@target");
        if (contentPath.endsWith("/[Content]/")) {
            p.setProperty("reddot.content.path", "");
        } else {
            p.setProperty("reddot.content.path", fixBackslashes(contentPath.split("/[Content]/")[1]));
        }

        p.setProperty("aspell.executable.path", fixBackslashes(getXmlText(
            "portal-tomcat-a/webapps/AcademusApps/WEB-INF/web.xml",
            "/web-app/servlet/servlet-name[text()='SpellCheckerServlet']/../init-param/param-name[text()='aspell']/../param-value")));

        int numdbConns;
        String driver;
        String jdbcUrl;
        String simpleSql;
        String user;
        String password;

        if (sphinx) {
            driver = getProperty(
                "portal-tomcat-a/webapps/portal/WEB-INF/classes/properties/unicon-resource-pool.properties",
                "PortalDbDBDriver");
            jdbcUrl = getProperty(
                "portal-tomcat-a/webapps/portal/WEB-INF/classes/properties/unicon-resource-pool.properties",
                "PortalDbDBURL");
            simpleSql = getProperty(
                "portal-tomcat-a/webapps/portal/WEB-INF/classes/properties/unicon-resource-pool.properties",
                "PortalDbDBSimpleSQL");
            user = getProperty(
                "portal-tomcat-a/webapps/portal/WEB-INF/classes/properties/unicon-resource-pool.properties",
                "PortalDbDBUser");
            password = getProperty(
                "portal-tomcat-a/webapps/portal/WEB-INF/classes/properties/unicon-resource-pool.properties",
                "PortalDbDBPassword");
            numdbConns = new Integer(getProperty(
                "portal-tomcat-a/webapps/portal/WEB-INF/classes/properties/unicon-resource-pool.properties",
                "PortalDbDBNumberOfResources")).intValue();
        } else {
            driver = getXmlText(
                "portal-tomcat-a/conf/Standalone/localhost/portal.xml",
                "/Context/Resource/@driver");
            jdbcUrl = getXmlText(
                "portal-tomcat-a/conf/Standalone/localhost/portal.xml",
                "/Context/Resource/@url");
            simpleSql = getXmlText(
                "portal-tomcat-a/conf/Standalone/localhost/portal.xml",
                "/Context/Resource/@simpleSQL");
            user = getXmlText(
                "portal-tomcat-a/conf/Standalone/localhost/portal.xml",
                "/Context/Resource/@user");
            password = getXmlText(
                "portal-tomcat-a/conf/Standalone/localhost/portal.xml",
                "/Context/Resource/@password");
            numdbConns = new Integer(getXmlText(
                "portal-tomcat-a/conf/Standalone/localhost/portal.xml",
                "/Context/Resource/@size")).intValue();
        }

        p.setProperty("jdbc.driver", fixBackslashes(driver));
        String sqlConcat = "||";
        String sqlLength = "length";
        if (jdbcUrl.toLowerCase().indexOf("jtds")>=0 || jdbcUrl.toLowerCase().indexOf("jsqlconnect")>=0) {
            sqlConcat = "+";
            sqlLength = "datalength";
        }
        p.setProperty("db.sqlconcat", sqlConcat);
        p.setProperty("db.length", sqlLength);
        p.setProperty("jdbc.simplesql", simpleSql);
        p.setProperty("jdbc.url", jdbcUrl);
        p.setProperty("jdbc.hostname", getJdbcHostname(jdbcUrl));
        p.setProperty("jdbc.port", getJdbcPort(jdbcUrl));
        p.setProperty("jdbc.database.name", getJdbcID(jdbcUrl));
        p.setProperty("jdbc.user", user);
        p.setProperty("jdbc.password", password);

        p.setProperty("jdbc.poolsize.uportal", ""+((int)(numdbConns*.4)));
        p.setProperty("jdbc.poolsize.briefcase", ""+((int)(numdbConns*.1)));
        p.setProperty("jdbc.poolsize.gateway", ""+((int)(numdbConns*.1)));
        p.setProperty("jdbc.poolsize.messaging", ""+((int)(numdbConns*.1)));
        p.setProperty("jdbc.poolsize.permissions", ""+((int)(numdbConns*.1)));
        p.setProperty("jdbc.poolsize.web.content", ""+((int)(numdbConns*.1)));
        p.setProperty("jdbc.poolsize.blojsom", ""+((int)(numdbConns*.1)));

        Map map = new TreeMap();
        Iterator itr = p.entrySet().iterator();
        while (itr.hasNext()) {
            Entry entry = (Entry)itr.next();
            map.put((String)entry.getKey(), (String)entry.getValue());
        }
        PrintWriter out = null;

        try {
            out = new PrintWriter(new FileWriter(new File(saveDir, "ant.install.properties")));
            itr = map.entrySet().iterator();
            while (itr.hasNext()) {
                Entry entry = (Entry)itr.next();
                out.println(entry.getKey()+"="+entry.getValue());
            }
            out.close();
        } finally {
            if (out != null) {out.close(); out = null;}
        }
    }

    public static void main(String[] args) {
        if (args.length < 2) {
            System.out.println("Usage: BuildToroInstallerConfiguration <Academus dir> <save dir>");
            System.exit(0);
        }
        System.setErr(System.out);

        String academusDir = args[0];
        File saveDir = new File(args[1]);

        if (!saveDir.exists()) {
            System.out.println("Save directory does not exist: " + saveDir.getAbsolutePath());
            System.exit(0);
        }

        if (".".equals(academusDir)) {
            academusDir = System.getProperty("user.dir");
        }
        File dir = new File(academusDir);
        if (!dir.exists()) {
            dir = new File(System.getProperty("user.dir")+"/"+args[0]);

            if (!dir.exists()) {
                System.out.println("Directory does not exist: " + dir.getAbsolutePath());
                System.exit(0);
            }
        }

        if (!dir.isDirectory()) {
            System.out.println("Argument not a directory: " + dir.getAbsolutePath());
            System.exit(0);
        }

        if (!"Academus".equals(dir.getName())) {
            System.out.println("Argument not the Academus directory: " + dir.getAbsolutePath());
            System.exit(0);
        }


        try {
            new BuildToroInstallerConfiguration(dir, saveDir, true).execute();
        } catch (Exception e) {
            StringWriter sw = new StringWriter();
            e.printStackTrace(new PrintWriter(sw));
            System.out.println(sw.toString());
        }
    }

}
