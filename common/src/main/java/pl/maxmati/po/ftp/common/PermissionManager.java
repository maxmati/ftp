package pl.maxmati.po.ftp.common;

import pl.maxmati.po.ftp.common.beans.User;

import java.nio.file.Path;

/**
 * @author maxmati
 * @version 1.0
 */
public interface PermissionManager {

    /**
     * Sets user against which permissions will be checked
     *
     * @param user specified user
     */
    void setUser(User user);

    /**
     * Checks if path is valid (for example exists)
     *
     * @param path specified path
     * @return true if path is valid false otherwise
     */
    boolean isValid(Path path);


    /**
     * Checks if specified @see{User} have write permission to file
     *
     * @param path path to check
     * @return true if user have write permission false otherwise
     */
    boolean haveWritePermission(Path path);

    /**
     * Checks if specified @see{User} have read permission to file
     *
     * @param path path to check
     * @return true if user have read permission false otherwise
     */
    boolean haveReadPermission(Path path);

    /**
     * Create new Entry with default permissions for specified file
     *
     * @param path path to file
     */
    void addFileEntry(Path path);

    /**
     * Remove entry from permission table
     *
     * @param path path to file
     */
    void removeFileEntry(Path path);

    /**
     * Set permission for file as specified.
     * @param path path to file
     * @param userCanRead unix r equivalent
     * @param userCanWrite unix w equivalent
     * @param groupCanRead unix r equivalent
     * @param groupCanWrite unix w equivalent
     */
    void setPermissions(Path path, boolean userCanRead, boolean userCanWrite, boolean groupCanRead, boolean groupCanWrite);

    /**
     * Checks if user is owner of the file
     *
     * @param path path to file to check
     * @return true if user is owner false otherwise
     */
    boolean isOwner(Path path);
}
