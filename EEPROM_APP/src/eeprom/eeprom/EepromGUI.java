package eeprom;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.*;
import java.util.List;
import com.fazecast.jSerialComm.SerialPort;

public class EepromGUI extends JFrame {
    private JComboBox<String> portCombo;
    private JTextField baudField;
    private JTextArea traceArea;
    private SerialPort serialPort;
    private JLabel connectionStatus;
    private JButton connectBtn;
    private JButton disconnectBtn;
    private boolean isConnected = false;
    private boolean isNewOperation = true;
    private byte[] lastReadData = null;
    private String lastOperation = "";
    private boolean saveDialogShown = false;

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
        portCombo.setPreferredSize(new Dimension(200, 30));
        List<String> ports = PortUtil.getAvailablePortNames();
        for (String p : ports) portCombo.addItem(p);
        settingsPanel.add(portCombo);

        settingsPanel.add(new JLabel("Baud:"));
        baudField = new JTextField("9600");
        baudField.setPreferredSize(new Dimension(100, 30));
        settingsPanel.add(baudField);

        connectBtn = createStyledButton("Connect");
        disconnectBtn = createStyledButton("Disconnect");
        disconnectBtn.setEnabled(false);
        
        // Connection status indicator
        connectionStatus = new JLabel("●");
        connectionStatus.setFont(new Font("Arial", Font.BOLD, 16));
        connectionStatus.setForeground(Color.RED);
        connectionStatus.setToolTipText("Disconnected");
        
        settingsPanel.add(connectBtn);
        settingsPanel.add(disconnectBtn);
        settingsPanel.add(connectionStatus);
        add(settingsPanel, BorderLayout.NORTH);

        // ===== CENTER: Tabs =====
        JTabbedPane tabbedPane = new JTabbedPane();

        // Tab 1: EEPROM Ops
        JPanel eepromPanel = new JPanel(new GridLayout(2, 2, 10, 10));
        eepromPanel.setBorder(BorderFactory.createTitledBorder("EEPROM Operations"));
        JButton readByteBtn = createStyledButton("Read Byte");
        JButton writeByteBtn = createStyledButton("Write Byte");
        JButton readAllBtn = createStyledButton("Read All");
        JButton writeAllBtn = createStyledButton("Write All");
        eepromPanel.add(readByteBtn); eepromPanel.add(writeByteBtn);
        eepromPanel.add(readAllBtn); eepromPanel.add(writeAllBtn);
        tabbedPane.add("EEPROM", eepromPanel);

        // Tab 2: Utility
        JPanel utilPanel = new JPanel(new GridLayout(1, 2, 10, 10));
        utilPanel.setBorder(BorderFactory.createTitledBorder("Utility"));
        JButton clearBtn = createStyledButton("Clear");
        JButton verifyBtn = createStyledButton("Verify");
        utilPanel.add(clearBtn); utilPanel.add(verifyBtn);
        tabbedPane.add("Utility", utilPanel);

        // Tab 3: Memory Service
        JPanel memPanel = new JPanel(new GridLayout(3, 2, 10, 10));
        memPanel.setBorder(BorderFactory.createTitledBorder("Memory Service"));
        JButton readStartupBtn = createStyledButton("Read Startup");
        JButton writeStartupBtn = createStyledButton("Write Startup");
        JButton readCalibBtn = createStyledButton("Read Calibration");
        JButton writeCalibBtn = createStyledButton("Write Calibration");
        JButton readDiagBtn = createStyledButton("Read Diagnostic");
        JButton writeDiagBtn = createStyledButton("Write Diagnostic");
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
        disconnectBtn.addActionListener(e -> disconnectFromArduino());

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
            // Show file chooser dialog
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setDialogTitle("Load EEPROM Data");
            fileChooser.setFileFilter(new FileNameExtensionFilter("Binary Files (*.bin)", "bin"));
            
            if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
                File selectedFile = fileChooser.getSelectedFile();
                byte[] data = loadDataFromFile(selectedFile);
                
                if (data != null) {
                    traceArea.append("Loaded " + data.length + " bytes from: " + selectedFile.getName() + "\n");
                    byte[] frame = buildWriteAllFrame(data);
                    sendFrame("WRITE_ALL | Size=" + data.length + " | File=" + selectedFile.getName(), frame);
                } else {
                    JOptionPane.showMessageDialog(this, 
                        "Error loading file. Please try again.", 
                        "Load Error", 
                        JOptionPane.ERROR_MESSAGE);
                }
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
            isConnected = true;
            connectionStatus.setForeground(Color.GREEN);
            connectionStatus.setToolTipText("Connected to " + portName);
            connectBtn.setEnabled(false);
            disconnectBtn.setEnabled(true);
            traceArea.append("Connected to " + portName + "\n");
        } else {
            isConnected = false;
            connectionStatus.setForeground(Color.RED);
            connectionStatus.setToolTipText("Connection failed");
            traceArea.append("Failed to connect to " + portName + "\n");
        }
    }
    
    private void disconnectFromArduino() {
        if (serialPort != null && serialPort.isOpen()) {
            serialPort.closePort();
        }
        isConnected = false;
        connectionStatus.setForeground(Color.RED);
        connectionStatus.setToolTipText("Disconnected");
        connectBtn.setEnabled(true);
        disconnectBtn.setEnabled(false);
        traceArea.append("Disconnected from serial port\n");
    }

    private void sendFrame(String op, byte[] frame) {
        traceArea.append("Operation: " + op + "\n");
        traceArea.append("Frame: " + toHex(frame) + "\n\n");
        isNewOperation = true; // Reset flag for new operation
        lastOperation = op; // Store operation type for file handling
        lastReadData = null; // Clear previous data
        saveDialogShown = false; // Reset save dialog flag
        if (isConnected && serialPort != null && serialPort.isOpen()) {
            serialPort.writeBytes(frame, frame.length);
        } else {
            traceArea.append("Warning: Not connected to serial port. Operation not sent.\n\n");
        }
    }

    private void readSerial() {
        byte[] buffer = new byte[1024];
        while (true) {
            if (serialPort != null && serialPort.isOpen()) {
                int bytesRead = serialPort.readBytes(buffer, buffer.length);
                if (bytesRead > 0) {
                    // Print "Received:" only once per operation
                    if (isNewOperation) {
                        traceArea.append("Received:\n");
                        isNewOperation = false;
                    }
                    
                    // Store received data for Read All operations
                    if (lastOperation.contains("READ_ALL")) {
                        storeReceivedData(buffer, bytesRead);
                    }
                    
                    // Parse mixed data - separate binary frames from ASCII text
                    parseMixedData(buffer, bytesRead);
                    
                    // Show save dialog for Read All operations after data is received
                    if (lastOperation.contains("READ_ALL") && lastReadData != null && !saveDialogShown) {
                        saveDialogShown = true;
                        SwingUtilities.invokeLater(this::showSaveDialog);
                    }
                }
            }
            try { Thread.sleep(100); } catch (Exception ex) {}
        }
    }
    
    private void parseMixedData(byte[] buffer, int length) {
        // Check if this looks like a complete ASCII message (startup text)
        if (isLikelyAsciiMessage(buffer, length)) {
            String asciiText = new String(buffer, 0, length);
            traceArea.append(asciiText);
            return;
        }
        
        // Otherwise, display as hex data
        String hexData = formatHexData(buffer, 0, length);
        traceArea.append(hexData + "\n");
    }
    
    private boolean isLikelyAsciiMessage(byte[] buffer, int length) {
        // Check for common ASCII patterns that indicate startup messages
        String text = new String(buffer, 0, length);
        
        // Look for startup message patterns
        if (text.contains("EEPROM") || text.contains("Emulator") || 
            text.contains("Started") || text.contains("===") ||
            text.contains("UART") || text.contains("SPI")) {
            return true;
        }
        
        // Check if it's mostly printable ASCII with reasonable length
        int printableCount = 0;
        for (int i = 0; i < length; i++) {
            byte b = buffer[i];
            if (b >= 32 && b <= 126) { // Printable ASCII range
                printableCount++;
            }
        }
        
        // Only consider it ASCII if it's mostly printable AND has reasonable length
        // AND doesn't look like binary data (no frame headers/tails)
        boolean hasFrameMarkers = false;
        for (int i = 0; i < length; i++) {
            if (buffer[i] == HEADER || buffer[i] == TAIL) {
                hasFrameMarkers = true;
                break;
            }
        }
        
        return !hasFrameMarkers && 
               (double) printableCount / length > 0.8 && 
               length > 10; // Reasonable length for ASCII message
    }
    
    private void storeReceivedData(byte[] buffer, int bytesRead) {
        if (lastReadData == null) {
            lastReadData = new byte[bytesRead];
            System.arraycopy(buffer, 0, lastReadData, 0, bytesRead);
        } else {
            // Append to existing data
            byte[] newData = new byte[lastReadData.length + bytesRead];
            System.arraycopy(lastReadData, 0, newData, 0, lastReadData.length);
            System.arraycopy(buffer, 0, newData, lastReadData.length, bytesRead);
            lastReadData = newData;
        }
    }
    
    private void showSaveDialog() {
        int result = JOptionPane.showConfirmDialog(this, 
            "Read All operation completed. Do you want to save the data to a file?", 
            "Save EEPROM Data", 
            JOptionPane.YES_NO_OPTION);
            
        if (result == JOptionPane.YES_OPTION) {
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setDialogTitle("Save EEPROM Data");
            fileChooser.setFileFilter(new FileNameExtensionFilter("Binary Files (*.bin)", "bin"));
            fileChooser.setSelectedFile(new File("eeprom_data.bin"));
            
            if (fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
                File selectedFile = fileChooser.getSelectedFile();
                if (saveDataToFile(selectedFile, lastReadData)) {
                    JOptionPane.showMessageDialog(this, 
                        "Data saved successfully to: " + selectedFile.getName(), 
                        "Save Complete", 
                        JOptionPane.INFORMATION_MESSAGE);
                } else {
                    JOptionPane.showMessageDialog(this, 
                        "Error saving file. Please try again.", 
                        "Save Error", 
                        JOptionPane.ERROR_MESSAGE);
                }
            }
        }
    }
    
    private boolean saveDataToFile(File file, byte[] data) {
        try (FileOutputStream fos = new FileOutputStream(file)) {
            fos.write(data);
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }
    
    private byte[] loadDataFromFile(File file) {
        try (FileInputStream fis = new FileInputStream(file)) {
            byte[] data = new byte[(int) file.length()];
            fis.read(data);
            return data;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
    
    
    private String formatHexData(byte[] data, int start, int length) {
        StringBuilder sb = new StringBuilder();
        for (int i = start; i < start + length; i++) {
            if ((i - start) % 16 == 0 && (i - start) > 0) {
                sb.append("\n");
            }
            sb.append(String.format("%02X ", data[i] & 0xFF));
            
            // Add extra space after every 4 bytes for better readability
            if ((i - start + 1) % 4 == 0 && (i - start + 1) % 16 != 0) {
                sb.append(" ");
            }
        }
        return sb.toString();
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
    
    
    private JButton createStyledButton(String text) {
        JButton button = new JButton(text);
        button.setPreferredSize(new Dimension(120, 35));
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setBackground(new Color(240, 240, 240));
        button.setForeground(Color.BLACK);
        button.setFont(new Font("Arial", Font.PLAIN, 12));
        
        // Add rounded border
        button.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(200, 200, 200), 1),
            new EmptyBorder(8, 12, 8, 12)
        ));
        
        // Add hover effect
        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                if (button.isEnabled()) {
                    button.setBackground(new Color(220, 220, 220));
                }
            }
            
            @Override
            public void mouseExited(MouseEvent e) {
                if (button.isEnabled()) {
                    button.setBackground(new Color(240, 240, 240));
                }
            }
        });
        
        return button;
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(EepromGUI::new);
    }
}
