package chat.client;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
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
import java.util.Base64;

public class XmlChatUI extends JFrame {
    private JPanel            messagesPanel;
    private JScrollPane       scrollPane;
    private JTextField        inputField;
    private JComboBox<String> groupSelector;
    private JButton           sendButton;
    private JButton           fileButton;
    private JButton           listButton;
    private XmlChatClient     client;
    private Font              mainFont   = new Font("SansSerif", Font.BOLD, 32);
    private String            myUsername;

    public XmlChatUI() {
        setTitle("XML Чат");
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

                String type = "FILE";
                String name     = file.getName().toLowerCase();

                if (name.endsWith(".wav") || name.endsWith(".mp3") || name.endsWith(".au")) {
                    type = "AUDIO";
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

        String portStr = JOptionPane.showInputDialog(this, "Порт:", "8081");
        if (portStr == null) System.exit(0);

        String username = JOptionPane.showInputDialog(this, "Ваш никнейм:");
        if (username == null || username.trim().equals("")) System.exit(0);

        this.myUsername = username;

        client = new XmlChatClient(server, Integer.parseInt(portStr), username, doc -> handleXmlEvent(doc));

        try {
            client.connect();
            setVisible(true);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Ошибка: " + e.getMessage());
            System.exit(1);
        }
    }

    private void handleXmlEvent(Document doc) {
        SwingUtilities.invokeLater(() -> {
            Element root      = doc.getDocumentElement();
            String  displayTxt = "";
            String  type       = "TEXT";
            byte[]  fileBytes  = null;
            String  filename   = "";

            if (root.getTagName().equals("success")) {
                NodeList listUsers = root.getElementsByTagName("listusers");
                if (listUsers.getLength() > 0) {
                    NodeList users = root.getElementsByTagName("name");
                    StringBuilder sb = new StringBuilder("Пользователи в сети:\n");

                    Object currentSelection = groupSelector.getSelectedItem();
                    groupSelector.removeAllItems();
                    groupSelector.addItem("Общий");

                    for (int i = 0; i < users.getLength(); i++) {
                        String uName = users.item(i).getTextContent();
                        sb.append("- ").append(uName).append("\n");

                        if (!uName.equals(myUsername)) {
                            groupSelector.addItem(uName);
                        }
                    }

                    if (currentSelection != null) {
                        groupSelector.setSelectedItem(currentSelection);
                    }

                    JOptionPane.showMessageDialog(this, sb.toString(), "Список участников", JOptionPane.INFORMATION_MESSAGE);
                    return;
                }
            }

            if (root.getTagName().equals("error")) {
                displayTxt = "[System Error] " + root.getElementsByTagName("message").item(0).getTextContent();
            } else if (root.getTagName().equals("event")) {
                String eventName = root.getAttribute("name");

                if (eventName.equals("userlogin")) {
                    displayTxt = "[Server] " + root.getElementsByTagName("name").item(0).getTextContent() + " присоединился к чату";
                } else if (eventName.equals("userlogout")) {
                    displayTxt = "[Server] " + root.getElementsByTagName("name").item(0).getTextContent() + " покинул чат";
                } else if (eventName.equals("message")) {
                    String sender = root.getElementsByTagName("name").item(0).getTextContent();
                    String msg    = root.getElementsByTagName("message").item(0).getTextContent();
                    String group  = "Общий";

                    NodeList groupNodes = root.getElementsByTagName("group");
                    if (groupNodes.getLength() > 0) {
                        group = groupNodes.item(0).getTextContent();
                    }

                    NodeList typeNodes = root.getElementsByTagName("fileType");
                    if (typeNodes.getLength() > 0) {
                        type = typeNodes.item(0).getTextContent();
                    }

                    NodeList dataNodes = root.getElementsByTagName("fileData");
                    if (dataNodes.getLength() > 0) {
                        fileBytes = Base64.getDecoder().decode(dataNodes.item(0).getTextContent());
                        filename  = msg;
                    }

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

                    if (type.equals("AUDIO")) {
                        displayTxt = sender + " [" + group + "] прикрепил аудио: " + filename;
                    } else if (type.equals("FILE")) {
                        displayTxt = sender + " [" + group + "] прикрепил файл: " + filename;
                    } else {
                        displayTxt = sender + " [" + group + "]: " + msg;
                    }
                }
            }

            if (displayTxt.equals("")) {
                return;
            }

            JPanel rowPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
            JLabel label    = new JLabel(displayTxt);
            label.setFont(mainFont);
            rowPanel.add(label);

            if (type.equals("AUDIO") && fileBytes != null) {
                JButton playBtn = new JButton("Слушать");
                playBtn.setFont(mainFont);
                byte[] finalAudioData = fileBytes;
                playBtn.addActionListener(e -> playAudio(finalAudioData));
                rowPanel.add(playBtn);
            } else if (type.equals("FILE") && fileBytes != null) {
                JButton saveBtn = new JButton("Скачать");
                saveBtn.setFont(mainFont);
                byte[] finalFileData = fileBytes;
                String finalFileName = filename;
                saveBtn.addActionListener(e -> saveFile(finalFileName, finalFileData));
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
            XmlChatUI ui = new XmlChatUI();
            ui.start();
        });
    }
}