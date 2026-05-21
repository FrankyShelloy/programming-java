package chat.server;

import chat.common.Message;
import chat.common.MessageType;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ChatServer {
    private static final int PORT = 8080;
    private static final int HISTORY_SIZE = 10;
    private final ExecutorService workerPool = Executors.newFixedThreadPool(10);
    private final Map<String, ObjectOutputStream> activeClients = new ConcurrentHashMap<>();
    private final Map<String, String> clientGroups = new ConcurrentHashMap<>();
    private final List<Message> history = new ArrayList<>();
    private PrintWriter fileLogger;

    public ChatServer() {
        try {
            this.fileLogger = new PrintWriter(new FileWriter("server_log.txt", true), true);
        } catch (IOException e) {
            System.err.println("Не удалось запустить файловый логгер: " + e.getMessage());
        }
    }

    public void logEvent(String event) {
        String msg = LocalDateTime.now() + " - " + event;
        System.out.println(msg);
        if (fileLogger != null) {
            fileLogger.println(msg);
        }
    }

    public void start() {
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            logEvent("Сервер запущен на порту: " + PORT);

            while (!Thread.currentThread().isInterrupted()) {
                Socket clientSocket = serverSocket.accept();
                logEvent("Новое подключение: " + clientSocket.getRemoteSocketAddress());
                workerPool.submit(new ClientHandler(clientSocket, this));
            }
        } catch (IOException e) {
            logEvent("Ошибка запуска сервера: " + e.getMessage());
        } finally {
            workerPool.shutdown();
            if (fileLogger != null) {
                fileLogger.close();
            }
        }
    }

    public void addClient(String username, ObjectOutputStream out) {
        activeClients.put(username, out);
        clientGroups.put(username, "Общий");
        logEvent("Клиент добавлен: " + username);
    }

    public void removeClient(String username) {
        if (username == null) return;
        activeClients.remove(username);
        clientGroups.remove(username);
        logEvent("Клиент удален: " + username);
    }

    public void changeGroup(String username, String group) {
        if (activeClients.containsKey(group)) {
            return;
        }
        clientGroups.put(username, group);
        logEvent("Пользователь " + username + " перешел в группу: " + group);
    }

    public void broadcast(Message message) {
        String targetGroup = message.getTargetGroup();
        if (targetGroup == null || targetGroup.equals("")) {
            targetGroup = "Общий";
        }

        boolean isPrivate = activeClients.containsKey(targetGroup);
        if (message.getType() == MessageType.TEXT && !isPrivate) {
            synchronized (history) {
                if (history.size() >= HISTORY_SIZE) {
                    history.remove(0);
                }
                history.add(message);
            }
        }

        List<String> deadClients = new ArrayList<>();
        String sender = message.getSender();

        for (Map.Entry<String, ObjectOutputStream> entry : activeClients.entrySet()) {
            String recipient = entry.getKey();
            ObjectOutputStream out = entry.getValue();

            boolean shouldSend = false;
            if (message.getType() == MessageType.USER_JOINED || message.getType() == MessageType.USER_LEFT) {
                shouldSend = true;
            }
            else if (isPrivate) {
                if (recipient.equals(sender) || recipient.equals(targetGroup)) {
                    shouldSend = true;
                }
            }
            else {
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
                    out.writeObject(message);
                    out.flush();
                } catch (IOException e) {
                    logEvent("Не удалось отправить сообщение пользователю " + recipient + ". Удаляем.");
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

    public List<Message> getHistory() {
        synchronized (history) {
            return new ArrayList<>(history);
        }
    }

    public Map<String, String> getClientGroups() {
        return clientGroups;
    }

    public static void main(String[] args) {
        new ChatServer().start();
    }
}