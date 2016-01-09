package pl.maxmati.po.ftp.server.database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by maxmati on 1/6/16
 */
public class ConnectionPool {
    @SuppressWarnings("SpellCheckingInspection")
    private static final String DATABASE_URL = "jdbc:mysql://mysql.maxmati.pl/poftp";
    @SuppressWarnings("SpellCheckingInspection")
    private static final String DATABASE_USERNAME = "poftp";
    @SuppressWarnings("SpellCheckingInspection")
    private static final String DATABASE_PASSWORD = "aarmzEjvaRYFstCE";

    private static final String CREATE_USERS_TABLE_QUERY =
            "CREATE TABLE IF NOT EXISTS `users` (" +
            "  `id` INT(1) NOT NULL AUTO_INCREMENT," +
            "  `username` VARCHAR(10) NOT NULL," +
            "  `password` VARCHAR(45) NOT NULL," +
            "  `salt` VARCHAR(20) NOT NULL," +
            "  PRIMARY KEY  (`id`)," +
            "  UNIQUE KEY `username` (`username`)" +
            ") ENGINE=InnoDB  DEFAULT CHARSET=utf8";
    private static final String CREATE_GROUPS_TABLE_QUERY =
            "CREATE TABLE IF NOT EXISTS `groups` (" +
            "  `id` INT(1) NOT NULL AUTO_INCREMENT," +
            "  `group_name` VARCHAR(10) NOT NULL," +
            "  PRIMARY KEY  (`id`)," +
            "  UNIQUE KEY `group_name` (`group_name`)" +
            ") ENGINE=InnoDB  DEFAULT CHARSET=utf8";
    private static final String CREATE_FILES_TABLE_QUERY =
            "CREATE TABLE IF NOT EXISTS `files` (" +
            "  `id` INT(11) NOT NULL AUTO_INCREMENT," +
            "  `filename` VARCHAR(50) NOT NULL," +
            "  `owner_id` INT(11) NOT NULL," +
            "  `group_id` INT(11) NOT NULL," +
            "  `user_read` TINYINT(1) NOT NULL DEFAULT '1'," +
            "  `user_write` TINYINT(1) NOT NULL DEFAULT '1'," +
            "  `group_read` TINYINT(1) NOT NULL DEFAULT '0'," +
            "  `group_write` TINYINT(1) NOT NULL DEFAULT '0'," +
            "  PRIMARY KEY  (`id`)," +
            "  UNIQUE KEY `filename` (`filename`)" +
            ") ENGINE=InnoDB  DEFAULT CHARSET=utf8";
    private static final String CREATE_USER_GROUP_TABLE_QUERY =
            "CREATE TABLE IF NOT EXISTS `user_group` (" +
            "  `user_id` INT(11) NOT NULL," +
            "  `group_id` INT(11) NOT NULL" +
            ") ENGINE=InnoDB DEFAULT CHARSET=utf8";

    private static ConnectionPool instance = null;
    private Connection connection = null;
    private List<Connection> freeConnection = new LinkedList<>();
    private List<Connection> reservedConnections = new LinkedList<>();

    public static ConnectionPool getInstance(){
        if(instance == null)
            instance = new ConnectionPool();
        return instance;
    }

    public Connection reserveConnection() throws SQLException {
        if(freeConnection.isEmpty()){
            freeConnection.add(createConnection());
        }
        Connection con = freeConnection.get(0);
        freeConnection.remove(0);
        reservedConnections.add(con);
        return con;
    }

    public void releaseConnection(Connection con){
        if(reservedConnections.contains(con)) {
            reservedConnections.remove(con);
            freeConnection.add(con);
        }
    }

//    public CommandConnection getConnection() throws SQLException {
//        if(connection == null || connection.isClosed()) {
//            connection = createConnection();
//            prepareDatabase();
//        }
//        return connection;
//    }

    private void prepareDatabase() throws SQLException {
        connection.createStatement().executeUpdate(CREATE_USERS_TABLE_QUERY);
        connection.createStatement().executeUpdate(CREATE_GROUPS_TABLE_QUERY);
        connection.createStatement().executeUpdate(CREATE_USER_GROUP_TABLE_QUERY);
        connection.createStatement().executeUpdate(CREATE_FILES_TABLE_QUERY);
    }

    private Connection createConnection() throws SQLException {
        return DriverManager.getConnection(DATABASE_URL, DATABASE_USERNAME, DATABASE_PASSWORD);
    }

}
