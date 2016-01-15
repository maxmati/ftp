package pl.maxmati.po.ftp.client;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import pl.maxmati.ftp.common.filesystem.Filesystem;
import pl.maxmati.po.ftp.client.events.*;
import pl.maxmati.po.ftp.client.widgets.filesystemTree.FileEntry;
import pl.maxmati.po.ftp.client.widgets.filesystemTree.FilesystemTree;

import java.net.URL;
import java.util.ResourceBundle;

/**
 * Created by maxmati on 1/12/16
 */
public class MainController implements Initializable {
    private EventDispatcher dispatcher = null;
    private FilesystemTree localFsTree = new FilesystemTree();

    @FXML private TextArea commandChannelHistory;

    @FXML private TextField serverUsername;
    @FXML private PasswordField serverPassword;
    @FXML private TextField serverAddress;
    @FXML private TextField serverPort;

    @FXML private Button connectButton;
    @FXML private TreeView<FileEntry> localTree;

    private boolean connected = false;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        commandChannelHistory.setEditable(false);
    }

    public void setFilesystem(Filesystem filesystem){
        localFsTree.init(filesystem, localTree);
    }

    public void setEventDispatcher(EventDispatcher dispatcher){
        this.dispatcher = dispatcher;
        dispatcher.registerListener(ConnectEvent.class, this::onConnectEvent);
        dispatcher.registerListener(CommandEvent.class, this::onCommandEvent);
        dispatcher.registerListener(ResponseEvent.class, this::onResponseEvent);
    }

    private void onCommandEvent(Event event){
        Platform.runLater(() -> {
            CommandEvent commandEvent = (CommandEvent) event;
            appendToHistory("Command: " + commandEvent.getCommand().toNetworkString());
        });
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
        commandChannelHistory.setText(commandChannelHistory.getText() + data);
        commandChannelHistory.setScrollTop(Double.MAX_VALUE);
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

}
