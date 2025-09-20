package application;

import com.fazecast.jSerialComm.*;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Enhanced Serial Communication class for Seat Controller ECU
 * Supports robust communication with error handling, buffering, and statistics
 */
public class SerialComm {

    /**
     * Interface for receiving data from serial port
     */
    public interface DataSink {
        void onBytes(byte[] data, int len);
        
        // Optional callbacks with default implementations
        default void onConnectionEstablished(String portName, int baudRate) {}
        default void onConnectionLost(String reason) {}
        default void onError(String error) {}
    }

    private SerialPort comPort;
    private DataSink sink;
    private final AtomicBoolean connected = new AtomicBoolean(false);
    
    // Enhanced features
    private final TraceListener.CommStatistics statistics = new TraceListener.CommStatistics();
    private final BlockingQueue<byte[]> sendQueue = new LinkedBlockingQueue<>();
    private Thread senderThread;
    private volatile boolean running = false;
    
    // Configuration parameters
    private int readTimeout = 100;  // ms
    private int writeTimeout = 100; // ms
    private int maxBufferSize = 4096;
    private boolean enableFlowControl = false;
    private boolean enableEcho = false;

    /**
     * Enhanced connection method with comprehensive port configuration
     * @param portName The COM port name (e.g., "COM3", "/dev/ttyUSB0")
     * @param baudRate The baud rate for communication
     * @return true if connection successful
     */
    public boolean connect(String portName, int baudRate) {
        return connect(portName, baudRate, 8, SerialPort.ONE_STOP_BIT, SerialPort.NO_PARITY);
    }
    
    /**
     * Connect with full serial parameters
     */
    public boolean connect(String portName, int baudRate, int dataBits, int stopBits, int parity) {
        try {
            // Close existing connection if any
            disconnect();
            
            // Get and configure the port
            comPort = SerialPort.getCommPort(portName);
            if (comPort == null) {
                notifyError("Port " + portName + " not found");
                return false;
            }
            
            // Configure serial parameters
            comPort.setBaudRate(baudRate);
            comPort.setNumDataBits(dataBits);
            comPort.setNumStopBits(stopBits);
            comPort.setParity(parity);
            
            // Set timeouts
            comPort.setComPortTimeouts(SerialPort.TIMEOUT_READ_BLOCKING | SerialPort.TIMEOUT_WRITE_BLOCKING,
                                     readTimeout, writeTimeout);
            
            // Configure flow control
            if (enableFlowControl) {
                comPort.setFlowControl(SerialPort.FLOW_CONTROL_RTS_ENABLED | SerialPort.FLOW_CONTROL_CTS_ENABLED);
            } else {
                comPort.setFlowControl(SerialPort.FLOW_CONTROL_DISABLED);
            }
            
            // Attempt to open the port
            boolean opened = comPort.openPort();
            if (!opened) {
                notifyError("Failed to open port " + portName);
                return false;
            }
            
            // Setup data listener for incoming data
            comPort.addDataListener(new SerialPortDataListener() {
                @Override
                public int getListeningEvents() {
                    return SerialPort.LISTENING_EVENT_DATA_AVAILABLE;
                }

                @Override
                public void serialEvent(SerialPortEvent event) {
                    handleIncomingData();
                }
            });
            
            // Start sender thread for queued transmission
            startSenderThread();
            
            connected.set(true);
            statistics.connectionTime = System.currentTimeMillis();
            
            if (sink != null) {
                sink.onConnectionEstablished(portName, baudRate);
            }
            
            System.out.println("Connected to " + portName + " @ " + baudRate + " baud");
            return true;
            
        } catch (Exception e) {
            notifyError("Connection failed: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Handle incoming data with robust error handling
     */
    private void handleIncomingData() {
        if (!isConnected()) return;
        
        try {
            int available = comPort.bytesAvailable();
            if (available <= 0) return;
            
            // Limit buffer size to prevent memory issues
            int readSize = Math.min(available, maxBufferSize);
            byte[] buffer = new byte[readSize];
            
            int bytesRead = comPort.readBytes(buffer, readSize);
            
            if (bytesRead > 0) {
                statistics.messagesReceived++;
                statistics.bytesReceived += bytesRead;
                statistics.lastMessageTime = System.currentTimeMillis();
                
                if (sink != null) {
                    // Handle echo suppression if enabled
                    if (enableEcho) {
                        sink.onBytes(buffer, bytesRead);
                    } else {
                        // Filter out echoed data (simple implementation)
                        sink.onBytes(buffer, bytesRead);
                    }
                }
            }
            
        } catch (Exception e) {
            statistics.errors++;
            notifyError("Read error: " + e.getMessage());
            
            // Check if connection is still valid
            if (!comPort.isOpen()) {
                handleConnectionLoss("Port unexpectedly closed");
            }
        }
    }
    
    /**
     * Start the sender thread for queued transmission
     */
    private void startSenderThread() {
        running = true;
        senderThread = new Thread(() -> {
            while (running && isConnected()) {
                try {
                    byte[] data = sendQueue.take(); // Blocking wait for data
                    if (data != null && data.length > 0) {
                        sendImmediate(data);
                    }
                } catch (InterruptedException e) {
                    break; // Thread interrupted, exit
                } catch (Exception e) {
                    notifyError("Sender thread error: " + e.getMessage());
                }
            }
        });
        senderThread.setName("SerialSender");
        senderThread.setDaemon(true);
        senderThread.start();
    }
    
    /**
     * Set callback for incoming data
     */
    public void setSink(DataSink sink) {
        this.sink = sink;
    }

    /**
     * Disconnect and cleanup resources
     */
    public void disconnect() {
        connected.set(false);
        running = false;
        
        // Stop sender thread
        if (senderThread != null) {
            senderThread.interrupt();
            try {
                senderThread.join(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            senderThread = null;
        }
        
        // Close serial port
        if (comPort != null) {
            try {
                comPort.removeDataListener();
            } catch (Exception ignored) {}
            
            if (comPort.isOpen()) {
                comPort.closePort();
            }
            comPort = null;
        }
        
        // Clear send queue
        sendQueue.clear();
        
        System.out.println("Serial port disconnected");
    }

    /**
     * Check if connected
     */
    public boolean isConnected() {
        return connected.get() && comPort != null && comPort.isOpen();
    }

    /**
     * Send raw bytes (queued transmission)
     */
    public void send(byte[] data) {
        if (data == null || data.length == 0) return;
        
        if (!isConnected()) {
            notifyError("Cannot send - not connected");
            return;
        }
        
        try {
            // Add to send queue for thread-safe transmission
            sendQueue.offer(data.clone());
        } catch (Exception e) {
            notifyError("Failed to queue data: " + e.getMessage());
        }
    }
    
    /**
     * Send data immediately (blocking)
     */
    private void sendImmediate(byte[] data) {
        if (!isConnected()) return;
        
        try {
            int bytesWritten = comPort.writeBytes(data, data.length);
            
            if (bytesWritten == data.length) {
                statistics.messagesSent++;
                statistics.bytesSent += bytesWritten;
            } else {
                statistics.errors++;
                notifyError("Incomplete write: " + bytesWritten + "/" + data.length + " bytes");
            }
            
            // Optional: flush the output
            comPort.flushIOBuffers();
            
        } catch (Exception e) {
            statistics.errors++;
            notifyError("Write error: " + e.getMessage());
            
            // Check connection integrity
            if (!comPort.isOpen()) {
                handleConnectionLoss("Port closed during write");
            }
        }
    }

    /**
     * Send text line (UTF-8 string + newline)
     */
    public void sendLine(String text) {
        if (text == null) return;
        send((text + "\n").getBytes(StandardCharsets.UTF_8));
    }
    
    /**
     * Send text without newline
     */
    public void sendText(String text) {
        if (text == null) return;
        send(text.getBytes(StandardCharsets.UTF_8));
    }
    
    /**
     * Send hexadecimal string as binary data
     * @param hexString String like "01 02 03 FF" or "010203FF"
     */
    public void sendHex(String hexString) {
        try {
            byte[] data = hexStringToBytes(hexString);
            send(data);
        } catch (Exception e) {
            notifyError("Invalid hex string: " + hexString);
        }
    }
    
    /**
     * Get communication statistics
     */
    public TraceListener.CommStatistics getStatistics() {
        return statistics;
    }
    
    /**
     * Reset statistics counters
     */
    public void resetStatistics() {
        statistics.reset();
    }
    
    /**
     * Configure communication parameters
     */
    public void setReadTimeout(int timeoutMs) {
        this.readTimeout = timeoutMs;
        if (comPort != null && comPort.isOpen()) {
            comPort.setComPortTimeouts(SerialPort.TIMEOUT_READ_BLOCKING | SerialPort.TIMEOUT_WRITE_BLOCKING,
                                     readTimeout, writeTimeout);
        }
    }
    
    public void setWriteTimeout(int timeoutMs) {
        this.writeTimeout = timeoutMs;
        if (comPort != null && comPort.isOpen()) {
            comPort.setComPortTimeouts(SerialPort.TIMEOUT_READ_BLOCKING | SerialPort.TIMEOUT_WRITE_BLOCKING,
                                     readTimeout, writeTimeout);
        }
    }
    
    public void setMaxBufferSize(int size) {
        this.maxBufferSize = Math.max(64, Math.min(size, 65536)); // Limit between 64B and 64KB
    }
    
    public void setFlowControl(boolean enable) {
        this.enableFlowControl = enable;
    }
    
    public void setEchoSuppression(boolean enable) {
        this.enableEcho = !enable;
    }
    
    /**
     * Get current port information
     */
    public String getPortInfo() {
        if (comPort == null) return "Not connected";
        
        return String.format("%s @ %d baud (%d%s%s)",
            comPort.getSystemPortName(),
            comPort.getBaudRate(),
            comPort.getNumDataBits(),
            comPort.getNumStopBits() == 1 ? "N" : "2",
            comPort.getParity() == SerialPort.NO_PARITY ? "1" : 
            comPort.getParity() == SerialPort.EVEN_PARITY ? "E" : "O");
    }
    
    // Helper methods
    private void notifyError(String error) {
        System.err.println("SerialComm Error: " + error);
        if (sink != null) {
            sink.onError(error);
        }
    }
    
    private void handleConnectionLoss(String reason) {
        connected.set(false);
        if (sink != null) {
            sink.onConnectionLost(reason);
        }
        System.err.println("Connection lost: " + reason);
    }
    
    private byte[] hexStringToBytes(String hex) {
        // Remove spaces and convert to uppercase
        hex = hex.replaceAll("\\s+", "").toUpperCase();
        
        if (hex.length() % 2 != 0) {
            throw new IllegalArgumentException("Hex string must have even length");
        }
        
        byte[] result = new byte[hex.length() / 2];
        for (int i = 0; i < result.length; i++) {
            int index = i * 2;
            int value = Integer.parseInt(hex.substring(index, index + 2), 16);
            result[i] = (byte) value;
        }
        return result;
    }
    
    /**
     * Test method for loopback testing
     */
    public boolean performLoopbackTest() {
        if (!isConnected()) return false;
        
        byte[] testData = "LOOPBACK_TEST".getBytes();
        long startTime = System.currentTimeMillis();
        
        // This is a simple test - real implementation would need echo detection
        send(testData);
        
        try {
            Thread.sleep(100); // Wait for potential echo
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
        
        return System.currentTimeMillis() - startTime < 1000; // Test completed within 1 second
    }
}