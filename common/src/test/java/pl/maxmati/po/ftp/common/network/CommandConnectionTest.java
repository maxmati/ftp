package pl.maxmati.po.ftp.common.network;

import org.junit.Test;
import pl.maxmati.po.ftp.common.Response;
import pl.maxmati.po.ftp.common.command.Command;

import static org.junit.Assert.*;

/**
 * Created by maxmati on 2/3/16
 */
public class CommandConnectionTest {

    @Test
    public void fetchCommand() throws Exception {
        final String data = "STOR test/file";
        CommandConnection connection = new CommandConnection(new SocketMock(data));
        final Command command = connection.fetchCommand();
        assertEquals(command, new Command(Command.Type.STOR, "test/file"));
    }

    @Test
    public void fetchCommandNONECommand() throws Exception {
        final String data = "fds test/file2";
        CommandConnection connection = new CommandConnection(new SocketMock(data));
        final Command command = connection.fetchCommand();
        assertEquals(new Command(Command.Type.NONE, "test/file2"), command);
    }

    @Test
    public void fetchCommandEndOfData() throws Exception {
        final String data = "";
        CommandConnection connection = new CommandConnection(new SocketMock(data));
        final Command command = connection.fetchCommand();
        assertNull(command);
    }


    @Test
    public void sendCommand() throws Exception {
        final SocketMock socketMock = new SocketMock("");
        final Command testCommand = new Command(Command.Type.APPE, "Test");
        CommandConnection connection = new CommandConnection(socketMock);
        connection.setOnCommandSentListener(command -> assertEquals(testCommand, command));
        connection.sendCommand(testCommand);
        assertEquals("APPE Test\r\n", socketMock.getOutputData());
    }

    @Test
    public void fetchResponse() throws Exception {
        final SocketMock socketMock = new SocketMock("257 \"dir\" is current directory\r\n");
        CommandConnection connection = new CommandConnection(socketMock);
        final Response response = connection.fetchResponse();

        assertEquals(new Response(Response.Type.CURRENT_DIRECTORY, "dir") ,response);
    }

    @Test
    public void fetchResponseEndOfData() throws Exception {
        final SocketMock socketMock = new SocketMock("");
        CommandConnection connection = new CommandConnection(socketMock);
        final Response response = connection.fetchResponse();

        assertNull(response);
    }

    @Test
    public void sendResponse() throws Exception {
        final SocketMock socketMock = new SocketMock("");
        CommandConnection connection = new CommandConnection(socketMock);

        connection.sendResponse(new Response(Response.Type.BYE));

        assertEquals("221 Bye\r\n", socketMock.getOutputData());
    }

    @Test
    public void close() throws Exception {
        final SocketMock socketMock = new SocketMock("");
        CommandConnection connection = new CommandConnection(socketMock);

        assertFalse(connection.isClosed());
        connection.close();
        assertTrue(connection.isClosed());
    }
}