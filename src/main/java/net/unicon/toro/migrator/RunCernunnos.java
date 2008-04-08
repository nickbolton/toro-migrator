package net.unicon.toro.migrator;

import java.io.PrintWriter;
import java.io.StringWriter;

public class RunCernunnos {

    public static void main(String[] args) {
        try {
            System.setErr(System.out);
            org.danann.cernunnos.runtime.Main.main(args);
        } catch (Throwable e) {
            StringWriter sw = new StringWriter();
            e.printStackTrace(new PrintWriter(sw));
            System.out.println(sw.toString());
            System.out.flush();
            System.exit(-1);
        }
        System.exit(0);
    }

}
