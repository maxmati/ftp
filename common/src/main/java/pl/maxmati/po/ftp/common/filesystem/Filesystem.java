package pl.maxmati.po.ftp.common.filesystem;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Path;
import java.util.List;

/**
 * Created by maxmati on 1/12/16
 */
public interface Filesystem {

    String listFilesName(Path directory);

    List<Path> listFiles(Path path);

    boolean isDirectory(Path path);

    void createDir(Path path);

    void remove(Path path, boolean directory);

    InputStream getFile(Path path);

    OutputStream storeFile(Path path, boolean append);

    void changeDirectory(Path path);

    Path getCWD();

    String getID();

    void setPermissions(Path path, boolean userCanRead, boolean userCanWrite,
                        boolean groupCanRead, boolean groupCanWrite);
}
