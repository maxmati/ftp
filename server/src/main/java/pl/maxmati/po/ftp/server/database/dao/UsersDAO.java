package pl.maxmati.po.ftp.server.database.dao;

import pl.maxmati.po.ftp.server.User;
import pl.maxmati.po.ftp.server.database.ConnectionPool;
import pl.maxmati.po.ftp.server.exceptions.DatabaseException;

import java.sql.*;

/**
 * Created by maxmati on 1/6/16
 */
public class UsersDAO {
    private static final String SELECT_FROM_USERS_WHERE_ID = "SELECT * FROM `users` WHERE id = ?";
    private static final String UPDATE_USER_QUERY =
            "UPDATE users " +
            "SET " +
                "username   = ?," +
                "password   = ?," +
                "salt       = ?" +
            "WHERE id = ?";
    private static final String CREATE_USER_QUERY = "INSERT INTO users (username, password, salt) VALUES (?, ?, ?)";

    private final ConnectionPool connectionPool;

    public UsersDAO(ConnectionPool connectionPool) {
        this.connectionPool = connectionPool;
    }

    public User findUserById(int id) {
        Connection con = null;
        try {
            con = connectionPool.reserveConnection();
            PreparedStatement ps = con.prepareStatement(SELECT_FROM_USERS_WHERE_ID);
            ps.setInt(1, id);

            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                String username = rs.getString("username");
                String password = rs.getString("password");
                String salt = rs.getString("salt");

                return new User(id, username, password, salt);
            } else
                return null;

        } catch (SQLException e) {
            throw new DatabaseException(e);
        } finally {
            connectionPool.releaseConnection(con);
        }
    }

    /**
     * Update user when user id != null or create new one otherwise.
     *
     * @param user User to save
     */
    public void save(User user){
        if(user.getId() == null){
            user.setId(create(user));
        } else {
            update(user);
        }
    }

    private void update(User user) {
        Connection con = null;
        try {
            con = connectionPool.reserveConnection();
            PreparedStatement ps = con.prepareStatement(UPDATE_USER_QUERY);
            ps.setString(1, user.getUsername());
            ps.setString(2, user.getPassword());
            ps.setString(3, user.getSalt());
            ps.setInt(4, user.getId());

            ps.executeUpdate();

        } catch (SQLException e) {
            throw new DatabaseException(e);
        } finally {
            connectionPool.releaseConnection(con);
        }
    }

    private int create(User user) {
        Connection con = null;
        try {
            con = connectionPool.reserveConnection();
            PreparedStatement ps = con.prepareStatement(CREATE_USER_QUERY, Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, user.getUsername());
            ps.setString(2, user.getPassword());
            ps.setString(3, user.getSalt());
            ps.executeUpdate();

            ResultSet rs = ps.getGeneratedKeys();
            if (rs.next()) {
                return rs.getInt(1);
            } else {
                throw new DatabaseException("Failed to create user: " + user);
            }

        } catch (SQLException e) {
            throw new DatabaseException(e);
        } finally {
            connectionPool.releaseConnection(con);
        }
    }
}
