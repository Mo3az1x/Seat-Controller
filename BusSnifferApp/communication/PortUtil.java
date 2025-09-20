package communication;

import com.fazecast.jSerialComm.SerialPort;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

/**
 * Enhanced Port Utilities for Seat Controller ECU development
 * Provides comprehensive serial port discovery, filtering, and management
 */
public class PortUtil {
    
    // Cache for port information to avoid repeated system calls
    private static final Map<String, PortInfo> portCache = new ConcurrentHashMap<>();
    private static long lastCacheUpdate = 0;
    private static final long CACHE_VALIDITY_MS = 5000; // 5 seconds cache validity
    
    // Known device patterns for automotive ECUs and development tools
    private static final Map<String, String> DEVICE_PATTERNS = new HashMap<>();
    static {
        DEVICE_PATTERNS.put(".*Arduino.*", "Arduino Development Board");
        DEVICE_PATTERNS.put(".*STM.*", "STM32 Microcontroller");
        DEVICE_PATTERNS.put(".*FTDI.*", "FTDI USB-Serial Converter");
        DEVICE_PATTERNS.put(".*Prolific.*", "Prolific USB-Serial");
        DEVICE_PATTERNS.put(".*CP210x.*", "Silicon Labs CP210x");
        DEVICE_PATTERNS.put(".*CH340.*", "CH340 USB-Serial");
        DEVICE_PATTERNS.put(".*Virtual.*", "Virtual COM Port");
        DEVICE_PATTERNS.put(".*Bluetooth.*", "Bluetooth Serial Port");
        DEVICE_PATTERNS.put(".*CAN.*", "CAN Interface");
        DEVICE_PATTERNS.put(".*ECU.*", "ECU Communication Port");
    }
    
    /**
     * Enhanced port information structure
     */
    public static class PortInfo {
        public final String systemPortName;
        public final String descriptivePortName; 
        public final String deviceType;
        public final boolean isPhysical;
        public final boolean isAvailable;
        public final int vendorId;
        public final int productId;
        public final String serialNumber;
        
        public PortInfo(SerialPort port) {
            this.systemPortName = port.getSystemPortName();
            this.descriptivePortName = port.getDescriptivePortName();
            this.deviceType = detectDeviceType(port.getDescriptivePortName());
            this.isPhysical = !port.getDescriptivePortName().toLowerCase().contains("virtual");
            this.isAvailable = !port.isOpen();
            this.vendorId = port.getVendorID();
            this.productId = port.getProductID();
            this.serialNumber = port.getSerialNumber();
        }
        
        @Override
        public String toString() {
            return String.format("%s (%s) - %s [%s]", 
                systemPortName, descriptivePortName, deviceType,
                isAvailable ? "Available" : "In Use");
        }
    }
    
    /**
     * List all available COM ports with enhanced information
     */
    public static void listPorts() {
        System.out.println("=== Available Serial Ports ===");
        
        PortInfo[] ports = getPortsWithInfo();
        if (ports.length == 0) {
            System.out.println("No serial ports found.");
            return;
        }
        
        // Group ports by type
        Map<String, List<PortInfo>> portsByType = new HashMap<>();
        for (PortInfo port : ports) {
            portsByType.computeIfAbsent(port.deviceType, k -> new ArrayList<>()).add(port);
        }
        
        int index = 0;
        for (Map.Entry<String, List<PortInfo>> entry : portsByType.entrySet()) {
            System.out.println("\n--- " + entry.getKey() + " ---");
            for (PortInfo port : entry.getValue()) {
                String status = port.isAvailable ? "✓" : "✗";
                String physical = port.isPhysical ? "Physical" : "Virtual";
                
                System.out.printf("[%d] %s %s (%s) - %s%n", 
                    index++, status, port.systemPortName, physical, port.descriptivePortName);
                
                if (port.vendorId != 0 || port.productId != 0) {
                    System.out.printf("    VID:PID = %04X:%04X", port.vendorId, port.productId);
                    if (port.serialNumber != null && !port.serialNumber.isEmpty()) {
                        System.out.printf(" SN:%s", port.serialNumber);
                    }
                    System.out.println();
                }
            }
        }
        
        System.out.println("\nRecommended ports for ECU development:");
        recommendPortsForECU(ports);
    }
    
    /**
     * Get port name by index from the list
     */
    public static String getPortNameByIndex(int index) {
        SerialPort[] ports = SerialPort.getCommPorts();
        if (index < 0 || index >= ports.length) return null;
        return ports[index].getSystemPortName();
    }

    /**
     * Get all port names as array (for ComboBox)
     */
    public static String[] getAllPorts() {
        SerialPort[] ports = SerialPort.getCommPorts();
        String[] names = new String[ports.length];
        for (int i = 0; i < ports.length; i++) {
            names[i] = ports[i].getSystemPortName();
        }
        return names;
    }
    
    /**
     * Get detailed port information
     */
    public static PortInfo[] getPortsWithInfo() {
        updatePortCache();
        return portCache.values().toArray(new PortInfo[0]);
    }
    
    /**
     * Find ports matching specific criteria
     */
    public static PortInfo[] findPorts(String deviceTypePattern, boolean physicalOnly, boolean availableOnly) {
        PortInfo[] allPorts = getPortsWithInfo();
        List<PortInfo> matchingPorts = new ArrayList<>();
        
        Pattern pattern = Pattern.compile(deviceTypePattern, Pattern.CASE_INSENSITIVE);
        
        for (PortInfo port : allPorts) {
            if (physicalOnly && !port.isPhysical) continue;
            if (availableOnly && !port.isAvailable) continue;
            if (deviceTypePattern != null && !pattern.matcher(port.deviceType).find()) continue;
            
            matchingPorts.add(port);
        }
        
        return matchingPorts.toArray(new PortInfo[0]);
    }
    
    /**
     * Get recommended ports for ECU development
     */
    public static PortInfo[] getRecommendedECUPorts() {
        // Look for known ECU development interfaces
        String[] preferredTypes = {
            ".*Arduino.*", ".*STM.*", ".*FTDI.*", ".*ECU.*", ".*CAN.*"
        };
        
        List<PortInfo> recommended = new ArrayList<>();
        for (String pattern : preferredTypes) {
            PortInfo[] matches = findPorts(pattern, true, true);
            recommended.addAll(Arrays.asList(matches));
        }
        
        return recommended.toArray(new PortInfo[0]);
    }
    
    /**
     * Test if a port supports the specified baud rate
     */
    public static boolean testBaudRate(String portName, int baudRate) {
        try {
            SerialPort port = SerialPort.getCommPort(portName);
            port.setBaudRate(baudRate);
            port.setComPortTimeouts(SerialPort.TIMEOUT_READ_BLOCKING, 100, 0);
            
            if (port.openPort()) {
                // Send a simple test command and wait for response
                port.writeBytes("AT\r\n".getBytes(), 4);
                
                byte[] buffer = new byte[64];
                int bytesRead = port.readBytes(buffer, buffer.length);
                
                port.closePort();
                
                // If we got any response, the baud rate is likely correct
                return bytesRead > 0;
            }
        } catch (Exception e) {
            System.err.println("Baud rate test failed: " + e.getMessage());
        }
        return false;
    }
    
    /**
     * Auto-detect the best baud rate for a port
     */
    public static int detectBaudRate(String portName) {
        int[] commonRates = {115200, 57600, 38400, 19200, 9600, 4800, 2400, 1200};
        
        System.out.println("Auto-detecting baud rate for " + portName + "...");
        
        for (int rate : commonRates) {
            System.out.print("Testing " + rate + "... ");
            if (testBaudRate(portName, rate)) {
                System.out.println("✓ Success!");
                return rate;
            }
            System.out.println("✗ Failed");
        }
        
        System.out.println("No suitable baud rate found, defaulting to 115200");
        return 115200;
    }
    
    /**
     * Check if a specific port exists and is available
     */
    public static boolean isPortAvailable(String portName) {
        if (portName == null || portName.trim().isEmpty()) return false;
        
        SerialPort[] ports = SerialPort.getCommPorts();
        for (SerialPort port : ports) {
            if (portName.equals(port.getSystemPortName())) {
                return !port.isOpen();
            }
        }
        return false;
    }
    
    /**
     * Get port information by name
     */
    public static PortInfo getPortInfo(String portName) {
        updatePortCache();
        return portCache.get(portName);
    }
    
    /**
     * Monitor port changes (connect/disconnect events)
     */
    public static void monitorPorts(PortChangeListener listener, int intervalMs) {
        Thread monitorThread = new Thread(() -> {
            Set<String> previousPorts = new HashSet<>();
            
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    Set<String> currentPorts = new HashSet<>();
                    PortInfo[] ports = getPortsWithInfo();
                    
                    for (PortInfo port : ports) {
                        currentPorts.add(port.systemPortName);
                    }
                    
                    // Detect new ports (connected)
                    for (String portName : currentPorts) {
                        if (!previousPorts.contains(portName)) {
                            PortInfo info = getPortInfo(portName);
                            if (listener != null) {
                                listener.onPortConnected(info);
                            }
                        }
                    }
                    
                    // Detect removed ports (disconnected)
                    for (String portName : previousPorts) {
                        if (!currentPorts.contains(portName)) {
                            if (listener != null) {
                                listener.onPortDisconnected(portName);
                            }
                        }
                    }
                    
                    previousPorts = currentPorts;
                    Thread.sleep(intervalMs);
                    
                } catch (InterruptedException e) {
                    break;
                } catch (Exception e) {
                    System.err.println("Port monitoring error: " + e.getMessage());
                }
            }
        });
        
        monitorThread.setName("PortMonitor");
        monitorThread.setDaemon(true);
        monitorThread.start();
    }
    
    /**
     * Interface for port change notifications
     */
    public interface PortChangeListener {
        void onPortConnected(PortInfo portInfo);
        void onPortDisconnected(String portName);
    }
    
    /**
     * Refresh the port cache
     */
    private static void updatePortCache() {
        long now = System.currentTimeMillis();
        if (now - lastCacheUpdate < CACHE_VALIDITY_MS && !portCache.isEmpty()) {
            return; // Cache is still valid
        }
        
        portCache.clear();
        SerialPort[] ports = SerialPort.getCommPorts();
        
        for (SerialPort port : ports) {
            PortInfo info = new PortInfo(port);
            portCache.put(port.getSystemPortName(), info);
        }
        
        lastCacheUpdate = now;
    }
    
    /**
     * Detect device type based on port description
     */
    private static String detectDeviceType(String description) {
        if (description == null || description.isEmpty()) {
            return "Unknown Device";
        }
        
        String desc = description.toLowerCase();
        
        // Check against known patterns
        for (Map.Entry<String, String> entry : DEVICE_PATTERNS.entrySet()) {
            if (Pattern.compile(entry.getKey(), Pattern.CASE_INSENSITIVE).matcher(description).find()) {
                return entry.getValue();
            }
        }
        
        // Additional heuristics
        if (desc.contains("usb")) return "USB Serial Device";
        if (desc.contains("com")) return "COM Port";
        if (desc.contains("serial")) return "Serial Port";
        if (desc.contains("modem")) return "Modem";
        
        return "Generic Serial Device";
    }
    
    /**
     * Recommend specific ports for ECU development
     */
    private static void recommendPortsForECU(PortInfo[] ports) {
        List<PortInfo> recommended = new ArrayList<>();
        
        // Score ports based on suitability for ECU development
        for (PortInfo port : ports) {
            int score = 0;
            
            // Physical ports preferred over virtual
            if (port.isPhysical) score += 10;
            
            // Available ports only
            if (port.isAvailable) score += 5;
            
            // Known development hardware gets bonus points
            String desc = port.descriptivePortName.toLowerCase();
            if (desc.contains("arduino")) score += 15;
            if (desc.contains("stm")) score += 15;
            if (desc.contains("ftdi")) score += 12;
            if (desc.contains("cp210")) score += 10;
            if (desc.contains("ecu")) score += 20;
            if (desc.contains("can")) score += 18;
            
            // Penalty for generic or problematic devices
            if (desc.contains("prolific")) score -= 5; // Known for driver issues
            if (desc.contains("bluetooth")) score -= 8; // Unreliable for development
            
            if (score >= 15) {
                recommended.add(port);
            }
        }
        
        // Sort by score (approximate, since we don't store scores)
        recommended.sort((a, b) -> {
            // Prioritize ECU-related ports
            if (a.descriptivePortName.toLowerCase().contains("ecu") && 
                !b.descriptivePortName.toLowerCase().contains("ecu")) return -1;
            if (!a.descriptivePortName.toLowerCase().contains("ecu") && 
                b.descriptivePortName.toLowerCase().contains("ecu")) return 1;
            
            // Then Arduino/STM32
            if (a.descriptivePortName.toLowerCase().contains("arduino") && 
                !b.descriptivePortName.toLowerCase().contains("arduino")) return -1;
            if (!a.descriptivePortName.toLowerCase().contains("arduino") && 
                b.descriptivePortName.toLowerCase().contains("arduino")) return 1;
            
            return a.systemPortName.compareTo(b.systemPortName);
        });
        
        if (recommended.isEmpty()) {
            System.out.println("  No specifically recommended ports found.");
            System.out.println("  Any available physical port should work.");
        } else {
            for (int i = 0; i < Math.min(3, recommended.size()); i++) {
                PortInfo port = recommended.get(i);
                System.out.printf("  ⭐ %s - %s%n", port.systemPortName, 
                    getRecommendationReason(port));
            }
        }
    }
    
    /**
     * Get recommendation reason for a port
     */
    private static String getRecommendationReason(PortInfo port) {
        String desc = port.descriptivePortName.toLowerCase();
        
        if (desc.contains("ecu")) return "ECU communication interface";
        if (desc.contains("arduino")) return "Arduino development board";
        if (desc.contains("stm")) return "STM32 microcontroller";
        if (desc.contains("ftdi")) return "Reliable FTDI USB-Serial converter";
        if (desc.contains("can")) return "CAN bus interface";
        
        return "Good compatibility for development";
    }
    
    /**
     * Validate port name format
     */
    public static boolean isValidPortName(String portName) {
        if (portName == null || portName.trim().isEmpty()) {
            return false;
        }
        
        // Windows: COM1, COM2, etc.
        if (Pattern.matches("COM\\d+", portName.toUpperCase())) {
            return true;
        }
        
        // Linux/Mac: /dev/ttyUSB0, /dev/ttyACM0, etc.
        if (Pattern.matches("/dev/tty(USB|ACM|S)\\d+", portName)) {
            return true;
        }
        
        // Generic serial device pattern
        if (Pattern.matches("/dev/serial\\d+", portName)) {
            return true;
        }
        
        return false;
    }
    
    /**
     * Get default port for the current OS
     */
    public static String getDefaultPort() {
        String os = System.getProperty("os.name").toLowerCase();
        
        if (os.contains("win")) {
            // Windows: Look for COM ports starting from COM1
            for (int i = 1; i <= 20; i++) {
                String portName = "COM" + i;
                if (isPortAvailable(portName)) {
                    return portName;
                }
            }
            return "COM1"; // Fallback
        } else {
            // Linux/Mac: Look for common USB serial devices
            String[] commonPorts = {
                "/dev/ttyUSB0", "/dev/ttyUSB1", 
                "/dev/ttyACM0", "/dev/ttyACM1",
                "/dev/ttyS0", "/dev/ttyS1"
            };
            
            for (String port : commonPorts) {
                if (isPortAvailable(port)) {
                    return port;
                }
            }
            return "/dev/ttyUSB0"; // Fallback
        }
    }
    
    /**
     * Clean up resources and invalidate cache
     */
    public static void cleanup() {
        portCache.clear();
        lastCacheUpdate = 0;
    }
    
    /**
     * Get system-specific port naming convention
     */
    public static String getPortNamingInfo() {
        String os = System.getProperty("os.name").toLowerCase();
        
        if (os.contains("win")) {
            return "Windows COM ports: COM1, COM2, COM3, etc.";
        } else if (os.contains("mac")) {
            return "macOS serial ports: /dev/tty.usbserial-*, /dev/tty.usbmodem-*, etc.";
        } else {
            return "Linux serial ports: /dev/ttyUSB0, /dev/ttyACM0, /dev/ttyS0, etc.";
        }
    }
    
    /**
     * Export port information to string for logging/debugging
     */
    public static String exportPortInfo() {
        StringBuilder sb = new StringBuilder();
        sb.append("=== Serial Port Information Export ===\n");
        sb.append("Timestamp: ").append(new Date()).append("\n");
        sb.append("OS: ").append(System.getProperty("os.name")).append("\n");
        sb.append("Port Naming: ").append(getPortNamingInfo()).append("\n\n");
        
        PortInfo[] ports = getPortsWithInfo();
        if (ports.length == 0) {
            sb.append("No serial ports detected.\n");
        } else {
            sb.append("Detected Ports (").append(ports.length).append("):\n");
            for (int i = 0; i < ports.length; i++) {
                PortInfo port = ports[i];
                sb.append(String.format("[%d] %s\n", i, port.toString()));
                sb.append(String.format("    Description: %s\n", port.descriptivePortName));
                sb.append(String.format("    Type: %s\n", port.deviceType));
                sb.append(String.format("    Physical: %s, Available: %s\n", 
                    port.isPhysical, port.isAvailable));
                if (port.vendorId != 0 || port.productId != 0) {
                    sb.append(String.format("    VID:PID: %04X:%04X\n", 
                        port.vendorId, port.productId));
                }
                if (port.serialNumber != null && !port.serialNumber.isEmpty()) {
                    sb.append(String.format("    Serial: %s\n", port.serialNumber));
                }
                sb.append("\n");
            }
        }
        
        return sb.toString();
    }
}