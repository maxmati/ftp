package pl.maxmati.ftp.common.network;

import pl.maxmati.ftp.common.Response;
import pl.maxmati.ftp.common.command.Command;

import java.io.IOException;
import java.io.PrintStream;
import java.net.Socket;
import java.util.Arrays;
import java.util.NoSuchElementException;
import java.util.Scanner;

/**
 * Created by maxmati on 1/8/16
 */
public class CommandConnection {
    private final Socket socket;
    private final Scanner scanner;
    private final PrintStream output;

    public CommandConnection(Socket socket) throws IOException {
        this.socket = socket;
        this.scanner = new Scanner(socket.getInputStream());
        this.output = new PrintStream(socket.getOutputStream());
    }

    public Command fetchCommand(){
        String line;
        try {
            line = scanner.nextLine();
        } catch (NoSuchElementException e){
            return null;
        }

        String[] tokens = line.split(" ");

        Command.Type type;
        try {
            type = Command.Type.valueOf(tokens[0]);
        } catch (IllegalArgumentException e){
            System.out.println("Unknown command " + tokens[0]);
            type = Command.Type.NONE;
        }
        final String[] params = Arrays.copyOfRange(tokens, 1, tokens.length);
        Command command = new Command(type, params);

        System.out.println("Received new command: " + command);
        return command;
    }

    public void sendCommand(Command command) {
        output.print(command.toNetworkString());
        System.out.println("Sent command: " + command);

    }

    public Response fetchResponse() {
        String line;
        try {
            line = scanner.nextLine();
        } catch (NoSuchElementException e){
            return null;
        }

        String[] tokens = line.split(" ", 2);

        final Response response = new Response(Integer.valueOf(tokens[0]), tokens[1]);

        System.out.println("Received response: " + response);
        return response;
    }

    public void sendResponse(Response response) {
        output.print(response.toNetworkString());
        System.out.println("Sent response: " + response);
    }

    public void close(){
        try {
            socket.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public boolean isClosed() {
        return socket.isClosed();
    }
}
