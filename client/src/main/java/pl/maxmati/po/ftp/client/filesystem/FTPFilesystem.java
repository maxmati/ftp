package pl.maxmati.po.ftp.client.filesystem;

import pl.maxmati.po.ftp.client.events.*;
import pl.maxmati.po.ftp.client.network.ClientPassiveConnection;
import pl.maxmati.po.ftp.client.session.ClientSession;
import pl.maxmati.po.ftp.common.Response;
import pl.maxmati.po.ftp.common.command.Command;
import pl.maxmati.po.ftp.common.exceptions.*;
import pl.maxmati.po.ftp.common.filesystem.Filesystem;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Created by maxmati on 1/16/16
 */
public class FTPFilesystem implements Filesystem {
    private static final Response.Type[] ERROR_CODES = {
            Response.Type.ABORTED_LOCAL_ERROR, Response.Type.BAD_SEQUENCE_OF_COMMANDS,
            Response.Type.DIRECTORY_NOT_EMPTY, Response.Type.INVALID_USER_OR_PASS,
            Response.Type.NO_DATA_CONNECTION, Response.Type.NO_SUCH_FILE_OR_DIR,
            Response.Type.NOT_DIRECTORY, Response.Type.NOT_REGULAR_FILE,
            Response.Type.PERMISSION_DENIED, Response.Type.SYNTAX_ERROR
    };
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

        initWaitForResponse(Response.Type.OPENING_PASSIVE_CONNECTION);
        dispatcher.dispatch(new CommandEvent(CommandEvent.Type.REQUEST, new Command(Command.Type.NLST)));
        waitForResponse();

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

        initWaitForResponse(Response.Type.OPENING_PASSIVE_CONNECTION);
        dispatcher.dispatch(new CommandEvent(CommandEvent.Type.REQUEST, new Command(Command.Type.NLST)));
        waitForResponse();

        initWaitForResponse(Response.Type.TRANSFER_COMPLETE);
        List<Path> files = passiveConnection.readAll().filter(s -> !s.isEmpty()).map(path::resolve).collect(Collectors.toList());
        waitForResponse();

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

        initWaitForResponse(true, Response.Type.CREATED_DIRECTORY);
        dispatcher.dispatch(new CommandEvent(CommandEvent.Type.REQUEST, new Command(Command.Type.MKD, path.toString())));

        final Response.Type response = waitForResponse();
        parseErrors(response, path.toString());
    }

    @Override
    public synchronized  void remove(Path path, boolean directory) {
        waitForInitialization();

        System.out.println("Removing: " + path);

        initWaitForResponse(true, Response.Type.REQUEST_COMPLETED);
        if(directory)
            dispatcher.dispatch(new CommandEvent(CommandEvent.Type.REQUEST, new Command(Command.Type.RMD, path.toString())));
        else
            dispatcher.dispatch(new CommandEvent(CommandEvent.Type.REQUEST, new Command(Command.Type.DELE, path.toString())));

        Response.Type response = waitForResponse();
        parseErrors(response, path.toString());

    }

    @Override
    public synchronized  InputStream getFile(Path path) {
        waitForInitialization();

        System.out.println("Retrieving file: " + path);

        ClientPassiveConnection passiveConnection = acquireClientPassiveConnection();

        initWaitForResponse(true, Response.Type.OPENING_PASSIVE_CONNECTION);
        dispatcher.dispatch(new CommandEvent(CommandEvent.Type.REQUEST, new Command(Command.Type.RETR, path.toString())));

        final Response.Type response = waitForResponse();
        System.out.println("!!!Filesystem received response: " + response);
        parseErrors(response, path.toString());

        return passiveConnection.getInputStream();
    }

    @Override
    public synchronized  OutputStream storeFile(Path path, boolean append) {
        waitForInitialization();

        System.out.println("Storing file: " + path);

        ClientPassiveConnection passiveConnection = acquireClientPassiveConnection();

        initWaitForResponse(true, Response.Type.OPENING_PASSIVE_CONNECTION);
        if(append)
            dispatcher.dispatch(new CommandEvent(CommandEvent.Type.REQUEST, new Command(Command.Type.APPE, path.toString())));
        else
            dispatcher.dispatch(new CommandEvent(CommandEvent.Type.REQUEST, new Command(Command.Type.STOR, path.toString())));


        Response.Type response = waitForResponse();
        parseErrors(response, path.toString());

        return passiveConnection.getOutputStream();
    }

    private void parseErrors(Response.Type responseType, Object... params) {
        switch (responseType){
            case DIRECTORY_NOT_EMPTY:
                throw new DirectoryNotEmptyException((String) params[0]);
            case FILE_EXISTS:
                throw new FileAlreadyExistsException((String) params[0]);
            case ABORTED_LOCAL_ERROR:
                throw new FilesystemException();
            case NO_SUCH_FILE_OR_DIR:
                throw new NoSuchFileException((String) params[0]);
            case NOT_DIRECTORY:
                throw new NotDirectoryException((String) params[0]);
            case NOT_REGULAR_FILE:
                throw new NotRegularFileException((String) params[0]);
            case PERMISSION_DENIED:
                throw new PermissionDeniedException();
        }
    }

    @Override
    public synchronized void changeDirectory(Path path) {
        waitForInitialization();

        initWaitForResponse(Response.Type.REQUEST_COMPLETED, Response.Type.NO_SUCH_FILE_OR_DIR); //TODO: param
        dispatcher.dispatch(new CommandEvent(CommandEvent.Type.REQUEST, new Command(Command.Type.CWD, path.toString())));
        switch (waitForResponse()){
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

    @Override
    public void setPermissions(Path path, boolean userCanRead, boolean userCanWrite, boolean groupCanRead, boolean groupCanWrite) {
        //TODO:
    }

    private ClientPassiveConnection acquireClientPassiveConnection() {
        if(!session.havePassiveConnection()) {
            initWaitForResponse(Response.Type.ENTERING_PASSIVE_MODE);
            dispatcher.dispatch(new CommandEvent(CommandEvent.Type.REQUEST, new Command(Command.Type.PASV)));
            waitForResponse();
        }

        return session.getPassiveConnection();
    }

    private void initWaitForResponse(boolean withErrors, Response.Type... types) {
        if(withErrors)
            initWaitForResponse(
                    Stream.concat(Arrays.stream(types), Arrays.stream(ERROR_CODES)).toArray(Response.Type[]::new));
        else
            initWaitForResponse(types);
    }

    private void initWaitForResponse(Response.Type... types) {
        waitingForResponse = Arrays.asList(types);
    }

    private Response.Type waitForResponse() {
        synchronized (waitingForResponseLock) {
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
            initWaitForResponse(Response.Type.CURRENT_DIRECTORY);
            dispatcher.dispatch(new CommandEvent(CommandEvent.Type.REQUEST, new Command(Command.Type.PWD)));
            waitForResponse();
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
