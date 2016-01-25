package pl.maxmati.po.ftp.server.gui;
/**
 * Created by maxmati on 1/22/16
 */

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;
import pl.maxmati.po.ftp.server.Config;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Paths;

public class GUIMain extends Application {

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        Config.load(Paths.get("FTPConfig.conf"));
        URL location = getClass().getResource("/layouts/server.fxml");
        FXMLLoader fxmlLoader = new FXMLLoader(location);

        try {
            Pane root = fxmlLoader.load();

//            MainController mainController = fxmlLoader.getController();
//            mainController.setExecutor(executor);
//            mainController.setLocalFilesystem(new LocalFilesystem(Paths.get("/home/maxmati")));
//            mainController.setEventDispatcher(eventDispatcher);

            primaryStage.setScene(new Scene(root));
            primaryStage.show();


        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
