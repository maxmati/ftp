package pl.maxmati.ftp.common.exceptions;

import pl.maxmati.ftp.common.Response;

/**
 * Created by maxmati on 1/12/16
 */
public class NotDirectoryException extends FilesystemException {
    public NotDirectoryException(String filename) {
        super(new Response(Response.Type.NOT_DIRECTORY));
    }
}
