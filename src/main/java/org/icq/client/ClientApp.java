package org.icq.client;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;

public class ClientApp extends Application {

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) throws Exception {
        Stage dialogStage = new Stage();
        dialogStage.setTitle("Підключення до сервера");

        Label ipLabel = new Label("IP-адреса сервера:");
        TextField ipField = new TextField("localhost");
        Label portLabel = new Label("Порт:");
        TextField portField = new TextField("12345");
        Label usernameLabel = new Label("Ім'я користувача:");
        TextField usernameField = new TextField();

        Button connectButton = new Button("Підключитися");
        connectButton.setDefaultButton(true);

        GridPane grid = new GridPane();
        grid.setAlignment(Pos.CENTER);
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20));
        grid.add(ipLabel, 0, 0);
        grid.add(ipField, 1, 0);
        grid.add(portLabel, 0, 1);
        grid.add(portField, 1, 1);
        grid.add(usernameLabel, 0, 2);
        grid.add(usernameField, 1, 2);
        grid.add(connectButton, 1, 3);

        VBox dialogVBox = new VBox(20);
        dialogVBox.setAlignment(Pos.CENTER);
        dialogVBox.getChildren().add(grid);

        Scene dialogScene = new Scene(dialogVBox, 400, 250);
        dialogStage.setScene(dialogScene);

        connectButton.setOnAction(e -> {
            String ip = ipField.getText().trim();
            String portText = portField.getText().trim();
            String username = usernameField.getText().trim();

            if (ip.isEmpty() || portText.isEmpty() || username.isEmpty()) {
                showAlert("Помилка", "Усі поля мають бути заповнені!");
                return;
            }

            int port;
            try {
                port = Integer.parseInt(portText);
                if (port < 1 || port > 65535) {
                    showAlert("Помилка", "Порт має бути числом від 1 до 65535!");
                    return;
                }
            } catch (NumberFormatException ex) {
                showAlert("Помилка", "Порт має бути числом!");
                return;
            }

            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/ICQInterface.fxml"));
                Scene scene = new Scene(loader.load());
                stage.setTitle("ICQ Chat");
                stage.setScene(scene);

                ICQClientController controller = loader.getController();
                controller.initializeClient(ip, port, username);

                stage.show();
                dialogStage.close();
            } catch (Exception ex) {
                showAlert("Помилка", "Не вдалося підключитися до сервера: " + ex.getMessage());
            }
        });

        dialogStage.setOnCloseRequest(e -> System.exit(0));


        dialogStage.showAndWait();
    }


    private void showAlert(String title, String message) {
        Alert alert = new Alert(AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
