package pl.maxmati.po.ftp.server.session;

import pl.maxmati.po.ftp.server.*;
import pl.maxmati.po.ftp.server.command.Command;
import pl.maxmati.po.ftp.server.command.CommandProcessor;
import pl.maxmati.po.ftp.server.database.ConnectionPool;
import pl.maxmati.po.ftp.server.database.User;
import pl.maxmati.po.ftp.server.database.dao.FilesDAO;
import pl.maxmati.po.ftp.server.database.dao.GroupsDAO;
import pl.maxmati.po.ftp.server.database.dao.UsersDAO;
import pl.maxmati.po.ftp.server.exceptions.FilesystemException;
import pl.maxmati.po.ftp.server.exceptions.PermissionDeniedException;
import pl.maxmati.po.ftp.server.network.CommandConnection;
import pl.maxmati.po.ftp.server.network.PassiveConnection;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by maxmati on 1/8/16
 */
public class Session implements Runnable{
    private final UsersManager manager;
    private final CommandConnection commandConnection;
    private final ExecutorService executor;
    private final Watchdog watchdog;
    private final PermissionManager permissionManager = new PermissionManager(new FilesDAO(ConnectionPool.getInstance(), new UsersDAO(ConnectionPool.getInstance()), new GroupsDAO(ConnectionPool.getInstance())));
    private final Filesystem filesystem = new LocalFilesystem(permissionManager);
    private final CommandProcessor processor = new CommandProcessor(this);
    private final AtomicBoolean running = new AtomicBoolean(true);

    private User user = null;
    private boolean authenticated = false;
    private PassiveConnection passiveConnection = null;
    private Path cwd = Paths.get("/home/maxmati/tmp");
    private final InetAddress address;

    public Session(Socket socket, UsersManager usersManager, ExecutorService executor) throws IOException {
        this.executor = executor;
        this.address = socket.getInetAddress();
        this.manager = usersManager;
        this.commandConnection = new CommandConnection(socket);
        watchdog = new Watchdog(60 * 1000, this::quit);

        commandConnection.sendResponse(new Response(Response.Type.HELLO));
    }

    @Override
    public void run() {
        try {
            while (running.get()) {
                Command command = commandConnection.fetchCommand();
                if(command != null) {
                    processor.processCommand(command);
                    watchdog.reset();
                }
            }
        } catch (Exception e){
            e.printStackTrace();
        } finally {
            System.out.println("Closing commandConnection with: " + address);
            System.out.println("Ending session.");
            if(!commandConnection.isClosed())
                commandConnection.close();
        }
    }

    public void dataSent(boolean success) {
        if(success)
            sendResponse(Response.Type.TRANSFER_COMPLETE);
        else
            sendResponse(Response.Type.ABORTED_LOCAL_ERROR);

        passiveConnection = null;
    }

    public void quit() {
        sendResponse(Response.Type.BYE);
        running.set(false);
        commandConnection.close();
    }

    public void receiveFile(String filename, boolean override) {
        if (!havePassiveConnection()) return;

        Path path = resolveIfRelative(filename);

        try {
            OutputStream stream = filesystem.storeFile(path, override);
            passiveConnection.receiveData(stream);
            sendResponse(Response.Type.OPENING_PASSIVE_CONNECTION, "binary", filename);
        } catch (FilesystemException e){
            sendResponse(e.getResponse());
        }

    }



    public void sendFile(String filename) {
        if (!havePassiveConnection()) return;

        Path path = resolveIfRelative(filename);

        try {
            InputStream stream = filesystem.getFile(path);
            passiveConnection.sendData(stream);
            sendResponse(Response.Type.OPENING_PASSIVE_CONNECTION, "binary", filename);
        } catch (FilesystemException e){
            sendResponse(e.getResponse());
        }
    }

    public void remove(String filename, boolean directory) {
        Path path = resolveIfRelative(filename);

        try {
            filesystem.remove(path, directory);
            sendResponse(Response.Type.REQUEST_COMPLETED, "RMD");
        } catch (FilesystemException e){
            sendResponse(e.getResponse());
        }

    }

    public void createDirectory(String directory) {
        Path path = Paths.get(directory);
        if(!path.isAbsolute())
            path = cwd.resolve(directory);

        try {
            filesystem.createDir(path);
            sendResponse(Response.Type.CREATED_DIRECTORY, directory);
        } catch (FilesystemException e){
            sendResponse(e.getResponse());
        }

    }

    public void changeDirectory(String path) {
        Path relPath = Paths.get(path);
        Path newPath = cwd.resolve(relPath).normalize();
        if(filesystem.isValidDirectory(newPath)){
            System.out.println("Changing working directory to: " + newPath);
            cwd = newPath;
            sendResponse(Response.Type.REQUEST_COMPLETED, "CWD");
        } else {
            sendResponse(Response.Type.NO_SUCH_FILE_OR_DIR, path);
        }
    }

    public void machineList() {
        if (havePassiveConnection()) return;

        try {
            passiveConnection.sendData(filesystem.listFiles(cwd));
            sendResponse(Response.Type.OPENING_PASSIVE_CONNECTION, "ASCII", "/bin/ls");
        } catch (PermissionDeniedException e) {
            sendResponse(Response.Type.PERMISSION_DENIED);
        }

    }

    public void listenForPassiveConnection() {
        int port = 2212;
        passiveConnection = new PassiveConnection(port, this, executor);

        commandConnection.sendResponse(
                new Response(
                        Response.Type.ENTERING_PASSIVE_MODE,
                        127, 0, 0, 1, port / 256, port % 256
                )
        );
    }

    public void fetchUser(String username) {
        if(user == null){
            user = manager.getByName(username);
            if(user != null) {
                commandConnection.sendResponse(new Response(Response.Type.PASSWORD_REQUIRED));
            } else
                commandConnection.sendResponse(new Response(Response.Type.INVALID_USER_OR_PASS));
        } else {
            commandConnection.sendResponse(new Response(Response.Type.BAD_SEQUENCE_OF_COMMANDS));
        }
    }

    public void validatePassword(String password) {
        if(user != null && !authenticated){
            authenticated = manager.validatePassword(user, password);
            if(authenticated) {
                commandConnection.sendResponse(new Response(Response.Type.USER_LOGGED_IN));
                permissionManager.setUser(user);
            } else
                commandConnection.sendResponse(new Response(Response.Type.INVALID_USER_OR_PASS));
        } else {
            commandConnection.sendResponse(new Response(Response.Type.BAD_SEQUENCE_OF_COMMANDS));
        }
    }

    public void sendResponse(Response.Type type, Object... params) {
        sendResponse(new Response(type, params));
    }

    public void sendWorkingDirectory() {
        sendResponse(Response.Type.CURRENT_DIRECTORY, cwd.toString());
    }

    private void sendResponse(Response response) {
        commandConnection.sendResponse(response);
    }

    private Path resolveIfRelative(String pathName) {
        Path path = Paths.get(pathName);
        if(!path.isAbsolute())
            path = cwd.resolve(path);
        return path;
    }

    private boolean havePassiveConnection() {
        if(passiveConnection == null){
            sendResponse(Response.Type.NO_DATA_CONNECTION);
            return false;
        }
        return true;
    }
}
