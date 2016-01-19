package pl.maxmati.po.ftp.server.session;

import pl.maxmati.ftp.common.Response;
import pl.maxmati.ftp.common.Watchdog;
import pl.maxmati.ftp.common.beans.User;
import pl.maxmati.ftp.common.command.Command;
import pl.maxmati.ftp.common.filesystem.Filesystem;
import pl.maxmati.ftp.common.network.CommandConnection;
import pl.maxmati.po.ftp.server.Config;
import pl.maxmati.po.ftp.server.DatabasePermissionManager;
import pl.maxmati.po.ftp.server.UsersManager;
import pl.maxmati.po.ftp.server.command.CommandProcessor;
import pl.maxmati.po.ftp.server.network.PassiveConnection;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
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
    private final CommandProcessor processor;
    private final DatabasePermissionManager permissionManager;

    private final AtomicBoolean running = new AtomicBoolean(true);

    private User user = null;
    private boolean authenticated = false;
    private PassiveConnection passiveConnection = null;
    private final InetAddress address;

    public Session(Socket socket, UsersManager usersManager, ExecutorService executor,
                   Filesystem filesystem, DatabasePermissionManager permissionManager) throws IOException {
        this.executor = executor;
        this.address = socket.getInetAddress();
        this.manager = usersManager;
        this.commandConnection = new CommandConnection(socket);
        this.processor = new CommandProcessor(this, filesystem);
        this.permissionManager = permissionManager;
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


    public void listenForPassiveConnection() {
        passiveConnection = new PassiveConnection(this, executor);

        int port = passiveConnection.getPort();

        final Config config = Config.getInstance();
        sendResponse(
                Response.Type.ENTERING_PASSIVE_MODE,
                config.getIP1(), config.getIP2(),
                config.getIP3(), config.getIP4(),
                port / 256, port % 256
        );
    }

    public void fetchUser(String username) {
        if(user == null){
            user = manager.getByName(username);
            if(user != null) {
                sendResponse(Response.Type.PASSWORD_REQUIRED);
            } else
                sendResponse(Response.Type.INVALID_USER_OR_PASS);
        } else {
            sendResponse(Response.Type.BAD_SEQUENCE_OF_COMMANDS);
        }
    }

    public void validatePassword(String password) {
        if(user != null && !authenticated){
            authenticated = manager.validatePassword(user, password);
            if(authenticated) {
                permissionManager.setUser(user);
                sendResponse(Response.Type.USER_LOGGED_IN);
            } else
                sendResponse(Response.Type.INVALID_USER_OR_PASS);
        } else {
            sendResponse(Response.Type.BAD_SEQUENCE_OF_COMMANDS);
        }
    }

    public void sendResponse(Response.Type type, Object... params) {
        sendResponse(new Response(type, params));
    }

    public void sendResponse(Response response) {
        commandConnection.sendResponse(response);
    }

    public boolean havePassiveConnection() {
        if(passiveConnection == null){
            sendResponse(Response.Type.NO_DATA_CONNECTION);
            return false;
        }
        return true;
    }

    public PassiveConnection getPassiveConnection() {
        return passiveConnection;
    }

    public boolean isAuthenticated() {
        return authenticated;
    }
}
