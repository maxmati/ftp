package pl.maxmati.po.ftp.server.exceptions;

import pl.maxmati.po.ftp.server.Response;

/**
 * Created by maxmati on 1/12/16
 */
public class FileAlreadyExistsException extends FilesystemException {
    public FileAlreadyExistsException(String filename) {
        super(new Response(Response.Type.FILE_EXISTS, filename));
    }
}
