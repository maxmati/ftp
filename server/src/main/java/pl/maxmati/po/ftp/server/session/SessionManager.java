package pl.maxmati.po.ftp.server.session;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by maxmati on 1/8/16
 */
public class SessionManager {
    private static final int COMMAND_PORT = 1221;
    private final ExecutorService executor = Executors.newCachedThreadPool();
    public void start(){
        try (ServerSocket serverSocket = new ServerSocket(COMMAND_PORT)){
            while (true) {
                Socket socket = serverSocket.accept();
                System.out.println("New connection on command port accepted from: " + socket.getInetAddress());
                initSession(socket);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void initSession(Socket socket) throws IOException {
        executor.submit(new Session(socket));
    }
}
