package chat.client;

import chat.common.XmlProtocol;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import java.io.ByteArrayInputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.function.Consumer;
import javax.xml.parsers.DocumentBuilderFactory;

public class XmlChatClient {
    private String host;
    private int port;
    private String username;
    private Socket socket;
    private OutputStream out;
    private String sessionId;
    private Consumer<Document> onEventReceived;

    public XmlChatClient(String host, int port, String username, Consumer<Document> onEventReceived) {
        this.host = host;
        this.port = port;
        this.username = username;
        this.onEventReceived = onEventReceived;
    }

    public void connect() throws Exception {
        socket = new Socket(host, port);
        out = socket.getOutputStream();
        out.flush();

        String loginXml = "<command name=\"login\"><name>" + username + "</name><type>CHAT_CLIENT</type></command>";
        XmlProtocol.writeMessage(out, loginXml);

        String responseStr = XmlProtocol.readMessage(socket.getInputStream());
        Document doc = parseXml(responseStr);
        Element root = doc.getDocumentElement();

        if (root.getTagName().equals("error")) {
            String reason = root.getElementsByTagName("message").item(0).getTextContent();
            throw new RuntimeException(reason);
        }

        sessionId = root.getElementsByTagName("session").item(0).getTextContent();
        new Thread(() -> listenForMessages()).start();
    }

    private void listenForMessages() {
        try {
            while (true) {
                String xmlStr = XmlProtocol.readMessage(socket.getInputStream());
                Document doc = parseXml(xmlStr);
                onEventReceived.accept(doc);
            }
        } catch (Exception e) {
            try {
                Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
                Element error = doc.createElement("error");
                Element msg = doc.createElement("message");
                msg.setTextContent("Соединение разорвано");
                error.appendChild(msg);
                doc.appendChild(error);
                onEventReceived.accept(doc);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    public void sendMessage(String text, String group) {
        try {
            String xml = "<command name=\"message\">" +
                    "<message>" + text + "</message>" +
                    "<session>" + sessionId + "</session>" +
                    "<group>" + group + "</group>" +
                    "<fileType>NONE</fileType>" +
                    "</command>";
            XmlProtocol.writeMessage(out, xml);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void sendFile(String filename, byte[] fileData, String type, String group) {
        try {
            String encodedData = Base64.getEncoder().encodeToString(fileData);
            String xml = "<command name=\"message\">" +
                    "<message>" + filename + "</message>" +
                    "<session>" + sessionId + "</session>" +
                    "<group>" + group + "</group>" +
                    "<fileType>" + type + "</fileType>" +
                    "<fileData>" + encodedData + "</fileData>" +
                    "</command>";
            XmlProtocol.writeMessage(out, xml);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void requestUserList() {
        try {
            String xml = "<command name=\"list\"><session>" + sessionId + "</session></command>";
            XmlProtocol.writeMessage(out, xml);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void disconnect() {
        try {
            String xml = "<command name=\"logout\"><session>" + sessionId + "</session></command>";
            XmlProtocol.writeMessage(out, xml);
            socket.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private Document parseXml(String xmlStr) throws Exception {
        return DocumentBuilderFactory.newInstance()
                .newDocumentBuilder()
                .parse(new ByteArrayInputStream(xmlStr.getBytes(StandardCharsets.UTF_8)));
    }
}