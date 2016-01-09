package pl.maxmati.po.ftp.server.session;

import pl.maxmati.po.ftp.server.UsersManager;

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
    private final UsersManager usersManager;

    public SessionManager(UsersManager usersManager) {
        this.usersManager = usersManager;
    }

    public void start(){
        try (ServerSocket serverSocket = new ServerSocket(COMMAND_PORT)){
            //noinspection InfiniteLoopStatement
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
        executor.submit(new Session(socket, usersManager, executor));
    }
}
