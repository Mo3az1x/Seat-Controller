package eeprom;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import com.fazecast.jSerialComm.SerialPort;

public class EepromGUI extends JFrame {
    private JComboBox<String> portCombo;
    private JTextField baudField;
    private JTextArea traceArea;
    private SerialPort serialPort;

    // ==== Frame constants ====
    private static final byte HEADER = 0x7E;
    private static final byte TAIL   = 0x7F;

    // EEPROM basic commands
    private static final byte CMD_READ_BYTE   = 0x01;
    private static final byte CMD_WRITE_BYTE  = 0x02;
    private static final byte CMD_READ_ALL    = 0x03;
    private static final byte CMD_WRITE_ALL   = 0x04;

    // Utility commands
    private static final byte CMD_CLEAR       = 0x05;
    private static final byte CMD_VERIFY      = 0x06;

    // Memory Service commands
    private static final byte CMD_READ_STARTUP      = 0x11;
    private static final byte CMD_WRITE_STARTUP     = 0x12;
    private static final byte CMD_READ_CALIBRATION  = 0x13;
    private static final byte CMD_WRITE_CALIBRATION = 0x14;
    private static final byte CMD_READ_DIAG         = 0x15;
    private static final byte CMD_WRITE_DIAG        = 0x16;

    public EepromGUI() {
        super("EEPROM Programmer + Utilities + Memory Service");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(750, 600);
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

        // ===== CENTER: Tabs =====
        JTabbedPane tabbedPane = new JTabbedPane();

        // Tab 1: EEPROM Ops
        JPanel eepromPanel = new JPanel(new GridLayout(2, 2, 10, 10));
        eepromPanel.setBorder(BorderFactory.createTitledBorder("EEPROM Operations"));
        JButton readByteBtn = new JButton("Read Byte");
        JButton writeByteBtn = new JButton("Write Byte");
        JButton readAllBtn = new JButton("Read All");
        JButton writeAllBtn = new JButton("Write All");
        eepromPanel.add(readByteBtn); eepromPanel.add(writeByteBtn);
        eepromPanel.add(readAllBtn); eepromPanel.add(writeAllBtn);
        tabbedPane.add("EEPROM", eepromPanel);

        // Tab 2: Utility
        JPanel utilPanel = new JPanel(new GridLayout(1, 2, 10, 10));
        utilPanel.setBorder(BorderFactory.createTitledBorder("Utility"));
        JButton clearBtn = new JButton("Clear");
        JButton verifyBtn = new JButton("Verify");
        utilPanel.add(clearBtn); utilPanel.add(verifyBtn);
        tabbedPane.add("Utility", utilPanel);

        // Tab 3: Memory Service
        JPanel memPanel = new JPanel(new GridLayout(3, 2, 10, 10));
        memPanel.setBorder(BorderFactory.createTitledBorder("Memory Service"));
        JButton readStartupBtn = new JButton("Read Startup");
        JButton writeStartupBtn = new JButton("Write Startup");
        JButton readCalibBtn = new JButton("Read Calibration");
        JButton writeCalibBtn = new JButton("Write Calibration");
        JButton readDiagBtn = new JButton("Read Diagnostic");
        JButton writeDiagBtn = new JButton("Write Diagnostic");
        memPanel.add(readStartupBtn); memPanel.add(writeStartupBtn);
        memPanel.add(readCalibBtn);  memPanel.add(writeCalibBtn);
        memPanel.add(readDiagBtn);   memPanel.add(writeDiagBtn);
        tabbedPane.add("Memory Service", memPanel);

        add(tabbedPane, BorderLayout.CENTER);

        // ===== BOTTOM: Trace Log =====
        JPanel tracePanel = new JPanel(new BorderLayout());
        tracePanel.setBorder(BorderFactory.createTitledBorder("Trace Log"));
        traceArea = new JTextArea();
        traceArea.setEditable(false);
        JScrollPane scroll = new JScrollPane(traceArea);
        scroll.setPreferredSize(new Dimension(720, 250));
        tracePanel.add(scroll, BorderLayout.CENTER);
        add(tracePanel, BorderLayout.SOUTH);

        // ===== Button Actions =====
        connectBtn.addActionListener(e -> connectToArduino());

        // EEPROM Ops
        readByteBtn.addActionListener(e -> {
            String addrStr = JOptionPane.showInputDialog(this, "Enter address:");
            if (addrStr != null) {
                int addr = Integer.parseInt(addrStr);
                byte[] frame = buildFrame(CMD_READ_BYTE, addr, 0);
                sendFrame("READ_BYTE | Addr=" + addr, frame);
            }
        });

        writeByteBtn.addActionListener(e -> {
            String addrStr = JOptionPane.showInputDialog(this, "Enter address:");
            String valStr = JOptionPane.showInputDialog(this, "Enter value (0-255):");
            if (addrStr != null && valStr != null) {
                int addr = Integer.parseInt(addrStr);
                int val = Integer.parseInt(valStr);
                byte[] frame = buildFrame(CMD_WRITE_BYTE, addr, val);
                sendFrame("WRITE_BYTE | Addr=" + addr + " | Val=" + val, frame);
            }
        });

        readAllBtn.addActionListener(e -> {
            String sizeStr = JOptionPane.showInputDialog(this, "Enter size:");
            if (sizeStr != null) {
                int size = Integer.parseInt(sizeStr);
                byte[] frame = buildFrame(CMD_READ_ALL, size, 0);
                sendFrame("READ_ALL | Size=" + size, frame);
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
                sendFrame("WRITE_ALL | Size=" + len, frame);
            }
        });

        // Utility Ops
        clearBtn.addActionListener(e -> {
            byte[] frame = buildFrame(CMD_CLEAR, 0, 0);
            sendFrame("CLEAR EEPROM", frame);
        });

        verifyBtn.addActionListener(e -> {
            String sizeStr = JOptionPane.showInputDialog(this, "Enter size for verify:");
            if (sizeStr != null) {
                int size = Integer.parseInt(sizeStr);
                byte[] frame = buildFrame(CMD_VERIFY, size, 0);
                sendFrame("VERIFY EEPROM | Size=" + size, frame);
            }
        });

        // Memory Service Ops
        readStartupBtn.addActionListener(e -> {
            byte[] frame = buildRangeFrame(CMD_READ_STARTUP, 0x0000, 0x0003);
            sendFrame("READ_STARTUP [0x0000–0x0003]", frame);
        });

        writeStartupBtn.addActionListener(e -> {
            String valStr = JOptionPane.showInputDialog(this, "Enter 4 bytes (comma separated):");
            if (valStr != null) {
                String[] parts = valStr.split(",");
                byte[] data = new byte[parts.length];
                for (int i = 0; i < parts.length; i++) data[i] = (byte) Integer.parseInt(parts[i].trim());
                byte[] frame = buildRangeWriteFrame(CMD_WRITE_STARTUP, 0x0000, 0x0003, data);
                sendFrame("WRITE_STARTUP [0x0000–0x0003]", frame);
            }
        });

        readCalibBtn.addActionListener(e -> {
            byte[] frame = buildRangeFrame(CMD_READ_CALIBRATION, 0x0004, 0x03FF);
            sendFrame("READ_CALIBRATION [0x0004–0x03FF]", frame);
        });

        writeCalibBtn.addActionListener(e -> {
            String valStr = JOptionPane.showInputDialog(this, "Enter calibration data (comma separated):");
            if (valStr != null) {
                String[] parts = valStr.split(",");
                byte[] data = new byte[parts.length];
                for (int i = 0; i < parts.length; i++) data[i] = (byte) Integer.parseInt(parts[i].trim());
                byte[] frame = buildRangeWriteFrame(CMD_WRITE_CALIBRATION, 0x0004, 0x03FF, data);
                sendFrame("WRITE_CALIBRATION [0x0004–0x03FF]", frame);
            }
        });

        readDiagBtn.addActionListener(e -> {
            byte[] frame = buildRangeFrame(CMD_READ_DIAG, 0x0400, 0x07FF);
            sendFrame("READ_DIAG [0x0400–0x07FF]", frame);
        });

        writeDiagBtn.addActionListener(e -> {
            String valStr = JOptionPane.showInputDialog(this, "Enter diagnostic data (comma separated):");
            if (valStr != null) {
                String[] parts = valStr.split(",");
                byte[] data = new byte[parts.length];
                for (int i = 0; i < parts.length; i++) data[i] = (byte) Integer.parseInt(parts[i].trim());
                byte[] frame = buildRangeWriteFrame(CMD_WRITE_DIAG, 0x0400, 0x07FF, data);
                sendFrame("WRITE_DIAG [0x0400–0x07FF]", frame);
            }
        });

        setLocationRelativeTo(null);
        setVisible(true);

        new Thread(this::readSerial).start();
    }

    private void connectToArduino() {
        String portName = (String) portCombo.getSelectedItem();
        int baudRate = Integer.parseInt(baudField.getText());
        serialPort = SerialPort.getCommPort(portName);
        serialPort.setBaudRate(baudRate);

        if (serialPort.openPort()) {
            traceArea.append("Connected to " + portName + "\n");
        } else {
            traceArea.append("Failed to connect to " + portName + "\n");
        }
    }

    private void sendFrame(String op, byte[] frame) {
        traceArea.append("Operation: " + op + "\n");
        traceArea.append("Frame: " + toHex(frame) + "\n\n");
        if (serialPort != null && serialPort.isOpen()) {
            serialPort.writeBytes(frame, frame.length);
        }
    }

    private void readSerial() {
        byte[] buffer = new byte[1024];
        while (true) {
            if (serialPort != null && serialPort.isOpen()) {
                int bytesRead = serialPort.readBytes(buffer, buffer.length);
                if (bytesRead > 0) {
                    StringBuilder response = new StringBuilder();
                    for (int i = 0; i < bytesRead; i++) {
                        response.append(String.format("%02X ", buffer[i]));
                    }
                    traceArea.append("Received: " + response.toString() + "\n");
                }
            }
            try { Thread.sleep(100); } catch (Exception ex) {}
        }
    }

    // ==== Frame Builders ====
    private byte[] buildFrame(byte cmd, int p1, int p2) {
        byte[] frame = new byte[12];
        frame[0] = HEADER;
        frame[1] = 0x00; frame[2] = 0x00; frame[3] = 0x00; frame[4] = 0x05;
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

    private byte[] buildRangeFrame(byte cmd, int startAddr, int endAddr) {
        byte[] frame = new byte[15];
        frame[0] = HEADER;
        frame[1] = 0x00; frame[2] = 0x00; frame[3] = 0x00; frame[4] = 0x08;
        frame[5] = cmd;
        frame[6] = (byte)((startAddr >> 8) & 0xFF);
        frame[7] = (byte)(startAddr & 0xFF);
        frame[8] = (byte)((endAddr >> 8) & 0xFF);
        frame[9] = (byte)(endAddr & 0xFF);
        frame[14] = TAIL;
        return frame;
    }

    private byte[] buildRangeWriteFrame(byte cmd, int startAddr, int endAddr, byte[] data) {
        int len = 4 + data.length;
        byte[] frame = new byte[7 + len];
        frame[0] = HEADER;
        frame[1] = (byte)((len >> 24) & 0xFF);
        frame[2] = (byte)((len >> 16) & 0xFF);
        frame[3] = (byte)((len >> 8) & 0xFF);
        frame[4] = (byte)(len & 0xFF);
        frame[5] = cmd;
        frame[6] = (byte)((startAddr >> 8) & 0xFF);
        frame[7] = (byte)(startAddr & 0xFF);
        frame[8] = (byte)((endAddr >> 8) & 0xFF);
        frame[9] = (byte)(endAddr & 0xFF);
        System.arraycopy(data, 0, frame, 10, data.length);
        frame[6 + len] = TAIL;
        return frame;
    }

    private String toHex(byte[] arr) {
        StringBuilder sb = new StringBuilder();
        for (byte b : arr) sb.append(String.format("%02X ", b));
        return sb.toString().trim();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(EepromGUI::new);
    }
}
