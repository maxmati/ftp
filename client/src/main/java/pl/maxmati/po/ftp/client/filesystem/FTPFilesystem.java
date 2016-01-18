package pl.maxmati.po.ftp.client.filesystem;

import pl.maxmati.ftp.common.Response;
import pl.maxmati.ftp.common.command.Command;
import pl.maxmati.ftp.common.exceptions.FilesystemException;
import pl.maxmati.ftp.common.exceptions.NoSuchFileException;
import pl.maxmati.ftp.common.filesystem.Filesystem;
import pl.maxmati.po.ftp.client.events.*;
import pl.maxmati.po.ftp.client.network.ClientPassiveConnection;
import pl.maxmati.po.ftp.client.session.ClientSession;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by maxmati on 1/16/16
 */
public class FTPFilesystem implements Filesystem {
    private final ClientSession session;
    private final EventDispatcher dispatcher;
    private final Object waitingForResponseLock = new Object();
    private List<Response.Type> waitingForResponse = new ArrayList<>();
    private Response.Type lastWaitedResponse = Response.Type.NONE;

    private Path cwd = null;
    private boolean initialized = false;

    public FTPFilesystem(ClientSession session, EventDispatcher dispatcher) {
        this.session = session;
        this.dispatcher = dispatcher;
        dispatcher.registerListener(ResponseEvent.class, this::onResponse);
        dispatcher.registerListener(ConnectEvent.class, this::onConnect);
    }


    @Override
    public synchronized String listFilesName(Path path) {
        waitForInitialization();

        System.out.println("Listing file names via FTP in: " + path);

        Path currentCwd = cwd;
        if(!path.equals(cwd))
            changeDirectory(path);

        ClientPassiveConnection passiveConnection = acquireClientPassiveConnection();

        dispatcher.dispatch(new CommandEvent(CommandEvent.Type.REQUEST, new Command(Command.Type.NLST)));
        waitForResponse(Response.Type.OPENING_PASSIVE_CONNECTION);

        String files = passiveConnection.readAll().collect(Collectors.joining("\n"));

        if(!path.equals(currentCwd))
            changeDirectory(currentCwd);

        return files;
    }

    @Override
    public synchronized  List<Path> listFiles(Path path) {
        waitForInitialization();

        System.out.println("Listing files via FTP in: " + path);

        Path currentCwd = cwd;
        if(!path.equals(cwd))
            changeDirectory(path);

        ClientPassiveConnection passiveConnection = acquireClientPassiveConnection();

        dispatcher.dispatch(new CommandEvent(CommandEvent.Type.REQUEST, new Command(Command.Type.NLST)));
        waitForResponse(Response.Type.OPENING_PASSIVE_CONNECTION);

        List<Path> files = passiveConnection.readAll().filter(s -> !s.isEmpty()).map(path::resolve).collect(Collectors.toList());

        waitForResponse(Response.Type.TRANSFER_COMPLETE);

        if(!path.equals(currentCwd))
            changeDirectory(currentCwd);

        return files;
    }

    @Override
    public synchronized  boolean isDirectory(Path path) {
        waitForInitialization();

        try {
            Path currentCwd = cwd;
            changeDirectory(path);
            changeDirectory(currentCwd);
            return true;
        } catch (FilesystemException e){
            return false;
        }
    }

    @Override
    public synchronized  void createDir(Path path) {
        waitForInitialization();

        System.out.println("Creating directory: " + path);

        dispatcher.dispatch(new CommandEvent(CommandEvent.Type.REQUEST, new Command(Command.Type.MKD, path.toString())));

        waitForResponse(Response.Type.REQUEST_COMPLETED); //TODO: errors
    }

    @Override
    public synchronized  void remove(Path path, boolean directory) {
        waitForInitialization();

        System.out.println("Removing: " + path);

        if(directory)
            dispatcher.dispatch(new CommandEvent(CommandEvent.Type.REQUEST, new Command(Command.Type.RMD, path.toString())));
        else
            dispatcher.dispatch(new CommandEvent(CommandEvent.Type.REQUEST, new Command(Command.Type.DELE, path.toString())));

        waitForResponse(Response.Type.REQUEST_COMPLETED); //TODO: errors

    }

    @Override
    public synchronized  InputStream getFile(Path path) {
        waitForInitialization();

        System.out.println("Retrieving file: " + path);

        ClientPassiveConnection passiveConnection = acquireClientPassiveConnection();

        dispatcher.dispatch(new CommandEvent(CommandEvent.Type.REQUEST, new Command(Command.Type.RETR, path.toString())));

        waitForResponse(Response.Type.OPENING_PASSIVE_CONNECTION);//TODO: errors

        return passiveConnection.getInputStream();
    }

    @Override
    public synchronized  OutputStream storeFile(Path path, boolean append) {
        waitForInitialization();

        System.out.println("Storing file: " + path);

        ClientPassiveConnection passiveConnection = acquireClientPassiveConnection();

        if(append)
            dispatcher.dispatch(new CommandEvent(CommandEvent.Type.REQUEST, new Command(Command.Type.APPE, path.toString())));
        else
            dispatcher.dispatch(new CommandEvent(CommandEvent.Type.REQUEST, new Command(Command.Type.STOR, path.toString())));


        waitForResponse(Response.Type.OPENING_PASSIVE_CONNECTION);//TODO: errors

        return passiveConnection.getOutputStream();
    }

    @Override
    public synchronized void changeDirectory(Path path) {
        waitForInitialization();

        dispatcher.dispatch(new CommandEvent(CommandEvent.Type.REQUEST, new Command(Command.Type.CWD, path.toString())));
        switch (waitForResponse(Response.Type.REQUEST_COMPLETED, Response.Type.NO_SUCH_FILE_OR_DIR)){ //TODO: param
            case NO_SUCH_FILE_OR_DIR:
                throw new NoSuchFileException(path.toString());
        }
    }

    @Override
    public synchronized  Path getCWD() {
        waitForInitialization();

        return cwd;
    }

    @Override
    public String getID() {
        return "ftp";
    }

    private ClientPassiveConnection acquireClientPassiveConnection() {
        if(!session.havePassiveConnection()) {
            dispatcher.dispatch(new CommandEvent(CommandEvent.Type.REQUEST, new Command(Command.Type.PASV)));
            waitForResponse(Response.Type.ENTERING_PASSIVE_MODE);
        }

        return session.getPassiveConnection();
    }

    private Response.Type waitForResponse(Response.Type type) {
        return waitForResponse(new Response.Type[]{type});
    }

    private Response.Type waitForResponse(Response.Type... type) {
        synchronized (waitingForResponseLock) {
            waitingForResponse = Arrays.asList(type);
            while (waitingForResponse.size() != 0)
                try {
                    waitingForResponseLock.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            return lastWaitedResponse;
        }
    }

    private void onResponse(Event event) {
        ResponseEvent responseEvent = (ResponseEvent) event;
        final Response response = responseEvent.getResponse();

        switch (response.getType()){
            case CURRENT_DIRECTORY:
                cwd = Paths.get((String) response.getParams()[0]);
                System.out.println("Changed cwd to: " + cwd);
                break;
        }

        synchronized (waitingForResponseLock){
            if(waitingForResponse.contains(response.getType())) {
                lastWaitedResponse = response.getType();
                waitingForResponse = new ArrayList<>();
            }
            waitingForResponseLock.notifyAll();
        }
    }

    private void onConnect(Event event) {
        if( ((ConnectEvent) event).getType() == ConnectEvent.Type.CONNECTED )
            initialize();

    }

    private void initialize() {
        synchronized (this) {
            dispatcher.dispatch(new CommandEvent(CommandEvent.Type.REQUEST, new Command(Command.Type.PWD)));
            waitForResponse(Response.Type.CURRENT_DIRECTORY);
            initialized = true;
            this.notifyAll();
        }
    }

    private synchronized void waitForInitialization(){
        while (!initialized)
            try {
                this.wait();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
    }
}
