package wikipedia.utils;

import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.Properties;
import java.util.Vector;

public class ConPool {

    private String URI;
    private String dbpass;
    private String dbuser;
    private String drivername;
    private int maxconn;
    private Vector<Connection> freeConnections = new Vector<Connection> ();
    static private ConPool ref;
    static private int clients;

    public ConPool(String URI, String dbuser, String dbpass, String drivername, int maxconn) {
        this.URI = URI;
        this.dbuser = dbuser;
        this.dbpass = dbpass;
        this.drivername = drivername;
        this.maxconn = maxconn;
        loadJDBCDriver();
    }

    private void loadJDBCDriver() {
        try {
            Driver driver = (Driver) Class.forName(this.drivername).newInstance();
            DriverManager.registerDriver(driver);
        } catch (Exception e) {
            System.out.println("Can't load/register JDBC driver " + this.drivername);
            e.printStackTrace();
        }
    }

    public static synchronized ConPool getInstance(String URI, String dbuser, String dbpass, String drivername, int maxconn) {
        if (ConPool.ref == null) {
            ConPool.ref = new ConPool(URI, dbuser, dbpass, drivername, maxconn);
        }
        ConPool.clients++;
        return ConPool.ref;
    }

    public synchronized Connection getConnection() {
        Connection rescon = null;
        if (!this.freeConnections.isEmpty()) {
            rescon = (Connection) this.freeConnections.get(this.freeConnections.size() - 1);
            this.freeConnections.remove(rescon);
            try {
                if (rescon.isClosed()) {
                    rescon = getConnection();
                }
            } catch (SQLException e) {
                rescon = getConnection();
            } catch (Exception e) {
                rescon = getConnection();
            }
        } else {
            rescon = createConnection();
        }
        return rescon;
    }

    private Connection createConnection() {
        Connection rescon = null;
        try {
            Properties p = new Properties();
            p.put("user", this.dbuser);
            p.put("password", this.dbpass);
            p.put("useUnicode", "true");
            p.put("characterEncoding", "UTF-8");
            rescon = DriverManager.getConnection(this.URI, p);
        } catch (SQLException e) {
            System.out.println("Cannot create a new connection! " + e.getMessage());
            e.printStackTrace();
            rescon = null;
        }
        return rescon;
    }

    public synchronized void returnConnection(Connection con) {
        if ((con != null) && (this.freeConnections.size() <= this.maxconn)) {
            this.freeConnections.add(con);
        }
    }

    public synchronized void release() {
        Iterator<Connection> allc = this.freeConnections.iterator();
        while (allc.hasNext()) {
            Connection con = (Connection) allc.next();
            try {
                con.close();
            } catch (SQLException e) {
                System.out.println("Cannot close connection! (Probably already closed?) " + e.getMessage());
                e.printStackTrace();
            }
        }
        this.freeConnections.clear();
    }
}
