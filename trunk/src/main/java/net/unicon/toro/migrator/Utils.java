package net.unicon.toro.migrator;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.URL;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class Utils {

    public static String readFile(File f) throws Exception {

        BufferedReader reader = null;
        StringBuffer sb = new StringBuffer();
        try {
            reader = new BufferedReader(new FileReader(f));
            String line;
            while ( (line = reader.readLine()) != null) {
                sb.append(line).append('\n');
            }
            return sb.toString();
        } finally {
            if (reader != null) {reader.close(); reader = null;}
        }
    }

    public static void writeFile(File f, String contents) throws Exception {
        PrintWriter out = null;

        try {
            out = new PrintWriter(new FileWriter(f));
            out.print(contents);
        } finally {
            if (out != null) { out.close(); out = null; }
        }
    }

    public static String replaceAll(String s, String exp, String replacement) {
        StringBuffer sb = new StringBuffer();
        String[] split = s.split("\n");

        Pattern p = Pattern.compile(exp);

        for (int i=0; i<split.length; i++) {
            Matcher m = p.matcher(split[i]);
            if (m.find()) {
                sb.append(replacement);
            } else {
                sb.append(split[i]);
            }
            sb.append('\n');

        }

        return sb.toString();
    }

    public static boolean checkInstallDir(File dir) {
        File tomcatDir = new File(dir, "unicon/Academus/portal-tomcat-a");
        boolean b = tomcatDir.exists();
        if (!b) {
            System.out.println("'"+dir.getAbsolutePath()+"' is not an Academus directory.");
            System.out.println("There should be a path to unicon/Academus/portal-tomcat-a under the install directory.");
        }
        return b;
    }

    public static File getInstallDir(File installDir) throws Exception {
        if (installDir != null) {
            if (checkInstallDir(installDir)) return installDir;
            installDir = null;
        }
        while (installDir == null) {
            System.out.print("Enter Academus install directory: ");

            File dir = new File(getStdinLine());
            if (checkInstallDir(dir)) {
                installDir = dir;
            }
        }
        return installDir;
    }

    public static String getStdinLine() throws Exception {
        StringBuffer sb = new StringBuffer();
        int ch;
        while ((ch = System.in.read()) != '\n') {
            sb.append((char)ch);
        }
        return sb.toString();
    }

    public static File makeTempDir(){
        String tempDir = System.getProperty("java.io.tmpdir");
        File extractDir = new File(tempDir, "migrator");
        extractDir.mkdirs();
        extractDir.deleteOnExit();
        return extractDir;
    }

    public static File getEnclosingJar(Object reference) {
        String thisClass = "/" + reference.getClass().getName().replace('.','/') + ".class";
        URL jarUrl = reference.getClass().getResource(thisClass);
        String stringForm = jarUrl.toString();
        //String fileForm = jarUrl.getFile();

        File file = null;
        int endIdx = stringForm.indexOf("!/");
        if(endIdx != -1){
            String unescaped = null;
            String fileNamePart = stringForm.substring("jar:file:".length(), endIdx);
            file = new File(fileNamePart);
            if ( ! file.exists()) {
                // try to unescape encase the URL Handler has escaped the " " to %20
                unescaped = unescape(fileNamePart);
                file = new File(unescaped);
            }
            return file;
        }
        throw new RuntimeException("Failed expanding Jar.");
    }

    public static void extract(File zipFile, File dir) throws Exception {
        ZipInputStream zis = null;

        try {
            zis = new ZipInputStream(new FileInputStream(zipFile));
            ZipEntry entry = null;
            ArrayList result = new ArrayList();
            while ( (entry = zis.getNextEntry()) != null) {
                result.add(extract(zis, entry, dir));
            }
        } finally {

        }
    }

    public static void createPath(String entryName, File toDir) throws Exception {
        int slashIdx = entryName.lastIndexOf('/');
        if (slashIdx >= 0) {
            // there is path info
            String firstPath = entryName.substring(0, slashIdx);
            File dir = new File(toDir, firstPath);
            if (!dir.exists()) {
                dir.mkdirs();
            }
        }
    }

    public static File extract(ZipInputStream zis, ZipEntry entry, File toDir) throws Exception {
        createPath(entry.getName(), toDir);
        File fileToUse = new File(toDir, entry.getName());
        if (!fileToUse.exists() && fileToUse.isDirectory()) {
            fileToUse.createNewFile();
        }
        if (fileToUse.isDirectory()) {
            return fileToUse;
        }

        BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(fileToUse), 1024);
        byte[] bytes = new byte[1024];
        int len = 0;
        while ( (len = zis.read(bytes)) >= 0) {
            bos.write(bytes, 0, len);
        }
        bos.close();
        zis.closeEntry();
        return fileToUse;
    }

    public static String unescape(final String s) {
        StringBuffer sb = new StringBuffer(s.length());

        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '%': {
                    try {
                        sb.append( (char) Integer.parseInt(s.substring(i + 1, i + 3), 16));
                        i += 2;
                        break;
                    }
                    catch (NumberFormatException nfe) {
                        throw new IllegalArgumentException();
                    }
                    catch (StringIndexOutOfBoundsException siob) {
                        String end = s.substring(i);
                        sb.append(end);
                        if (end.length() == 2) i++;
                    }
                    break;
                }
                default: {
                    sb.append(c);
                    break;
                }
            }
        }
        return sb.toString();
    }

    private static void echoMessage(PrintWriter log, String message, boolean stdout) throws Exception {
        if (stdout) {
            System.out.println(message);
        }
        if (log != null) {
            log.println(message);
        }
    }

    public static void invokeCommand(String command, String[] env) throws Exception {
        invokeCommand(command, env, null);
    }

    public static void invokeCommand(String command, String[] env, File logfile) throws Exception {
        BufferedReader reader = null;
        PrintWriter log = null;
        try {
            if (logfile != null) {
                log = new PrintWriter(new FileWriter(logfile));
            }
            echoMessage(log, command, true);
            Process p = Runtime.getRuntime().exec(command, env);
            reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                echoMessage(log, line, true);
            }

            int rc = p.waitFor();
            if (rc != 0) {
                throw new RuntimeException(command + " failed: " + rc);
            }
        } finally {
            if (reader != null) {reader.close(); reader = null;}
            if (log != null) {log.close(); log = null;}
        }
    }

    public static int invokeCommandWithRC(String command, String[] env) throws Exception {
        BufferedReader reader = null;
        System.out.println(command);
        try {
            Process p = Runtime.getRuntime().exec(command, env);

            reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println(line);
            }
            return p.waitFor();
        } finally {
            if (reader != null) {reader.close(); reader = null;}
        }
    }

    public static File getJavaExec(File installDir) throws Exception {
        File javaHomeDir = new File(installDir, "unicon/tools/j2sdk");
        if (!javaHomeDir.exists()) {
            throw new RuntimeException("Failed to find new jdk: " + javaHomeDir.getAbsolutePath());
        }
        File javaBinDir = new File(javaHomeDir, "bin");
        File jvmExe = new File(javaBinDir, "java");
        if (!jvmExe.exists()) {
            jvmExe = new File(javaBinDir, "java.exe");
            if (!jvmExe.exists()) {
                System.err.println("JVM executable does not exist: " + jvmExe.getAbsolutePath());
                System.exit(-1);
            }
        }
        return jvmExe;
    }

    public static void copyFile(String f1, String f2) throws Exception {
        copyFile(f1, f2, true);
    }

    public static void copyFile(String f1, String f2, boolean verbose) throws Exception {
        copyFile(new File(f1), new File(f2), verbose);
    }

    public static void copyFile(File f1, File f2) throws Exception {
        copyFile(f1, f2, true);
    }

    public static void copyFile(File f1, File f2, boolean verbose) throws Exception {
        if (!f1.exists()) {
            if (verbose) {
                System.out.println("Skipping " + f1.getAbsolutePath());
            }
            return;
        }
        if (!f2.getParentFile().exists()) {
            f2.getParentFile().mkdirs();
        }
        if (verbose) {
            System.out.println("Copying " + f1.getAbsolutePath() + " to " + f2.getAbsolutePath());
        }
        FileInputStream fis  = new FileInputStream(f1);
        FileOutputStream fos = new FileOutputStream(f2);
        try {
            byte[] buf = new byte[1024];
            int i = 0;
            while ((i = fis.read(buf)) != -1) {
                fos.write(buf, 0, i);
            }
        }
        catch (Exception e) {
            throw e;
        }
        finally {
            if (fis != null) fis.close();
            if (fos != null) fos.close();
        }
    }

    public static void copyDir(File src, File dest) throws Exception {
        copyDir(src, dest, true);
    }

    public static void copyDir(File src, File dest, boolean verbose) throws Exception {

        if (!dest.getParentFile().exists()) {
            dest.getParentFile().mkdirs();
        }
        if (src.isDirectory()) {
            if (verbose) {
                System.out.println("Copying directory " + src.getAbsolutePath() + " to " + dest.getAbsolutePath());
            }

            dest.mkdirs();
            String list[] = src.list();

            for (int i = 0; i < list.length; i++) {
                File dest1 = new File(dest, list[i]);
                File src1 = new File(src, list[i]);
                copyDir(src1, dest1, verbose);
            }
        } else {
            copyFile(src, dest, verbose);
        }
    }
}
