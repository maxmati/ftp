package pl.maxmati.po.ftp.server.network;

import pl.maxmati.po.ftp.server.session.SessionInterface;

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
public class PassiveConnection implements PassiveConnectionInterface {
    private final ServerSocket serverSocket;
    private final SessionInterface sessionInterface;
    private final ExecutorService executor;
    private Socket socket = null;

    public PassiveConnection(SessionInterface sessionInterface, ExecutorService executor) {
        this.executor = executor;
        this.sessionInterface = sessionInterface;
        try {
            this.serverSocket = new ServerSocket(0);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        System.out.println("Started listening for passive connection on port: " + getPort());
        startAccepting();
    }

    @Override
    public void sendData(String data){
        executor.submit(() -> {
            waitForConnection();

            try {
                PrintStream stream = new PrintStream(socket.getOutputStream());
                stream.print(data);
                stream.close();
                sessionInterface.dataSent(true);
            } catch (Exception e) {
                e.printStackTrace();
                sessionInterface.dataSent(false);
            }
        });

    }

    @Override
    public void receiveData(OutputStream out) {
        executor.submit(() -> {
            waitForConnection();

            try(InputStream in = socket.getInputStream()) {
                pipeStream(in, out);
                out.close();
                sessionInterface.dataSent(true);
            } catch (Exception e) {
                sessionInterface.dataSent(false);
                e.printStackTrace();
            }

        });
    }

    @Override
    public void sendData(InputStream in) {
        executor.submit(() -> {
            waitForConnection();

            try(OutputStream out = socket.getOutputStream()) {
                pipeStream(in, out);
                in.close();
                sessionInterface.dataSent(true);
            } catch (Exception e) {
                sessionInterface.dataSent(false);
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

    @Override
    public int getPort() {
        return serverSocket.getLocalPort();
    }

    @Override
    public boolean abort() {
        if(socket != null) {
            try {
                socket.close();
                return true;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return false;
    }
}
