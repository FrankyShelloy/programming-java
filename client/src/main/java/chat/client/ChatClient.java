package chat.client;

import chat.common.Message;
import chat.common.MessageType;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.function.Consumer;

public class ChatClient {
    private String host;
    private int port;
    private String username;
    private Socket socket;
    private ObjectOutputStream out;
    private ObjectInputStream in;
    private Consumer<Message> onMessageReceived;

    public ChatClient(String host, int port, String username, Consumer<Message> onMessageReceived) {
        this.host = host;
        this.port = port;
        this.username = username;
        this.onMessageReceived = onMessageReceived;
    }

    public void connect() throws Exception {
        socket = new Socket(host, port);
        out = new ObjectOutputStream(socket.getOutputStream());
        out.flush();
        in = new ObjectInputStream(socket.getInputStream());

        Message loginMsg = new Message(MessageType.LOGIN, username, "");
        out.writeObject(loginMsg);
        out.flush();

        Message response = (Message) in.readObject();
        if (response.getType() != MessageType.SUCCESS) {
            throw new RuntimeException("Ошибка подключения: " + response.getContent());
        }

        new Thread(() -> listenForMessages()).start();
    }

    private void listenForMessages() {
        try {
            while (true) {
                Message msg = (Message) in.readObject();
                onMessageReceived.accept(msg);
            }
        } catch (Exception e) {
            onMessageReceived.accept(new Message(MessageType.ERROR, "System", "Соединение разорвано"));
        }
    }

    public void sendMessage(String text, String group) {
        try {
            Message msg = new Message(MessageType.TEXT, username, text);
            msg.setTargetGroup(group);
            out.writeObject(msg);
            out.flush();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void sendFile(String filename, byte[] fileData, MessageType type, String group) {
        try {
            Message msg = new Message(type, username, filename);
            msg.setFileData(fileData);
            msg.setTargetGroup(group);
            out.writeObject(msg);
            out.flush();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void requestUserList() {
        try {
            Message msg = new Message(MessageType.LIST, username, "");
            out.writeObject(msg);
            out.flush();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void disconnect() {
        try {
            Message logoutMsg = new Message(MessageType.LOGOUT, username, "");
            out.writeObject(logoutMsg);
            out.flush();
            socket.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}