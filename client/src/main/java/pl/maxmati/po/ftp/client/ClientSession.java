package pl.maxmati.po.ftp.client;

import pl.maxmati.ftp.common.Response;
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

    public ClientSession(ExecutorService executor) {
        this.executor = executor;
    }

    public void connect(String hostname, int port){
        Socket socket = new Socket();
        try {
            socket.connect(new InetSocketAddress(hostname, port));
            connection = new CommandConnection(socket);
            executor.submit(this);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        while (true){
            Response response = connection.fetchResponse();
        }
    }
}
