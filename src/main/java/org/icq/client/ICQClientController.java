package org.icq.client;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import java.io.*;
import java.util.*;
import javax.xml.parsers.*;
import org.w3c.dom.*;

public class ICQClientController {
    @FXML private TextArea chatArea;
    @FXML private TextField inputField;
    @FXML private TextField searchField;
    @FXML private ListView<String> userList;
    @FXML private Button sendButton;
    @FXML private Button closeButton;
    @FXML private Label userName;
    @FXML private Label targetName;
    private PrintWriter out;
    private BufferedReader in;
    private ICQClient client;
    private String username;
    private String selectedUser;
    private Map<String, List<String>> messageHistory;
    private Document lastUserListDoc;

    public ICQClientController() {
        messageHistory = new HashMap<>();
    }

    @FXML
    void handleSend() {
        String text = inputField.getText().trim();

        if (selectedUser == null) {
            chatArea.appendText("❌ Оберіть користувача зі списку\n");
            return;
        }

        if (!text.isEmpty()) {
            out.println("<message from=\"" + username + "\" to=\"" + selectedUser + "\">" + text + "</message>");
            inputField.clear();
        }
    }

    @FXML
    void handleClose() {
        try {
            out.println("<disconnect username=\"" + username + "\"/>");
            client.close();
            Platform.exit();
        } catch (IOException e) {
            chatArea.appendText("❌ Помилка при відключенні: " + e.getMessage() + "\n");
        }
    }

    public void initializeClient(String serverAddress, int port, String username, ICQClient client, BufferedReader in, PrintWriter out) throws IOException {
        this.username = username;
        this.client = client;
        this.out = out;
        this.in = in;
        targetName.setText("");
        userName.setText(username);

        searchField.textProperty().addListener((obs, oldValue, newValue) -> {
            updateUserListWithFilter(newValue);
        });

        new Thread(() -> {
            try {
                String xmlMessage;
                while ((xmlMessage = client.readLine()) != null) {
                    Document doc = parseXML(xmlMessage);
                    String rootTag = doc.getDocumentElement().getTagName();
                    Platform.runLater(() -> {
                        if (rootTag.equals("userlist")) {
                            updateUserList(doc);
                        } else if (rootTag.equals("error")) {
                            chatArea.appendText("❌ " + doc.getDocumentElement().getTextContent() + "\n");
                        } else if (rootTag.equals("connect")) {
                            chatArea.appendText("✅ " + doc.getDocumentElement().getTextContent() + "\n");
                        } else if (rootTag.equals("message")) {
                            String from = doc.getDocumentElement().getAttribute("from");
                            String to = doc.getDocumentElement().getAttribute("to");
                            String text = doc.getDocumentElement().getTextContent();
                            if (to.equals(username)) {
                                String message = "📩 [від " + from + "]: " + text + "\n";
                                messageHistory.computeIfAbsent(from, k -> new ArrayList<>()).add(message);
                                if (selectedUser != null && from.equals(selectedUser)) {
                                    chatArea.appendText(message);
                                }
                            }
                        } else if (rootTag.equals("sent")) {
                            String to = doc.getDocumentElement().getAttribute("to");
                            String text = doc.getDocumentElement().getTextContent();
                            String message = "📤 [до " + to + "]: " + text + "\n";
                            messageHistory.computeIfAbsent(to, k -> new ArrayList<>()).add(message);
                            if (selectedUser != null && to.equals(selectedUser)) {
                                chatArea.appendText(message);
                            }
                        } else if (rootTag.equals("user_left")) {
                            String leftUsername = doc.getDocumentElement().getAttribute("username");
                            String message = "❌ Користувач " + leftUsername + " покинув чат\n";
                            messageHistory.computeIfAbsent(leftUsername, k -> new ArrayList<>()).add(message);
                            if (selectedUser != null && leftUsername.equals(selectedUser)) {
                                chatArea.appendText(message);
                            }
                        }
                    });
                }
            } catch (Exception e) {
                Platform.runLater(() -> chatArea.appendText("❌ З’єднання втрачено\n"));
            }
        }).start();

        userList.setOnMouseClicked(e -> {
            String selected = userList.getSelectionModel().getSelectedItem();
            if (selected != null) {
                selectedUser = selected;
                targetName.setText(selected);
                updateChatArea();
            }
        });
    }

    private void updateUserList(Document doc) {
        lastUserListDoc = doc;
        updateUserListWithFilter(searchField.getText());
    }

    private void updateUserListWithFilter(String filter) {
        userList.getItems().clear();
        if (lastUserListDoc == null) return;
        NodeList users = lastUserListDoc.getDocumentElement().getElementsByTagName("user");
        for (int i = 0; i < users.getLength(); i++) {
            String user = users.item(i).getTextContent();
            if (!user.isEmpty() && !user.equals(username) && user.toLowerCase().contains(filter.toLowerCase())) {
                userList.getItems().add(user);
            }
        }
    }

    private void updateChatArea() {
        chatArea.clear();
        if (selectedUser != null) {
            List<String> messages = messageHistory.getOrDefault(selectedUser, new ArrayList<>());
            for (String message : messages) {
                chatArea.appendText(message);
            }
        }
    }

    private Document parseXML(String xml) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        DocumentBuilder builder = factory.newDocumentBuilder();
        return builder.parse(new ByteArrayInputStream(xml.getBytes("UTF-8")));
    }
}