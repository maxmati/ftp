package pl.maxmati.po.ftp.server.database.dao;

import pl.maxmati.po.ftp.common.beans.Group;
import pl.maxmati.po.ftp.common.beans.User;
import pl.maxmati.po.ftp.server.database.ConnectionPool;
import pl.maxmati.po.ftp.server.exceptions.DatabaseException;

import java.sql.*;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by maxmati on 1/7/16
 */
public class GroupsDAO {
    private static final String SELECT_FROM_GROUPS_WHERE_ID = "SELECT * FROM groups WHERE id = ?";
    private static final String FETCH_GROUP_MEMBERS_QUERY =
            "SELECT U.* " +
            "FROM user_group UG " +
            "LEFT JOIN users U " +
            "ON U.id = UG.user_id " +
            "WHERE UG.group_id = ? ";
    private static final String UPDATE_GROUP_QUERY =
            "UPDATE groups " +
            "SET group_name = ? " +
            "WHERE id = ?";
    private static final String CREATE_GROUP_QUERY = "INSERT INTO groups (group_name) VALUES (?)";
    private static final String SELECT_FROM_GROUPS = "SELECT * FROM `groups`";

    private final ConnectionPool connectionPool;

    public GroupsDAO(ConnectionPool connectionPool) {
        this.connectionPool = connectionPool;
    }

    public Group findGroupById(int id){
        Connection con = null;
        try {
            con = connectionPool.reserveConnection();
            PreparedStatement ps = con.prepareStatement(SELECT_FROM_GROUPS_WHERE_ID);
            ps.setInt(1, id);

            ResultSet rs = ps.executeQuery();
            if(rs.next()) {
                String name = rs.getString("group_name");

                return new Group(id, name);
            } else
                return null;

        } catch (SQLException e) {
            throw new DatabaseException(e);
        } finally {
            connectionPool.releaseConnection(con);
        }
    }

    public void fetchMembers(Group group) {
        Connection con = null;
        try {
            con = connectionPool.reserveConnection();

            PreparedStatement ps = con.prepareStatement(FETCH_GROUP_MEMBERS_QUERY);
            ps.setInt(1, group.getId());

            List<User> members = new LinkedList<>();
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                int id = rs.getInt("id");
                String username = rs.getString("username");
                String password = rs.getString("password");
                String salt = rs.getString("salt");

                members.add(new User(id, username, password, salt));
            }
            group.setMembers(members);
            group.markMembersSynchronized();

        } catch (SQLException e) {
            throw new DatabaseException(e);
        } finally {
            connectionPool.releaseConnection(con);
        }
    }

    public void save(Group group){
        if(group.getId() == null){
            group.setId(create(group));
        } else {
            update(group);
        }
    }

    public void saveMembers(Group group){
        Connection con = null;
        try {
            con = connectionPool.reserveConnection();

            con.setAutoCommit(false);

            PreparedStatement removeStatement = con.prepareStatement("DELETE FROM user_group WHERE group_id = ?");
            removeStatement.setInt(1, group.getId());
            removeStatement.executeUpdate();

            PreparedStatement addStatement = con.prepareStatement("INSERT INTO user_group (user_id, group_id) VALUES (?, ?)");
            addStatement.setInt(2, group.getId());
            for(User user : group.getMembers()){
                addStatement.setInt(1, user.getId());
                addStatement.executeUpdate();
            }

            con.commit();
            group.markMembersSynchronized();

            con.setAutoCommit(true);
        } catch (SQLException e) {
            try {
                assert con != null;
                con.rollback();
            } catch (SQLException e1) {
                throw new DatabaseException(e1);
            }
            throw new DatabaseException(e);

        } finally {
            try {
                assert con != null;
                con.setAutoCommit(false);
            } catch (SQLException e) {
                e.printStackTrace();
            } finally {
                connectionPool.releaseConnection(con);
            }
        }
    }

    public List<Group> findGroups() {
        List<Group> results = new ArrayList<>();
        Connection con = null;
        try {
            con = connectionPool.reserveConnection();
            PreparedStatement ps = con.prepareStatement(SELECT_FROM_GROUPS);

            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                Integer id = rs.getInt("id");
                String name = rs.getString("group_name");

                results.add(new Group(id, name));
            }

        } catch (SQLException e) {
            throw new DatabaseException(e);
        } finally {
            connectionPool.releaseConnection(con);
        }
        return results;
    }

    public void removeById(int id) {
        Connection con = null;
        try {
            con = connectionPool.reserveConnection();
            PreparedStatement ps = con.prepareStatement("DELETE FROM `groups` WHERE id = ?");
            ps.setInt(1, id);

            ps.executeUpdate();

        } catch (SQLException e) {
            throw new DatabaseException(e);
        } finally {
            connectionPool.releaseConnection(con);
        }
    }

    public void removeUserFromGroup(int userId, int groupId) {
        Connection con = null;
        try {
            con = connectionPool.reserveConnection();
            PreparedStatement ps = con.prepareStatement("DELETE FROM `user_group` WHERE user_id = ? AND group_id = ?");
            ps.setInt(1, userId);
            ps.setInt(2, groupId);

            ps.executeUpdate();

        } catch (SQLException e) {
            throw new DatabaseException(e);
        } finally {
            connectionPool.releaseConnection(con);
        }
    }

    public void addUserToGroup(int userId, int groupId) {
        Connection con = null;
        try {
            con = connectionPool.reserveConnection();
            PreparedStatement ps = con.prepareStatement("INSERT INTO user_group (user_id, group_id) VALUES (?, ?)");
            ps.setInt(1, userId);
            ps.setInt(2, groupId);

            ps.executeUpdate();

        } catch (SQLException e) {
            throw new DatabaseException(e);
        } finally {
            connectionPool.releaseConnection(con);
        }
    }

    private void update(Group group) {
        Connection con = null;
        try {
            con = connectionPool.reserveConnection();

            PreparedStatement ps = con.prepareStatement(UPDATE_GROUP_QUERY);
            ps.setString(1, group.getName());
            ps.setInt(2, group.getId());

            ps.executeUpdate();

        } catch (SQLException e) {
            throw new DatabaseException(e);
        } finally {
            connectionPool.releaseConnection(con);
        }
    }

    private int create(Group group) {
        Connection con = null;
        try {
            con = connectionPool.reserveConnection();

            PreparedStatement ps = con.prepareStatement(CREATE_GROUP_QUERY, Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, group.getName());
            ps.executeUpdate();

            ResultSet rs = ps.getGeneratedKeys();
            if(rs.next()){
                return rs.getInt(1);
            } else {
                throw new DatabaseException("Failed to create user: " + group);
            }

        } catch (SQLException e) {
            throw new DatabaseException(e);
        } finally {
            connectionPool.releaseConnection(con);
        }
    }
}
