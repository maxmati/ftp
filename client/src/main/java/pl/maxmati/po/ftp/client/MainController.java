package pl.maxmati.po.ftp.client;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import pl.maxmati.ftp.common.command.Command;
import pl.maxmati.ftp.common.exceptions.FilesystemException;
import pl.maxmati.ftp.common.filesystem.Filesystem;
import pl.maxmati.po.ftp.client.events.*;
import pl.maxmati.po.ftp.client.widgets.filesystemTree.FilesystemTree;

import java.net.URL;
import java.util.Arrays;
import java.util.ResourceBundle;
import java.util.concurrent.ExecutorService;

/**
 * Created by maxmati on 1/12/16
 */
public class MainController implements Initializable {
    private ExecutorService executor = null;
    private EventDispatcher dispatcher = null;
    private MetaCommandExecutor commandExecutor = null;

    @FXML private TextArea commandChannelHistory;
    @FXML private TextField serverUsername;
    @FXML private PasswordField serverPassword;

    @FXML private TextField serverAddress;

    @FXML private TextField FTPCommand;

    @FXML private TextField serverPort;
    @FXML private TextField rawFTPCommand;

    @FXML private Button connectButton;
    @FXML private FilesystemTree localTree;

    @FXML private FilesystemTree remoteTree;
    private boolean connected = false;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        commandChannelHistory.setEditable(false);
    }

    public void setExecutor(ExecutorService executor) {
        this.executor = executor;
    }

    public void setLocalFilesystem(Filesystem filesystem){
        localTree.init(filesystem, executor);
    }

    public void setRemoteFilesystem(Filesystem filesystem) {
        remoteTree.init(filesystem, executor);
    }

    public void setMetaCommandExecutor(MetaCommandExecutor commandExecutor){
        this.commandExecutor = commandExecutor;
    }

    public void setEventDispatcher(EventDispatcher dispatcher){
        this.dispatcher = dispatcher;
        dispatcher.registerListener(ConnectEvent.class, this::onConnectEvent);
        dispatcher.registerListener(CommandEvent.class, this::onCommandEvent);
        dispatcher.registerListener(ResponseEvent.class, this::onResponseEvent);
    }

    private void onCommandEvent(Event event){
        CommandEvent commandEvent = (CommandEvent) event;
        if(commandEvent.getType() == CommandEvent.Type.PERFORMED)
            Platform.runLater( () ->
                    appendToHistory( "Command: " + commandEvent.getCommand().toNetworkString() )
            );
    }

    private void onResponseEvent(Event event){
        Platform.runLater(() -> {
            ResponseEvent responseEvent = (ResponseEvent) event;
            appendToHistory("Response: " + responseEvent.getResponse().toNetworkString());
        });
    }

    private void onConnectEvent(Event event){
        Platform.runLater(() -> {
            ConnectEvent connectEvent = (ConnectEvent) event;

            switch (connectEvent.getType()) {
                case REQUEST_CONNECTION:
                    connectButton.setDisable(true);
                    connectButton.setText("Connecting");
                    serverUsername.setDisable(true);
                    serverPassword.setDisable(true);
                    serverAddress.setDisable(true);
                    serverPort.setDisable(true);
                    break;
                case CONNECTED:
                    rawFTPCommand.setDisable(false);
                    FTPCommand.setDisable(false);
                    connectButton.setDisable(false);
                    connectButton.setText("Disconnect");
                    connected = true;
                    break;
                case REQUEST_DISCONNECT:
                    connectButton.setDisable(true);
                    connectButton.setText("Disconnecting");
                    break;
                case ERROR_BAD_PASS:
                    showError("Unable to authenticate", "You probably specified wrong username or password");
                    break;
                case ERROR_UNABLE_CONNECT:
                    showError("Unable to connect", "Program was unable to connect with FTP server");
                case DISCONNECTED:
                    rawFTPCommand.setDisable(true);
                    FTPCommand.setDisable(true);
                    connectButton.setDisable(false);
                    connectButton.setText("Connect");
                    serverUsername.setDisable(false);
                    serverPassword.setDisable(false);
                    serverAddress.setDisable(false);
                    serverPort.setDisable(false);
                    remoteTree.clear();
                    connected = false;
                    break;
            }
        });
    }

    private void showError(String title, String description) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(title);
        alert.setContentText(description);
        alert.show();
    }

    private void appendToHistory(String data) {
        commandChannelHistory.appendText(data);
    }

    @FXML private void onConnectButtonClicked(){
        final String username = serverUsername.getText();
        final String password = serverPassword.getText();
        final String hostname = serverAddress.getText();
        final String portText = serverPort.getText();

        if(username.isEmpty() || password.isEmpty() || hostname.isEmpty() || portText.isEmpty())
            return;

        final Integer port = Integer.valueOf(portText);

        if(!connected)
            dispatcher.dispatch(new ConnectEvent(ConnectEvent.Type.REQUEST_CONNECTION, hostname, port, username, password));
        else
            dispatcher.dispatch(new ConnectEvent(ConnectEvent.Type.REQUEST_DISCONNECT));
    }

    public void sendRawCommand() {
        final String[] tokens = rawFTPCommand.getText().split(" ");
        try {
            final Command.Type type = Command.Type.valueOf(tokens[0]);
            rawFTPCommand.setText("");
            final String[] params = Arrays.copyOfRange(tokens, 1, tokens.length);
            dispatcher.dispatch(new CommandEvent(CommandEvent.Type.REQUEST, new Command(type, params)));
        } catch (IllegalArgumentException e){
            e.printStackTrace();
        }
    }

    public void onFTPCommand() {
        String command = FTPCommand.getText();
        if(command.isEmpty())
            return;

        executor.execute(() -> {
            try {
                commandExecutor.executeCommand(command);
            } catch (FilesystemException e) {
                Platform.runLater(() -> Dialogs.showErrorDialog(e) );
            }
        });
        FTPCommand.clear();

    }
}
