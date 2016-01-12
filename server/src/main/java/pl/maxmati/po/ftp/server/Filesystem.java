package pl.maxmati.po.ftp.server;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Path;

/**
 * Created by maxmati on 1/12/16
 */
public interface Filesystem {
    String listFiles(Path directory);

    boolean isDirectory(Path path);

    void createDir(Path path);

    void remove(Path path, boolean directory);

    InputStream getFile(Path path);

    OutputStream storeFile(Path path, boolean override);

    void changeDirectory(Path path);

    Path getCWD();
}
