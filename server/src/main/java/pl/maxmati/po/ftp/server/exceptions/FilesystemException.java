package pl.maxmati.po.ftp.server.exceptions;

import pl.maxmati.po.ftp.server.Response;

import java.io.IOException;

/**
 * Created by maxmati on 1/9/16
 */
public class FilesystemException extends RuntimeException {
    private final Response response;

    public FilesystemException(Response response) {
        this.response = response;
    }

    public FilesystemException(){
        response = new Response(Response.Type.ABORTED_LOCAL_ERROR);
    }

    public FilesystemException(IOException e) {
        super(e);
        response = new Response(Response.Type.ABORTED_LOCAL_ERROR);
    }

    public Response getResponse() {
        return response;
    }
}
