package pl.maxmati.po.ftp.client;

import pl.maxmati.ftp.common.Response;
import pl.maxmati.ftp.common.command.Command;
import pl.maxmati.ftp.common.network.CommandConnection;
import pl.maxmati.po.ftp.client.events.CommandEvent;
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

    public ClientSession(ExecutorService executor, EventDispatcher dispatcher) {
        this.executor = executor;
        this.dispatcher = dispatcher;
    }

    public void connect(String hostname, int port){
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
        connection.sendCommand(new Command(Command.Type.QUIT));
        running = false;
        connection.close();
    }

    @Override
    public void run() {
        while (running){
            Response response = connection.fetchResponse();
            if(response != null)
                dispatcher.dispatch(new ResponseEvent(response));
        }
    }
}
