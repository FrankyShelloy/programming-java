package chat.server;

import java.io.ByteArrayInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;

public class XmlChatServer {
    private static final int PORT = 8081;
    private static final int HISTORY_SIZE = 10;
    private static final boolean LOG_ENABLED = true;
    private ExecutorService workerPool = Executors.newFixedThreadPool(20);
    private Map<String, OutputStream> activeClients = new ConcurrentHashMap<>();
    private Map<String, String> sessionToUser = new ConcurrentHashMap<>();
    private Map<String, String> userToSession = new ConcurrentHashMap<>();
    private Map<String, String> clientGroups = new ConcurrentHashMap<>();
    private List<String> history = new LinkedList<>();
    private PrintWriter fileLogger;

    public XmlChatServer() {
        if (LOG_ENABLED) {
            try {
                fileLogger = new PrintWriter(new FileWriter("server_xml_log.txt", true), true);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void logEvent(String event) {
        if (LOG_ENABLED && fileLogger != null) {
            fileLogger.println(LocalDateTime.now() + " - " + event);
        }
        System.out.println(event);
    }

    public void start() {
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            logEvent("XML Сервер запущен на порту: " + PORT);

            while (true) {
                Socket clientSocket = serverSocket.accept();
                logEvent("Новое XML подключение: " + clientSocket.getRemoteSocketAddress());
                workerPool.submit(new XmlClientHandler(clientSocket, this));
            }
        } catch (IOException e) {
            logEvent("Ошибка запуска XML сервера: " + e.getMessage());
        } finally {
            workerPool.shutdown();
            if (fileLogger != null) {
                fileLogger.close();
            }
        }
    }

    public synchronized void addClient(String username, String sessionId, OutputStream os) {
        activeClients.put(username, os);
        sessionToUser.put(sessionId, username);
        userToSession.put(username, sessionId);
        clientGroups.put(username, "Общий");
    }

    public synchronized void removeClient(String username) {
        if (username != null) {
            activeClients.remove(username);
            clientGroups.remove(username);
            String sessionId = userToSession.remove(username);
            if (sessionId != null) {
                sessionToUser.remove(sessionId);
            }
        }
    }

    public void changeGroup(String username, String group) {
        if (activeClients.containsKey(group)) {
            return;
        }
        clientGroups.put(username, group);
    }

    public Map<String, String> getClientGroups() {
        return clientGroups;
    }

    public List<String> getHistory() {
        synchronized (history) {
            return new ArrayList<>(history);
        }
    }

    public void broadcast(String xmlMessage, String sender, String targetGroup, boolean isEvent) {
        boolean isPrivate = !isEvent && activeClients.containsKey(targetGroup);

        if (!isEvent && !isPrivate) {
            synchronized (history) {
                if (history.size() >= HISTORY_SIZE) {
                    history.remove(0);
                }
                history.add(xmlMessage);
            }
        }

        List<String> deadClients = new ArrayList<>();

        for (Map.Entry<String, OutputStream> entry : activeClients.entrySet()) {
            String recipient = entry.getKey();
            OutputStream os = entry.getValue();

            boolean shouldSend = false;

            if (isEvent) {
                shouldSend = true;
            } else if (isPrivate) {
                if (recipient.equals(sender) || recipient.equals(targetGroup)) {
                    shouldSend = true;
                }
            } else {
                String recipientGroup = clientGroups.get(recipient);
                if (recipientGroup == null) {
                    recipientGroup = "Общий";
                }
                if (targetGroup.equals(recipientGroup)) {
                    shouldSend = true;
                }
            }

            if (shouldSend) {
                try {
                    chat.common.XmlProtocol.writeMessage(os, xmlMessage);
                } catch (Exception e) {
                    logEvent("Ошибка отправки XML пользователю " + recipient + ". Сокет закрыт.");
                    deadClients.add(recipient);
                }
            }
        }

        if (deadClients.size() > 0) {
            for (int i = 0; i < deadClients.size(); i++) {
                removeClient(deadClients.get(i));
            }
        }
    }

    public Document parseXml(String xmlStr) throws Exception {
        return DocumentBuilderFactory.newInstance()
                .newDocumentBuilder()
                .parse(new ByteArrayInputStream(xmlStr.getBytes(StandardCharsets.UTF_8)));
    }

    public static void main(String[] args) {
        new XmlChatServer().start();
    }
}