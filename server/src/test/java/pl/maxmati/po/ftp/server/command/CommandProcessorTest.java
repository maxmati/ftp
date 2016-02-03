package pl.maxmati.po.ftp.server.command;

import org.junit.Test;
import pl.maxmati.po.ftp.common.Response;
import pl.maxmati.po.ftp.common.command.Command;
import pl.maxmati.po.ftp.common.exceptions.PermissionDeniedException;
import pl.maxmati.po.ftp.common.filesystem.Filesystem;
import pl.maxmati.po.ftp.server.network.PassiveConnectionInterface;
import pl.maxmati.po.ftp.server.session.SessionInterface;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Path;
import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 * Created by maxmati on 2/3/16
 */
public class CommandProcessorTest {

    @Test
    public void processSyntaxError() throws Exception {
        SessionMock session = new SessionMock();
        CommandProcessor processor = new CommandProcessor(session, null);
        processor.processCommand(new Command(Command.Type.USER));

        assertEquals(new Response(Response.Type.SYNTAX_ERROR), session.response);
    }

    @Test
    public void processNotAuth() throws Exception {
        SessionMock session = new SessionMock();
        session.authenticated = false;
        CommandProcessor processor = new CommandProcessor(session, null);
        processor.processCommand(new Command(Command.Type.QUIT));

        assertEquals(new Response(Response.Type.BAD_SEQUENCE_OF_COMMANDS), session.response);
    }

    @Test
    public void processUSERCommand() throws Exception {
        SessionMock session = new SessionMock();
        CommandProcessor processor = new CommandProcessor(session, null);
        processor.processCommand(new Command(Command.Type.USER, "TestUser"));

        assertEquals("TestUser", session.user);
    }

    @Test
    public void processPASSCommand() throws Exception {
        SessionMock session = new SessionMock();
        CommandProcessor processor = new CommandProcessor(session, null);
        processor.processCommand(new Command(Command.Type.PASS, "pass"));

        assertEquals("pass", session.pass);
    }

    @Test
    public void processNLSTCommand() throws Exception {
        SessionMock session = new SessionMock();
        FilesystemMock filesystem = new FilesystemMock();
        CommandProcessor processor = new CommandProcessor(session, filesystem);
        processor.processCommand(new Command(Command.Type.NLST));

        assertEquals(filesystem.filenames, session.passiveConnection.sentData);
        assertEquals(new Response(Response.Type.OPENING_PASSIVE_CONNECTION, "ASCII", "/bin/ls"), session.response);

        filesystem.filenames = null;
        processor.processCommand(new Command(Command.Type.NLST));
        assertEquals(new Response(Response.Type.PERMISSION_DENIED), session.response);
    }


    private class SessionMock implements SessionInterface{
        public String user = null;
        public Response response = null;
        public String pass = null;
        public boolean authenticated = true;
        public PassiveConnectionMock passiveConnection = new PassiveConnectionMock();

        @Override
        public void run() {

        }

        @Override
        public void dataSent(boolean success) {

        }

        @Override
        public void quit() {

        }

        @Override
        public void listenForPassiveConnection() {

        }

        @Override
        public void fetchUser(String username) {
            user = username;
        }

        @Override
        public void validatePassword(String password) {
            pass = password;
        }

        @Override
        public void sendResponse(Response.Type type, Object... params) {
            sendResponse(new Response(type, params));
        }

        @Override
        public void sendResponse(Response response) {
            this.response = response;
        }

        @Override
        public boolean havePassiveConnection() {
            return true;
        }

        @Override
        public PassiveConnectionInterface getPassiveConnection() {
            return passiveConnection;
        }

        @Override
        public boolean isAuthenticated() {
            return authenticated;
        }

        @Override
        public void abortTransfer() {

        }

    }

    private class PassiveConnectionMock implements PassiveConnectionInterface{
        private String sentData;


        @Override
        public void sendData(String data) {
            this.sentData = data;
        }

        @Override
        public void receiveData(OutputStream out) {

        }

        @Override
        public void sendData(InputStream in) {

        }

        @Override
        public int getPort() {
            return 0;
        }

        @Override
        public boolean abort() {
            return false;
        }
    }

    private class FilesystemMock implements Filesystem{

        public String filenames = "testFile\r\n";

        @Override
        public String listFilesName(Path directory) {
            if(filenames == null) throw new PermissionDeniedException();
            return filenames;
        }

        @Override
        public List<Path> listFiles(Path path) {
            return null;
        }

        @Override
        public boolean isDirectory(Path path) {
            return false;
        }

        @Override
        public void createDir(Path path) {

        }

        @Override
        public void remove(Path path, boolean directory) {

        }

        @Override
        public InputStream getFile(Path path) {
            return null;
        }

        @Override
        public OutputStream storeFile(Path path, boolean append) {
            return null;
        }

        @Override
        public void changeDirectory(Path path) {

        }

        @Override
        public Path getCWD() {
            return null;
        }

        @Override
        public String getID() {
            return null;
        }

        @Override
        public void setPermissions(Path path, boolean userCanRead, boolean userCanWrite, boolean groupCanRead, boolean groupCanWrite) {

        }
    }
}