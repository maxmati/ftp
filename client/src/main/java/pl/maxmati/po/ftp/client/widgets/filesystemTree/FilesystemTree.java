package pl.maxmati.po.ftp.client.widgets.filesystemTree;

import javafx.application.Platform;
import javafx.scene.control.TreeCell;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.effect.InnerShadow;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.paint.Color;
import pl.maxmati.ftp.common.exceptions.FilesystemException;
import pl.maxmati.ftp.common.filesystem.Filesystem;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.ExecutorService;

/**
 * Created by maxmati on 1/13/16
 */
public class FilesystemTree extends TreeView<FileEntry>{
    private Filesystem filesystem = null;

    public void init(Filesystem filesystem, ExecutorService executor){
        this.filesystem = filesystem;


        this.setCellFactory(p -> new FileEntryCell(filesystem, executor));

        Path rootPath = Paths.get("/");
        TreeItem<FileEntry> rootItem = createTreeItem(rootPath, true, true);
        Platform.runLater(() -> this.setRoot(rootItem));
        populateDir(filesystem, rootPath, rootItem);
    }

    private void populateDir(Filesystem filesystem, Path path, TreeItem<FileEntry> parent) {
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
           if(newValue && !item.getValue().isPopulated())
               populateDir(filesystem, item.getValue().getPath(), item);
        });

        return item;
    }

    public void clear() {
        setRoot(null);
    }

    private final class FileEntryCell extends TreeCell<FileEntry> {

        public FileEntryCell(Filesystem filesystem, ExecutorService executor) {
            setOnDragDetected(event -> {
                ClipboardContent content;

                content = new ClipboardContent();
                content.putString(filesystem.getID() + ":" + getTreeItem().getValue().getPath().toString());

                Dragboard dragboard;

                dragboard = getTreeView().startDragAndDrop(TransferMode.COPY);
                dragboard.setContent(content);

                event.consume();
            });
            setOnDragOver(event -> {
                final boolean sameFilesystems =
                        event.getDragboard().getString().split(":")[0].equals(filesystem.getID());
                if(sameFilesystems)
                    return;
                if(getTreeItem() == null || getTreeItem().isLeaf())
                    return;

                InnerShadow shadow;

                shadow = new InnerShadow();
                shadow.setOffsetX(1.0);
                shadow.setColor(Color.web("#666666"));
                shadow.setOffsetY(1.0);
                setEffect(shadow);


                event.acceptTransferModes(TransferMode.COPY);
            });
            setOnDragExited(event -> setEffect(null));

            setOnDragDropped(event -> {
                final FileEntryCell dstCell = (FileEntryCell) event.getGestureTarget();

                final FilesystemTree srcT = (FilesystemTree) event.getGestureSource();
                final FilesystemTree dstT = (FilesystemTree) dstCell.getTreeView();

                final Path srcFile = Paths.get(event.getDragboard().getString().split(":")[1]);
                final Path dstFile = getTreeItem().getValue().getPath().resolve(srcFile.getFileName());

                InputStream inputStream = srcT.filesystem.getFile(srcFile);
                OutputStream outputStream = dstT.filesystem.storeFile(dstFile, false);


                executor.execute(() -> {
                    int n;
                    byte[] buffer = new byte[1024];
                    try {
                        while ((n = inputStream.read(buffer)) > -1) {
                            outputStream.write(buffer, 0, n);
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    } finally {
                        try {
                            inputStream.close();
                            outputStream.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }

                    }

                    TreeItem<FileEntry> dstItem = dstCell.getTreeItem();
                    dstItem.getChildren().clear();
                    populateDir(dstT.filesystem, dstItem.getValue().getPath(), dstItem);
                });

                event.setDropCompleted(true);
            });
        }

        @Override
        protected void updateItem(FileEntry item, boolean empty) {
            super.updateItem(item, empty);

            if(!empty) {
                setText(item.toString());
            } else {
                setText(null);
            }
        }
    }
}
