package ru.nsu.ccfit.factory;

import ru.nsu.ccfit.threadpool.ThreadPool;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.List;

public class FactoryGUI extends JFrame {
    private final Storage<Body> bodyStorage;
    private final Storage<Motor> motorStorage;
    private final Storage<Accessory> accessoryStorage;
    private final Storage<Auto> autoStorage;
    private final ThreadPool threadPool;
    private final Supplier<Body> bodySupplier;
    private final Supplier<Motor> motorSupplier;
    private final List<Supplier<Accessory>> accessorySuppliers;
    
    private final JLabel bodyLabel = new JLabel();
    private final JLabel motorLabel = new JLabel();
    private final JLabel accessoryLabel = new JLabel();
    private final JLabel autoLabel = new JLabel();
    private final JLabel tasksLabel = new JLabel();
    private final JLabel totalProducedLabel = new JLabel();
    private final Color goldColor = new Color(255, 215, 0);
    private final Font mainFont = new Font("Arial", Font.BOLD, 18);
    private final Font titleFont = new Font("Arial", Font.BOLD, 22);
    private Image backgroundImage;

    public FactoryGUI(Storage<Body> bs, Storage<Motor> ms, Storage<Accessory> as, Storage<Auto> aus, ThreadPool tp,
                      Supplier<Body> bodySupplier, Supplier<Motor> motorSupplier, List<Supplier<Accessory>> accessorySuppliers, List<Dealer> dealers,
                      List<Thread> allThreads) {
        this.bodyStorage = bs;
        this.motorStorage = ms;
        this.accessoryStorage = as;
        this.autoStorage = aus;
        this.threadPool = tp;
        this.bodySupplier = bodySupplier;
        this.motorSupplier = motorSupplier;
        this.accessorySuppliers = accessorySuppliers;

        try {
            backgroundImage = javax.imageio.ImageIO.read(new java.io.File("fone.png"));
        } catch (java.io.IOException e) {
            e.printStackTrace();
        }

        setContentPane(new JPanel(new BorderLayout(20, 20)) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                if (backgroundImage != null) {
                    g.drawImage(backgroundImage, 0, 0, getWidth(), getHeight(), this);
                }
            }
        });

        setTitle("Эмулятор Автозавода");
        setSize(1000, 900);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        JPanel statsPanel = new JPanel(new GridLayout(6, 1, 10, 10));
        statsPanel.setOpaque(false);
        statsPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Статистика складов", 0, 0, titleFont));
        
        setupLabel(bodyLabel);
        setupLabel(motorLabel);
        setupLabel(accessoryLabel);
        setupLabel(autoLabel);
        setupLabel(tasksLabel);
        setupLabel(totalProducedLabel);

        statsPanel.add(bodyLabel);
        statsPanel.add(motorLabel);
        statsPanel.add(accessoryLabel);
        statsPanel.add(autoLabel);
        statsPanel.add(tasksLabel);
        statsPanel.add(totalProducedLabel);

        JPanel controlPanel = new JPanel(new GridLayout(4, 1, 20, 20));
        controlPanel.setOpaque(false);
        controlPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Управление скоростью (задержка в мс)", 0, 0, titleFont));
        
        controlPanel.add(createSliderPanel("Поставка кузовов", 0, 5000, 1000, bodySupplier::setDelay));
        controlPanel.add(createSliderPanel("Поставка двигателей", 0, 5000, 1000, motorSupplier::setDelay));
        controlPanel.add(createSliderPanel("Поставка аксессуаров", 0, 5000, 1000, val -> {
            for (Supplier<Accessory> s : accessorySuppliers) s.setDelay(val);
        }));
        controlPanel.add(createSliderPanel("Продажа (Дилеры)", 0, 5000, 2000, val -> {
            for (Dealer d : dealers) d.setDelay(val);
        }));

        add(statsPanel, BorderLayout.NORTH);
        add(controlPanel, BorderLayout.CENTER);

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                Dealer.closeLogger();
                threadPool.shutdown();
                for (Thread t : allThreads) {
                    t.interrupt();
                }
                System.exit(0);
            }
        });

        Timer timer = new Timer(100, e -> updateStats());
        timer.start();
        
        setVisible(true);
    }

    private void setupLabel(JLabel label) {
        label.setFont(mainFont);
        label.setForeground(goldColor);
        label.setHorizontalAlignment(SwingConstants.CENTER);
    }

    private JPanel createSliderPanel(String labelText, int min, int max, int initial, java.util.function.Consumer<Integer> onUpdate) {
        JPanel panel = new JPanel(new BorderLayout(5, 5));
        panel.setOpaque(false);
        JLabel titleLabel = new JLabel(labelText);
        titleLabel.setFont(mainFont);
        titleLabel.setForeground(goldColor);
        titleLabel.setHorizontalAlignment(SwingConstants.CENTER);

        JSlider slider = new JSlider(min, max, initial);
        slider.setOpaque(false);
        slider.setPaintTicks(true);
        slider.setPaintLabels(true);
        slider.setMajorTickSpacing(1000);
        slider.setMinorTickSpacing(250);
        slider.setFont(new Font("Arial", Font.BOLD, 14));
        slider.setForeground(goldColor);
        
        slider.addChangeListener(e -> {
            if (!slider.getValueIsAdjusting()) {
                onUpdate.accept(slider.getValue());
            }
        });
        
        panel.add(titleLabel, BorderLayout.NORTH);
        panel.add(slider, BorderLayout.CENTER);
        return panel;
    }

    private void updateStats() {
        bodyLabel.setText("Кузовов на складе: " + bodyStorage.getItemCount() + " / " + bodyStorage.getCapacity() + " (Всего: " + bodySupplier.getTotalProduced() + ")");
        motorLabel.setText("Двигателей на складе: " + motorStorage.getItemCount() + " / " + motorStorage.getCapacity() + " (Всего: " + motorSupplier.getTotalProduced() + ")");
        
        long accTotal = 0;
        for (Supplier<Accessory> s : accessorySuppliers) accTotal += s.getTotalProduced();
        accessoryLabel.setText("Аксессуаров на складе: " + accessoryStorage.getItemCount() + " / " + accessoryStorage.getCapacity() + " (Всего: " + accTotal + ")");
        
        autoLabel.setText("Машин на складе: " + autoStorage.getItemCount() + " / " + autoStorage.getCapacity());
        tasksLabel.setText("Задач в очереди ThreadPool: " + threadPool.getTaskCount());
        totalProducedLabel.setText("Всего собрано машин: " + BuildAutoTask.getTotalBuilt());
    }
}
