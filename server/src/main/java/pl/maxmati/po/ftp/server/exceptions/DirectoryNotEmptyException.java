package pl.maxmati.po.ftp.server.exceptions;

import pl.maxmati.po.ftp.server.Response;

/**
 * Created by maxmati on 1/12/16
 */
public class DirectoryNotEmptyException extends FilesystemException {

    public DirectoryNotEmptyException(String filename) {
        super(new Response(Response.Type.DIRECTORY_NOT_EMPTY, filename));
    }
}
