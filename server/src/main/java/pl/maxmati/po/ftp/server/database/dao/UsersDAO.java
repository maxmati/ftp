package pl.maxmati.po.ftp.server.database.dao;

import pl.maxmati.po.ftp.common.beans.User;
import pl.maxmati.po.ftp.server.database.ConnectionPool;
import pl.maxmati.po.ftp.server.exceptions.DatabaseException;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by maxmati on 1/6/16
 */
public class UsersDAO {
    private static final String PASSWORD = "password";
    private static final String SALT = "salt";
    private static final String ID = "id";

    private static final String SELECT_FROM_USERS_WHERE_ID = "SELECT * FROM `users` WHERE id = ?";
    private static final String SELECT_FROM_USERS_WHERE_USERNAME = "SELECT * FROM users WHERE username = ?";
    private static final String UPDATE_USER_QUERY =
            "UPDATE users " +
            "SET " +
                "username   = ?," +
                "password   = ?," +
                "salt       = ?" +
            "WHERE id = ?";
    private static final String CREATE_USER_QUERY = "INSERT INTO users (username, password, salt) VALUES (?, ?, ?)";
    private static final String SELECT_FROM_USERS = "SELECT * FROM `users`";

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

    public User findUserByUsername(String username) {
        Connection con = null;
        try {
            con = connectionPool.reserveConnection();
            PreparedStatement ps = con.prepareStatement(SELECT_FROM_USERS_WHERE_USERNAME);
            ps.setString(1, username);

            ResultSet rs = ps.executeQuery();
            if(rs.next()){
                int id = rs.getInt(ID);
                String password = rs.getString(PASSWORD);
                String salt = rs.getString(SALT);

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

    public int getUserGroupId(User user) {
        Connection con = null;
        try {
            con = connectionPool.reserveConnection();
            PreparedStatement ps = con.prepareStatement("SELECT group_id FROM user_group WHERE user_id = ?");
            ps.setInt(1, user.getId());
            ResultSet rs = ps.executeQuery();

            if(rs.next()){
                return rs.getInt("group_id");
            } else
                throw new DatabaseException("Failed to get group id of user: " + user);

        } catch (SQLException e) {
            throw new DatabaseException(e);
        } finally {
            connectionPool.releaseConnection(con);
        }
    }

    public List<User> findUsers() {
        List<User> results = new ArrayList<>();
        Connection con = null;
        try {
            con = connectionPool.reserveConnection();
            PreparedStatement ps = con.prepareStatement(SELECT_FROM_USERS);

            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                Integer id = rs.getInt("id");
                String username = rs.getString("username");
                String password = rs.getString("password");
                String salt = rs.getString("salt");

                results.add(new User(id, username, password, salt));
            }

        } catch (SQLException e) {
            throw new DatabaseException(e);
        } finally {
            connectionPool.releaseConnection(con);
        }
        return results;
    }

    public void deleteById(int id) {
        Connection con = null;
        try {
            con = connectionPool.reserveConnection();
            PreparedStatement ps = con.prepareStatement("DELETE FROM `users` WHERE id = ?");
            ps.setInt(1, id);

            ps.executeUpdate();

        } catch (SQLException e) {
            throw new DatabaseException(e);
        } finally {
            connectionPool.releaseConnection(con);
        }
    }
}
