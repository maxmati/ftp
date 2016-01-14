package pl.maxmati.po.ftp.client;

import pl.maxmati.ftp.common.Response;
import pl.maxmati.ftp.common.command.Command;
import pl.maxmati.ftp.common.network.CommandConnection;

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

    public ClientSession(ExecutorService executor) {
        this.executor = executor;
    }

    public void connect(String hostname, int port){
        Socket socket = new Socket();
        try {
            socket.connect(new InetSocketAddress(hostname, port));
            connection = new CommandConnection(socket);
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
        }
    }
}
