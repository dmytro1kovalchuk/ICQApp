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
import java.io.*;
import javax.xml.parsers.*;
import org.w3c.dom.*;

public class ClientApp extends Application {
    private Label errorLabel;
    private Stage dialogStage;
    private VBox dialogVBox;
    private TextField ipField, portField, usernameField;
    private ICQClient client;
    private PrintWriter out;
    private BufferedReader in;
    private boolean isUsernameValid = false;
    private String username;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) throws Exception {
        dialogStage = new Stage();
        dialogStage.setTitle("Підключення до сервера");

        Label ipLabel = new Label("IP-адреса сервера:");
        ipField = new TextField("localhost");
        Label portLabel = new Label("Порт:");
        portField = new TextField("12345");
        Label usernameLabel = new Label("Ім'я користувача:");
        usernameField = new TextField();
        errorLabel = new Label();
        errorLabel.setStyle("-fx-text-fill: red;");

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
        grid.add(errorLabel, 0, 3, 2, 1);
        grid.add(connectButton, 1, 4);

        dialogVBox = new VBox(20);
        dialogVBox.setAlignment(Pos.CENTER);
        dialogVBox.getChildren().add(grid);

        Scene dialogScene = new Scene(dialogVBox, 400, 300);
        dialogStage.setScene(dialogScene);

        connectButton.setOnAction(e -> {
            String ip = ipField.getText().trim();
            String portText = portField.getText().trim();
            String usernameInput = usernameField.getText().trim();

            if (ip.isEmpty() || portText.isEmpty() || usernameInput.isEmpty()) {
                errorLabel.setText("Усі поля мають бути заповнені!");
                return;
            }

            int port;
            try {
                port = Integer.parseInt(portText);
                if (port < 1 || port > 65535) {
                    errorLabel.setText("Порт має бути числом від 1 до 65535!");
                    return;
                }
            } catch (NumberFormatException ex) {
                errorLabel.setText("Порт має бути числом!");
                return;
            }

            try {
                if (client == null || client.getSocket().isClosed()) {
                    client = new ICQClient(ip, port);
                    out = client.getOut();
                    in = client.getIn();
                }
                this.username = usernameInput;
                out.println("<connect username=\"" + username + "\"/>");
                String response = in.readLine();
                if (response == null) {
                    errorLabel.setText("Сервер не відповідає!");
                    client.close();
                    client = null;
                    return;
                }

                Document doc = parseXML(response);
                String rootTag = doc.getDocumentElement().getTagName();
                if (rootTag.equals("error")) {
                    errorLabel.setText(doc.getDocumentElement().getTextContent());
                    isUsernameValid = false;
                } else if (rootTag.equals("connect")) {
                    isUsernameValid = true;
                    FXMLLoader loader = new FXMLLoader(getClass().getResource("/ICQInterface.fxml"));
                    Scene scene = new Scene(loader.load());
                    stage.setTitle("ICQ Chat");
                    stage.setScene(scene);

                    ICQClientController controller = loader.getController();
                    controller.initializeClient(ip, port, username, client, in, out);

                    // Надсилання запиту на відключення при натисканні на хрестик основної сцени
                    stage.setOnCloseRequest(event -> { // Змінено 'e' на 'event'
                        if (client != null && !client.getSocket().isClosed()) {
                            out.println("<disconnect username=\"" + username + "\"/>");
                            try {
                                client.close();
                            } catch (IOException ex) {
                                System.err.println("Помилка закриття з'єднання: " + ex.getMessage());
                            }
                        }
                        System.exit(0);
                    });

                    stage.show();
                    dialogStage.close();
                }
            } catch (Exception ex) {
                errorLabel.setText("Не вдалося підключитися до сервера: " + ex.getMessage());
                isUsernameValid = false;
                try {
                    if (client != null) client.close();
                    client = null;
                } catch (IOException ioEx) {
                    errorLabel.setText("Помилка закриття з'єднання: " + ioEx.getMessage());
                }
            }
        });

        // Надсилання запиту на відключення при натисканні на хрестик вікна логіну
        dialogStage.setOnCloseRequest(e -> {
            if (client != null && !client.getSocket().isClosed()) {
                if (username != null && isUsernameValid) {
                    out.println("<disconnect username=\"" + username + "\"/>");
                }
                try {
                    client.close();
                } catch (IOException ex) {
                    System.err.println("Помилка закриття з'єднання: " + ex.getMessage());
                }
            }
            System.exit(0);
        });
        dialogStage.showAndWait();
    }

    private Document parseXML(String xml) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        DocumentBuilder builder = factory.newDocumentBuilder();
        return builder.parse(new ByteArrayInputStream(xml.getBytes("UTF-8")));
    }
}