package pl.maxmati.po.ftp.client.widgets.filesystemTree;

import javafx.application.Platform;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import pl.maxmati.ftp.common.exceptions.FilesystemException;
import pl.maxmati.ftp.common.filesystem.Filesystem;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Created by maxmati on 1/13/16
 */
public class FilesystemTree {
    private Filesystem filesystem = null;

    public void init(Filesystem filesystem, TreeView<FileEntry> treeView){
        this.filesystem = filesystem;

        Platform.runLater(() -> {
            Path rootPath = Paths.get("/");
            TreeItem<FileEntry> rootItem = createTreeItem(rootPath, true);
            populateDir(filesystem, rootPath, rootItem, true);

            treeView.setRoot(rootItem);
        });
    }

    private void populateDir(Filesystem filesystem, Path path, TreeItem<FileEntry> parent, boolean recursive) {
        if (parent.getValue().isPopulated())
            return;
        parent.getValue().setPopulated(true);

        for (Path file : filesystem.listFiles(path)){

            TreeItem<FileEntry> item = createTreeItem(file, filesystem.getCWD().startsWith(file));

            parent.getChildren().add(item);
            if(recursive && filesystem.isDirectory(file))
                try {
                    populateDir(filesystem, file, item, filesystem.getCWD().startsWith(file));
                } catch (FilesystemException ignored){}
        }
    }

    private TreeItem<FileEntry> createTreeItem(Path path, boolean expanded){
        TreeItem<FileEntry> item = new TreeItem<> (new FileEntry(path));
        item.setExpanded(expanded);

        item.expandedProperty().addListener((observable, oldValue, newValue) -> {
           if(newValue)
               populateDir(filesystem, item.getValue().getPath(), item, true);
        });


        return item;
    }
}
