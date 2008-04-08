package net.unicon.toro.migrator;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class MigratePortletGuids {

    /**
     * @param args
     */
    public static void main(String[] args) {
        if (args.length < 6) {
            System.out.println("Usage: MigratePortletGuids jdbcDriver jdbcUrl jdbcUsername jdbcPassword oldGuid1 newGuid1 [.. oldGuidN newGuidN]");
            System.exit(-1);
        }
        System.setErr(System.out);
        String jdbcDriver = args[0];
        String jdbcUrl = args[1];
        String jdbcUsername = args[2];
        String jdbcPassword = args[3];

        System.out.println("JDBC settings: " + jdbcDriver + ", " + jdbcUrl + ", " + jdbcUsername + ", " + jdbcPassword);

        try {
            Class.forName(jdbcDriver);
            Connection conn = null;
            PreparedStatement ps = null;
            ResultSet rs = null;
            try {
                conn = DriverManager.getConnection(jdbcUrl,jdbcUsername,jdbcPassword);
                ps = conn.prepareStatement("update up_channel_param set chan_parm_val=? where chan_parm_nm='portletDefinitionId' and chan_parm_val = ?");
                try {
                    for (int i=4; i<args.length; i+=2) {
                        ps.setString(1, args[i+1]);
                        ps.setString(2, args[i]);
                        ps.execute();
                    }
                    System.exit(0);
                } catch (SQLException e) {
                    StringWriter sw = new StringWriter();
                    e.printStackTrace(new PrintWriter(sw));
                    System.out.println(sw.toString());
                    System.exit(1);
                }
            } finally {
                if (rs != null) {rs.close(); rs = null;}
                if (ps != null) {ps.close(); ps = null;}
                if (conn != null) {conn.close(); conn = null;}
            }
        } catch (Exception e) {
            StringWriter sw = new StringWriter();
            e.printStackTrace(new PrintWriter(sw));
            System.out.println(sw.toString());
            System.exit(-1);
        }


    }

}
