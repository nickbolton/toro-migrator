package net.unicon.toro.installer.tools;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.FileChannel;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Utils {
    
    private static Utils singleton = new Utils();
    
    private Utils() {}
    
    public static Utils instance() { return singleton; }
    
    public void backupFile(File file, boolean markIfOriginal) {
        File nextBackup = getNextBackupFile(file, markIfOriginal);
        copy(file, nextBackup);
    }
    
    private void copy(File src, File dst) {
        try {
            FileChannel srcChannel = new FileInputStream(src).getChannel();
            
            // Create channel on the destination
            FileChannel dstChannel = new FileOutputStream(dst).getChannel();
        
            // Copy file contents from source to destination
            dstChannel.transferFrom(srcChannel, 0, srcChannel.size());
        
            // Close the channels
            srcChannel.close();
            dstChannel.close();
            
        } catch (IOException e) {
           throw new RuntimeException("Failed copying file: " + src.getAbsolutePath() + " -> " + dst.getAbsolutePath());
        }
    }

    
    private File getNextBackupFile(final File f, boolean markIfOriginal) {
        final Pattern filenamePattern = Pattern.compile(f.getName()+"\\.([0-9]+)");
        FileFilter filter = new FileFilter() {
            public boolean accept(File potentialFile) {
                Matcher m = filenamePattern.matcher(potentialFile.getName());
                return m.find();
            }
        };
        
        File parent = f.getParentFile();
        File backupDir = new File(parent, "toro_installer_backups");
        backupDir.mkdir();
        File[] backups = backupDir.listFiles(filter);
        if (backups.length == 0) {
            File origFile = new File(backupDir, f.getName()+".orig");
            if (markIfOriginal && !origFile.exists()) {
                return origFile;
            }
            return new File(backupDir, f.getName()+".0");
        }
        int maxIndex = 0;
        for (int i=0; i<backups.length; i++) {
            Matcher m = filenamePattern.matcher(backups[i].getName());
            if (m.find()) {
                
                int thisIndex = new Integer(m.group(1));
                if (thisIndex > maxIndex) {
                    maxIndex = thisIndex;
                }
            }
        }
        
        return new File(backupDir, f.getName()+'.'+(maxIndex+1));
    }

}
