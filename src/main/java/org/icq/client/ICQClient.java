
package org.icq.client;

import java.io.*;
import java.net.Socket;
import java.util.Scanner;


public class ICQClient {
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;

    public ICQClient(String serverAddress, int port) throws IOException {
        socket = new Socket(serverAddress, port);
        out = new PrintWriter(socket.getOutputStream(), true);
        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
    }



    public void sendUsername(String username) {
        out.println("<connect username=\"" + username + "\"/>");
    }

    public void sendMessage(String recipient, String message) {
        out.println("<message from=\"" + username + "\" to=\"" + recipient + "\">" + message + "</message>");
    }

    public String readLine() throws IOException {
        return in.readLine();
    }

    public void close() throws IOException {
        socket.close();
    }

    public BufferedReader getIn() {
        return in;
    }

    public PrintWriter getOut() {
        return out;
    }

    public Socket getSocket() {
        return socket;
    }

    public void start(String username) {
        this.username = username;
        sendUsername(username);

        new Thread(new ClientHandler(in)).start();

        Scanner scanner = new Scanner(System.in);
        while (true) {
            String recipient = scanner.nextLine();
            String message = scanner.nextLine();
            sendMessage(recipient, message);
        }
    }

    private String username;
}
