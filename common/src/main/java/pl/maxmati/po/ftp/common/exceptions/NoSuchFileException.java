package pl.maxmati.po.ftp.common.exceptions;

import pl.maxmati.po.ftp.common.Response;

/**
 * Created by maxmati on 1/12/16
 */
public class NoSuchFileException extends FilesystemException {

    public NoSuchFileException(String filename) {
        super(new Response(Response.Type.NO_SUCH_FILE_OR_DIR, filename));
    }
}
