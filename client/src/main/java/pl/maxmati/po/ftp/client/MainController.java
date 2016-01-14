package pl.maxmati.po.ftp.client;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.control.TreeView;
import pl.maxmati.ftp.common.filesystem.Filesystem;
import pl.maxmati.po.ftp.client.events.ConnectEvent;
import pl.maxmati.po.ftp.client.events.Event;
import pl.maxmati.po.ftp.client.events.EventDispatcher;
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

    @FXML private TextField serverAddress;
    @FXML private TextField serverPort;

    @FXML private Button connectButton;
    @FXML private TreeView<FileEntry> localTree;

    private boolean connected = false;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
    }

    public void setEventDispatcher(EventDispatcher dispatcher){
        this.dispatcher = dispatcher;
        dispatcher.registerListener(ConnectEvent.class, this::onConnectEvent);
    }

    public void setFilesystem(Filesystem filesystem){
        localFsTree.init(filesystem, localTree);
    }


    private void onConnectEvent(Event event){
        ConnectEvent connectEvent = (ConnectEvent) event;

        switch (connectEvent.getType()){
            case REQUEST_CONNECTION:
                connectButton.setDisable(true);
                connectButton.setText("Connecting");
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
                serverAddress.setDisable(false);
                serverPort.setDisable(false);
                connected = false;
                break;
        }
    }

    @FXML private void onConnectButtonClicked(){
        final String hostname = serverAddress.getText();
        final Integer port = Integer.valueOf(serverPort.getText());

        if(!connected)
            dispatcher.dispatch(new ConnectEvent(ConnectEvent.Type.REQUEST_CONNECTION, hostname, port));
        else
            dispatcher.dispatch(new ConnectEvent(ConnectEvent.Type.REQUEST_DISCONNECT));
    }

}
