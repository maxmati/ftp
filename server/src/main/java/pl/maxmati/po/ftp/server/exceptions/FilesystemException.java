package pl.maxmati.po.ftp.server.exceptions;

import java.io.IOException;

/**
 * Created by maxmati on 1/9/16
 */
public class FilesystemException extends RuntimeException {
    public FilesystemException(IOException e) {
        super(e);
    }
}
