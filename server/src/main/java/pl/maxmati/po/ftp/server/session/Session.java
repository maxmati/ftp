package pl.maxmati.po.ftp.server.session;

import pl.maxmati.po.ftp.server.Command;
import pl.maxmati.po.ftp.server.Response;
import pl.maxmati.po.ftp.server.User;
import pl.maxmati.po.ftp.server.UsersManager;
import pl.maxmati.po.ftp.server.network.SessionSocket;

import java.io.IOException;
import java.net.Socket;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by maxmati on 1/8/16
 */
public class Session implements Runnable{
    private final Socket socket;
    private final UsersManager manager;
    private final SessionSocket reader;

    private final AtomicBoolean running = new AtomicBoolean(true);

    private User user = null;
    private boolean authenticated = false;

    public Session(Socket socket, UsersManager usersManager) throws IOException {
        this.socket = socket;
        this.manager = usersManager;
        this.reader = new SessionSocket(socket);
    }

    @Override
    public void run() {
        try {
            while (running.get()) {
                Command command = reader.fetchCommand();
                processCommand(command);
            }
        } catch (Exception e){
            e.printStackTrace();
        } finally {
            System.out.println("Closing connection with: " + socket.getInetAddress());
            System.out.println("Ending session.");
            try {
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void quit() {
        reader.sendResponse(new Response(Response.BYE_CODE));
        running.set(false);
    }

    private void processCommand(Command command) {
        if(!command.hasValidNumberOfArgs()) {
            System.out.println("Syntax error in command: " + command + ". Invalid number of args");
            reader.sendResponse(new Response(Response.SYNTAX_ERROR_CODE));
            return;
        }
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
        }
    }

    private void startLoggingIn(String username) {
        if(user == null){
            user = manager.getByName(username);
            if(user != null)
                reader.sendResponse(new Response(Response.PASSWORD_REQUIRED_CODE));
            else
                reader.sendResponse(new Response(Response.INVALID_USER_OR_PASS_CODE));
        } else {
            reader.sendResponse(new Response(Response.BAD_SEQUENCE_OF_COMMANDS_CODE));
        }
    }

    private void finishLoggingIn(String password) {
        if(user != null && !authenticated){
            authenticated = manager.validatePassword(user, password);
            if(authenticated)
                reader.sendResponse(new Response(Response.USER_LOGGED_IN_CODE));
            else
                reader.sendResponse(new Response(Response.INVALID_USER_OR_PASS_CODE));
        } else {
            reader.sendResponse(new Response(Response.BAD_SEQUENCE_OF_COMMANDS_CODE));
        }
    }
}
