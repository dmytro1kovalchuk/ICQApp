package org.icq.server;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;
import java.util.*;

public class ServerController {
    @FXML
    private TextArea logArea;
    @FXML
    private ListView<String> sessionList;
    private Map<String, List<String>> dialogHistory;
    private String selectedDialog;

    public ServerController() {
        dialogHistory = new HashMap<>();
    }

    public void initialize() {
        sessionList.setOnMouseClicked(e -> {
            String selected = sessionList.getSelectionModel().getSelectedItem();
            if (selected != null) {
                selectedDialog = selected;
                updateLogArea();
            }
        });
    }

    public synchronized void addMessage(String from, String to, String message) {
        String dialogKey = getDialogKey(from, to);
        String formattedMessage = "[від " + from + " до " + to + "]: " + message + "\n";
        dialogHistory.computeIfAbsent(dialogKey, k -> new ArrayList<>()).add(formattedMessage);

        Platform.runLater(() -> {
            if (!sessionList.getItems().contains(dialogKey)) {
                sessionList.getItems().add(dialogKey);
            }
            if (dialogKey.equals(selectedDialog)) {
                logArea.appendText(formattedMessage);
            }
        });
    }

    public synchronized void addDisconnect(String username) {
        String message = "❌ Користувач " + username + " покинув чат\n";
        for (String dialogKey : dialogHistory.keySet()) {
            if (dialogKey.contains(username)) {
                dialogHistory.get(dialogKey).add(message);
                if (dialogKey.equals(selectedDialog)) {
                    Platform.runLater(() -> logArea.appendText(message));
                }
            }
        }
    }

    private void updateLogArea() {
        logArea.clear();
        if (selectedDialog != null) {
            List<String> messages = dialogHistory.getOrDefault(selectedDialog, new ArrayList<>());
            for (String message : messages) {
                logArea.appendText(message);
            }
        }
    }

    private String getDialogKey(String user1, String user2) {
        return user1.compareTo(user2) < 0 ? user1 + " ↔ " + user2 : user2 + " ↔ " + user1;
    }
}