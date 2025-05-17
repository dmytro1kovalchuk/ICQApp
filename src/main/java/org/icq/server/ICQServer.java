package org.icq.server;

import java.net.*;
import java.util.HashMap;
import java.util.Map;

public class ICQServer implements Runnable {
    public static Map<String, ClientConnection> clients = new HashMap<>();
    private final int port;
    private ServerSocket serverSocket;
    private final ServerController controller;

    public ICQServer(int port, ServerController controller) {
        this.port = port;
        this.controller = controller;
    }

    @Override
    public void run() {
        try {
            serverSocket = new ServerSocket(port);
            System.out.println("Сервер запущено на порті " + port);
            while (!serverSocket.isClosed()) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("Нове підключення: " + clientSocket);
                ClientConnection clientConnection = new ClientConnection(clientSocket, controller);
                new Thread(clientConnection).start();
            }
        } catch (Exception e) {
            System.err.println("Помилка сервера: " + e.getMessage());
        }
    }

    public void shutdown() {
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
            for (ClientConnection client : clients.values()) {
                client.close();
            }
            clients.clear();
        } catch (Exception e) {
            System.err.println("Помилка при завершенні роботи сервера: " + e.getMessage());
        }
    }
}