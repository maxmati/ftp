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
//    private Command currentCommand = null;

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
            commandConnection.sendResponse(new Response(Response.TRANSFER_COMPLETE_CODE));
        else
            commandConnection.sendResponse(new Response(451));

        passiveConnection = null;
    }

    private void quit() {
        commandConnection.sendResponse(new Response(Response.BYE_CODE));
        running.set(false);
        commandConnection.close();
    }

    private void processCommand(Command command) {
        if(!command.hasValidNumberOfArgs()) {
            System.out.println("Syntax error in command: " + command + ". Invalid number of args");
            commandConnection.sendResponse(new Response(Response.SYNTAX_ERROR_CODE));
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
                commandConnection.sendResponse(new Response(Response.COMMAND_SUCCESSFUL_CODE));
                break;
            case PASV:
                startPassiveConnection();
                break;
            case NLST:
                startMachineListing(command);
                break;
        }
    }

    private void startMachineListing(Command command) {
        if(passiveConnection == null){
            commandConnection.sendResponse(new Response(Response.BAD_SEQUENCE_OF_COMMANDS_CODE));
            return;
        }

//        currentCommand = command;
        commandConnection.sendResponse(new Response(Response.OPENING_PASSIVE_CONNECTION_CODE, "ASCII", "/bin/ls"));
        passiveConnection.sendData(filesystem.listFiles(cwd));

    }

    private void startPassiveConnection() {
        int port = 2212;
        passiveConnection = new PassiveConnection(port, this, executor);

        commandConnection.sendResponse(
                new Response(
                        Response.ENTERING_PASSIVE_MODE_CODE,
                        127, 0, 0, 1, port / 256, port % 256
                )
        );
    }

    private void startLoggingIn(String username) {
        if(user == null){
            user = manager.getByName(username);
            if(user != null)
                commandConnection.sendResponse(new Response(Response.PASSWORD_REQUIRED_CODE));
            else
                commandConnection.sendResponse(new Response(Response.INVALID_USER_OR_PASS_CODE));
        } else {
            commandConnection.sendResponse(new Response(Response.BAD_SEQUENCE_OF_COMMANDS_CODE));
        }
    }

    private void finishLoggingIn(String password) {
        if(user != null && !authenticated){
            authenticated = manager.validatePassword(user, password);
            if(authenticated)
                commandConnection.sendResponse(new Response(Response.USER_LOGGED_IN_CODE));
            else
                commandConnection.sendResponse(new Response(Response.INVALID_USER_OR_PASS_CODE));
        } else {
            commandConnection.sendResponse(new Response(Response.BAD_SEQUENCE_OF_COMMANDS_CODE));
        }
    }
}
