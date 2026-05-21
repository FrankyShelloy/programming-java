package chat.client;

import chat.common.Message;
import chat.common.MessageType;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.nio.file.Files;

public class ChatUI extends JFrame {
    private JPanel            messagesPanel;
    private JScrollPane       scrollPane;
    private JTextField        inputField;
    private JComboBox<String> groupSelector;
    private JButton           sendButton;
    private JButton           fileButton;
    private JButton           listButton;
    private ChatClient        client;
    private Font              mainFont      = new Font("SansSerif", Font.BOLD, 32);
    private String            myUsername;

    public ChatUI() {
        setTitle("Чат");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1600, 1000);
        setLocationRelativeTo(null);

        JPanel topPanel   = new JPanel(new BorderLayout());
        JLabel groupLabel = new JLabel(" Группа/Ник: ");
        groupLabel.setFont(mainFont);

        groupSelector = new JComboBox<String>();
        groupSelector.addItem("Общий");
        groupSelector.setEditable(true);
        groupSelector.setFont(mainFont);

        listButton = new JButton("Кто в сети");
        listButton.setFont(mainFont);

        topPanel.add(groupLabel, BorderLayout.WEST);
        topPanel.add(groupSelector, BorderLayout.CENTER);
        topPanel.add(listButton, BorderLayout.EAST);

        messagesPanel = new JPanel();
        messagesPanel.setLayout(new BoxLayout(messagesPanel, BoxLayout.Y_AXIS));
        scrollPane    = new JScrollPane(messagesPanel);

        inputField = new JTextField();
        inputField.setFont(mainFont);

        sendButton = new JButton("Отправить");
        sendButton.setFont(mainFont);

        fileButton = new JButton(" + ");
        fileButton.setFont(mainFont);

        JPanel buttonPanel = new JPanel(new GridLayout(1, 2));
        buttonPanel.add(fileButton);
        buttonPanel.add(sendButton);

        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.add(inputField, BorderLayout.CENTER);
        bottomPanel.add(buttonPanel, BorderLayout.EAST);

        add(topPanel, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);
        add(bottomPanel, BorderLayout.SOUTH);

        sendButton.addActionListener(e -> sendMessage());
        inputField.addActionListener(e -> sendMessage());
        fileButton.addActionListener(e -> selectAndSendFile());
        listButton.addActionListener(e -> {
            if (client != null) {
                client.requestUserList();
            }
        });

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                if (client != null) {
                    client.disconnect();
                }
            }
        });
    }

    private void sendMessage() {
        String text = inputField.getText().trim();

        Object selected = groupSelector.getSelectedItem();
        String group    = "Общий";

        if (selected != null && !selected.toString().trim().equals("")) {
            group = selected.toString().trim();

            boolean exists = false;
            for (int i = 0; i < groupSelector.getItemCount(); i++) {
                if (group.equals(groupSelector.getItemAt(i))) {
                    exists = true;
                    break;
                }
            }
            if (!exists) {
                groupSelector.addItem(group);
            }
        }

        if (!text.equals("") && client != null) {
            client.sendMessage(text, group);
            inputField.setText("");
        }
    }

    private void selectAndSendFile() {
        JFileChooser fileChooser = new JFileChooser();
        int          result      = fileChooser.showOpenDialog(this);

        if (result == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            try {
                byte[] fileData = Files.readAllBytes(file.toPath());

                Object selected = groupSelector.getSelectedItem();
                String group    = "Общий";

                if (selected != null && !selected.toString().trim().equals("")) {
                    group = selected.toString().trim();

                    boolean exists = false;
                    for (int i = 0; i < groupSelector.getItemCount(); i++) {
                        if (group.equals(groupSelector.getItemAt(i))) {
                            exists = true;
                            break;
                        }
                    }
                    if (!exists) {
                        groupSelector.addItem(group);
                    }
                }

                MessageType type = MessageType.FILE;
                String      name = file.getName().toLowerCase();

                if (name.endsWith(".wav") || name.endsWith(".mp3") || name.endsWith(".au")) {
                    type = MessageType.AUDIO;
                }

                client.sendFile(file.getName(), fileData, type, group);
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this, "Ошибка чтения файла: " + e.getMessage());
            }
        }
    }

    private void playAudio(byte[] audioData) {
        new Thread(() -> {
            try {
                ByteArrayInputStream bais = new ByteArrayInputStream(audioData);
                AudioInputStream     ais  = AudioSystem.getAudioInputStream(bais);
                Clip                 clip = AudioSystem.getClip();
                clip.open(ais);
                clip.start();
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this, "Ошибка воспроизведения аудио: " + e.getMessage());
            }
        }).start();
    }

    private void saveFile(String filename, byte[] fileData) {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setSelectedFile(new File(filename));
        int          result      = fileChooser.showSaveDialog(this);

        if (result == JFileChooser.APPROVE_OPTION) {
            try {
                Files.write(fileChooser.getSelectedFile().toPath(), fileData);
                JOptionPane.showMessageDialog(this, "Файл успешно сохранен!");
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this, "Ошибка сохранения файла: " + e.getMessage());
            }
        }
    }

    public void start() {
        Font dialogFont = new Font("SansSerif", Font.BOLD, 32);
        UIManager.put("OptionPane.messageFont", dialogFont);
        UIManager.put("OptionPane.buttonFont", dialogFont);
        UIManager.put("TextField.font", dialogFont);

        String server = JOptionPane.showInputDialog(this, "IP сервера:", "localhost");
        if (server == null) System.exit(0);

        String portStr = JOptionPane.showInputDialog(this, "Порт:", "8080");
        if (portStr == null) System.exit(0);

        String username = JOptionPane.showInputDialog(this, "Ваш никнейм:");
        if (username == null || username.trim().equals("")) System.exit(0);

        this.myUsername = username;

        client = new ChatClient(server, Integer.parseInt(portStr), username, msg -> handleMessage(msg));

        try {
            client.connect();
            setVisible(true);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Ошибка: " + e.getMessage());
            System.exit(1);
        }
    }

    private void handleMessage(Message msg) {
        SwingUtilities.invokeLater(() -> {
            String displayTxt = "";
            String sender     = msg.getSender();

            if (sender != null && !sender.equals("Server") && !sender.equals("System") && !sender.equals(myUsername)) {
                boolean exists = false;
                for (int i = 0; i < groupSelector.getItemCount(); i++) {
                    if (sender.equals(groupSelector.getItemAt(i))) {
                        exists = true;
                        break;
                    }
                }
                if (!exists) {
                    groupSelector.addItem(sender);
                }
            }

            switch (msg.getType()) {
                case TEXT:
                    displayTxt = sender + " [" + msg.getTargetGroup() + "]: " + msg.getContent();
                    break;
                case FILE:
                    displayTxt = sender + " [" + msg.getTargetGroup() + "] прикрепил файл: " + msg.getContent();
                    break;
                case AUDIO:
                    displayTxt = sender + " [" + msg.getTargetGroup() + "] прикрепил аудио: " + msg.getContent();
                    break;
                case USER_LIST:
                    String[] users = msg.getContent().split(",");
                    StringBuilder sb = new StringBuilder("Пользователи в сети:\n");
                    for (String user : users) {
                        sb.append("- ").append(user.trim()).append("\n");
                    }
                    JOptionPane.showMessageDialog(this, sb.toString(), "Список участников", JOptionPane.INFORMATION_MESSAGE);
                    return;
                case USER_JOINED:
                case USER_LEFT:
                case ERROR:
                case SUCCESS:
                    displayTxt = "[" + sender + "] " + msg.getContent();
                    break;
                default:
                    break;
            }

            if (displayTxt.equals("")) {
                return;
            }

            JPanel rowPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
            JLabel label    = new JLabel(displayTxt);
            label.setFont(mainFont);
            rowPanel.add(label);

            if (msg.getType() == MessageType.AUDIO && msg.getFileData() != null) {
                JButton playBtn = new JButton("Слушать");
                playBtn.setFont(mainFont);
                playBtn.addActionListener(e -> playAudio(msg.getFileData()));
                rowPanel.add(playBtn);
            } else if (msg.getType() == MessageType.FILE && msg.getFileData() != null) {
                JButton saveBtn = new JButton("Скачать");
                saveBtn.setFont(mainFont);
                saveBtn.addActionListener(e -> saveFile(msg.getContent(), msg.getFileData()));
                rowPanel.add(saveBtn);
            }

            messagesPanel.add(rowPanel);
            messagesPanel.revalidate();
            messagesPanel.repaint();

            SwingUtilities.invokeLater(() -> {
                JScrollBar vertical = scrollPane.getVerticalScrollBar();
                vertical.setValue(vertical.getMaximum());
            });
        });
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            ChatUI ui = new ChatUI();
            ui.start();
        });
    }
}