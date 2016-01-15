package pl.maxmati.po.ftp.client;

import pl.maxmati.ftp.common.Response;
import pl.maxmati.ftp.common.beans.User;
import pl.maxmati.ftp.common.command.Command;
import pl.maxmati.ftp.common.network.CommandConnection;
import pl.maxmati.po.ftp.client.events.CommandEvent;
import pl.maxmati.po.ftp.client.events.ConnectEvent;
import pl.maxmati.po.ftp.client.events.EventDispatcher;
import pl.maxmati.po.ftp.client.events.ResponseEvent;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.concurrent.ExecutorService;

/**
 * Created by maxmati on 1/14/16
 */
public class ClientSession implements Runnable{
    private final ExecutorService executor;

    private CommandConnection connection = null;

    private boolean running = false;
    private EventDispatcher dispatcher;

    private User user = null;
    private AuthenticationStatus authenticationStatus = AuthenticationStatus.USER_REQUIRED;

    public ClientSession(ExecutorService executor, EventDispatcher dispatcher) {
        this.executor = executor;
        this.dispatcher = dispatcher;
    }

    public void connect(String hostname, int port, String username, String password){
        user = new User(username, password);

        Socket socket = new Socket();
        try {
            socket.connect(new InetSocketAddress(hostname, port));
            connection = new CommandConnection(socket);
            connection.setOnCommandSentListener(command -> dispatcher.dispatch(new CommandEvent(command)));
            running = true;
            executor.submit(this);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void disconnect() {
        sendCommand(Command.Type.QUIT);
        running = false;
        connection.close();
        authenticationStatus = AuthenticationStatus.USER_REQUIRED;
        user = null;
    }

    @Override
    public void run() {
        while (running){
            Response response = connection.fetchResponse();
            if(response != null) {
                dispatcher.dispatch(new ResponseEvent(response));
                if(!handleAuthentication(response)) continue;
            }
        }
    }

    private boolean handleAuthentication(Response response) {
        if(authenticationStatus == AuthenticationStatus.USER_REQUIRED &&
                response.getType() == Response.Type.HELLO) {
            sendCommand(Command.Type.USER, user.getUsername());
            authenticationStatus = AuthenticationStatus.USER_PROVIDED;
        } else if (authenticationStatus == AuthenticationStatus.USER_PROVIDED &&
                response.getType() == Response.Type.PASSWORD_REQUIRED){
            sendCommand(Command.Type.PASS, user.getPassword());
            authenticationStatus = AuthenticationStatus.PASSWORD_PROVIDED;
        } else if (authenticationStatus == AuthenticationStatus.PASSWORD_PROVIDED){
            if(response.getType() == Response.Type.USER_LOGGED_IN){
                dispatcher.dispatch(new ConnectEvent(ConnectEvent.Type.CONNECTED));
                authenticationStatus = AuthenticationStatus.DONE;
                return true;
            }
        } else if (authenticationStatus == AuthenticationStatus.DONE)
            return true;
        return false;
    }

    private void sendCommand(Command.Type type, String... params){
        connection.sendCommand(new Command(type, params));
    }

    private enum  AuthenticationStatus {
        USER_PROVIDED, PASSWORD_PROVIDED, DONE, USER_REQUIRED
    }
}
