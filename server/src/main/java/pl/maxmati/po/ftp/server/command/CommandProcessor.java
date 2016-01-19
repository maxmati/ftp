package pl.maxmati.po.ftp.server.command;

import pl.maxmati.ftp.common.Response;
import pl.maxmati.ftp.common.command.Command;
import pl.maxmati.ftp.common.exceptions.FilesystemException;
import pl.maxmati.ftp.common.exceptions.PermissionDeniedException;
import pl.maxmati.ftp.common.filesystem.Filesystem;
import pl.maxmati.po.ftp.server.session.Session;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

/**
 * Created by maxmati on 1/12/16
 */
public class CommandProcessor {
    private final Session session;
    private final Filesystem filesystem;
    private static final List<Command.Type> WITHOUT_AUTH_COMMAND_TYPES =
            Arrays.asList(Command.Type.USER, Command.Type.PASS);
    private boolean aborted = false;

    public CommandProcessor(Session session, Filesystem filesystem) {
        this.session = session;
        this.filesystem = filesystem;
    }

    public void processCommand(Command command) {
        if(!command.hasValidNumberOfArgs()) {
            System.out.println("Syntax error in command: " + command + ". Invalid number of args");
            session.sendResponse(Response.Type.SYNTAX_ERROR);
            return;
        }

        if( !session.isAuthenticated() && !WITHOUT_AUTH_COMMAND_TYPES.contains(command.getType()) ){
            session.sendResponse(Response.Type.BAD_SEQUENCE_OF_COMMANDS);
            return;
        }

        switch (command.getType()){
            case USER:
                session.fetchUser(command.getParam(0));
                break;
            case PASS:
                session.validatePassword(command.getParam(0));
                break;
            case QUIT:
                session.quit();
                break;
            case NOOP:
                session.sendResponse(Response.Type.COMMAND_SUCCESSFUL);
                break;
            case PASV:
                session.listenForPassiveConnection();
                break;
            case NLST:
                machineList(session);
                break;
            case PWD:
                sendWorkingDirectory(session);
                break;
            case CWD:
                changeDirectory(command.getParam(0), session);
                break;
            case MKD:
                createDirectory(command.getParam(0), session);
                break;
            case RMD:
                remove(command.getParam(0), true, session);
                break;
            case DELE:
                remove(command.getParam(0), false, session);
                break;
            case RETR:
                sendFile(command.getParam(0), session);
                break;
            case STOR:
                receiveFile(command.getParam(0), false, session);
                break;
            case APPE:
                receiveFile(command.getParam(0), true, session);
                break;
            case ABOR:
                session.abortTransfer();
                aborted = true;
                session.sendResponse(Response.Type.CLOSING_DATA_CONNECTION);
                break;
            case NONE:
                session.sendResponse(Response.Type.NOT_IMPLEMENTED);
                break;
        }
    }

    private void machineList(Session session) {
        if (!session.havePassiveConnection()) return;

        try {
            session.getPassiveConnection().sendData(filesystem.listFilesName(Paths.get("")));
            session.sendResponse(Response.Type.OPENING_PASSIVE_CONNECTION, "ASCII", "/bin/ls");
        } catch (PermissionDeniedException e) {
            session.sendResponse(Response.Type.PERMISSION_DENIED);
        }

    }

    private void sendWorkingDirectory(Session session) {
        session.sendResponse(Response.Type.CURRENT_DIRECTORY, filesystem.getCWD().toString());
    }

    private void changeDirectory(String path, Session session) {
        try{
            filesystem.changeDirectory(Paths.get(path));
            session.sendResponse(Response.Type.REQUEST_COMPLETED, "CWD");
        } catch( FilesystemException e){
            session.sendResponse(e.getResponse());
        }
    }

    private void createDirectory(String directory, Session session) {
        try {
            filesystem.createDir(Paths.get(directory));
            session.sendResponse(Response.Type.CREATED_DIRECTORY, directory);
        } catch (FilesystemException e){
            session.sendResponse(e.getResponse());
        }
    }

    private void remove(String filename, boolean directory, Session session) {
        try {
            filesystem.remove(Paths.get(filename), directory);
            session.sendResponse(Response.Type.REQUEST_COMPLETED, "RMD");
        } catch (FilesystemException e){
            session.sendResponse(e.getResponse());
        }

    }

    private void sendFile(String filename, Session session) {
        if (!session.havePassiveConnection()) return;

        aborted = false;

        try {
            InputStream stream = filesystem.getFile(Paths.get(filename));
            session.getPassiveConnection().sendData(stream);
            session.sendResponse(Response.Type.OPENING_PASSIVE_CONNECTION, "binary", filename);
        } catch (FilesystemException e){
            if(!aborted)
                session.sendResponse(e.getResponse());
        }
    }

    private void receiveFile(String filename, boolean override, Session session) {
        if (!session.havePassiveConnection()) return;
        aborted = false;

        try {
            OutputStream stream = filesystem.storeFile(Paths.get(filename), override);
            session.getPassiveConnection().receiveData(stream);
            session.sendResponse(Response.Type.OPENING_PASSIVE_CONNECTION, "binary", filename);
        } catch (FilesystemException e){
            if(!aborted)
                session.sendResponse(e.getResponse());
        }

    }
}
