package application;

import javax.swing.*;

import communication.PortUtil;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Properties;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;

public class SeatControllerBusSniffer extends JFrame {
    private JComboBox<String> portCombo;
    private JComboBox<String> protocolCombo;
    private JTextField baudField;
    private JLabel statusLabel;
    private JLabel frameTypeLabel;
    private WaveformPanel waveformPanel;
    
    // Enhanced input panels for seat controller specific messages
    private JPanel seatControlPanel;
    private JTextField heightTargetField, slideTargetField, inclineTargetField;
    private JTextField heightCurrentField, slideCurrentField, inclineCurrentField;
    private JLabel gearboxStatusLabel;
    private JButton sendSeatControlBtn;
    
    // Fault monitoring panel
    private JPanel faultPanel;
    private JLabel fault1StatusLabel, fault2StatusLabel;
    private JButton triggerFault1Btn, triggerFault2Btn;
    
    // EEPROM operations panel
    private JPanel eepromPanel;
    private JTextField eepromAddressField, eepromValueField;
    private JButton readByteBtn, writeByteBtn, readAllBtn, writeAllBtn;
    
    private JTextArea traceArea;
    private SnifferManager sniffer;
    private JToggleButton connectBtn;
    
    // Message timing
    private Timer aliveTimer, gearboxTimer, seatCurrentTimer;
    private int aliveCounter = 0;
    
    public SeatControllerBusSniffer() {
        super("Seat Controller ECU Bus Sniffer");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1000, 800);
        setLayout(new BorderLayout(10, 10));
        
        initializeComponents();
        setupLayout();
        loadConfig();
        setVisible(true);
    }
    
    private void initializeComponents() {
        // === Settings Panel Components ===
        portCombo = new JComboBox<>();
        String[] ports = PortUtil.getAllPorts();
        for (String p : ports) {
            if (p != null && !p.isEmpty()) portCombo.addItem(p);
        }
        
        protocolCombo = new JComboBox<>(new String[]{"I2C", "SPI", "UART"});
        baudField = new JTextField("9600");
        connectBtn = new JToggleButton("Connect");
        connectBtn.addActionListener(e -> toggleConnection());
        
        // === Seat Control Panel Components ===
        seatControlPanel = new JPanel(new GridLayout(4, 4, 5, 5));
        seatControlPanel.setBorder(BorderFactory.createTitledBorder("Seat Control Messages"));
        
        heightTargetField = new JTextField("3.0");
        slideTargetField = new JTextField("5.0");
        inclineTargetField = new JTextField("85.0");
        
        heightCurrentField = new JTextField("0.0");
        heightCurrentField.setEditable(false);
        slideCurrentField = new JTextField("0.0");
        slideCurrentField.setEditable(false);
        inclineCurrentField = new JTextField("0.0");
        inclineCurrentField.setEditable(false);
        
        sendSeatControlBtn = new JButton("Send Seat Control");
        sendSeatControlBtn.addActionListener(e -> sendSeatControlRequest());
        
        // === Fault Panel Components ===
        faultPanel = new JPanel(new GridLayout(2, 3, 5, 5));
        faultPanel.setBorder(BorderFactory.createTitledBorder("Fault Monitoring"));
        
        fault1StatusLabel = new JLabel("OK");
        fault1StatusLabel.setForeground(Color.GREEN);
        fault2StatusLabel = new JLabel("OK");
        fault2StatusLabel.setForeground(Color.GREEN);
        
        triggerFault1Btn = new JButton("Trigger Fault 1");
        triggerFault1Btn.addActionListener(e -> triggerFault(1));
        triggerFault2Btn = new JButton("Trigger Fault 2");
        triggerFault2Btn.addActionListener(e -> triggerFault(2));
        
        // === EEPROM Panel Components ===
        eepromPanel = new JPanel(new GridLayout(3, 4, 5, 5));
        eepromPanel.setBorder(BorderFactory.createTitledBorder("EEPROM Operations"));
        
        eepromAddressField = new JTextField("0x00");
        eepromValueField = new JTextField("0xFF");
        
        readByteBtn = new JButton("Read Byte");
        readByteBtn.addActionListener(e -> performEEPROMOperation("ReadByte"));
        writeByteBtn = new JButton("Write Byte");
        writeByteBtn.addActionListener(e -> performEEPROMOperation("WriteByte"));
        readAllBtn = new JButton("Read All");
        readAllBtn.addActionListener(e -> performEEPROMOperation("ReadAll"));
        writeAllBtn = new JButton("Write All");
        writeAllBtn.addActionListener(e -> performEEPROMOperation("WriteAll"));
        
        // === Other Components ===
        frameTypeLabel = new JLabel("Frame: -");
        waveformPanel = new WaveformPanel();
        waveformPanel.setPreferredSize(new Dimension(900, 120));
        
        gearboxStatusLabel = new JLabel("Gearbox: Gear=0, Torque=0/0");
        
        traceArea = new JTextArea(15, 60);
        traceArea.setEditable(false);
        traceArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        
        statusLabel = new JLabel("Disconnected");
    }
    
    private void setupLayout() {
        // Top panel with settings
        JPanel settingsPanel = new JPanel(new GridLayout(4, 2, 5, 5));
        settingsPanel.setBorder(BorderFactory.createTitledBorder("Connection Settings"));
        
        settingsPanel.add(new JLabel("COM Port:"));
        settingsPanel.add(portCombo);
        settingsPanel.add(new JLabel("Protocol:"));
        settingsPanel.add(protocolCombo);
        settingsPanel.add(new JLabel("Baud Rate:"));
        settingsPanel.add(baudField);
        settingsPanel.add(new JLabel(""));
        settingsPanel.add(connectBtn);
        
        add(settingsPanel, BorderLayout.NORTH);
        
        // Center panel with controls
        JPanel centerPanel = new JPanel();
        centerPanel.setLayout(new BoxLayout(centerPanel, BoxLayout.Y_AXIS));
        
        // Waveform panel
        JPanel wavePanel = new JPanel(new BorderLayout());
        wavePanel.setBorder(BorderFactory.createTitledBorder("Communication Waveform"));
        wavePanel.add(frameTypeLabel, BorderLayout.NORTH);
        wavePanel.add(waveformPanel, BorderLayout.CENTER);
        centerPanel.add(wavePanel);
        
        // Setup seat control panel layout
        setupSeatControlPanel();
        setupFaultPanel();
        setupEEPROMPanel();
        
        // Control panels in tabs
        JTabbedPane controlTabs = new JTabbedPane();
        controlTabs.addTab("Seat Control", seatControlPanel);
        controlTabs.addTab("Fault Monitor", faultPanel);
        controlTabs.addTab("EEPROM", eepromPanel);
        centerPanel.add(controlTabs);
        
        // Status panel
        JPanel statusPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        statusPanel.add(gearboxStatusLabel);
        centerPanel.add(statusPanel);
        
        add(centerPanel, BorderLayout.CENTER);
        
        // Bottom panel with trace
        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.setBorder(BorderFactory.createTitledBorder("Message Trace"));
        JScrollPane scrollPane = new JScrollPane(traceArea);
        bottomPanel.add(scrollPane, BorderLayout.CENTER);
        bottomPanel.add(statusLabel, BorderLayout.SOUTH);
        
        add(bottomPanel, BorderLayout.SOUTH);
    }
    
    private void setupSeatControlPanel() {
        seatControlPanel.add(new JLabel("Height Target (cm):"));
        seatControlPanel.add(heightTargetField);
        seatControlPanel.add(new JLabel("Range: 2.0 - 5.3"));
        seatControlPanel.add(new JLabel(""));
        
        seatControlPanel.add(new JLabel("Slide Target (cm):"));
        seatControlPanel.add(slideTargetField);
        seatControlPanel.add(new JLabel("Range: 3.0 - 7.5"));
        seatControlPanel.add(new JLabel(""));
        
        seatControlPanel.add(new JLabel("Incline Target (°):"));
        seatControlPanel.add(inclineTargetField);
        seatControlPanel.add(new JLabel("Range: 67.1° - 105.1°"));
        seatControlPanel.add(sendSeatControlBtn);
        
        seatControlPanel.add(new JLabel("Height Current:"));
        seatControlPanel.add(heightCurrentField);
        seatControlPanel.add(new JLabel("Slide Current:"));
        seatControlPanel.add(slideCurrentField);
    }
    
    private void setupFaultPanel() {
        faultPanel.add(new JLabel("Fault 1 Status:"));
        faultPanel.add(fault1StatusLabel);
        faultPanel.add(triggerFault1Btn);
        
        faultPanel.add(new JLabel("Fault 2 Status:"));
        faultPanel.add(fault2StatusLabel);
        faultPanel.add(triggerFault2Btn);
    }
    
    private void setupEEPROMPanel() {
        eepromPanel.add(new JLabel("Address:"));
        eepromPanel.add(eepromAddressField);
        eepromPanel.add(new JLabel("Value:"));
        eepromPanel.add(eepromValueField);
        
        eepromPanel.add(readByteBtn);
        eepromPanel.add(writeByteBtn);
        eepromPanel.add(readAllBtn);
        eepromPanel.add(writeAllBtn);
        
        // Add empty labels for grid alignment
        eepromPanel.add(new JLabel(""));
        eepromPanel.add(new JLabel(""));
        eepromPanel.add(new JLabel(""));
        eepromPanel.add(new JLabel(""));
    }
    
    private void toggleConnection() {
        if (sniffer == null) {
            String port = (String) portCombo.getSelectedItem();
            String protocol = (String) protocolCombo.getSelectedItem();
            int baud;
            
            try {
                baud = Integer.parseInt(baudField.getText().trim());
            } catch (NumberFormatException e) {
                JOptionPane.showMessageDialog(this, "Invalid baud rate");
                connectBtn.setSelected(false);
                return;
            }
            
            sniffer = new SnifferManager(new TraceListener() {
                @Override
                public void onTrace(String message) {
                    SwingUtilities.invokeLater(() -> {
                        String timestamp = new SimpleDateFormat("HH:mm:ss.SSS").format(new Date());
                        traceArea.append("[" + timestamp + "] " + message + "\n");
                        traceArea.setCaretPosition(traceArea.getDocument().getLength());
                        
                        // Update frame type and waveform
                        String frameType = detectFrameType(message);
                        frameTypeLabel.setText("Frame: " + frameType);
                        waveformPanel.addEvent(new FrameEvent(frameType));
                        
                        // Parse specific messages
                        parseMessage(message);
                    });
                }
                
                @Override
                public void onFrame(byte[] data, int len) {
                    SwingUtilities.invokeLater(() -> {
                        String hexData = SeatControllerBusSniffer.this.bytesToHex(data, len);
                        traceArea.append("[FRAME] " + hexData + "\n");
                    });
                }
            });
            
            if (sniffer.start(port, baud)) {
                statusLabel.setText("Connected to " + port + " (" + protocol + ")");
                connectBtn.setText("Disconnect");
                saveConfig(port, protocol, baud);
                startPeriodicMessages();
            } else {
                sniffer = null;
                statusLabel.setText("Connection Failed!");
                connectBtn.setSelected(false);
            }
        } else {
            stopPeriodicMessages();
            sniffer.close();
            sniffer = null;
            statusLabel.setText("Disconnected");
            connectBtn.setText("Connect");
        }
    }
    
    private void sendSeatControlRequest() {
        if (sniffer != null) {
            try {
                double height = Double.parseDouble(heightTargetField.getText());
                double slide = Double.parseDouble(slideTargetField.getText());
                double incline = Double.parseDouble(inclineTargetField.getText());
                
                // Validate ranges
                if (height < 2.0 || height > 5.3) {
                    JOptionPane.showMessageDialog(this, "Height must be between 2.0 and 5.3 cm");
                    return;
                }
                if (slide < 3.0 || slide > 7.5) {
                    JOptionPane.showMessageDialog(this, "Slide must be between 3.0 and 7.5 cm");
                    return;
                }
                if (incline < 67.0 || incline > 105.0) {
                    JOptionPane.showMessageDialog(this, "Incline must be between 67.0 and 105.0 degrees");
                    return;
                }
                
                // Send seat control command (simplified)
                String command = String.format("SEAT_CONTROL %.1f %.1f %.1f", height, slide, incline);
                sniffer.sendCommand(command);
                traceArea.append("[SENT] SeatControl_Req: H=" + height + " S=" + slide + " I=" + incline + "\n");
                
            } catch (NumberFormatException e) {
                JOptionPane.showMessageDialog(this, "Invalid numeric input");
            }
        }
    }
    
    private void triggerFault(int faultNumber) {
        if (sniffer != null) {
            String command = "FAULT_" + faultNumber;
            sniffer.sendCommand(command);
            
            JLabel faultLabel = (faultNumber == 1) ? fault1StatusLabel : fault2StatusLabel;
            faultLabel.setText("FAULT");
            faultLabel.setForeground(Color.RED);
            
            // Auto-clear fault after deactivation time
            int deactivationTime = (faultNumber == 1) ? 2000 : 5000;
            Timer clearTimer = new Timer(deactivationTime, e -> {
                faultLabel.setText("OK");
                faultLabel.setForeground(Color.GREEN);
            });
            clearTimer.setRepeats(false);
            clearTimer.start();
        }
    }
    
    private void performEEPROMOperation(String operation) {
        if (sniffer != null) {
            try {
                int address = parseHexOrDec(eepromAddressField.getText());
                
                switch (operation) {
                    case "ReadByte":
                        sniffer.sendReadByte(address);
                        break;
                    case "WriteByte":
                        int value = parseHexOrDec(eepromValueField.getText());
                        sniffer.sendWriteByte(address, value);
                        break;
                    case "ReadAll":
                        sniffer.sendReadByte(256);
                        break;
                    case "WriteAll":
                        byte[] pattern = new byte[256];
                        for (int i = 0; i < pattern.length; i++) {
                            pattern[i] = (byte)(i & 0xFF);
                        }
                        sniffer.sendWriteAll(pattern);
                        break;
                }
            } catch (NumberFormatException e) {
                JOptionPane.showMessageDialog(this, "Invalid address or value format");
            }
        }
    }
    
    private int parseHexOrDec(String text) {
        text = text.trim();
        if (text.startsWith("0x") || text.startsWith("0X")) {
            return Integer.parseInt(text.substring(2), 16);
        } else {
            return Integer.parseInt(text);
        }
    }
    
    private void startPeriodicMessages() {
        // Alive message every 5 seconds (reduced for demo)
        aliveTimer = new Timer(5000, e -> {
            if (sniffer != null) {
                sniffer.sendCommand("ALIVE_MSG " + System.currentTimeMillis() + " " + aliveCounter++);
            }
        });
        aliveTimer.start();
        
        // Gearbox status every 10 seconds
        gearboxTimer = new Timer(10000, e -> {
            if (sniffer != null) {
                int gear = (int)(Math.random() * 6) + 1;
                int currentTorque = (int)(Math.random() * 300);
                int maxTorque = 400;
                sniffer.sendCommand(String.format("GEARBOX_STATUS %d %d %d", gear, currentTorque, maxTorque));
                
                SwingUtilities.invokeLater(() -> 
                    gearboxStatusLabel.setText("Gearbox: Gear=" + gear + ", Torque=" + currentTorque + "/" + maxTorque));
            }
        });
        gearboxTimer.start();
        
        // Seat current position every 2 seconds
        seatCurrentTimer = new Timer(2000, e -> {
            if (sniffer != null) {
                double height = 2.0 + Math.random() * 3.3;
                double slide = 3.0 + Math.random() * 4.5;
                double incline = 67.0 + Math.random() * 38.0;
                
                sniffer.sendCommand(String.format("SEAT_CURRENT %.1f %.1f %.1f", height, slide, incline));
                
                SwingUtilities.invokeLater(() -> {
                    heightCurrentField.setText(String.format("%.1f", height));
                    slideCurrentField.setText(String.format("%.1f", slide));
                    inclineCurrentField.setText(String.format("%.1f", incline));
                });
            }
        });
        seatCurrentTimer.start();
    }
    
    private void stopPeriodicMessages() {
        if (aliveTimer != null) aliveTimer.stop();
        if (gearboxTimer != null) gearboxTimer.stop();
        if (seatCurrentTimer != null) seatCurrentTimer.stop();
    }
    
    private void parseMessage(String message) {
        // Simple message parsing for demo
        if (message.contains("SEAT_HEIGHT_CURRENT")) {
            // Could extract and update current height display
        } else if (message.contains("FAULT")) {
            // Could update fault status display
        }
    }
    
    private String detectFrameType(String msg) {
        if (msg == null) return "Unknown";
        String m = msg.toUpperCase();
        if (m.contains("ALIVE")) return "Alive";
        if (m.contains("GEARBOX")) return "Gearbox";
        if (m.contains("SEAT")) return "Seat";
        if (m.contains("FAULT")) return "Fault";
        if (m.contains("EEPROM") || m.contains("READ") || m.contains("WRITE")) return "EEPROM";
        if (m.contains("ERROR")) return "Error";
        return "Data";
    }
    
    private String bytesToHex(byte[] data, int len) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < len; i++) {
            sb.append(String.format("%02X ", data[i]));
        }
        return sb.toString().trim();
    }
    
    private void loadConfig() {
        try {
            Properties p = new Properties();
            File f = new File("seat_sniffer.properties");
            if (!f.exists()) return;
            p.load(new FileInputStream(f));
            baudField.setText(p.getProperty("baud", "115200"));
            String lastPort = p.getProperty("port");
            if (lastPort != null) portCombo.setSelectedItem(lastPort);
            String lastProtocol = p.getProperty("protocol");
            if (lastProtocol != null) protocolCombo.setSelectedItem(lastProtocol);
        } catch (Exception ignore) {}
    }
    
    private void saveConfig(String port, String protocol, int baud) {
        try {
            Properties p = new Properties();
            p.setProperty("port", port);
            p.setProperty("protocol", protocol);
            p.setProperty("baud", String.valueOf(baud));
            p.store(new FileOutputStream("seat_sniffer.properties"), "Seat Controller Sniffer Config");
        } catch (Exception ignore) {}
    }
    
    private static class FrameEvent {
        String type;
        long timestamp;
        FrameEvent(String t) { 
            type = t; 
            timestamp = System.currentTimeMillis();
        }
    }
    
    private static class WaveformPanel extends JPanel {
        private final java.util.List<FrameEvent> events = new ArrayList<>();
        
        void addEvent(FrameEvent ev) {
            events.add(ev);
            if (events.size() > 40) events.remove(0);
            repaint();
        }
        
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            
            int x = 10;
            for (FrameEvent ev : events) {
                Color color = getColorForFrameType(ev.type);
                g2.setColor(color);
                g2.fillRect(x, 30, 18, 40);
                g2.setColor(Color.BLACK);
                g2.drawRect(x, 30, 18, 40);
                
                g2.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 8));
                String abbrev = getAbbreviation(ev.type);
                FontMetrics fm = g2.getFontMetrics();
                int textX = x + (18 - fm.stringWidth(abbrev)) / 2;
                g2.drawString(abbrev, textX, 55);
                
                x += 22;
            }
            
            // Draw timeline
            g2.setColor(Color.GRAY);
            g2.drawLine(10, 80, getWidth() - 10, 80);
            
            // Draw legend
            x = 10;
            String[] types = {"Alive", "Gearbox", "Seat", "EEPROM", "Fault", "Error"};
            for (String type : types) {
                g2.setColor(getColorForFrameType(type));
                g2.fillRect(x, 90, 10, 10);
                g2.setColor(Color.BLACK);
                g2.drawRect(x, 90, 10, 10);
                g2.drawString(type, x + 15, 100);
                x += 80;
            }
        }
        
        private Color getColorForFrameType(String type) {
            switch (type) {
                case "Alive": return Color.GREEN;
                case "Gearbox": return Color.BLUE;
                case "Seat": return Color.ORANGE;
                case "EEPROM": return Color.CYAN;
                case "Fault": return Color.RED;
                case "Error": return Color.MAGENTA;
                default: return Color.LIGHT_GRAY;
            }
        }
        
        private String getAbbreviation(String type) {
            switch (type) {
                case "Alive": return "A";
                case "Gearbox": return "G";
                case "Seat": return "S";
                case "EEPROM": return "E";
                case "Fault": return "F";
                case "Error": return "!";
                default: return "?";
            }
        }
    }
    
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getLookAndFeel());
            } catch (Exception e) {
                // Use default look and feel
            }
            new SeatControllerBusSniffer();
        });
    }
}