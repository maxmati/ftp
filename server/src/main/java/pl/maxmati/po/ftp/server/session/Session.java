package pl.maxmati.po.ftp.server.session;

import pl.maxmati.po.ftp.server.*;
import pl.maxmati.po.ftp.server.network.Connection;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by maxmati on 1/8/16
 */
public class Session implements Runnable{
    private final InetAddress address;
    private final UsersManager manager;
    private final Connection connection;
    private final Watchdog watchdog;

    private final AtomicBoolean running = new AtomicBoolean(true);

    private User user = null;
    private boolean authenticated = false;

    public Session(Socket socket, UsersManager usersManager) throws IOException {
        this.address = socket.getInetAddress();
        this.manager = usersManager;
        this.connection = new Connection(socket);
        watchdog = new Watchdog(60 * 1000, this::quit);
    }

    @Override
    public void run() {
        try {
            while (running.get()) {
                Command command = connection.fetchCommand();
                if(command != null)
                    processCommand(command);
            }
        } catch (Exception e){
            e.printStackTrace();
        } finally {
            System.out.println("Closing connection with: " + address);
            System.out.println("Ending session.");
            if(!connection.isClosed())
                connection.close();
        }
    }

    private void quit() {
        connection.sendResponse(new Response(Response.BYE_CODE));
        running.set(false);
        connection.close();
    }

    private void processCommand(Command command) {
        if(!command.hasValidNumberOfArgs()) {
            System.out.println("Syntax error in command: " + command + ". Invalid number of args");
            connection.sendResponse(new Response(Response.SYNTAX_ERROR_CODE));
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
                connection.sendResponse(new Response(Response.COMMAND_SUCCESSFUL_CODE));
                break;
        }
    }

    private void startLoggingIn(String username) {
        if(user == null){
            user = manager.getByName(username);
            if(user != null)
                connection.sendResponse(new Response(Response.PASSWORD_REQUIRED_CODE));
            else
                connection.sendResponse(new Response(Response.INVALID_USER_OR_PASS_CODE));
        } else {
            connection.sendResponse(new Response(Response.BAD_SEQUENCE_OF_COMMANDS_CODE));
        }
    }

    private void finishLoggingIn(String password) {
        if(user != null && !authenticated){
            authenticated = manager.validatePassword(user, password);
            if(authenticated)
                connection.sendResponse(new Response(Response.USER_LOGGED_IN_CODE));
            else
                connection.sendResponse(new Response(Response.INVALID_USER_OR_PASS_CODE));
        } else {
            connection.sendResponse(new Response(Response.BAD_SEQUENCE_OF_COMMANDS_CODE));
        }
    }
}
