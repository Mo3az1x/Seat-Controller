package communication;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

import application.SeatControllerBusSniffer;
import application.SeatControllerSnifferManager;
import application.TraceListener;

import java.text.SimpleDateFormat;

/**
 * Enhanced Main Console Interface for Seat Controller ECU Bus Sniffer
 * Provides comprehensive command-line interface for ECU development and testing
 */
public class Main {
    
    private static final Scanner scanner = new Scanner(System.in);
    private static SeatControllerSnifferManager sniffer;
    private static final AtomicBoolean running = new AtomicBoolean(true);
    private static String currentPort = null;
    private static String currentProtocol = "UART";
    private static int currentBaud = 115200;
    
    // Command history and statistics
    private static final List<String> commandHistory = new ArrayList<>();
    private static int commandCount = 0;
    
    public static void main(String[] args) {
        printBanner();
        
        // Handle command line arguments
        if (args.length > 0) {
            handleCommandLineArgs(args);
        }
        
        // Interactive mode
        runInteractiveMode();
        
        cleanup();
        System.out.println("Seat Controller ECU Sniffer terminated.");
    }
    
    private static void printBanner() {
        System.out.println("╔═══════════════════════════════════════════════════════════════╗");
        System.out.println("║          SEAT CONTROLLER ECU BUS SNIFFER v2.0                ║");
        System.out.println("║          Enhanced for Automotive ECU Development             ║");
        System.out.println("║          UART communication only (legacy cmds kept)          ║");
        System.out.println("╚═══════════════════════════════════════════════════════════════╝");
        System.out.println();
    }
    
    private static void handleCommandLineArgs(String[] args) {
        // Simple argument parsing
        for (int i = 0; i < args.length; i++) {
            switch (args[i].toLowerCase()) {
                case "-port":
                case "--port":
                    if (i + 1 < args.length) {
                        currentPort = args[++i];
                        System.out.println("Default port set to: " + currentPort);
                    }
                    break;
                case "-baud":
                case "--baud":
                    if (i + 1 < args.length) {
                        try {
                            currentBaud = Integer.parseInt(args[++i]);
                            System.out.println("Default baud rate set to: " + currentBaud);
                        } catch (NumberFormatException e) {
                            System.err.println("Invalid baud rate: " + args[i]);
                        }
                    }
                    break;
                case "-protocol":
                case "--protocol":
                    if (i + 1 < args.length) {
                        i++; // consume value, but UART is forced
                        currentProtocol = "UART";
                        System.out.println("Protocol forced to UART (I2C/SPI removed)");
                    }
                    break;
                case "-help":
                case "--help":
                case "-h":
                    printUsage();
                    System.exit(0);
                    break;
                case "-gui":
                case "--gui":
                    System.out.println("Starting GUI mode...");
                    javax.swing.SwingUtilities.invokeLater(() -> new SeatControllerBusSniffer());
                    return;
            }
        }
    }
    
    private static void printUsage() {
        System.out.println("Usage: java -jar SeatControllerSniffer.jar [options]");
        System.out.println("Options:");
        System.out.println("  -port <name>      Set default COM port (e.g., COM3, /dev/ttyUSB0)");
        System.out.println("  -baud <rate>      Set default baud rate (default: 115200)");
        System.out.println("  -protocol <type>  Legacy option; UART is forced");
        System.out.println("  -gui              Start graphical interface");
        System.out.println("  -help             Show this help message");
        System.out.println();
        System.out.println("Examples:");
        System.out.println("  java -jar SeatControllerSniffer.jar -port COM3 -baud 115200");
        System.out.println("  java -jar SeatControllerSniffer.jar -gui");
    }
    
    private static void runInteractiveMode() {
        showMainMenu();
        
        while (running.get()) {
            try {
                System.out.print("\nSeat-ECU> ");
                String input = scanner.nextLine().trim();
                
                if (input.isEmpty()) continue;
                
                // Add to history
                commandHistory.add(input);
                commandCount++;
                
                // Process command
                processCommand(input);
                
            } catch (Exception e) {
                System.err.println("Error processing command: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }
    
    private static void showMainMenu() {
        System.out.println("=== MAIN MENU ===");
        System.out.println("Connection Commands:");
        System.out.println("  scan              - Scan for available COM ports");
        System.out.println("  connect [port]    - Connect to ECU (auto-detect port if not specified)");
        System.out.println("  disconnect        - Disconnect from ECU");
        System.out.println("  status            - Show connection status");
        System.out.println("  protocol <type>   - Legacy; shows UART-only note");
        System.out.println();
        System.out.println("ECU Commands:");
        System.out.println("  seat <h> <s> <i>  - Send seat control (height, slide, incline)");
        System.out.println("  alive             - Send alive message");
        System.out.println("  fault <1|2>       - Trigger fault condition");
        System.out.println("  gearbox <g> <t>   - Send gearbox status (gear, torque)");
        System.out.println();
        System.out.println("EEPROM Commands:");
        System.out.println("  read <addr>       - Read byte from EEPROM");
        System.out.println("  write <addr> <val>- Write byte to EEPROM");
        System.out.println("  readall [size]    - Read all EEPROM (default 256 bytes)");
        System.out.println("  writeall <file>   - Write file to EEPROM");
        System.out.println("  dump <start> <len>- Hex dump EEPROM range");
        System.out.println();
        System.out.println("Profile Commands:");
        System.out.println("  saveprofile <id>  - Save current profile to EEPROM");
        System.out.println("  loadprofile <id>  - Load profile from EEPROM");
        System.out.println("Profiles:");
        System.out.println("  saveprofile <id>  - Save current profile to EEPROM");
        System.out.println("  loadprofile <id>  - Load profile from EEPROM");
        System.out.println();
        System.out.println("Utility Commands:");
        System.out.println("  monitor           - Start continuous monitoring");
        System.out.println("  send <hex>        - Send raw hex data");
        System.out.println("  log <on|off>      - Enable/disable logging");
        System.out.println("  stats             - Show communication statistics");
        System.out.println("  history           - Show command history");
        System.out.println("  clear             - Clear screen");
        System.out.println("  help              - Show this menu");
        System.out.println("  exit              - Exit application");
        System.out.println();
        System.out.println("Profile Commands:");
        System.out.println("  saveprofile <id>  - Save current profile to EEPROM");
        System.out.println("  loadprofile <id>  - Load profile from EEPROM");
    }
    
    private static void processCommand(String input) {
        String[] parts = input.toLowerCase().split("\\s+");
        String command = parts[0];
        
        switch (command) {
            case "scan":
                scanPorts();
                break;
            case "connect":
                connectToECU(parts.length > 1 ? parts[1] : null);
                break;
            case "disconnect":
                disconnectFromECU();
                break;
            case "status":
                showConnectionStatus();
                break;
            case "protocol":
                setProtocol(parts.length > 1 ? parts[1] : null);
                break;
            case "seat":
                sendSeatControl(parts);
                break;
            case "alive":
                sendAliveMessage();
                break;
            case "fault":
                triggerFault(parts.length > 1 ? parts[1] : "1");
                break;
            case "gearbox":
                sendGearboxStatus(parts);
                break;
            case "read":
                readEEPROM(parts);
                break;
            case "write":
                writeEEPROM(parts);
                break;
            case "readall":
                readAllEEPROM(parts.length > 1 ? parts[1] : "256");
                break;
            case "writeall":
                writeAllEEPROM(parts.length > 1 ? parts[1] : null);
                break;
            case "dump":
                dumpEEPROM(parts);
                break;
            case "saveprofile":
                saveProfile(parts);
                break;
            case "loadprofile":
                loadProfile(parts);
                break;
            case "monitor":
                startMonitoring();
                break;
            case "send":
                sendRawData(input.substring(4).trim());
                break;
            case "log":
                toggleLogging(parts.length > 1 ? parts[1] : "toggle");
                break;
            case "stats":
                showStatistics();
                break;
            case "history":
                showHistory();
                break;
            case "clear":
                clearScreen();
                break;
            case "help":
            case "menu":
                showMainMenu();
                break;
            case "exit":
            case "quit":
                exit();
                break;
            default:
                System.out.println("Unknown command: " + command);
                System.out.println("Type 'help' for available commands.");
        }
    }

    private static void saveProfile(String[] parts) {
        if (!checkConnection()) return;
        if (parts.length < 2) {
            System.err.println("Usage: saveprofile <id>");
            return;
        }
        try {
            int id = Integer.parseInt(parts[1]);
            sniffer.saveProfile(id);
            System.out.println("✓ Saving profile id=" + id);
        } catch (NumberFormatException e) {
            System.err.println("Invalid profile id: " + parts[1]);
        }
    }

    private static void loadProfile(String[] parts) {
        if (!checkConnection()) return;
        if (parts.length < 2) {
            System.err.println("Usage: loadprofile <id>");
            return;
        }
        try {
            int id = Integer.parseInt(parts[1]);
            sniffer.loadProfile(id);
            System.out.println("✓ Loading profile id=" + id);
        } catch (NumberFormatException e) {
            System.err.println("Invalid profile id: " + parts[1]);
        }
    }
    
    private static void scanPorts() {
        System.out.println("Scanning for available ports...");
        PortUtil.listPorts();
        
        // Show recommended ports
        PortUtil.PortInfo[] recommended = PortUtil.getRecommendedECUPorts();
        if (recommended.length > 0) {
            System.out.println("\nRecommended for ECU development:");
            for (int i = 0; i < recommended.length; i++) {
                System.out.printf("  [R%d] %s - %s%n", i, 
                    recommended[i].systemPortName, recommended[i].descriptivePortName);
            }
        }
    }
    
    private static void connectToECU(String portName) {
        if (sniffer != null && sniffer.isConnected()) {
            System.out.println("Already connected. Use 'disconnect' first.");
            return;
        }
        
        // Auto-detect port if not specified
        if (portName == null) {
            if (currentPort != null) {
                portName = currentPort;
            } else {
                portName = PortUtil.getDefaultPort();
                System.out.println("Auto-detected port: " + portName);
            }
        }
        
        // Validate port
        if (!PortUtil.isValidPortName(portName)) {
            System.err.println("Invalid port name: " + portName);
            return;
        }
        
        if (!PortUtil.isPortAvailable(portName)) {
            System.err.println("Port not available: " + portName);
            return;
        }
        
        // Create sniffer with console trace listener
        sniffer = new SeatControllerSnifferManager(new ConsoleTraceListener());
        
        System.out.printf("Connecting to %s @ %d baud (%s protocol)...\n", 
            portName, currentBaud, currentProtocol);
        
        if (sniffer.start(portName, currentBaud, currentProtocol)) {
            currentPort = portName;
            System.out.println("✓ Connected successfully!");
            
            // Send initial alive message to announce presence
            Timer initTimer = new Timer();
            initTimer.schedule(new TimerTask() {
                @Override
                public void run() {
                    sendAliveMessage();
                }
            }, 1000); // 1 second delay
            
        } else {
            System.err.println("✗ Connection failed!");
            sniffer = null;
        }
    }
    
    private static void disconnectFromECU() {
        if (sniffer == null) {
            System.out.println("Not connected.");
            return;
        }
        
        System.out.println("Disconnecting...");
        sniffer.close();
        sniffer = null;
        System.out.println("✓ Disconnected.");
    }
    
    private static void showConnectionStatus() {
        if (sniffer == null) {
            System.out.println("Status: NOT CONNECTED");
            System.out.println("Default port: " + (currentPort != null ? currentPort : "Auto-detect"));
            System.out.println("Default baud: " + currentBaud);
            System.out.println("Protocol: UART (forced)");
        } else {
            System.out.println("Status: CONNECTED");
            System.out.println("Port: " + currentPort);
            System.out.println("Baud: " + currentBaud);
            System.out.println("Protocol: UART (forced)");
            System.out.println("Uptime: " + getUptimeString());
        }
    }
    
    private static void setProtocol(String protocol) {
        if (protocol == null) {
            System.out.println("Current protocol: " + currentProtocol);
            System.out.println("Note: UART-only. I2C/SPI removed.");
            return;
        }
        
        System.out.println("Note: UART-only mode. Ignoring requested protocol '" + protocol + "'.");
        currentProtocol = "UART";
    }
    
    private static void sendSeatControl(String[] parts) {
        if (!checkConnection()) return;
        
        if (parts.length < 4) {
            System.err.println("Usage: seat <height_cm> <slide_cm> <incline_deg>");
            System.err.println("Example: seat 3.5 5.0 85.0");
            return;
        }
        
        try {
            double height = Double.parseDouble(parts[1]);
            double slide = Double.parseDouble(parts[2]);
            double incline = Double.parseDouble(parts[3]);
            
            // Validate ranges
            if (height < 2.0 || height > 5.3) {
                System.err.println("Height must be between 2.0 and 5.3 cm");
                return;
            }
            if (slide < 3.0 || slide > 7.5) {
                System.err.println("Slide must be between 3.0 and 7.5 cm");
                return;
            }
            if (incline < 67.0 || incline > 105.0) {
                System.err.println("Incline must be between 67.0 and 105.0 degrees");
                return;
            }
            
            sniffer.sendSeatControlRequest(height, slide, incline);
            System.out.printf("✓ Sent seat control: H=%.1f S=%.1f I=%.1f\n", height, slide, incline);
            
        } catch (NumberFormatException e) {
            System.err.println("Invalid numeric values provided");
        }
    }
    
    private static void sendAliveMessage() {
        if (!checkConnection()) return;
        
        long timestamp = System.currentTimeMillis() & 0xFFFF;
        int counter = commandCount & 0xFFFF;
        
        sniffer.sendAliveMessage(timestamp, counter);
        System.out.printf("✓ Sent alive message: ts=%d cnt=%d\n", timestamp, counter);
    }
    
    private static void triggerFault(String faultStr) {
        if (!checkConnection()) return;
        
        try {
            int faultNumber = Integer.parseInt(faultStr);
            if (faultNumber < 1 || faultNumber > 2) {
                System.err.println("Fault number must be 1 or 2");
                return;
            }
            
            sniffer.sendFaultMessage(faultNumber);
            System.out.println("✓ Triggered Fault " + faultNumber);
            
            // Show fault timing info
            String timing = faultNumber == 1 ? "1ms activation, 2ms deactivation" : 
                                             "2ms activation, 5ms deactivation";
            System.out.println("  Timing: " + timing);
            
        } catch (NumberFormatException e) {
            System.err.println("Invalid fault number: " + faultStr);
        }
    }
    
    private static void sendGearboxStatus(String[] parts) {
        if (!checkConnection()) return;
        
        if (parts.length < 3) {
            System.err.println("Usage: gearbox <gear> <torque>");
            System.err.println("Example: gearbox 3 250");
            return;
        }
        
        try {
            int gear = Integer.parseInt(parts[1]);
            int torque = Integer.parseInt(parts[2]);
            int maxTorque = 400; // Default max torque
            
            sniffer.sendGearboxStatus(gear, torque, maxTorque);
            System.out.printf("✓ Sent gearbox status: Gear=%d Torque=%d/%d\n", 
                gear, torque, maxTorque);
            
        } catch (NumberFormatException e) {
            System.err.println("Invalid numeric values provided");
        }
    }
    
    private static void readEEPROM(String[] parts) {
        if (!checkConnection()) return;
        
        if (parts.length < 2) {
            System.err.println("Usage: read <address>");
            System.err.println("Example: read 0x10 or read 16");
            return;
        }
        
        try {
            int address = parseAddress(parts[1]);
            sniffer.sendReadByte(address);
            System.out.printf("✓ Reading EEPROM address 0x%02X\n", address);
            
        } catch (NumberFormatException e) {
            System.err.println("Invalid address: " + parts[1]);
        }
    }
    
    private static void writeEEPROM(String[] parts) {
        if (!checkConnection()) return;
        
        if (parts.length < 3) {
            System.err.println("Usage: write <address> <value>");
            System.err.println("Example: write 0x10 0xFF or write 16 255");
            return;
        }
        
        try {
            int address = parseAddress(parts[1]);
            int value = parseAddress(parts[2]); // Can parse hex or decimal
            
            if (value < 0 || value > 255) {
                System.err.println("Value must be between 0 and 255");
                return;
            }
            
            sniffer.sendWriteByte(address, value);
            System.out.printf("✓ Writing 0x%02X to EEPROM address 0x%02X\n", value, address);
            
        } catch (NumberFormatException e) {
            System.err.println("Invalid address or value provided");
        }
    }
    
    private static void readAllEEPROM(String sizeStr) {
        if (!checkConnection()) return;
        
        try {
            int size = Integer.parseInt(sizeStr);
            if (size <= 0 || size > 65536) {
                System.err.println("Size must be between 1 and 65536 bytes");
                return;
            }
            
            sniffer.sendReadAll(size);
            System.out.printf("✓ Reading all EEPROM (%d bytes)\n", size);
            
        } catch (NumberFormatException e) {
            System.err.println("Invalid size: " + sizeStr);
        }
    }
    
    private static void writeAllEEPROM(String filename) {
        if (!checkConnection()) return;
        
        if (filename == null) {
            System.err.println("Usage: writeall <filename>");
            System.err.println("Example: writeall calibration.bin");
            return;
        }
        
        try {
            // For demo purposes, create a test pattern
            byte[] data = new byte[256];
            for (int i = 0; i < data.length; i++) {
                data[i] = (byte)(i & 0xFF);
            }
            
            sniffer.sendWriteAll(data);
            System.out.printf("✓ Writing %d bytes to EEPROM (test pattern)\n", data.length);
            System.out.println("Note: File loading not implemented in demo version");
            
        } catch (Exception e) {
            System.err.println("Error writing EEPROM: " + e.getMessage());
        }
    }
    
    private static void dumpEEPROM(String[] parts) {
        if (parts.length < 3) {
            System.err.println("Usage: dump <start_addr> <length>");
            System.err.println("Example: dump 0x00 16");
            return;
        }
        
        try {
            int startAddr = parseAddress(parts[1]);
            int length = Integer.parseInt(parts[2]);
            
            System.out.printf("EEPROM Hex Dump (0x%02X - 0x%02X):\n", startAddr, startAddr + length - 1);
            
            // For demo, show simulated data
            for (int i = 0; i < length; i += 16) {
                System.out.printf("%04X: ", startAddr + i);
                
                // Hex values
                for (int j = 0; j < 16 && (i + j) < length; j++) {
                    int addr = startAddr + i + j;
                    System.out.printf("%02X ", addr & 0xFF); // Simulated data
                }
                
                // ASCII representation
                System.out.print(" |");
                for (int j = 0; j < 16 && (i + j) < length; j++) {
                    int value = (startAddr + i + j) & 0xFF;
                    char c = (value >= 32 && value <= 126) ? (char)value : '.';
                    System.out.print(c);
                }
                System.out.println("|");
            }
            
            System.out.println("Note: This is simulated data. Use 'readall' for actual EEPROM content.");
            
        } catch (NumberFormatException e) {
            System.err.println("Invalid address or length provided");
        }
    }
    
    private static void startMonitoring() {
        if (!checkConnection()) return;
        
        System.out.println("Starting continuous monitoring...");
        System.out.println("Press 'q' + Enter to quit monitoring mode");
        
        // Create monitoring thread
        Thread monitorThread = new Thread(() -> {
            Timer timer = new Timer();
            
            // Send periodic alive messages
            timer.scheduleAtFixedRate(new TimerTask() {
                @Override
                public void run() {
                    if (sniffer != null) {
                        sendAliveMessage();
                    }
                }
            }, 0, 5000); // Every 5 seconds
            
            // Send periodic gearbox status
            timer.scheduleAtFixedRate(new TimerTask() {
                private int gear = 1;
                @Override
                public void run() {
                    if (sniffer != null) {
                        int torque = 100 + (int)(Math.random() * 200);
                        sniffer.sendGearboxStatus(gear, torque, 400);
                        gear = (gear % 6) + 1; // Cycle through gears 1-6
                    }
                }
            }, 1000, 10000); // Every 10 seconds
            
            // Wait for user input to stop
            try {
                while (true) {
                    String input = scanner.nextLine().trim();
                    if ("q".equalsIgnoreCase(input)) {
                        timer.cancel();
                        break;
                    }
                }
            } catch (Exception e) {
                timer.cancel();
            }
        });
        
        monitorThread.setDaemon(true);
        monitorThread.start();
    }
    
    private static void sendRawData(String hexData) {
        if (!checkConnection()) return;
        
        if (hexData.isEmpty()) {
            System.err.println("Usage: send <hex_data>");
            System.err.println("Example: send 01 02 03 FF");
            return;
        }
        
        try {
            byte[] data = parseHexString(hexData);
            sniffer.sendRaw(data);
            System.out.printf("✓ Sent %d bytes: %s\n", data.length, hexData.toUpperCase());
            
        } catch (Exception e) {
            System.err.println("Invalid hex data: " + e.getMessage());
        }
    }
    
    private static void toggleLogging(String state) {
        switch (state.toLowerCase()) {
            case "on":
            case "enable":
                System.out.println("✓ Logging enabled");
                // Implementation would enable file logging
                break;
            case "off":
            case "disable":
                System.out.println("✓ Logging disabled");
                // Implementation would disable file logging
                break;
            case "toggle":
            default:
                System.out.println("Logging toggle not implemented in demo version");
                break;
        }
    }
    
    private static void showStatistics() {
        if (sniffer == null) {
            System.out.println("No connection statistics available.");
            return;
        }
        
        TraceListener.CommStatistics stats = sniffer.getStatistics();
        System.out.println("=== Communication Statistics ===");
        System.out.printf("Messages Received: %d\n", stats.messagesReceived);
        System.out.printf("Messages Sent: %d\n", stats.messagesSent);
        System.out.printf("Bytes Received: %d\n", stats.bytesReceived);
        System.out.printf("Bytes Sent: %d\n", stats.bytesSent);
        System.out.printf("Errors: %d\n", stats.errors);
        System.out.printf("Message Rate: %.1f msg/s\n", stats.getMessageRate());
        System.out.printf("Byte Rate: %.1f bytes/s\n", stats.getByteRate());
        
        if (currentPort != null) {
            System.out.printf("Connection: %s @ %d baud (%s)\n", 
                currentPort, currentBaud, currentProtocol);
        }
    }
    
    private static void showHistory() {
        System.out.println("=== Command History ===");
        if (commandHistory.isEmpty()) {
            System.out.println("No commands in history.");
            return;
        }
        
        int start = Math.max(0, commandHistory.size() - 20); // Show last 20 commands
        for (int i = start; i < commandHistory.size(); i++) {
            System.out.printf("[%3d] %s\n", i + 1, commandHistory.get(i));
        }
        
        if (commandHistory.size() > 20) {
            System.out.printf("... (%d earlier commands not shown)\n", 
                commandHistory.size() - 20);
        }
    }
    
    private static void clearScreen() {
        // ANSI escape code to clear screen
        System.out.print("\033[2J\033[H");
        System.out.flush();
        
        // Alternative for Windows
        try {
            if (System.getProperty("os.name").toLowerCase().contains("windows")) {
                new ProcessBuilder("cmd", "/c", "cls").inheritIO().start().waitFor();
            }
        } catch (Exception e) {
            // Fallback: print several newlines
            for (int i = 0; i < 50; i++) {
                System.out.println();
            }
        }
    }
    
    private static void exit() {
        System.out.println("Shutting down...");
        running.set(false);
        
        if (sniffer != null) {
            sniffer.close();
        }
    }
    
    // Helper methods
    private static boolean checkConnection() {
        if (sniffer == null) {
            System.err.println("Not connected to ECU. Use 'connect' command first.");
            return false;
        }
        return true;
    }
    
    private static int parseAddress(String addr) throws NumberFormatException {
        addr = addr.trim();
        if (addr.toLowerCase().startsWith("0x")) {
            return Integer.parseInt(addr.substring(2), 16);
        } else {
            return Integer.parseInt(addr);
        }
    }
    
    private static byte[] parseHexString(String hex) throws IllegalArgumentException {
        hex = hex.replaceAll("\\s+", "").toUpperCase();
        
        if (hex.length() % 2 != 0) {
            throw new IllegalArgumentException("Hex string must have even length");
        }
        
        byte[] result = new byte[hex.length() / 2];
        for (int i = 0; i < result.length; i++) {
            int index = i * 2;
            result[i] = (byte) Integer.parseInt(hex.substring(index, index + 2), 16);
        }
        return result;
    }
    
    private static String getUptimeString() {
        if (sniffer == null) return "0s";
        
        TraceListener.CommStatistics stats = sniffer.getStatistics();
        long uptime = System.currentTimeMillis() - stats.connectionTime;
        
        long seconds = uptime / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        
        seconds %= 60;
        minutes %= 60;
        
        if (hours > 0) {
            return String.format("%dh %dm %ds", hours, minutes, seconds);
        } else if (minutes > 0) {
            return String.format("%dm %ds", minutes, seconds);
        } else {
            return String.format("%ds", seconds);
        }
    }
    
    private static void cleanup() {
        if (sniffer != null) {
            sniffer.close();
        }
        scanner.close();
    }
    
    /**
     * Console implementation of TraceListener
     */
    public static class ConsoleTraceListener implements TraceListener {
        private final SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss.SSS");
        
        @Override
        public void onTrace(String message) {
            String timestamp = timeFormat.format(new Date());
            System.out.println("[" + timestamp + "] " + message);
        }
        
        @Override
        public void onFrame(byte[] data, int len) {
            String timestamp = timeFormat.format(new Date());
            String hexData = bytesToHex(Arrays.copyOf(data, len));
            System.out.println("[" + timestamp + "] FRAME: " + hexData);
        }
        
        @Override
        public void onSeatControllerMessage(SeatControllerMessageType messageType, Object data) {
            String timestamp = timeFormat.format(new Date());
            System.out.printf("[%s] SEAT_MSG: %s = %s%n", timestamp, messageType, data);
        }
        
        @Override
        public void onEEPROMResponse(EEPROMOperation operation, int address, byte[] data, boolean success) {
            String timestamp = timeFormat.format(new Date());
            String result = success ? "SUCCESS" : "FAILED";
            String dataStr = (data != null && data.length > 0) ? bytesToHex(data) : "N/A";
            System.out.printf("[%s] EEPROM_%s: addr=0x%02X data=%s result=%s%n", 
                timestamp, operation, address, dataStr, result);
        }
        
        @Override
        public void onFaultStatus(int faultNumber, boolean active, long timestamp) {
            String timeStr = timeFormat.format(new Date(timestamp));
            String status = active ? "ACTIVE" : "CLEARED";
            System.out.printf("[%s] FAULT_%d: %s%n", timeStr, faultNumber, status);
        }
        
        @Override
        public void onConnectionStatus(boolean connected, String portName, String protocol) {
            String timestamp = timeFormat.format(new Date());
            String status = connected ? "CONNECTED" : "DISCONNECTED";
            System.out.printf("[%s] CONNECTION: %s - %s (%s)%n", 
                timestamp, status, portName, protocol);
        }
        
        public String bytesToHex(byte[] data) {
            if (data == null || data.length == 0) return "";
            StringBuilder sb = new StringBuilder();
            for (byte b : data) {
                sb.append(String.format("%02X ", b));
            }
            return sb.toString().trim();
        }
    }
}