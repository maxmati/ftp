package pl.maxmati.po.ftp.server.command;

import pl.maxmati.po.ftp.server.Response;
import pl.maxmati.po.ftp.server.session.Session;

/**
 * Created by maxmati on 1/12/16
 */
public class CommandProcessor {
    private final Session session;

    public CommandProcessor(Session session) {
        this.session = session;
    }

    public void processCommand(Command command) {
        if(!command.hasValidNumberOfArgs()) {
            System.out.println("Syntax error in command: " + command + ". Invalid number of args");
            session.sendResponse(Response.Type.SYNTAX_ERROR);
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
                session.machineList();
                break;
            case PWD:
                session.sendWorkingDirectory();
                break;
            case CWD:
                session.changeDirectory(command.getParam(0));
                break;
            case MKD:
                session.createDirectory(command.getParam(0));
                break;
            case RMD:
                session.remove(command.getParam(0), true);
                break;
            case DELE:
                session.remove(command.getParam(0), false);
                break;
            case RETR:
                session.sendFile(command.getParam(0));
                break;
            case STOR:
                session.receiveFile(command.getParam(0), false);
                break;
            case APPE:
                session.receiveFile(command.getParam(0), true);
                break;
            case NONE:
                break;
            default:
                session.sendResponse(Response.Type.NOT_IMPLEMENTED);
        }
    }
}
