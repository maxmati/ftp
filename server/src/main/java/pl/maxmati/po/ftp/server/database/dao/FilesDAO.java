package pl.maxmati.po.ftp.server.database.dao;

import pl.maxmati.ftp.common.beans.Group;
import pl.maxmati.ftp.common.beans.User;
import pl.maxmati.po.ftp.server.beans.File;
import pl.maxmati.po.ftp.server.database.ConnectionPool;
import pl.maxmati.po.ftp.server.exceptions.DatabaseException;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Created by maxmati on 1/12/16
 */
public class FilesDAO {
    private static final String UPDATE_FILES_QUERY =
            "UPDATE `files` " +
            "SET " +
                    "`filename` = ?, " +
                    "`owner_id` = ?, " +
                    "`group_id` = ?, " +
                    "`user_read` = ?, " +
                    "`user_write` = ?, " +
                    "`group_read` = ?, " +
                    "`group_write` = ? " +
            "WHERE `id` = ?;";
    
    private final ConnectionPool connectionPool;
    private final UsersDAO usersDAO;
    private final GroupsDAO groupsDAO;

    public FilesDAO(ConnectionPool connectionPool, UsersDAO usersDAO, GroupsDAO groupsDAO) {
        this.connectionPool = connectionPool;
        this.usersDAO = usersDAO;
        this.groupsDAO = groupsDAO;
    }

    public boolean existsByFilename(String filename){
        Connection con = null;
        try {
            con = connectionPool.reserveConnection();
            PreparedStatement ps = con.prepareStatement("SELECT 1 FROM files WHERE filename = ?");
            ps.setString(1, filename);

            ResultSet rs = ps.executeQuery();
            return rs.next();

        } catch (SQLException e) {
            throw new DatabaseException(e);
        } finally {
            connectionPool.releaseConnection(con);
        }
    }

    public File findByFilename(String filename) {
        Connection con = null;
        try {
            con = connectionPool.reserveConnection();
            PreparedStatement ps = con.prepareStatement("SELECT * FROM files WHERE filename = ?");
            ps.setString(1, filename);

            ResultSet rs = ps.executeQuery();
            if(rs.next()){
                int id = rs.getInt("id");
                int ownerId = rs.getInt("owner_id");
                int groupId = rs.getInt("group_id");
                boolean userCanRead = rs.getBoolean("user_read");
                boolean userCanWrite = rs.getBoolean("user_write");
                boolean groupCanRead = rs.getBoolean("group_read");
                boolean groupCanWrite = rs.getBoolean("group_write");

                User owner = usersDAO.findUserById(ownerId);
                Group group = groupsDAO.findGroupById(groupId);
                groupsDAO.fetchMembers(group);

                return new File(id, filename, owner, group, userCanRead,
                        userCanWrite, groupCanRead, groupCanWrite);
            } else
                return null;

        } catch (SQLException e) {
            throw new DatabaseException(e);
        } finally {
            connectionPool.releaseConnection(con);
        }
    }

    public void addFileEntry(String filename, User user) {
        Connection con = null;
        try {
            con = connectionPool.reserveConnection();
            PreparedStatement ps = con.prepareStatement("INSERT INTO files(filename, owner_id, group_id) VALUES (?, ?, ?)");
            ps.setString(1, filename);
            ps.setInt(2, user.getId());
            ps.setInt(3, usersDAO.getUserGroupId(user));

            ps.executeUpdate();

        } catch (SQLException e) {
            throw new DatabaseException(e);
        } finally {
            connectionPool.releaseConnection(con);
        }
    }

    public void removeFileEntry(String filename) {
        Connection con = null;
        try {
            con = connectionPool.reserveConnection();
            PreparedStatement ps = con.prepareStatement("DELETE FROM files WHERE filename = ?");
            ps.setString(1, filename);

            ps.executeUpdate();

        } catch (SQLException e) {
            throw new DatabaseException(e);
        } finally {
            connectionPool.releaseConnection(con);
        }
    }

    public void save(File file) {
        Connection con = null;
        try {
            con = connectionPool.reserveConnection();
            PreparedStatement ps = con.prepareStatement(UPDATE_FILES_QUERY);
            ps.setString(1, file.getFilename());
            ps.setInt(2, file.getOwner().getId());
            ps.setInt(3, file.getGroup().getId());
            ps.setBoolean(4, file.isOwnerCanRead());
            ps.setBoolean(5, file.isOwnerCanWrite());
            ps.setBoolean(6, file.isGroupCanRead());
            ps.setBoolean(7, file.isGroupCanWrite());
            ps.setInt(8, file.getId());

            ps.executeUpdate();

        } catch (SQLException e) {
            throw new DatabaseException(e);
        } finally {
            connectionPool.releaseConnection(con);
        }
    }
}
