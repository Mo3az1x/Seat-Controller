package application;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.Properties;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.File;

public class BusSnifferGUI extends JFrame {
    private JComboBox<String> portCombo;
    private JTextField baudField;
    private JLabel statusLabel;
    private JLabel frameTypeLabel;
    private WaveformPanel waveformPanel;

    // User input
    private JTextField oneTimeSignalField;
    private JButton sendOneTimeBtn;
    private JTextField periodicSignal1Field, periodicRate1Field;
    private JTextField periodicSignal2Field, periodicRate2Field;
    private Timer periodicTimer1, periodicTimer2;

    private JTextArea traceArea;
    private SnifferManager sniffer;
    private JToggleButton connectBtn;

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

        // === Center Panel (Waveform + inputs) ===
        JPanel centerPanel = new JPanel();
        centerPanel.setLayout(new BoxLayout(centerPanel, BoxLayout.Y_AXIS));

        // Waveform
        JPanel wavePanel = new JPanel(new BorderLayout());
        wavePanel.setBorder(BorderFactory.createTitledBorder("Waveform"));
        frameTypeLabel = new JLabel("Frame: -");
        wavePanel.add(frameTypeLabel, BorderLayout.NORTH);
        waveformPanel = new WaveformPanel();
        waveformPanel.setPreferredSize(new Dimension(760, 150));
        wavePanel.add(waveformPanel, BorderLayout.CENTER);
        centerPanel.add(wavePanel);

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

        // load last config
        loadConfig();
        setVisible(true);
    }

    private void toggleConnection() {
        if (sniffer == null) {
            String port = (String) portCombo.getSelectedItem();
            int baud = Integer.parseInt(baudField.getText().trim());

            // ✨ use new TraceListener with onFrame support
            sniffer = new SnifferManager(new TraceListener() {
                @Override
                public void onTrace(String message) {
                    traceArea.append(message + "\n");
                    frameTypeLabel.setText("Frame: " + detectFrameType(message));
                }

                @Override
                public void onFrame(byte[] data, int len) {
                    // TODO Auto-generated method stub
                    throw new UnsupportedOperationException("Unimplemented method 'onFrame'");
                }

                // You may implement additional TraceListener methods here if needed.
            });

            if (sniffer.start(port, baud)) {
                statusLabel.setText("Connected to " + port);
                connectBtn.setText("Disconnect");
                saveConfig(port, baud);
                startPeriodic();
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
        }
    }

    private void sendOneTimeSignal() {
        if (sniffer != null) {
            String sig = oneTimeSignalField.getText().trim();
            if (!sig.isEmpty()) {
                sniffer.sendRaw(sig.getBytes());
                traceArea.append("Sent: " + sig + "\n");
            }
        }
    }

    private void startPeriodic() {
        try {
            int rate1 = Integer.parseInt(periodicRate1Field.getText().trim());
            if (rate1 > 0) {
                periodicTimer1 = new Timer(rate1, e -> {
                    String s = periodicSignal1Field.getText().trim();
                    if (sniffer != null && !s.isEmpty()) {
                        sniffer.sendRaw(s.getBytes());
                        traceArea.append("Periodic1: " + s + "\n");
                    }
                });
                periodicTimer1.start();
            }
        } catch (Exception ignore) {}

        try {
            int rate2 = Integer.parseInt(periodicRate2Field.getText().trim());
            if (rate2 > 0) {
                periodicTimer2 = new Timer(rate2, e -> {
                    String s = periodicSignal2Field.getText().trim();
                    if (sniffer != null && !s.isEmpty()) {
                        sniffer.sendRaw(s.getBytes());
                        traceArea.append("Periodic2: " + s + "\n");
                    }
                });
                periodicTimer2.start();
            }
        } catch (Exception ignore) {}
    }

    private void stopPeriodic() {
        if (periodicTimer1 != null) periodicTimer1.stop();
        if (periodicTimer2 != null) periodicTimer2.stop();
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

    private String detectFrameType(String msg) {
        if (msg == null) return "Unknown";
        String m = msg.toUpperCase();
        if (m.contains("READ")) return "Read";
        if (m.contains("WRITE")) return "Write";
        if (m.contains("ERROR")) return "Error";
        return "Info";
    }

    private static class FrameEvent {
        String type;
        FrameEvent(String t) { type = t; }
    }

    private static class WaveformPanel extends JPanel {
        private final java.util.List<FrameEvent> events = new ArrayList<>();
        void addEvent(FrameEvent ev) {
            events.add(ev);
            if (events.size() > 30) events.remove(0);
            repaint();
        }
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            int x = 10;
            for (FrameEvent ev : events) {
                g.setColor(Color.CYAN);
                g.fillRect(x, 30, 60, 20);
                g.setColor(Color.BLACK);
                g.drawRect(x, 30, 60, 20);
                g.drawString(ev.type, x + 5, 45);
                x += 70;
            }
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(BusSnifferGUI::new);
    }
}
