package pl.maxmati.po.ftp.server;

import pl.maxmati.po.ftp.common.PermissionManager;
import pl.maxmati.po.ftp.common.beans.User;
import pl.maxmati.po.ftp.server.beans.File;
import pl.maxmati.po.ftp.server.database.dao.FilesDAO;

import java.nio.file.Path;

/**
 * Created by maxmati on 1/12/16
 */
public class DatabasePermissionManager implements PermissionManager {
    private final FilesDAO filesDAO;
    private User user = null;

    public DatabasePermissionManager(FilesDAO filesDAO) {
        this.filesDAO = filesDAO;
    }

    @Override
    public void setUser(User user) {
        this.user = user;
    }

    @Override
    public boolean isValid(Path path) {
        return filesDAO.existsByFilename(path.toString());
    }

    @Override
    public boolean haveWritePermission(Path path) {
        return havePermission(path, true);
    }

    @Override
    public boolean haveReadPermission(Path path) {
        return havePermission(path, false);
    }

    @Override
    public void addFileEntry(Path path) {
        filesDAO.addFileEntry(path.toString(), user);
    }

    @Override
    public void removeFileEntry(Path path) {
        filesDAO.removeFileEntry(path.toString());
    }

    @Override
    public void setPermissions(Path path, boolean userCanRead, boolean userCanWrite, boolean groupCanRead, boolean groupCanWrite) {
        File file = filesDAO.findByFilename(path.toString());
        if(file == null)
            return;

        file.setOwnerCanRead(userCanRead);
        file.setOwnerCanWrite(userCanWrite);
        file.setGroupCanRead(groupCanRead);
        file.setGroupCanWrite(groupCanWrite);

        filesDAO.save(file);
    }

    @Override
    public boolean isOwner(Path path) {
        File file = filesDAO.findByFilename(path.toString());
        return file != null && file.getOwner().equals(user);
    }

    private boolean havePermission(Path path, boolean write) {
        File file = filesDAO.findByFilename(path.toString());
        if(file == null)
            return false;

        if(file.getOwner().equals(user)){
            if(write && file.isOwnerCanWrite())
                return true;
            if(!write && file.isOwnerCanRead())
                return true;
        } else if(file.getGroup().getMembers().contains(user)) {
            if(write && file.isGroupCanWrite())
                return true;
            if(!write && file.isGroupCanRead())
                return true;
        }
        return false;
    }
}
