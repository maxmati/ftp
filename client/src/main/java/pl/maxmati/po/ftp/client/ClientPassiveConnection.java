package pl.maxmati.po.ftp.client;

import java.io.*;
import java.net.Socket;
import java.util.stream.Stream;

/**
 * Created by maxmati on 1/16/16
 */
public class ClientPassiveConnection {
    private final Socket socket;
    private boolean used = false;

    public ClientPassiveConnection(String hostname, int port) {
        try {
            socket = new Socket(hostname, port);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        System.out.println("Created new passive connection with: " + hostname + ":" + port);
    }

    public synchronized boolean valid() {
        return socket.isConnected() && !socket.isClosed() && !used;
    }

    public synchronized Stream<String> readAll() {
        used = true;
        BufferedReader bufferedReader;
        try {
            bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return bufferedReader.lines();
    }

    public OutputStream getOutputStream() {
        used = true;
        try {
            return socket.getOutputStream();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public InputStream getInputStream() {
        used = true;
        try {
            return socket.getInputStream();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
