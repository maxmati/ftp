package pl.maxmati.po.ftp.server.command;

import pl.maxmati.po.ftp.common.Response;
import pl.maxmati.po.ftp.common.command.Command;
import pl.maxmati.po.ftp.common.exceptions.FilesystemException;
import pl.maxmati.po.ftp.common.exceptions.PermissionDeniedException;
import pl.maxmati.po.ftp.common.filesystem.Filesystem;
import pl.maxmati.po.ftp.server.session.SessionInterface;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

/**
 * Created by maxmati on 1/12/16
 */
public class CommandProcessor {
    private final SessionInterface sessionInterface;
    private final Filesystem filesystem;
    private static final List<Command.Type> WITHOUT_AUTH_COMMAND_TYPES =
            Arrays.asList(Command.Type.USER, Command.Type.PASS);
    private boolean aborted = false;

    public CommandProcessor(SessionInterface sessionInterface, Filesystem filesystem) {
        this.sessionInterface = sessionInterface;
        this.filesystem = filesystem;
    }

    public void processCommand(Command command) {
        if(!command.hasValidNumberOfArgs()) {
            System.out.println("Syntax error in command: " + command + ". Invalid number of args");
            sessionInterface.sendResponse(Response.Type.SYNTAX_ERROR);
            return;
        }

        if( !sessionInterface.isAuthenticated() && !WITHOUT_AUTH_COMMAND_TYPES.contains(command.getType()) ){
            sessionInterface.sendResponse(Response.Type.BAD_SEQUENCE_OF_COMMANDS);
            return;
        }

        switch (command.getType()){
            case USER:
                sessionInterface.fetchUser(command.getParam(0));
                break;
            case PASS:
                sessionInterface.validatePassword(command.getParam(0));
                break;
            case QUIT:
                sessionInterface.quit();
                break;
            case NOOP:
                sessionInterface.sendResponse(Response.Type.COMMAND_SUCCESSFUL);
                break;
            case PASV:
                sessionInterface.listenForPassiveConnection();
                break;
            case NLST:
                machineList(sessionInterface);
                break;
            case PWD:
                sendWorkingDirectory(sessionInterface);
                break;
            case CWD:
                changeDirectory(command.getParam(0), sessionInterface);
                break;
            case MKD:
                createDirectory(command.getParam(0), sessionInterface);
                break;
            case RMD:
                remove(command.getParam(0), true, sessionInterface);
                break;
            case DELE:
                remove(command.getParam(0), false, sessionInterface);
                break;
            case RETR:
                sendFile(command.getParam(0), sessionInterface);
                break;
            case STOR:
                receiveFile(command.getParam(0), false, sessionInterface);
                break;
            case APPE:
                receiveFile(command.getParam(0), true, sessionInterface);
                break;
            case ABOR:
                sessionInterface.abortTransfer();
                aborted = true;
                sessionInterface.sendResponse(Response.Type.CLOSING_DATA_CONNECTION);
                break;
            case CHMOD:
                setPermissions(command.getParam(0), command.getParam(1));
                break;
            case NONE:
                sessionInterface.sendResponse(Response.Type.NOT_IMPLEMENTED);
                break;
        }
    }

    private void setPermissions(String filename, String mods) {
        int user = Character.getNumericValue(mods.charAt(0));
        int group = Character.getNumericValue(mods.charAt(1));

        boolean userCanRead = (user & 0x1) != 0;
        boolean userCanWrite = (user & 0x2) != 0;

        boolean groupCanRead = (group & 0x1) != 0;
        boolean groupCanWrite = (group & 0x2) != 0;

        filesystem.setPermissions(Paths.get(filename), userCanRead, userCanWrite, groupCanRead, groupCanWrite);

        sessionInterface.sendResponse(Response.Type.COMMAND_SUCCESSFUL);
    }

    private void machineList(SessionInterface sessionInterface) {
        if (!sessionInterface.havePassiveConnection()) return;

        try {
            sessionInterface.getPassiveConnection().sendData(filesystem.listFilesName(Paths.get("")));
            sessionInterface.sendResponse(Response.Type.OPENING_PASSIVE_CONNECTION, "ASCII", "/bin/ls");
        } catch (PermissionDeniedException e) {
            sessionInterface.sendResponse(Response.Type.PERMISSION_DENIED);
        }

    }

    private void sendWorkingDirectory(SessionInterface sessionInterface) {
        sessionInterface.sendResponse(Response.Type.CURRENT_DIRECTORY, filesystem.getCWD().toString());
    }

    private void changeDirectory(String path, SessionInterface sessionInterface) {
        try{
            filesystem.changeDirectory(Paths.get(path));
            sessionInterface.sendResponse(Response.Type.REQUEST_COMPLETED, "CWD");
        } catch( FilesystemException e){
            sessionInterface.sendResponse(e.getResponse());
        }
    }

    private void createDirectory(String directory, SessionInterface sessionInterface) {
        try {
            filesystem.createDir(Paths.get(directory));
            sessionInterface.sendResponse(Response.Type.CREATED_DIRECTORY, directory);
        } catch (FilesystemException e){
            sessionInterface.sendResponse(e.getResponse());
        }
    }

    private void remove(String filename, boolean directory, SessionInterface sessionInterface) {
        try {
            filesystem.remove(Paths.get(filename), directory);
            sessionInterface.sendResponse(Response.Type.REQUEST_COMPLETED, "RMD");
        } catch (FilesystemException e){
            sessionInterface.sendResponse(e.getResponse());
        }

    }

    private void sendFile(String filename, SessionInterface sessionInterface) {
        if (!sessionInterface.havePassiveConnection()) return;

        aborted = false;

        try {
            InputStream stream = filesystem.getFile(Paths.get(filename));
            sessionInterface.getPassiveConnection().sendData(stream);
            sessionInterface.sendResponse(Response.Type.OPENING_PASSIVE_CONNECTION, "binary", filename);
        } catch (FilesystemException e){
            if(!aborted)
                sessionInterface.sendResponse(e.getResponse());
        }
    }

    private void receiveFile(String filename, boolean override, SessionInterface sessionInterface) {
        if (!sessionInterface.havePassiveConnection()) return;
        aborted = false;

        try {
            OutputStream stream = filesystem.storeFile(Paths.get(filename), override);
            sessionInterface.getPassiveConnection().receiveData(stream);
            sessionInterface.sendResponse(Response.Type.OPENING_PASSIVE_CONNECTION, "binary", filename);
        } catch (FilesystemException e){
            if(!aborted)
                sessionInterface.sendResponse(e.getResponse());
        }

    }
}
