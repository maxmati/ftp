package pl.maxmati.po.ftp.server.exceptions;

import pl.maxmati.po.ftp.server.Response;

/**
 * Created by maxmati on 1/12/16
 */
public class NotRegularFileException extends FilesystemException {
    public NotRegularFileException(String filename) {
        super(new Response(Response.Type.NOT_REGULAR_FILE, filename));
    }
}
