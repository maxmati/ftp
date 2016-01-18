package pl.maxmati.po.ftp.client;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import pl.maxmati.ftp.common.command.Command;
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

    @FXML private TextArea commandChannelHistory;
    @FXML private TextField serverUsername;
    @FXML private PasswordField serverPassword;

    @FXML private TextField serverAddress;


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
                    connectButton.setDisable(false);
                    connectButton.setText("Disconnect");
                    connected = true;
                    break;
                case REQUEST_DISCONNECT:
                    connectButton.setDisable(true);
                    connectButton.setText("Disconnecting");
                    break;
                case DISCONNECTED:
                    connectButton.setDisable(false);
                    connectButton.setText("Connect");
                    serverUsername.setDisable(false);
                    serverPassword.setDisable(false);
                    serverAddress.setDisable(false);
                    serverPort.setDisable(false);
                    connected = false;
                    break;
            }
        });
    }

    private void appendToHistory(String data) {
        commandChannelHistory.appendText(data);
    }

    @FXML private void onConnectButtonClicked(){
        final String username = serverUsername.getText();
        final String password = serverPassword.getText();
        final String hostname = serverAddress.getText();
        final Integer port = Integer.valueOf(serverPort.getText());

        if(!connected)
            dispatcher.dispatch(new ConnectEvent(ConnectEvent.Type.REQUEST_CONNECTION, hostname, port, username, password));
        else
            dispatcher.dispatch(new ConnectEvent(ConnectEvent.Type.REQUEST_DISCONNECT));
    }

    public void sendRawCommand(ActionEvent actionEvent) {
        final String[] tokens = rawFTPCommand.getText().split(" ");
        final Command.Type type = Command.Type.valueOf(tokens[0]);
        final String[] params = Arrays.copyOfRange(tokens, 1, tokens.length);
        rawFTPCommand.setText("");
        dispatcher.dispatch(new CommandEvent(CommandEvent.Type.REQUEST, new Command(type, params)));
    }
}
