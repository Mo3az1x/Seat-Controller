package eeprom;

import javax.swing.*;
import java.awt.*;
import java.util.List;

public class EepromGUI extends JFrame {
    private JComboBox<String> portCombo;
    private JTextField baudField;
    private JTextArea traceArea;

    // Frame constants
    private static final byte HEADER = 0x7E;
    private static final byte TAIL   = 0x7F;

    private static final byte CMD_READ_BYTE  = 0x01;
    private static final byte CMD_WRITE_BYTE = 0x02;
    private static final byte CMD_READ_ALL   = 0x03;
    private static final byte CMD_WRITE_ALL  = 0x04;

    public EepromGUI() {
        super("EEPROM Programmer");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(600, 500);
        setLayout(new BorderLayout(10, 10));

        // ===== TOP: Settings =====
        JPanel settingsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        settingsPanel.setBorder(BorderFactory.createTitledBorder("Connection Settings"));

        settingsPanel.add(new JLabel("Port:"));
        portCombo = new JComboBox<>();
        portCombo.setPreferredSize(new Dimension(200, 25));
        List<String> ports = PortUtil.getAvailablePortNames();
        for (String p : ports) portCombo.addItem(p);
        settingsPanel.add(portCombo);

        settingsPanel.add(new JLabel("Baud:"));
        baudField = new JTextField("9600");
        baudField.setPreferredSize(new Dimension(100, 25));
        settingsPanel.add(baudField);

        JButton connectBtn = new JButton("Connect");
        settingsPanel.add(connectBtn);

        add(settingsPanel, BorderLayout.NORTH);

        // ===== CENTER: EEPROM Commands =====
        JPanel commandsPanel = new JPanel(new GridLayout(2, 2, 10, 10));
        commandsPanel.setBorder(BorderFactory.createTitledBorder("EEPROM Operations"));

        JButton readByteBtn = new JButton("Read Byte");
        JButton writeByteBtn = new JButton("Write Byte");
        JButton readAllBtn = new JButton("Read All");
        JButton writeAllBtn = new JButton("Write All");

        commandsPanel.add(readByteBtn);
        commandsPanel.add(writeByteBtn);
        commandsPanel.add(readAllBtn);
        commandsPanel.add(writeAllBtn);

        add(commandsPanel, BorderLayout.CENTER);

        // ===== BOTTOM: Trace Output =====
        JPanel tracePanel = new JPanel(new BorderLayout());
        tracePanel.setBorder(BorderFactory.createTitledBorder("Trace Log"));

        traceArea = new JTextArea();
        traceArea.setEditable(false);
        JScrollPane scroll = new JScrollPane(traceArea);
        scroll.setPreferredSize(new Dimension(580, 200));
        tracePanel.add(scroll, BorderLayout.CENTER);

        add(tracePanel, BorderLayout.SOUTH);

        // ====== Button Actions ======
        readByteBtn.addActionListener(e -> {
            String addrStr = JOptionPane.showInputDialog(this, "Enter address (int):");
            if (addrStr != null) {
                int addr = Integer.parseInt(addrStr);
                byte[] frame = buildFrame(CMD_READ_BYTE, addr, 0);
                logFrame("Read Byte", frame);
            }
        });

        writeByteBtn.addActionListener(e -> {
            String addrStr = JOptionPane.showInputDialog(this, "Enter address (int):");
            String valStr = JOptionPane.showInputDialog(this, "Enter value (0-255):");
            if (addrStr != null && valStr != null) {
                int addr = Integer.parseInt(addrStr);
                int val = Integer.parseInt(valStr);
                byte[] frame = buildFrame(CMD_WRITE_BYTE, addr, val);
                logFrame("Write Byte", frame);
            }
        });

        readAllBtn.addActionListener(e -> {
            String sizeStr = JOptionPane.showInputDialog(this, "Enter size:");
            if (sizeStr != null) {
                int size = Integer.parseInt(sizeStr);
                byte[] frame = buildFrame(CMD_READ_ALL, size, 0);
                logFrame("Read All", frame);
            }
        });

        writeAllBtn.addActionListener(e -> {
            String lenStr = JOptionPane.showInputDialog(this, "Enter number of bytes:");
            if (lenStr != null) {
                int len = Integer.parseInt(lenStr);
                byte[] data = new byte[len];
                for (int i = 0; i < len; i++) {
                    String valStr = JOptionPane.showInputDialog(this, "Byte[" + i + "]:");
                    data[i] = (byte) Integer.parseInt(valStr);
                }
                byte[] frame = buildWriteAllFrame(data);
                logFrame("Write All", frame);
            }
        });

        setLocationRelativeTo(null);
        setVisible(true);
    }

    // ===== Frame Builders =====
    private byte[] buildFrame(byte cmd, int p1, int p2) {
        byte[] frame = new byte[12];
        frame[0] = HEADER;
        frame[1] = 0x00; frame[2] = 0x00; frame[3] = 0x00; frame[4] = 0x05; // length = 5
        frame[5] = cmd;
        frame[6] = (byte)((p1 >> 24) & 0xFF);
        frame[7] = (byte)((p1 >> 16) & 0xFF);
        frame[8] = (byte)((p1 >> 8) & 0xFF);
        frame[9] = (byte)(p1 & 0xFF);
        frame[10] = (byte)(p2 & 0xFF);
        frame[11] = TAIL;
        return frame;
    }

    private byte[] buildWriteAllFrame(byte[] data) {
        int len = data.length;
        byte[] frame = new byte[7 + len];
        frame[0] = HEADER;
        frame[1] = (byte)((len >> 24) & 0xFF);
        frame[2] = (byte)((len >> 16) & 0xFF);
        frame[3] = (byte)((len >> 8) & 0xFF);
        frame[4] = (byte)(len & 0xFF);
        frame[5] = CMD_WRITE_ALL;
        System.arraycopy(data, 0, frame, 6, len);
        frame[6 + len] = TAIL;
        return frame;
    }

    private void logFrame(String op, byte[] frame) {
        traceArea.append(op + " → Frame: " + toHex(frame) + "\n");
    }

    private String toHex(byte[] arr) {
        StringBuilder sb = new StringBuilder();
        for (byte b : arr) {
            sb.append(String.format("%02X ", b));
        }
        return sb.toString().trim();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(EepromGUI::new);
    }
}
