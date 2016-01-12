package pl.maxmati.po.ftp.server.exceptions;

import pl.maxmati.po.ftp.server.Response;

/**
 * Created by maxmati on 1/12/16
 */
public class NotDirectoryException extends FilesystemException {
    public NotDirectoryException(String filename) {
        super(new Response(Response.Type.NOT_DIRECTORY));
    }
}
