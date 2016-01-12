package pl.maxmati.po.ftp.server;

import pl.maxmati.po.ftp.server.database.File;
import pl.maxmati.po.ftp.server.database.User;
import pl.maxmati.po.ftp.server.database.dao.FilesDAO;

import java.nio.file.Path;

/**
 * Created by maxmati on 1/12/16
 */
public class PermissionManager {
    private final FilesDAO filesDAO;
    private User user = null;

    public PermissionManager(FilesDAO filesDAO) {
        this.filesDAO = filesDAO;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public boolean isValid(Path path) {
        return filesDAO.existsByFilename(path.toString());
    }

    public boolean haveWritePermission(Path path) {
        return havePermission(path, true);
    }

    public boolean haveReadPermission(Path path) {
        return havePermission(path, false);
    }

    public void addFileEntry(Path path) {
        filesDAO.addFileEntry(path.toString(), user);
    }

    public void removeFileEntry(Path path) {
        filesDAO.removeFileEntry(path.toString());
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
