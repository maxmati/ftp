package pl.maxmati.po.ftp.client;

import javafx.scene.control.Alert;
import pl.maxmati.ftp.common.exceptions.*;

/**
 * Created by maxmati on 2/2/16
 */
public class Dialogs {
    public static void showErrorDialog(FilesystemException e) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText("Error processing your request");
        if(e instanceof DirectoryNotEmptyException){
            alert.setContentText("Specified directory isn't empty.");
        } else if( e instanceof FileAlreadyExistsException) {
            alert.setContentText("Specified file already exists.");
        } else if( e instanceof NoSuchFileException) {
            alert.setContentText("File not found");
        } else if( e instanceof NotDirectoryException) {
            alert.setContentText("Specified file isn't directory");
        } else if( e instanceof NotRegularFileException ) {
            alert.setContentText("specified file isn't a regular file");
        } else if( e instanceof PermissionDeniedException ) {
            alert.setContentText("You don't have permissions to access this file or directory.");
        } else {
            alert.setContentText("Something went wrong.");
        }

        alert.show();
    }
}
