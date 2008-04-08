package net.unicon.toro.migrator;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class CheckDbMigration {

    /**
     * @param args
     */
    public static void main(String[] args) {
        
        if (args.length < 4) {
            System.out.println("Usage: CheckDbMigration jdbcDriver jdbcUrl jdbcUsername jdbcPassword");
            System.exit(-1);
        }
        System.setErr(System.out);
        String jdbcDriver = args[0];
        String jdbcUrl = args[1];
        String jdbcUsername = args[2];
        String jdbcPassword = args[3];
        
        try {
            Class.forName(jdbcDriver);
            Connection conn = null;
            PreparedStatement ps = null;
            ResultSet rs = null;
            try {
                conn = DriverManager.getConnection(jdbcUrl,jdbcUsername,jdbcPassword);
                ps = conn.prepareStatement("select * from hg_usage");
                try {
                    rs = ps.executeQuery();
                    System.exit(0);
                } catch (SQLException e) {
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
