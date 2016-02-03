package pl.maxmati.po.ftp.server.session;

import pl.maxmati.po.ftp.common.Response;
import pl.maxmati.po.ftp.common.Watchdog;
import pl.maxmati.po.ftp.common.beans.User;
import pl.maxmati.po.ftp.common.command.Command;
import pl.maxmati.po.ftp.common.filesystem.Filesystem;
import pl.maxmati.po.ftp.common.network.CommandConnection;
import pl.maxmati.po.ftp.server.Config;
import pl.maxmati.po.ftp.server.DatabasePermissionManager;
import pl.maxmati.po.ftp.server.UsersManager;
import pl.maxmati.po.ftp.server.command.CommandProcessor;
import pl.maxmati.po.ftp.server.network.PassiveConnection;
import pl.maxmati.po.ftp.server.network.PassiveConnectionInterface;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by maxmati on 1/8/16
 */
public class Session implements SessionInterface {
    private final UsersManager manager;
    private final CommandConnection commandConnection;
    private final ExecutorService executor;
    private final Watchdog watchdog;
    private final CommandProcessor processor;
    private final DatabasePermissionManager permissionManager;

    private final AtomicBoolean running = new AtomicBoolean(true);

    private User user = null;
    private boolean authenticated = false;
    private PassiveConnectionInterface passiveConnectionInterface = null;
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

    @Override
    public void dataSent(boolean success) {
        if(success)
            sendResponse(Response.Type.TRANSFER_COMPLETE);
        else
            sendResponse(Response.Type.ABORTED_LOCAL_ERROR);

        passiveConnectionInterface = null;
    }

    @Override
    public void quit() {
        sendResponse(Response.Type.BYE);
        running.set(false);
        watchdog.stop();
        commandConnection.close();
    }


    @Override
    public void listenForPassiveConnection() {
        passiveConnectionInterface = new PassiveConnection(this, executor);

        int port = passiveConnectionInterface.getPort();

        final Config config = Config.getInstance();
        sendResponse(
                Response.Type.ENTERING_PASSIVE_MODE,
                config.getIP1(), config.getIP2(),
                config.getIP3(), config.getIP4(),
                port / 256, port % 256
        );
    }

    @Override
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

    @Override
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

    @Override
    public void sendResponse(Response.Type type, Object... params) {
        sendResponse(new Response(type, params));
    }

    @Override
    public void sendResponse(Response response) {
        commandConnection.sendResponse(response);
    }

    @Override
    public boolean havePassiveConnection() {
        if(passiveConnectionInterface == null){
            sendResponse(Response.Type.NO_DATA_CONNECTION);
            return false;
        }
        return true;
    }

    @Override
    public PassiveConnectionInterface getPassiveConnection() {
        return passiveConnectionInterface;
    }

    @Override
    public boolean isAuthenticated() {
        return authenticated;
    }

    @Override
    public void abortTransfer() {
        if(passiveConnectionInterface != null && passiveConnectionInterface.abort())
            sendResponse(Response.Type.TRANSFER_ABORTED);
    }
}
