package org.icq.server;

import java.io.*;
import java.net.Socket;
import javax.xml.parsers.*;
import org.w3c.dom.*;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

public class ClientConnection implements Runnable {
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private String username;
    private final ServerController controller;

    public ClientConnection(Socket socket, ServerController controller) {
        this.socket = socket;
        this.controller = controller;
    }

    @Override
    public void run() {
        try {
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);

            String xmlMessage;
            while ((xmlMessage = in.readLine()) != null) {
                Document doc = parseXML(xmlMessage);
                String rootTag = doc.getDocumentElement().getTagName();

                if (rootTag.equals("connect")) {
                    String newUsername = doc.getDocumentElement().getAttribute("username");
                    // Перевіряємо, чи ім'я вже зайняте
                    if (ICQServer.clients.containsKey(newUsername)) {
                        sendError("Ім'я вже зайняте.");
                        socket.close(); // Закриваємо з'єднання для нового клієнта
                        return; // Завершуємо обробку цього з'єднання
                    }
                    // Якщо ім'я вільне, зберігаємо його і продовжуємо
                    this.username = newUsername;
                    ICQServer.clients.put(username, this);
                    sendMessage(createConnectResponse());
                    sendUserListToAll();
                } else if (rootTag.equals("message")) {
                    String recipient = doc.getDocumentElement().getAttribute("to");
                    String messageText = doc.getDocumentElement().getTextContent();
                    ClientConnection target = ICQServer.clients.get(recipient);
                    if (target != null) {
                        target.sendMessage(createMessage(username, recipient, messageText));
                        sendMessageToSelf(recipient, messageText);
                        controller.addMessage(username, recipient, messageText);
                    } else {
                        sendError("Користувач '" + recipient + "' не знайдений.");
                    }
                } else if (rootTag.equals("disconnect")) {
                    String disconnectUsername = doc.getDocumentElement().getAttribute("username");
                    if (username != null && username.equals(disconnectUsername)) {
                        ICQServer.clients.remove(username);
                        controller.addDisconnect(username);
                        sendUserLeftNotification(username);
                        sendUserListToAll();
                        socket.close();
                        return;
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("❌ Проблема з клієнтом: " + username);
        } finally {
            if (username != null) {
                ICQServer.clients.remove(username);
                controller.addDisconnect(username);
                sendUserListToAll();
            }
            try {
                socket.close();
            } catch (IOException e) {
                System.err.println("❌ Помилка закриття з'єднання: " + e.getMessage());
            }
        }
    }

    public void close() throws IOException {
        socket.close();
    }

    private void sendMessage(String xml) {
        out.println(xml);
    }

    private void sendError(String errorMessage) {
        sendMessage(createError(errorMessage));
    }

    private void sendMessageToSelf(String recipient, String message) {
        sendMessage(createSentMessage(recipient, message));
    }

    private void sendUserListToAll() {
        String xmlUserList = createUserList();
        for (ClientConnection client : ICQServer.clients.values()) {
            client.sendMessage(xmlUserList);
        }
    }

    private void sendUserLeftNotification(String username) {
        if (username == null) return;
        String xmlUserLeft = createUserLeftMessage(username);
        for (ClientConnection client : ICQServer.clients.values()) {
            client.sendMessage(xmlUserLeft);
        }
    }

    private Document parseXML(String xml) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        DocumentBuilder builder = factory.newDocumentBuilder();
        return builder.parse(new ByteArrayInputStream(xml.getBytes("UTF-8")));
    }

    private String createConnectResponse() {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.newDocument();
            Element connect = doc.createElement("connect");
            connect.setAttribute("status", "success");
            connect.setTextContent("Успішно підключено як " + username);
            doc.appendChild(connect);
            return documentToString(doc);
        } catch (Exception e) {
            return createError("Помилка створення відповіді");
        }
    }

    private String createMessage(String from, String to, String text) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.newDocument();
            Element message = doc.createElement("message");
            message.setAttribute("from", from);
            message.setAttribute("to", to);
            message.setTextContent(text);
            doc.appendChild(message);
            return documentToString(doc);
        } catch (Exception e) {
            return createError("Помилка створення повідомлення");
        }
    }

    private String createSentMessage(String to, String text) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.newDocument();
            Element sent = doc.createElement("sent");
            sent.setAttribute("to", to);
            sent.setTextContent(text);
            doc.appendChild(sent);
            return documentToString(doc);
        } catch (Exception e) {
            return createError("Помилка створення повідомлення про надсилання");
        }
    }

    private String createUserList() {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.newDocument();
            Element userlist = doc.createElement("userlist");
            for (String name : ICQServer.clients.keySet()) {
                Element user = doc.createElement("user");
                user.setTextContent(name);
                userlist.appendChild(user);
            }
            doc.appendChild(userlist);
            return documentToString(doc);
        } catch (Exception e) {
            return createError("Помилка створення списку користувачів");
        }
    }

    private String createUserLeftMessage(String username) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.newDocument();
            Element userLeft = doc.createElement("user_left");
            userLeft.setAttribute("username", username);
            doc.appendChild(userLeft);
            return documentToString(doc);
        } catch (Exception e) {
            return createError("Помилка створення повідомлення про відключення");
        }
    }

    private String createError(String errorMessage) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.newDocument();
            Element error = doc.createElement("error");
            error.setTextContent(errorMessage);
            doc.appendChild(error);
            return documentToString(doc);
        } catch (Exception e) {
            return "<error>Помилка створення повідомлення про помилку</error>";
        }
    }

    private String documentToString(Document doc) throws Exception {
        TransformerFactory tf = TransformerFactory.newInstance();
        Transformer transformer = tf.newTransformer();
        transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
        transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
        StringWriter writer = new StringWriter();
        transformer.transform(new DOMSource(doc), new StreamResult(writer));
        return writer.getBuffer().toString();
    }
}