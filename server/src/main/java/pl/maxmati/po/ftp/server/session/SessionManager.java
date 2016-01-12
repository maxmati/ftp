package pl.maxmati.po.ftp.server.session;

import pl.maxmati.po.ftp.server.LocalFilesystem;
import pl.maxmati.po.ftp.server.PermissionManager;
import pl.maxmati.po.ftp.server.UsersManager;
import pl.maxmati.po.ftp.server.database.dao.FilesDAO;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Paths;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by maxmati on 1/8/16
 */
public class SessionManager {
    private static final int COMMAND_PORT = 1221;
    private final ExecutorService executor = Executors.newCachedThreadPool();
    private final UsersManager usersManager;
    private final FilesDAO filesDAO;

    public SessionManager(UsersManager usersManager, FilesDAO filesDAO) {
        this.usersManager = usersManager;
        this.filesDAO = filesDAO;
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
        PermissionManager permissionManager = new PermissionManager(filesDAO);
        executor.submit(
                new Session(
                        socket,
                        usersManager,
                        executor,
                        new LocalFilesystem(
                                permissionManager,
                                Paths.get("/home/maxmati/tmp")
                        ),
                        permissionManager
                )
        );
    }
}
