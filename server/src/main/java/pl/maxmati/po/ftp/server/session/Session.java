package pl.maxmati.po.ftp.server.session;

import pl.maxmati.po.ftp.server.*;
import pl.maxmati.po.ftp.server.network.CommandConnection;
import pl.maxmati.po.ftp.server.network.PassiveConnection;

import java.io.IOException;
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
    private final InetAddress address;
    private final UsersManager manager;
    private final CommandConnection commandConnection;
    private final ExecutorService executor;
    private final Watchdog watchdog;
    private final Filesystem filesystem = new Filesystem();

    private final AtomicBoolean running = new AtomicBoolean(true);

    private User user = null;
    private boolean authenticated = false;
    private PassiveConnection passiveConnection = null;
    private Path cwd = Paths.get("/home/maxmati/");

    public Session(Socket socket, UsersManager usersManager, ExecutorService executor) throws IOException {
        this.executor = executor;
        this.address = socket.getInetAddress();
        this.manager = usersManager;
        this.commandConnection = new CommandConnection(socket);
        watchdog = new Watchdog(60 * 1000, this::quit);
    }

    @Override
    public void run() {
        try {
            while (running.get()) {
                Command command = commandConnection.fetchCommand();
                if(command != null)
                    processCommand(command);
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
            commandConnection.sendResponse(new Response(Response.Type.TRANSFER_COMPLETE));
        else
            commandConnection.sendResponse(new Response(Response.Type.ABORTED_LOCAL_ERROR));

        passiveConnection = null;
    }

    private void quit() {
        commandConnection.sendResponse(new Response(Response.Type.BYE));
        running.set(false);
        commandConnection.close();
    }

    private void processCommand(Command command) {
        if(!command.hasValidNumberOfArgs()) {
            System.out.println("Syntax error in command: " + command + ". Invalid number of args");
            commandConnection.sendResponse(new Response(Response.Type.SYNTAX_ERROR));
            return;
        }
        watchdog.reset();
        switch (command.getType()){
            case USER:
                startLoggingIn(command.getParam(0));
                break;
            case PASS:
                finishLoggingIn(command.getParam(0));
                break;
            case QUIT:
                quit();
                break;
            case NOOP:
                commandConnection.sendResponse(new Response(Response.Type.COMMAND_SUCCESSFUL));
                break;
            case PASV:
                startPassiveConnection();
                break;
            case NLST:
                startMachineListing();
                break;
            case PWD:
                commandConnection.sendResponse(new Response(Response.Type.CURRENT_DIRECTORY, cwd.toString()));
                break;
            case CWD:
                changeDirectory(command.getParam(0));
                break;
        }
    }

    private void changeDirectory(String path) {
        Path relPath = Paths.get(path);
        Path newPath = cwd.resolve(relPath).normalize();
        if(filesystem.isValid(newPath)){
            System.out.println("Changing working directory to: " + newPath);
            cwd = newPath;
            commandConnection.sendResponse(new Response(Response.Type.REQUEST_COMPLETED, "CWD"));
        } else {
            commandConnection.sendResponse(new Response(Response.Type.NO_SUCH_FILE_OR_DIR, path));
        }
    }

    private void startMachineListing() {
        if(passiveConnection == null){
            commandConnection.sendResponse(new Response(Response.Type.BAD_SEQUENCE_OF_COMMANDS));
            return;
        }

        commandConnection.sendResponse(new Response(Response.Type.OPENING_PASSIVE_CONNECTION, "ASCII", "/bin/ls"));
        passiveConnection.sendData(filesystem.listFiles(cwd));

    }

    private void startPassiveConnection() {
        int port = 2212;
        passiveConnection = new PassiveConnection(port, this, executor);

        commandConnection.sendResponse(
                new Response(
                        Response.Type.ENTERING_PASSIVE_MODE,
                        127, 0, 0, 1, port / 256, port % 256
                )
        );
    }

    private void startLoggingIn(String username) {
        if(user == null){
            user = manager.getByName(username);
            if(user != null)
                commandConnection.sendResponse(new Response(Response.Type.PASSWORD_REQUIRED));
            else
                commandConnection.sendResponse(new Response(Response.Type.INVALID_USER_OR_PASS));
        } else {
            commandConnection.sendResponse(new Response(Response.Type.BAD_SEQUENCE_OF_COMMANDS));
        }
    }

    private void finishLoggingIn(String password) {
        if(user != null && !authenticated){
            authenticated = manager.validatePassword(user, password);
            if(authenticated)
                commandConnection.sendResponse(new Response(Response.Type.USER_LOGGED_IN));
            else
                commandConnection.sendResponse(new Response(Response.Type.INVALID_USER_OR_PASS));
        } else {
            commandConnection.sendResponse(new Response(Response.Type.BAD_SEQUENCE_OF_COMMANDS));
        }
    }
}
