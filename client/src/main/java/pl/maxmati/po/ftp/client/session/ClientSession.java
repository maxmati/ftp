package pl.maxmati.po.ftp.client.session;

import pl.maxmati.po.ftp.client.events.*;
import pl.maxmati.po.ftp.client.network.ClientPassiveConnection;
import pl.maxmati.po.ftp.common.Response;
import pl.maxmati.po.ftp.common.beans.User;
import pl.maxmati.po.ftp.common.command.Command;
import pl.maxmati.po.ftp.common.network.CommandConnection;

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
    private ClientPassiveConnection passiveConnection = null;

    public ClientSession(ExecutorService executor, EventDispatcher dispatcher) {
        this.executor = executor;
        this.dispatcher = dispatcher;

        dispatcher.registerListener(CommandEvent.class, this::onCommandEvent);
    }

    private void onCommandEvent(Event event) {
        CommandEvent commandEvent = (CommandEvent) event;
        if(commandEvent.getType() == CommandEvent.Type.REQUEST){
            connection.sendCommand(commandEvent.getCommand());
        }
    }

    public boolean connect(String hostname, int port, String username, String password){
        user = new User(username, password);

        Socket socket = new Socket();
        try {
            socket.connect(new InetSocketAddress(hostname, port));
            connection = new CommandConnection(socket);
            connection.setOnCommandSentListener(command ->
                    dispatcher.dispatch(new CommandEvent(CommandEvent.Type.PERFORMED, command))
            );
            running = true;
            executor.submit(this);

        } catch (IOException e) {
            e.printStackTrace();
            if(connection != null)
                connection.close();
            running = false;
            return false;
        }
        return true;
    }

    public void disconnect() {
        dispatcher.dispatch(new ConnectEvent(ConnectEvent.Type.DISCONNECTED));

        sendCommand(Command.Type.QUIT);
        running = false;
        if(havePassiveConnection()) {
            passiveConnection.close();
        }
        connection.close();
        authenticationStatus = AuthenticationStatus.USER_REQUIRED;
        user = null;
    }

    @Override
    public void run() {
        try {
            while (running) {
                Response response = connection.fetchResponse();
                if (response != null) {
                    dispatcher.dispatch(new ResponseEvent(response));

                    if (response.getType() == Response.Type.BYE) disconnect();
                    if (!handleAuthentication(response)) continue;

                    handlePassiveConnection(response);
                } else
                    running = false;
            }
        } catch (Exception e){
            e.printStackTrace();
        }
    }

    private void handlePassiveConnection(Response response) {
        if(response.getType() != Response.Type.ENTERING_PASSIVE_MODE)
            return;

        Object[] params = response.getParams();

        final String ip = params[0] + "." + params[1] + "." + params[2] + "." + params[3];
        final int port = (256 * (Integer) params[4]) + (Integer) params[5];

        synchronized (this) {
            passiveConnection = new ClientPassiveConnection(ip, port);
            this.notifyAll();
        }
    }

    private boolean handleAuthentication(Response response) {
        if(response.getType() == Response.Type.INVALID_USER_OR_PASS)
            dispatcher.dispatch(new ConnectEvent(ConnectEvent.Type.ERROR_BAD_PASS));

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

    public synchronized ClientPassiveConnection getPassiveConnection() {
        while (!havePassiveConnection())
            try {
                this.wait();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }


        return passiveConnection;
    }

    public synchronized boolean havePassiveConnection(){
        return passiveConnection != null && passiveConnection.valid();
    }

    private enum  AuthenticationStatus {
        USER_PROVIDED, PASSWORD_PROVIDED, DONE, USER_REQUIRED
    }
}
