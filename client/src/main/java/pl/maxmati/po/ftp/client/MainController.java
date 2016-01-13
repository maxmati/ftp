package pl.maxmati.po.ftp.client;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.TreeView;
import pl.maxmati.ftp.common.filesystem.Filesystem;

import java.net.URL;
import java.util.ResourceBundle;

/**
 * Created by maxmati on 1/12/16
 */
public class MainController implements Initializable {
    private FilesystemTree localFsTree = new FilesystemTree();

    @FXML private TreeView<FileEntry> localTree;


    @Override
    public void initialize(URL location, ResourceBundle resources) {

    }

    public void setFilesystem(Filesystem filesystem){
        localFsTree.init(filesystem, localTree);

    }

}
