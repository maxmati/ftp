package pl.maxmati.po.ftp.common.exceptions;

import pl.maxmati.po.ftp.common.Response;

/**
 * Created by maxmati on 1/12/16
 */
public class PermissionDeniedException extends FilesystemException {
    public PermissionDeniedException() {
        super(new Response(Response.Type.PERMISSION_DENIED));
    }
}
