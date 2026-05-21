package chat.server;

import chat.common.XmlProtocol;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import java.io.OutputStream;
import java.net.Socket;
import java.util.List;
import java.util.UUID;

public class XmlClientHandler implements Runnable {
    private Socket socket;
    private XmlChatServer server;
    private long lastMessageTime = 0;

    public XmlClientHandler(Socket socket, XmlChatServer server) {
        this.socket = socket;
        this.server = server;
    }

    @Override
    public void run() {
        String clientName = null;
        String sessionId = null;
        try {
            server.logEvent("Поток [" + Thread.currentThread().getName() + "] НАЧАЛ обработку сокета: " + socket.getRemoteSocketAddress());
            OutputStream os = socket.getOutputStream();
            os.flush();

            while (true) {
                String xmlStr = "";
                try {
                    xmlStr = XmlProtocol.readMessage(socket.getInputStream());
                } catch (Exception e) {
                    break;
                }

                Document doc = server.parseXml(xmlStr);
                Element root = doc.getDocumentElement();

                if (!root.getTagName().equals("command")) {
                    sendError(os, "Неверный формат команды");
                    continue;
                }

                String commandName = root.getAttribute("name");

                if (commandName.equals("login")) {
                    clientName = root.getElementsByTagName("name").item(0).getTextContent();
                    sessionId = UUID.randomUUID().toString();
                    server.addClient(clientName, sessionId, os);

                    String successXml = "<success><session>" + sessionId + "</session></success>";
                    XmlProtocol.writeMessage(os, successXml);

                    List<String> history = server.getHistory();
                    for (int i = 0; i < history.size(); i++) {
                        XmlProtocol.writeMessage(os, history.get(i));
                    }

                    String joinEvent = "<event name=\"userlogin\"><name>" + clientName + "</name></event>";
                    server.broadcast(joinEvent, "Server", "Общий", true);
                    continue;
                }

                long currentTime = System.currentTimeMillis();
                if (currentTime - lastMessageTime < 500) {
                    sendError(os, "Превышен лимит сообщений (Rate Limit). Подождите.");
                    continue;
                }
                lastMessageTime = currentTime;

                if (commandName.equals("logout")) {
                    String reqSession = root.getElementsByTagName("session").item(0).getTextContent();
                    if (sessionId != null && sessionId.equals(reqSession)) {
                        XmlProtocol.writeMessage(os, "<success></success>");
                        break;
                    }
                }

                if (commandName.equals("list")) {
                    String listXml = "<success><listusers>";
                    for (String user : server.getClientGroups().keySet()) {
                        listXml = listXml + "<user><name>" + user + "</name><type>CHAT_CLIENT</type></user>";
                    }
                    listXml = listXml + "</listusers></success>";
                    XmlProtocol.writeMessage(os, listXml);
                    continue;
                }

                if (commandName.equals("message")) {
                    String msgContent = root.getElementsByTagName("message").item(0).getTextContent();

                    String group = "Общий";
                    NodeList groupNodes = root.getElementsByTagName("group");
                    if (groupNodes.getLength() > 0) {
                        group = groupNodes.item(0).getTextContent();
                    }
                    server.changeGroup(clientName, group);

                    String fileType = "NONE";
                    NodeList fileTypeNodes = root.getElementsByTagName("fileType");
                    if (fileTypeNodes.getLength() > 0) {
                        fileType = fileTypeNodes.item(0).getTextContent();
                    }

                    String fileDataStr = "";
                    NodeList fileDataNodes = root.getElementsByTagName("fileData");
                    if (fileDataNodes.getLength() > 0) {
                        fileDataStr = fileDataNodes.item(0).getTextContent();
                    }

                    String eventXml = "<event name=\"message\">" +
                            "<message>" + msgContent + "</message>" +
                            "<name>" + clientName + "</name>" +
                            "<group>" + group + "</group>" +
                            "<fileType>" + fileType + "</fileType>";

                    if (!fileDataStr.equals("")) {
                        eventXml = eventXml + "<fileData>" + fileDataStr + "</fileData>";
                    }
                    eventXml = eventXml + "</event>";

                    server.broadcast(eventXml, clientName, group, false);
                    XmlProtocol.writeMessage(os, "<success></success>");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            server.logEvent("Поток [" + Thread.currentThread().getName() + "] ЗАВЕРШИЛ РАБОТУ. Клиент: " + clientName);
            if (clientName != null) {
                server.removeClient(clientName);
                try {
                    String leaveEvent = "<event name=\"userlogout\"><name>" + clientName + "</name></event>";
                    server.broadcast(leaveEvent, "Server", "Общий", true);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            try {
                socket.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void sendError(OutputStream os, String reason) {
        try {
            XmlProtocol.writeMessage(os, "<error><message>" + reason + "</message></error>");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}