package pl.maxmati.po.ftp.client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
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
}
