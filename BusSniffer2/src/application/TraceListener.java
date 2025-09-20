package application;

/**
 * Enhanced TraceListener interface for Seat Controller ECU communication
 * Supports both text-based and binary frame-based communication
 */
public interface TraceListener {
    
    /**
     * Called when a text-based trace message is received
     * @param message The trace message to display
     */
    void onTrace(String message);

    /**
     * Called when a binary frame is received
     * @param data The raw frame data
     * @param len The length of valid data in the array
     */
    void onFrame(byte[] data, int len);
    
    /**
     * Called when a specific seat controller message is parsed
     * @param messageType The type of message (ALIVE, GEARBOX, SEAT_CONTROL, etc.)
     * @param data Parsed message data
     */
    default void onSeatControllerMessage(SeatControllerMessageType messageType, Object data) {
        // Default implementation - can be overridden for specific message handling
        onTrace("SEAT_MSG: " + messageType + " = " + data.toString());
    }
    
    /**
     * Called when an EEPROM operation response is received
     * @param operation The EEPROM operation type
     * @param address The memory address
     * @param data The data read/written
     * @param success Whether the operation was successful
     */
    default void onEEPROMResponse(EEPROMOperation operation, int address, byte[] data, boolean success) {
        String result = success ? "SUCCESS" : "FAILED";
        String dataStr = (data != null && data.length > 0) ? bytesToHex(data) : "N/A";
        onTrace("EEPROM_" + operation + ": addr=0x" + Integer.toHexString(address) + 
                " data=" + dataStr + " result=" + result);
    }
    
    /**
     * Called when a fault condition is detected or cleared
     * @param faultNumber The fault identifier (1, 2, etc.)
     * @param active Whether the fault is active or cleared
     * @param timestamp When the fault occurred
     */
    default void onFaultStatus(int faultNumber, boolean active, long timestamp) {
        String status = active ? "ACTIVE" : "CLEARED";
        onTrace("FAULT_" + faultNumber + ": " + status + " at " + timestamp);
    }
    
    /**
     * Called when communication statistics need to be updated
     * @param stats Communication statistics object
     */
    default void onStatisticsUpdate(CommStatistics stats) {
        onTrace("STATS: RX=" + stats.messagesReceived + " TX=" + stats.messagesSent + 
                " Errors=" + stats.errors + " Rate=" + stats.getMessageRate() + "msg/s");
    }
    
    /**
     * Called when the connection status changes
     * @param connected True if connected, false if disconnected
     * @param portName The port name
     * @param protocol The communication protocol being used
     */
    default void onConnectionStatus(boolean connected, String portName, String protocol) {
        String status = connected ? "CONNECTED" : "DISCONNECTED";
        onTrace("CONNECTION: " + status + " - " + portName + " (" + protocol + ")");
    }
    
    /**
     * Called when a calibration data update is received
     * @param parameter The calibration parameter name
     * @param oldValue The previous value
     * @param newValue The new value
     */
    default void onCalibrationUpdate(String parameter, double oldValue, double newValue) {
        onTrace("CALIBRATION: " + parameter + " changed from " + oldValue + " to " + newValue);
    }
    
    // Helper method for converting bytes to hex string
    default String bytesToHex(byte[] data) {
        if (data == null || data.length == 0) return "";
        StringBuilder sb = new StringBuilder();
        for (byte b : data) {
            sb.append(String.format("%02X ", b));
        }
        return sb.toString().trim();
    }
    
    /**
     * Enumeration of seat controller message types
     */
    enum SeatControllerMessageType {
        ALIVE_MSG,
        GEARBOX_STATUS,
        SEAT_HEIGHT_TARGET,
        SEAT_SLIDE_TARGET, 
        SEAT_INCLINE_TARGET,
        SEAT_HEIGHT_CURRENT,
        SEAT_SLIDE_CURRENT,
        SEAT_INCLINE_CURRENT,
        SEAT_CONTROL_REQUEST,
        FAULT_MESSAGE,
        DIAGNOSTIC_MESSAGE,
        USER_PROFILE_DATA,
        CALIBRATION_DATA,
        UNKNOWN
    }
    
    /**
     * Enumeration of EEPROM operation types
     */
    enum EEPROMOperation {
        READ_BYTE,
        WRITE_BYTE,
        READ_ALL,
        WRITE_ALL,
        ERASE,
        VERIFY
    }
    
    /**
     * Communication statistics data structure
     */
    class CommStatistics {
        public long messagesReceived = 0;
        public long messagesSent = 0;
        public long errors = 0;
        public long bytesReceived = 0;
        public long bytesSent = 0;
        public long connectionTime = 0;
        public long lastMessageTime = 0;
        
        private long startTime = System.currentTimeMillis();
        
        public double getMessageRate() {
            long elapsed = System.currentTimeMillis() - startTime;
            if (elapsed <= 0) return 0.0;
            return (messagesReceived + messagesSent) * 1000.0 / elapsed;
        }
        
        public double getByteRate() {
            long elapsed = System.currentTimeMillis() - startTime;
            if (elapsed <= 0) return 0.0;
            return (bytesReceived + bytesSent) * 1000.0 / elapsed;
        }
        
        public void reset() {
            messagesReceived = messagesSent = errors = 0;
            bytesReceived = bytesSent = 0;
            startTime = System.currentTimeMillis();
        }
    }
}