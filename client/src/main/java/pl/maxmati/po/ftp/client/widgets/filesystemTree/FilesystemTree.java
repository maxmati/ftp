package pl.maxmati.po.ftp.client.widgets.filesystemTree;

import javafx.application.Platform;
import javafx.scene.control.*;
import javafx.scene.effect.InnerShadow;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.paint.Color;
import pl.maxmati.ftp.common.exceptions.FilesystemException;
import pl.maxmati.ftp.common.filesystem.Filesystem;
import pl.maxmati.po.ftp.client.Dialogs;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
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

        TreeItem<FileEntry> item = new TreeItem<FileEntry> (new FileEntry(path, isDirectory)){

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

        private final ContextMenu fileMenu = new ContextMenu();
        private final ContextMenu dirMenu = new ContextMenu();
        private final MenuItem removeDir;

        public FileEntryCell(Filesystem filesystem, ExecutorService executor) {

            MenuItem removeFile = new MenuItem("Remove");
            removeFile.setOnAction(event -> {
                try {
                    filesystem.remove(getTreeItem().getValue().getPath(), false);
                    refresh(filesystem, getTreeItem().getParent());
                } catch (FilesystemException e){
                    Dialogs.showErrorDialog(e);
                }
            });
            fileMenu.getItems().add(removeFile);

            removeDir = new MenuItem("Remove");
            removeDir.setOnAction(event -> {
                try {
                    filesystem.remove(getTreeItem().getValue().getPath(), true);
                    refresh(filesystem, getTreeItem().getParent());
                } catch (FilesystemException e){
                    Dialogs.showErrorDialog(e);
                }
            });
            dirMenu.getItems().add(removeDir);

            MenuItem createDir = new MenuItem("Create directory");
            createDir.setOnAction(event -> {
                TextInputDialog dialog = new TextInputDialog("New_folder");
                dialog.setTitle("Creating new folder");
                dialog.setHeaderText("What name?");
                dialog.setContentText("Please enter new folder name:");

                Optional<String> result = dialog.showAndWait();
                if (result.isPresent()){
                    try {
                        filesystem.createDir(getItem().getPath().resolve(result.get()));
                        refresh(filesystem, getTreeItem());
                        getTreeItem().setExpanded(true);
                    } catch (FilesystemException e){
                        Dialogs.showErrorDialog(e);
                    }
                }
            });

            dirMenu.getItems().add(createDir);



            dirMenu.setOnShown(e -> {
                if(!getItem().isPopulated()) {
                    try {
                        populateDir(filesystem, getItem().getPath(), getTreeItem());
                    }catch (FilesystemException ignore){}
                }

                removeDir.setDisable(!getTreeItem().getChildren().isEmpty());
            });

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

                try {
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
                        refresh(dstT.filesystem, dstItem);
                    });
                } catch (FilesystemException e){
                    Dialogs.showErrorDialog(e);
                }

                event.setDropCompleted(true);
            });
        }

        private void refresh(Filesystem filesystem, TreeItem<FileEntry> dstItem) {
            dstItem.getChildren().clear();
            populateDir(filesystem, dstItem.getValue().getPath(), dstItem);
        }

        @Override
        protected void updateItem(FileEntry item, boolean empty) {
            super.updateItem(item, empty);

            if(!empty) {
                setText(item.toString());

                if(!item.isDirectory())
                    setContextMenu(fileMenu);
                else
                    setContextMenu(dirMenu);

            } else {
                setText(null);
            }
        }
    }
}
