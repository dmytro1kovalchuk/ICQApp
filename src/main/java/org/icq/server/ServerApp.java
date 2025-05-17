package org.icq.server;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class ServerApp extends Application {

    private ICQServer server;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/ServerInterface.fxml"));
        Scene scene = new Scene(loader.load());
        stage.setTitle("ICQ Server");
        stage.setScene(scene);

        ServerController controller = loader.getController();

        server = new ICQServer(12345, controller);
        new Thread(server).start();

        stage.setOnCloseRequest(e -> {
            server.shutdown();
            System.exit(0);
        });

        stage.show();
    }
}