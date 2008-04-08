package net.unicon.toro.migrator;

import java.io.File;
import java.io.FilenameFilter;

public class Bootstrap {



    public Bootstrap() {

    }

    public void execute(String dir) throws Exception {
        File installDir = Utils.getInstallDir(dir != null ? new File(dir) : null);
        File jarFile = Utils.getEnclosingJar(this);
        File extractDir = Utils.makeTempDir();
        System.out.println("Extracting self to " + extractDir.getAbsolutePath() + " ...");
        Utils.extract(jarFile, extractDir);

        File jvmExe = Utils.getJavaExec(installDir);
        StringBuffer cmd = new StringBuffer(jvmExe.getAbsolutePath());
        cmd.append(" -classpath ");
        File[] jarFiles = extractDir.listFiles(new JarFilenameFilter());
        for (int i=0; i<jarFiles.length; i++) {
            if (i>0) {
                cmd.append(System.getProperty("path.separator"));
            }
            cmd.append(jarFiles[i].getAbsolutePath());
        }
        cmd.append(System.getProperty("path.separator")).append(extractDir.getAbsolutePath()).
            append(" net.unicon.toro.migrator.Migrator ").
            append(extractDir.getAbsolutePath()).append(' ').
            append(installDir.getAbsolutePath());

        Utils.invokeCommand(cmd.toString(), null, new File("migrator.log"));

    }

    /**
     * @param args
     */
    public static void main(String[] args) {
        try {
            String dir = null;
            if (args.length > 0) {
                dir = args[0];
            }
            new Bootstrap().execute(dir);
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    private static class JarFilenameFilter implements FilenameFilter {
        public boolean accept(File dir, String name) {
            return name != null && name.endsWith(".jar");
        }
    }
}
