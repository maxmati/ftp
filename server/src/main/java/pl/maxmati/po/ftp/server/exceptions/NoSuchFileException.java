package pl.maxmati.po.ftp.server.exceptions;

import pl.maxmati.po.ftp.server.Response;

/**
 * Created by maxmati on 1/12/16
 */
public class NoSuchFileException extends FilesystemException {

    public NoSuchFileException(String filename) {
        super(new Response(Response.Type.NO_SUCH_FILE_OR_DIR, filename));
    }
}
