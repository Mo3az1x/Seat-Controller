package application;

import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.util.Properties;
import java.util.Timer;
import java.util.TimerTask;

public class BusSnifferGUI extends JFrame {
    private JComboBox<String> portCombo;
    private JTextField baudField;
    private JLabel statusLabel;
    private JLabel stateLabel;

    // User input
    private JTextField oneTimeSignalField;
    private JButton sendOneTimeBtn;
    private JTextField periodicSignal1Field, periodicRate1Field;
    private JTextField periodicSignal2Field, periodicRate2Field;
    private Timer periodicTimer1, periodicTimer2;

    private JTextArea traceArea;
    private SnifferManager sniffer;
    private JToggleButton connectBtn;

    private JButton saveProfileBtn, loadProfileBtn;

    // 🆕 State Manager
    private enum State { OFF, IDLE, BUSY, LOCKED, ERROR, SHUTDOWN }
    private State currentState = State.OFF;

    public BusSnifferGUI() {
        super("Bus Sniffer Application");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(800, 600);
        setLayout(new BorderLayout(10, 10));

        // === Settings Panel ===
        JPanel settingsPanel = new JPanel(new GridLayout(3, 2, 5, 5));
        settingsPanel.setBorder(BorderFactory.createTitledBorder("Settings"));

        settingsPanel.add(new JLabel("COM:"));
        portCombo = new JComboBox<>();
        String[] ports = PortUtil.getAllPorts();
        for (String p : ports) {
            if (p != null && !p.isEmpty()) portCombo.addItem(p);
        }
        settingsPanel.add(portCombo);

        settingsPanel.add(new JLabel("Baud:"));
        baudField = new JTextField("9600");
        settingsPanel.add(baudField);

        settingsPanel.add(new JLabel(""));
        connectBtn = new JToggleButton("Connect");
        connectBtn.addActionListener(e -> toggleConnection());
        settingsPanel.add(connectBtn);

        add(settingsPanel, BorderLayout.NORTH);

        // === Center Panel (Inputs + State + Profiles) ===
        JPanel centerPanel = new JPanel();
        centerPanel.setLayout(new BoxLayout(centerPanel, BoxLayout.Y_AXIS));

        // One-time input
        JPanel oneTimePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        oneTimePanel.setBorder(BorderFactory.createTitledBorder("User Input"));
        oneTimeSignalField = new JTextField(15);
        sendOneTimeBtn = new JButton("Send");
        sendOneTimeBtn.addActionListener(e -> sendOneTimeSignal());
        oneTimePanel.add(new JLabel("Signal:"));
        oneTimePanel.add(oneTimeSignalField);
        oneTimePanel.add(sendOneTimeBtn);
        centerPanel.add(oneTimePanel);

        // Periodic inputs
        JPanel periodicPanel = new JPanel(new GridLayout(2, 4, 5, 5));
        periodicPanel.setBorder(BorderFactory.createTitledBorder("Periodic"));

        periodicPanel.add(new JLabel("Signal1:"));
        periodicSignal1Field = new JTextField(10);
        periodicPanel.add(periodicSignal1Field);
        periodicPanel.add(new JLabel("Rate1 (ms):"));
        periodicRate1Field = new JTextField(5);
        periodicPanel.add(periodicRate1Field);

        periodicPanel.add(new JLabel("Signal2:"));
        periodicSignal2Field = new JTextField(10);
        periodicPanel.add(periodicSignal2Field);
        periodicPanel.add(new JLabel("Rate2 (ms):"));
        periodicRate2Field = new JTextField(5);
        periodicPanel.add(periodicRate2Field);

        centerPanel.add(periodicPanel);

        // === State label ===
        JPanel statePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        statePanel.setBorder(BorderFactory.createTitledBorder("System State"));
        stateLabel = new JLabel("State: OFF");
        statePanel.add(stateLabel);
        centerPanel.add(statePanel);

        // === Profile Buttons ===
        JPanel profilePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        profilePanel.setBorder(BorderFactory.createTitledBorder("Profiles"));
        saveProfileBtn = new JButton("Save Profile");
        loadProfileBtn = new JButton("Load Profile");

        saveProfileBtn.addActionListener(e -> saveProfile());
        loadProfileBtn.addActionListener(e -> loadProfile());

        profilePanel.add(saveProfileBtn);
        profilePanel.add(loadProfileBtn);
        centerPanel.add(profilePanel);

        add(centerPanel, BorderLayout.CENTER);

        // === Trace Area ===
        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.setBorder(BorderFactory.createTitledBorder("Trace"));
        traceArea = new JTextArea(10, 40);
        traceArea.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(traceArea);
        bottomPanel.add(scrollPane, BorderLayout.CENTER);

        statusLabel = new JLabel("Disconnected");
        bottomPanel.add(statusLabel, BorderLayout.SOUTH);

        add(bottomPanel, BorderLayout.SOUTH);

        // Load last config
        loadConfig();
        setVisible(true);
    }

    private void toggleConnection() {
        if (sniffer == null) {
            String port = (String) portCombo.getSelectedItem();
            int baud = Integer.parseInt(baudField.getText().trim());

            sniffer = new SnifferManager(new TraceListener() {
                @Override
                public void onTrace(String message) {
                    traceArea.append(message + "\n");
                }

                @Override
                public void onFrame(byte[] data, int len) {
                    traceArea.append("Frame received, len=" + len + "\n");
                }
            });

            if (sniffer.start(port, baud)) {
                statusLabel.setText("Connected to " + port);
                connectBtn.setText("Disconnect");
                saveConfig(port, baud);
                startPeriodic();
                setState(State.IDLE);
            } else {
                sniffer = null;
                statusLabel.setText("Failed!");
                connectBtn.setSelected(false);
            }
        } else {
            stopPeriodic();
            sniffer.close();
            sniffer = null;
            statusLabel.setText("Disconnected");
            connectBtn.setText("Connect");
            setState(State.OFF);
        }
    }

    private void sendOneTimeSignal() {
        if (sniffer != null) {
            String sig = oneTimeSignalField.getText().trim();
            if (!sig.isEmpty()) {
                sniffer.sendRaw(sig.getBytes());
                traceArea.append("Sent: " + sig + "\n");
                setState(State.BUSY);
            }
        }
    }

    private void startPeriodic() {
        try {
            int rate1 = Integer.parseInt(periodicRate1Field.getText().trim());
            if (rate1 > 0) {
                periodicTimer1 = new Timer();
                periodicTimer1.scheduleAtFixedRate(new TimerTask() {
                    @Override
                    public void run() {
                        String s = periodicSignal1Field.getText().trim();
                        if (sniffer != null && !s.isEmpty()) {
                            sniffer.sendRaw(s.getBytes());
                            SwingUtilities.invokeLater(() ->
                                traceArea.append("Periodic1: " + s + "\n"));
                        }
                    }
                }, 0, rate1);
            }
        } catch (Exception ignore) {}

        try {
            int rate2 = Integer.parseInt(periodicRate2Field.getText().trim());
            if (rate2 > 0) {
                periodicTimer2 = new Timer();
                periodicTimer2.scheduleAtFixedRate(new TimerTask() {
                    @Override
                    public void run() {
                        String s = periodicSignal2Field.getText().trim();
                        if (sniffer != null && !s.isEmpty()) {
                            sniffer.sendRaw(s.getBytes());
                            SwingUtilities.invokeLater(() ->
                                traceArea.append("Periodic2: " + s + "\n"));
                        }
                    }
                }, 0, rate2);
            }
        } catch (Exception ignore) {}
    }

    private void stopPeriodic() {
        if (periodicTimer1 != null) periodicTimer1.cancel();
        if (periodicTimer2 != null) periodicTimer2.cancel();
    }

    private void loadConfig() {
        try {
            Properties p = new Properties();
            File f = new File("sniffer.properties");
            if (!f.exists()) return;
            p.load(new FileInputStream(f));
            baudField.setText(p.getProperty("baud", "9600"));
            String lastPort = p.getProperty("port");
            if (lastPort != null) portCombo.setSelectedItem(lastPort);
        } catch (Exception ignore) {}
    }

    private void saveConfig(String port, int baud) {
        try {
            Properties p = new Properties();
            p.setProperty("port", port);
            p.setProperty("baud", String.valueOf(baud));
            p.store(new FileOutputStream("sniffer.properties"), "Sniffer config");
        } catch (Exception ignore) {}
    }

    // === Profiles ===
    private void saveProfile() {
        if (sniffer == null) {
            JOptionPane.showMessageDialog(this, "Not connected.");
            return;
        }
        byte[] data = sniffer.getLastReadAll();
        if (data == null) {
            JOptionPane.showMessageDialog(this, "No EEPROM data to save. Perform Read All first.");
            return;
        }
        JFileChooser chooser = new JFileChooser();
        if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            try (FileOutputStream fos = new FileOutputStream(chooser.getSelectedFile())) {
                fos.write(data);
                traceArea.append("Profile saved: " + chooser.getSelectedFile() + "\n");
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(this, "Error saving profile: " + ex.getMessage());
            }
        }
    }

    private void loadProfile() {
        if (sniffer == null) {
            JOptionPane.showMessageDialog(this, "Not connected.");
            return;
        }
        JFileChooser chooser = new JFileChooser();
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            try {
                byte[] data = java.nio.file.Files.readAllBytes(chooser.getSelectedFile().toPath());
                sniffer.sendWriteAll(data);
                traceArea.append("Profile loaded and sent: " + chooser.getSelectedFile() + "\n");
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(this, "Error loading profile: " + ex.getMessage());
            }
        }
    }

    // === State machine ===
    private void setState(State newState) {
        currentState = newState;
        stateLabel.setText("State: " + currentState);
        traceArea.append(">> State changed to " + currentState + "\n");
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(BusSnifferGUI::new);
    }
}
