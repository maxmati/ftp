package pl.maxmati.po.ftp.server.network;

import pl.maxmati.po.ftp.server.session.Session;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
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
        System.out.println("Started listening for passive connection on port: " + port);
        startAccepting();
    }

    public void sendData(String data){
        executor.submit(() -> {
            waitForConnection();

            try {
                PrintStream stream = new PrintStream(socket.getOutputStream());
                stream.print(data);
                stream.close();
                session.dataSent(true);
            } catch (Exception e) {
                e.printStackTrace();
                session.dataSent(false);
            }
        });

    }

    public void receiveData(OutputStream out) {
        executor.submit(() -> {
            waitForConnection();

            try(InputStream in = socket.getInputStream()) {
                pipeStream(in, out);
                out.close();
                session.dataSent(true);
            } catch (Exception e) {
                session.dataSent(false);
                e.printStackTrace();
            }

        });
    }

    public void sendData(InputStream in) {
        executor.submit(() -> {
            waitForConnection();

            try(OutputStream out = socket.getOutputStream()) {
                pipeStream(in, out);
                in.close();
                session.dataSent(true);
            } catch (Exception e) {
                session.dataSent(false);
                e.printStackTrace();
            }
        });
    }

    private void waitForConnection() {
        try {
            synchronized (this) {
                while (socket == null)
                    this.wait();
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void startAccepting(){
        executor.submit(() -> {
            try {
                synchronized (this) {
                    socket = serverSocket.accept();

                    System.out.println("New connection on passive port " +
                            String.valueOf(serverSocket.getLocalPort()) +
                            " from: " + socket.getInetAddress());

                    serverSocket.close();
                    this.notify();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

    }

    private void pipeStream(InputStream in, OutputStream out) throws IOException {
        int n;
        byte[] buffer = new byte[1024];
        while ((n = in.read(buffer)) > -1){
            out.write(buffer, 0, n);
        }
    }
}
