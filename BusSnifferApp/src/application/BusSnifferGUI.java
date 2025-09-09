package application;

import javax.swing.*;
import java.awt.*;

public class BusSnifferGUI extends JFrame {
    private JComboBox<String> portCombo;
    private JTextField baudField;
    private JLabel statusLabel;

    // Components for User Input and Periodic tasks
    private JTextField oneTimeSignalField;
    private JButton sendOneTimeBtn;
    private JTextField periodicSignal1Field;
    private JTextField periodicRate1Field;
    private JTextField periodicSignal2Field;
    private JTextField periodicRate2Field;
  
    // Trace area
    private JTextArea traceArea;

    private SnifferManager sniffer;

    public BusSnifferGUI() {
        super("Bus Sniffer Application");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(500, 600); // Adjusted size to fit all components
        setLayout(new BorderLayout(10, 10)); // Add some spacing

        // ====== TOP Panel: Settings ======
        JPanel settingsPanel = new JPanel(new GridLayout(3, 2, 5, 5));
        settingsPanel.setBorder(BorderFactory.createTitledBorder("Settings"));

        // COM Ports
        settingsPanel.add(new JLabel("COM:"));
        portCombo = new JComboBox<>();
        String[] ports = PortUtil.getAllPorts();
        for (String p : ports) portCombo.addItem(p);
        settingsPanel.add(portCombo);

        // Baud Rate
        settingsPanel.add(new JLabel("Rate:"));
        baudField = new JTextField("9600");
        settingsPanel.add(baudField);

        // On/Off button as per diagram
        settingsPanel.add(new JLabel()); // Empty label for spacing
        JToggleButton onOffToggle = new JToggleButton("ON/OFF");
        settingsPanel.add(onOffToggle);

        // This button will control the connection, similar to the original connectBtn
        onOffToggle.addActionListener(e -> toggleConnection());

        add(settingsPanel, BorderLayout.NORTH);

        // ====== CENTER Panel: User Input and Periodic Tasks ======
        JPanel centerPanel = new JPanel();
        centerPanel.setLayout(new BoxLayout(centerPanel, BoxLayout.Y_AXIS));

        // User Input (One-time signal)
        JPanel oneTimePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        oneTimePanel.setBorder(BorderFactory.createTitledBorder("User Input"));
        oneTimeSignalField = new JTextField(15);
        sendOneTimeBtn = new JButton("Send");
        oneTimePanel.add(new JLabel("Signal:"));
        oneTimePanel.add(oneTimeSignalField);
        oneTimePanel.add(sendOneTimeBtn);
        centerPanel.add(oneTimePanel);
        
        // Periodic Tasks
        JPanel periodicPanel = new JPanel(new GridLayout(2, 4, 5, 5));
        periodicPanel.setBorder(BorderFactory.createTitledBorder("Periodic tasks"));

        // Periodic Task 1
        periodicPanel.add(new JLabel("Periodic:"));
        periodicSignal1Field = new JTextField(10);
        periodicPanel.add(periodicSignal1Field);
        periodicPanel.add(new JLabel("User Input:"));
        periodicRate1Field = new JTextField(5);
        periodicPanel.add(periodicRate1Field);
    

        // Periodic Task 2
        periodicPanel.add(new JLabel("Periodic:"));
        periodicSignal2Field = new JTextField(10);
        periodicPanel.add(periodicSignal2Field);
        periodicPanel.add(new JLabel("User Input:"));
        periodicRate2Field = new JTextField(5);
        periodicPanel.add(periodicRate2Field);


        centerPanel.add(periodicPanel);

        add(centerPanel, BorderLayout.CENTER);

        // ====== BOTTOM Panel: Trace Area and Status Bar ======
        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.setBorder(BorderFactory.createTitledBorder("Trace"));
        traceArea = new JTextArea(10, 40); // Set preferred rows and columns
        traceArea.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(traceArea);
        scrollPane.setPreferredSize(new Dimension(480, 180)); // Explicit size for the scroll pane
        bottomPanel.add(scrollPane, BorderLayout.CENTER);

        // Status bar
        JPanel statusBarPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        statusLabel = new JLabel("Disconnected");
        statusBarPanel.add(statusLabel);
        bottomPanel.add(statusBarPanel, BorderLayout.SOUTH);

        add(bottomPanel, BorderLayout.SOUTH);
        
        // Finalize frame
        pack(); // Packs components to their preferred size
        setVisible(true);
    }

    private void toggleConnection() {
    if (sniffer == null) {
        // Connect
        String portName = (String) portCombo.getSelectedItem();
        int baud = Integer.parseInt(baudField.getText().trim());

        // Create SnifferManager with TraceListener
        sniffer = new SnifferManager(message -> {
            traceArea.append(message + "\n");
            traceArea.setCaretPosition(traceArea.getDocument().getLength());
        });

        boolean ok = sniffer.start(portName, baud);
        if (ok) {
            statusLabel.setText("Connected to " + portName);
            statusLabel.setBackground(Color.GREEN);
            traceArea.append("Connected to " + portName + " @ " + baud + " baud.\n");
        } else {
            statusLabel.setText("Failed to connect!");
            statusLabel.setBackground(Color.RED);
            traceArea.append("Failed to open port " + portName + "\n");
            sniffer = null;
        }
    } else {
        // Disconnect
        sniffer.close();
        sniffer = null;
        statusLabel.setText("Disconnected");
        statusLabel.setBackground(Color.LIGHT_GRAY);
        traceArea.append("Disconnected from serial port.\n");
    }
}

    public static void main(String[] args) {
        SwingUtilities.invokeLater(BusSnifferGUI::new);
    }
}