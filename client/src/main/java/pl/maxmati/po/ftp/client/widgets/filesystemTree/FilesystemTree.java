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


        Path rootPath = Paths.get("/");
        TreeItem<FileEntry> rootItem = createTreeItem(rootPath, true, true);
        populateDir(filesystem, rootPath, rootItem);
        Platform.runLater(() -> treeView.setRoot(rootItem));
    }

    private void populateDir(Filesystem filesystem, Path path, TreeItem<FileEntry> parent) {
        if (parent.getValue().isPopulated())
            return;
        parent.getValue().setPopulated(true);

        for (Path file : filesystem.listFiles(path)){
            final boolean layInCWDPath = filesystem.getCWD().startsWith(file);
            final boolean isDirectory = filesystem.isDirectory(file);

            TreeItem<FileEntry> item = createTreeItem(file, layInCWDPath, isDirectory);

            Platform.runLater(() -> parent.getChildren().add(item));
            if(layInCWDPath && isDirectory)
                try {
                    populateDir(filesystem, file, item);
                } catch (FilesystemException ignored){}
        }
    }

    private TreeItem<FileEntry> createTreeItem(Path path, boolean expanded, boolean isDirectory){
        TreeItem<FileEntry> item = new TreeItem<FileEntry> (new FileEntry(path)){
            @Override
            public boolean isLeaf() {
                return !isDirectory;
            }
        };
        item.setExpanded(expanded);

        item.expandedProperty().addListener((observable, oldValue, newValue) -> {
           if(newValue)
               populateDir(filesystem, item.getValue().getPath(), item);
        });


        return item;
    }
}
