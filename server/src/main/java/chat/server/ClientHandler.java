package chat.server;

import chat.common.Message;
import chat.common.MessageType;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.List;

public class ClientHandler implements Runnable {
    private Socket socket;
    private ChatServer server;
    private long lastMessageTime = 0;

    public ClientHandler(Socket socket, ChatServer server) {
        this.socket = socket;
        this.server = server;
    }

    @Override
    public void run() {
        String clientName = null;
        try {
            server.logEvent("Обработка клиента в потоке: " + Thread.currentThread().getName());

            ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
            out.flush();
            ObjectInputStream in = new ObjectInputStream(socket.getInputStream());

            Message loginMessage = (Message) in.readObject();
            if (loginMessage.getType() == MessageType.LOGIN) {
                clientName = loginMessage.getSender();

                out.writeObject(new Message(MessageType.SUCCESS, "Server", "Вы успешно вошли"));
                out.flush();

                List<Message> history = server.getHistory();
                for (int i = 0; i < history.size(); i++) {
                    out.writeObject(history.get(i));
                    out.flush();
                }

                server.addClient(clientName, out);
                server.broadcast(new Message(MessageType.USER_JOINED, "Server", clientName + " присоединился к чату"));
            } else {
                out.writeObject(new Message(MessageType.ERROR, "Server", "Ожидалась авторизация"));
                out.flush();
                return;
            }

            while (true) {
                Message msg = (Message) in.readObject();

                if (msg.getType() == MessageType.LOGOUT) {
                    break;
                }

                if (msg.getType() == MessageType.LIST) {
                    StringBuilder users = new StringBuilder();
                    for (String user : server.getClientGroups().keySet()) {
                        if (users.length() > 0) {
                            users.append(", ");
                        }
                        users.append(user);
                    }
                    Message listMsg = new Message(MessageType.USER_LIST, "Server", users.toString());
                    out.writeObject(listMsg);
                    out.flush();
                    continue;
                }

                long currentTime = System.currentTimeMillis();
                if (currentTime - lastMessageTime < 500) {
                    out.writeObject(new Message(MessageType.ERROR, "Server", "Превышен лимит сообщений (Rate Limit). Подождите."));
                    out.flush();
                    continue;
                }
                lastMessageTime = currentTime;

                if (msg.getTargetGroup() != null && !msg.getTargetGroup().equals("")) {
                    server.changeGroup(clientName, msg.getTargetGroup());
                }

                server.logEvent("Получено сообщение от " + msg.getSender());
                server.broadcast(msg);
            }

        } catch (Exception e) {
            server.logEvent("Ошибка или отключение клиента " + clientName + ": " + e.getMessage());
        } finally {
            if (clientName != null) {
                server.removeClient(clientName);
                server.broadcast(new Message(MessageType.USER_LEFT, "Server", clientName + " покинул чат"));
            }
            try {
                socket.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}