package pl.maxmati.po.ftp.server.network;

import pl.maxmati.po.ftp.server.session.Session;

import java.io.IOException;
import java.io.PrintStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;

/**
 * Created by maxmati on 1/8/16
 */
public class PassiveConnection{
    private final ServerSocket serverSocket;
    private final Session session;
    private final ExecutorService executor;
    private Socket socket = null;

    public PassiveConnection(int port, Session session, ExecutorService executor) {
        this.executor = executor;
        this.session = session;
        try {
            this.serverSocket = new ServerSocket(port);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        System.out.println("Start passive connection on port: " + port);
        startAccepting();
    }

    public void sendData(String data){
        executor.submit(() -> {
            try {
                synchronized (this) {
                    while (socket == null)
                        this.wait();
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            try {
                PrintStream stream = new PrintStream(socket.getOutputStream());
                stream.print(data);
                stream.close();
                session.dataSent(true);
            } catch (IOException e) {
                e.printStackTrace();
                session.dataSent(false);
            }
        });

    }

    private void startAccepting(){
        executor.submit(() -> {
            try {
                synchronized (this) {
                    socket = serverSocket.accept();
                    serverSocket.close();
                    this.notify();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }
}
