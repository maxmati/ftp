package pl.maxmati.po.ftp.common.network;

import java.io.*;
import java.net.Socket;

/**
 * Created by maxmati on 2/3/16
 */
public class SocketMock extends Socket {
    private final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
    private final String data;
    private boolean closed = false;

    SocketMock(String data) {
        this.data = data;
    }

    @Override
    public InputStream getInputStream() throws IOException {
        return new ByteArrayInputStream(data.getBytes());
    }

    @Override
    public OutputStream getOutputStream() throws IOException {
        return byteArrayOutputStream;
    }

    @Override
    public boolean isClosed() {
        return closed;
    }

    @Override
    public synchronized void close() throws IOException {
        closed = true;
    }

    public String getOutputData(){
        final String data = byteArrayOutputStream.toString();
        byteArrayOutputStream.reset();
        return data;
    }
}