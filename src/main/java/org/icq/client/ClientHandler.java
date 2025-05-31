package org.icq.client;

import java.io.*;
import javax.xml.parsers.*;
import org.w3c.dom.*;

public class ClientHandler implements Runnable {
    private BufferedReader in;

    public ClientHandler(BufferedReader in) {
        this.in = in;
    }

    @Override
    public void run() {
        String xmlMessage;  
        try {
            while ((xmlMessage = in.readLine()) != null) {
                Document doc = parseXML(xmlMessage);
                String rootTag = doc.getDocumentElement().getTagName();
                if (rootTag.equals("error")) {
                    System.out.println("❌ " + doc.getDocumentElement().getTextContent());
                } else if (rootTag.equals("connect")) {
                    System.out.println("✅ " + doc.getDocumentElement().getTextContent());
                } else if (rootTag.equals("message")) {
                    String from = doc.getDocumentElement().getAttribute("from");
                    String text = doc.getDocumentElement().getTextContent();
                    System.out.println("📩 [від " + from + "]: " + text);
                } else if (rootTag.equals("sent")) {
                    String to = doc.getDocumentElement().getAttribute("to");
                    String text = doc.getDocumentElement().getTextContent();
                    System.out.println("📤 [до " + to + "]: " + text);
                } else if (rootTag.equals("userlist")) {
                    StringBuilder sb = new StringBuilder("🧑‍💻 Онлайн: ");
                    NodeList users = doc.getDocumentElement().getElementsByTagName("user");
                    for (int i = 0; i < users.getLength(); i++) {
                        sb.append(users.item(i).getTextContent()).append(" ");
                    }
                    System.out.println(sb.toString());
                }
            }
        } catch (Exception e) {
            System.err.println("З'єднання з сервером втрачено.");
        }
    }

    private Document parseXML(String xml) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        DocumentBuilder builder = factory.newDocumentBuilder();
        return builder.parse(new ByteArrayInputStream(xml.getBytes("UTF-8")));
    }
}
