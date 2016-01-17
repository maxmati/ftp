package pl.maxmati.po.ftp.client;

/**
 * Created by maxmati on 1/12/16
 */

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;
import pl.maxmati.ftp.common.filesystem.LocalFilesystem;
import pl.maxmati.po.ftp.client.events.ConnectEvent;
import pl.maxmati.po.ftp.client.events.EventDispatcher;
import pl.maxmati.po.ftp.client.filesystem.FTPFilesystem;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Paths;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Main extends Application {


    private final ExecutorService executor = Executors.newCachedThreadPool();
    private final EventDispatcher eventDispatcher = new EventDispatcher(executor);
    private final SessionManager sessionManager = new SessionManager(eventDispatcher, executor);
    private final FTPFilesystem ftpFilesystem = new FTPFilesystem(sessionManager.getSession(), eventDispatcher);

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {



        URL location = getClass().getResource("/layouts/main.fxml");
        FXMLLoader fxmlLoader = new FXMLLoader(location);

        try {
            Pane root = fxmlLoader.load();

            MainController mainController = fxmlLoader.getController();
            mainController.setLocalFilesystem(new LocalFilesystem(Paths.get("/home/maxmati")));
            mainController.setEventDispatcher(eventDispatcher);

            primaryStage.setScene(new Scene(root));
            primaryStage.show();

            eventDispatcher.registerListener(ConnectEvent.class, event -> {
                if( ((ConnectEvent)event).getType() == ConnectEvent.Type.CONNECTED ){
                    mainController.setRemoteFilesystem(ftpFilesystem);
//                    System.out.println(ftpFilesystem.listFilesName(Paths.get("/home/maxmati/tmp")));
                }
            });

        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}
