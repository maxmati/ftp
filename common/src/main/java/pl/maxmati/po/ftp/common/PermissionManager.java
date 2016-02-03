package pl.maxmati.po.ftp.common;

import pl.maxmati.po.ftp.common.beans.User;

import java.nio.file.Path;

/**
 * Created by maxmati on 1/12/16
 */
public interface PermissionManager {
    void setUser(User user);

    boolean isValid(Path path);

    boolean haveWritePermission(Path path);

    boolean haveReadPermission(Path path);

    void addFileEntry(Path path);

    void removeFileEntry(Path path);

    void setPermissions(Path path, boolean userCanRead, boolean userCanWrite, boolean groupCanRead, boolean groupCanWrite);

    boolean isOwner(Path path);
}
