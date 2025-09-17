package application;

import javax.swing.SwingUtilities;

import application.TraceListener.CommStatistics;
import communication.Main.ConsoleTraceListener;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class SeatControllerSnifferManager implements SerialComm.DataSink, AutoCloseable {
    private final SerialComm serial = new SerialComm();
    private TraceListener listener;
    private String currentProtocol;

    // Frame format constants
    private static final byte HEADER = 0x7E;
    private static final byte TAIL   = 0x7F;

    // Seat Controller specific command IDs
    private static final byte CMD_READ_BYTE           = 0x01;
    private static final byte CMD_WRITE_BYTE          = 0x02;
    private static final byte CMD_READ_ALL            = 0x03;
    private static final byte CMD_WRITE_ALL           = 0x04;
    private static final byte CMD_ALIVE_MSG           = 0x10;
    private static final byte CMD_GEARBOX_STATUS      = 0x11;
    private static final byte CMD_SEAT_HEIGHT_TARGET  = 0x20;
    private static final byte CMD_SEAT_SLIDE_TARGET   = 0x21;
    private static final byte CMD_SEAT_INCLINE_TARGET = 0x22;
    private static final byte CMD_SEAT_HEIGHT_CURRENT = 0x30;
    private static final byte CMD_SEAT_SLIDE_CURRENT  = 0x31;
    private static final byte CMD_SEAT_INCLINE_CURRENT= 0x32;
    private static final byte CMD_SEAT_CONTROL_REQ    = 0x40;
    private static final byte CMD_FAULT_1             = 0x50;
    private static final byte CMD_FAULT_2             = 0x51;

    // Response IDs (command + 0x80)
    private static final byte RES_READ_BYTE           = (byte)0x81;
    private static final byte RES_WRITE_BYTE          = (byte)0x82;
    private static final byte RES_READ_ALL            = (byte)0x83;
    private static final byte RES_WRITE_ALL           = (byte)0x84;

    // Buffer for accumulating incoming data
    private StringBuilder textBuffer = new StringBuilder();
    private ByteBuffer frameBuffer = ByteBuffer.allocate(1024);
    private boolean inFrame = false;

    public SeatControllerSnifferManager(ConsoleTraceListener listener) {
        this.listener = listener;
    }

    /**
     * Connect to the serial port and start listening
     */
    public boolean start(String portName, int baud, String protocol) {
        this.currentProtocol = protocol;
        serial.setSink(this);
        boolean ok = serial.connect(portName, baud);
        if (!ok) {
            System.err.println("Failed to open port " + portName);
            return false;
        }
        log("Connected to " + portName + " @ " + baud + " baud using " + protocol + " protocol");
        return true;
    }

    // ======= SEAT CONTROLLER SPECIFIC COMMANDS =======
    
    /**
     * Send alive message (periodic, 5ms)
     * Structure: {u16 timestamp, u16 counter}
     */
    public void sendAliveMessage(long timestamp, int counter) {
        ByteBuffer payload = ByteBuffer.allocate(4);
        payload.order(ByteOrder.LITTLE_ENDIAN);
        payload.putShort((short)(timestamp & 0xFFFF));
        payload.putShort((short)(counter & 0xFFFF));
        
        byte[] frame = buildFrame(CMD_ALIVE_MSG, payload.array());
        serial.send(frame);
        log("ALIVE: ts=" + (timestamp & 0xFFFF) + " cnt=" + (counter & 0xFFFF));
    }
    
    /**
     * Send gearbox status (periodic, 10ms)
     * Structure: {u16 GearNumber, u16 CurrentTorque, u16 MaxAllowedTorque}
     */
    public void sendGearboxStatus(int gear, int currentTorque, int maxTorque) {
        ByteBuffer payload = ByteBuffer.allocate(6);
        payload.order(ByteOrder.LITTLE_ENDIAN);
        payload.putShort((short)gear);
        payload.putShort((short)currentTorque);
        payload.putShort((short)maxTorque);
        
        byte[] frame = buildFrame(CMD_GEARBOX_STATUS, payload.array());
        serial.send(frame);
        log("GEARBOX: gear=" + gear + " torque=" + currentTorque + "/" + maxTorque);
    }
    
    /**
     * Send seat control request
     * Structure: {u16 height, u16 slide, u16 incline}
     */
    public void sendSeatControlRequest(double heightCm, double slideCm, double inclineDeg) {
        // Convert physical values to raw values (using calibration)
        int heightRaw = (int)(heightCm * 100); // Convert cm to mm
        int slideRaw = (int)(slideCm * 100);   // Convert cm to mm
        int inclineRaw = (int)(inclineDeg * 100); // Convert degrees to centidegrees
        
        ByteBuffer payload = ByteBuffer.allocate(6);
        payload.order(ByteOrder.LITTLE_ENDIAN);
        payload.putShort((short)heightRaw);
        payload.putShort((short)slideRaw);
        payload.putShort((short)inclineRaw);
        
        byte[] frame = buildFrame(CMD_SEAT_CONTROL_REQ, payload.array());
        serial.send(frame);
        log("SEAT_CONTROL_REQ: H=" + heightCm + "cm S=" + slideCm + "cm I=" + inclineDeg + "°");
    }
    
    /**
     * Send individual seat target messages
     */
    public void sendSeatHeightTarget(double heightCm) {
        int heightMM = (int)(heightCm * 10); // Convert cm to mm
        ByteBuffer payload = ByteBuffer.allocate(2);
        payload.order(ByteOrder.LITTLE_ENDIAN);
        payload.putShort((short)heightMM);
        
        byte[] frame = buildFrame(CMD_SEAT_HEIGHT_TARGET, payload.array());
        serial.send(frame);
        log("SEAT_HEIGHT_TARGET: " + heightMM + "mm");
    }
    
    public void sendSeatSlideTarget(double slideCm) {
        int slideMM = (int)(slideCm * 10);
        ByteBuffer payload = ByteBuffer.allocate(2);
        payload.order(ByteOrder.LITTLE_ENDIAN);
        payload.putShort((short)slideMM);
        
        byte[] frame = buildFrame(CMD_SEAT_SLIDE_TARGET, payload.array());
        serial.send(frame);
        log("SEAT_SLIDE_TARGET: " + slideMM + "mm");
    }
    
    public void sendSeatInclineTarget(double inclineDeg) {
        int inclineRad = (int)(Math.toRadians(inclineDeg) * 1000); // Convert to milliradians
        ByteBuffer payload = ByteBuffer.allocate(2);
        payload.order(ByteOrder.LITTLE_ENDIAN);
        payload.putShort((short)inclineRad);
        
        byte[] frame = buildFrame(CMD_SEAT_INCLINE_TARGET, payload.array());
        serial.send(frame);
        log("SEAT_INCLINE_TARGET: " + inclineRad + "mrad (" + inclineDeg + "°)");
    }
    
    /**
     * Send current seat position (periodic, 2ms)
     */
    public void sendSeatCurrentPosition(double heightCm, double slideCm, double inclineDeg) {
        // Send individual current position messages
        sendSeatHeightCurrent(heightCm);
        sendSeatSlideCurrent(slideCm);
        sendSeatInclineCurrent(inclineDeg);
    }
    
    private void sendSeatHeightCurrent(double heightCm) {
        int heightMM = (int)(heightCm * 10);
        ByteBuffer payload = ByteBuffer.allocate(2);
        payload.order(ByteOrder.LITTLE_ENDIAN);
        payload.putShort((short)heightMM);
        
        byte[] frame = buildFrame(CMD_SEAT_HEIGHT_CURRENT, payload.array());
        serial.send(frame);
    }
    
    private void sendSeatSlideCurrent(double slideCm) {
        int slideMM = (int)(slideCm * 10);
        ByteBuffer payload = ByteBuffer.allocate(2);
        payload.order(ByteOrder.LITTLE_ENDIAN);
        payload.putShort((short)slideMM);
        
        byte[] frame = buildFrame(CMD_SEAT_SLIDE_CURRENT, payload.array());
        serial.send(frame);
    }
    
    private void sendSeatInclineCurrent(double inclineDeg) {
        int inclineRad = (int)(Math.toRadians(inclineDeg) * 1000);
        ByteBuffer payload = ByteBuffer.allocate(2);
        payload.order(ByteOrder.LITTLE_ENDIAN);
        payload.putShort((short)inclineRad);
        
        byte[] frame = buildFrame(CMD_SEAT_INCLINE_CURRENT, payload.array());
        serial.send(frame);
    }
    
    /**
     * Send fault messages
     */
    public void sendFaultMessage(int faultNumber) {
        byte cmdId = (faultNumber == 1) ? CMD_FAULT_1 : CMD_FAULT_2;
        ByteBuffer payload = ByteBuffer.allocate(4);
        payload.order(ByteOrder.LITTLE_ENDIAN);
        payload.putInt((int)(System.currentTimeMillis() & 0xFFFFFFFF)); // Fault timestamp
        
        byte[] frame = buildFrame(cmdId, payload.array());
        serial.send(frame);
        log("FAULT_" + faultNumber + ": triggered at " + System.currentTimeMillis());
    }

    // ======= EEPROM COMMANDS =======
    
    public void sendReadByte(int addr) {
        ByteBuffer payload = ByteBuffer.allocate(4);
        payload.order(ByteOrder.LITTLE_ENDIAN);
        payload.putInt(addr);
        
        byte[] frame = buildFrame(CMD_READ_BYTE, payload.array());
        serial.send(frame);
        log("EEPROM_READ_BYTE: addr=0x" + Integer.toHexString(addr));
    }

    public void sendWriteByte(int addr, int value) {
        ByteBuffer payload = ByteBuffer.allocate(5);
        payload.order(ByteOrder.LITTLE_ENDIAN);
        payload.putInt(addr);
        payload.put((byte)(value & 0xFF));
        
        byte[] frame = buildFrame(CMD_WRITE_BYTE, payload.array());
        serial.send(frame);
        log("EEPROM_WRITE_BYTE: addr=0x" + Integer.toHexString(addr) + " val=0x" + Integer.toHexString(value));
    }

    public void sendReadAll(int size) {
        ByteBuffer payload = ByteBuffer.allocate(4);
        payload.order(ByteOrder.LITTLE_ENDIAN);
        payload.putInt(size);
        
        byte[] frame = buildFrame(CMD_READ_ALL, payload.array());
        serial.send(frame);
        log("EEPROM_READ_ALL: size=" + size + " bytes");
    }

    public void sendWriteAll(byte[] data) {
        ByteBuffer payload = ByteBuffer.allocate(4 + data.length);
        payload.order(ByteOrder.LITTLE_ENDIAN);
        payload.putInt(data.length);
        payload.put(data);
        
        byte[] frame = buildFrame(CMD_WRITE_ALL, payload.array());
        serial.send(frame);
        log("EEPROM_WRITE_ALL: " + data.length + " bytes");
    }
    
    // ======= PROTOCOL-SPECIFIC FRAME BUILDING =======
    
    private byte[] buildFrame(byte cmdId, byte[] payload) {
        switch (currentProtocol) {
            case "I2C":
                return buildI2CFrame(cmdId, payload);
            case "SPI":
                return buildSPIFrame(cmdId, payload);
            case "UART":
            default:
                return buildUARTFrame(cmdId, payload);
        }
    }
    
    /**
     * Build UART frame with header/tail structure
     * Format: [HEADER][LENGTH][CMD_ID][PAYLOAD][CHECKSUM][TAIL]
     */
    private byte[] buildUARTFrame(byte cmdId, byte[] payload) {
        int frameLen = 1 + 4 + 1 + payload.length + 1 + 1; // H + LEN + CMD + PAYLOAD + CHK + T
        ByteBuffer frame = ByteBuffer.allocate(frameLen);
        
        frame.put(HEADER);
        frame.putInt(payload.length + 1); // Include command ID in length
        frame.put(cmdId);
        frame.put(payload);
        
        // Calculate checksum (simple XOR)
        byte checksum = cmdId;
        for (byte b : payload) {
            checksum ^= b;
        }
        frame.put(checksum);
        frame.put(TAIL);
        
        return frame.array();
    }
    
    /**
     * Build I2C frame (simplified for simulation)
     * Format: [SLAVE_ADDR][CMD_ID][PAYLOAD_LEN][PAYLOAD]
     */
    private byte[] buildI2CFrame(byte cmdId, byte[] payload) {
        ByteBuffer frame = ByteBuffer.allocate(3 + payload.length);
        
        frame.put((byte)0x50); // Simulated I2C slave address for seat controller
        frame.put(cmdId);
        frame.put((byte)payload.length);
        frame.put(payload);
        
        return frame.array();
    }
    
    /**
     * Build SPI frame (for EEPROM operations mainly)
     * Format: [CMD_ID][PAYLOAD_LEN][PAYLOAD]
     */
    private byte[] buildSPIFrame(byte cmdId, byte[] payload) {
        ByteBuffer frame = ByteBuffer.allocate(2 + payload.length);
        
        frame.put(cmdId);
        frame.put((byte)payload.length);
        frame.put(payload);
        
        return frame.array();
    }

    /**
     * Handle incoming bytes from the ECU
     */
    @Override
    public void onBytes(byte[] data, int len) {
        if (len < 1) return;

        // Check if this looks like a framed response
        if (data[0] == HEADER && len > 6) {
            handleBinaryFrame(data, len);
        } else {
            // Handle as text/debug output
            handleTextData(data, len);
        }
    }

    private void handleBinaryFrame(byte[] data, int len) {
        try {
            // Parse UART frame format
            if (len < 7) return; // Minimum frame size
            
            ByteBuffer buffer = ByteBuffer.wrap(data);
            buffer.order(ByteOrder.LITTLE_ENDIAN);
            
            byte header = buffer.get();
            if (header != HEADER) return;
            
            int payloadLen = buffer.getInt();
            byte cmdId = buffer.get();
            
            // Extract payload
            byte[] payload = new byte[payloadLen - 1]; // Subtract 1 for cmdId
            buffer.get(payload);
            
            byte checksum = buffer.get();
            byte tail = buffer.get();
            
            if (tail != TAIL) {
                log("Invalid frame tail: " + String.format("0x%02X", tail));
                return;
            }
            
            // Process response based on command ID
            handleFrameResponse(cmdId, payload);
            
        } catch (Exception e) {
            log("Error parsing frame: " + e.getMessage());
            // Fall back to hex dump
            log("HEX: " + bytesToHex(data, len));
        }
    }
    
    private void handleFrameResponse(byte cmdId, byte[] payload) {
        ByteBuffer buffer = ByteBuffer.wrap(payload);
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        
        switch (cmdId) {
            case RES_READ_BYTE:
                if (payload.length >= 5) {
                    int addr = buffer.getInt();
                    int val = buffer.get() & 0xFF;
                    log("EEPROM_READ_RESPONSE: addr=0x" + Integer.toHexString(addr) + 
                        " val=0x" + Integer.toHexString(val));
                }
                break;
                
            case RES_WRITE_BYTE:
                if (payload.length >= 1) {
                    boolean success = (buffer.get() == 1);
                    log("EEPROM_WRITE_RESPONSE: " + (success ? "SUCCESS" : "FAILED"));
                }
                break;
                
            case RES_READ_ALL:
                log("EEPROM_READ_ALL_RESPONSE: " + payload.length + " bytes received");
                // Could display hex dump of first few bytes
                if (payload.length > 0) {
                    String hexPreview = bytesToHex(payload, Math.min(payload.length, 16));
                    log("First 16 bytes: " + hexPreview);
                }
                break;
                
            case RES_WRITE_ALL:
                if (payload.length >= 1) {
                    boolean success = (buffer.get() == 1);
                    log("EEPROM_WRITE_ALL_RESPONSE: " + (success ? "SUCCESS" : "FAILED"));
                }
                break;
                
            case CMD_ALIVE_MSG:
                if (payload.length >= 4) {
                    int timestamp = buffer.getShort() & 0xFFFF;
                    int counter = buffer.getShort() & 0xFFFF;
                    log("ALIVE_RECEIVED: ts=" + timestamp + " cnt=" + counter);
                }
                break;
                
            case CMD_SEAT_HEIGHT_CURRENT:
                if (payload.length >= 2) {
                    int heightMM = buffer.getShort() & 0xFFFF;
                    double heightCM = heightMM / 10.0;
                    log("SEAT_HEIGHT_CURRENT: " + heightCM + " cm");
                }
                break;
                
            case CMD_SEAT_SLIDE_CURRENT:
                if (payload.length >= 2) {
                    int slideMM = buffer.getShort() & 0xFFFF;
                    double slideCM = slideMM / 10.0;
                    log("SEAT_SLIDE_CURRENT: " + slideCM + " cm");
                }
                break;
                
            case CMD_SEAT_INCLINE_CURRENT:
                if (payload.length >= 2) {
                    int inclineMRad = buffer.getShort();
                    double inclineDeg = Math.toDegrees(inclineMRad / 1000.0);
                    log("SEAT_INCLINE_CURRENT: " + String.format("%.1f", inclineDeg) + "°");
                }
                break;
                
            default:
                log("UNKNOWN_RESPONSE: cmdId=0x" + Integer.toHexString(cmdId & 0xFF) + 
                    " payload=" + bytesToHex(payload, payload.length));
                break;
        }
    }

    private void handleTextData(byte[] data, int len) {
        String part = new String(data, 0, len, java.nio.charset.StandardCharsets.UTF_8);
        textBuffer.append(part);

        // Process complete lines
        String content = textBuffer.toString();
        int newlinePos;
        while ((newlinePos = content.indexOf('\n')) != -1) {
            String line = content.substring(0, newlinePos).trim();
            if (!line.isEmpty()) {
                log("DEBUG: " + line);
            }
            content = content.substring(newlinePos + 1);
        }
        
        // Keep remaining partial line in buffer
        textBuffer.setLength(0);
        textBuffer.append(content);
        
        // If buffer gets too long without newline, dump it
        if (textBuffer.length() > 500) {
            log("PARTIAL: " + textBuffer.toString());
            textBuffer.setLength(0);
        }
    }

    private void log(String message) {
        if (listener != null) {
            SwingUtilities.invokeLater(() -> listener.onTrace(message));
        }
    }

    private String bytesToHex(byte[] data, int len) {
        StringBuilder sb = new StringBuilder(len * 3);
        for (int i = 0; i < len; i++) {
            sb.append(String.format("%02X ", data[i] & 0xFF));
        }
        return sb.toString().trim();
    }

    @Override
    public void close() {
        serial.disconnect();
    }
    
    // Additional utility methods for raw communication
    public void sendRaw(byte[] data) {
        serial.send(data);
        log("RAW_SENT: " + bytesToHex(data, data.length));
    }
    
    public void sendCommand(String command) {
        serial.sendLine(command);
        log("CMD_SENT: " + command);
    }

    public boolean isConnected() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'isConnected'");
    }

    public CommStatistics getStatistics() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getStatistics'");
    }
}