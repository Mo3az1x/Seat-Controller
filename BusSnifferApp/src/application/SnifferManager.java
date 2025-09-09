package application;
import javax.swing.SwingUtilities;

public class SnifferManager implements SerialComm.DataSink, AutoCloseable {
    private final SerialComm serial = new SerialComm();
    private TraceListener listener; // listener for trace messages
    public SnifferManager(TraceListener listener) {
        this.listener = listener;
    }

    


    /**
     * Connect to the serial port and start listening
     */
    public boolean start(String portName, int baud) {
        serial.setSink(this);
        boolean ok = serial.connect(portName, baud);
        if (!ok) {
            System.err.println("Failed to open port " + portName);
            return false;
        }
        System.out.println("Connected to " + portName + " @ " + baud + " baud. Sniffing...");
        return true;
    }

    /**
     * Handle incoming bytes from STM
     */
@Override
    public void onBytes(byte[] data, int len) {
        String message;
        String text = tryDecodeUtf8(data, len);
        if (text != null) message = "TXT: " + text;
        else message = "HEX: " + hexLine(data, len);

        // Send message to GUI
        if (listener != null) {
            SwingUtilities.invokeLater(() -> listener.onTrace(message));
        }
    }

    public void sendCommand(String cmd) {
        try {
            byte value = parseCommand(cmd);
            String hexStr = String.format("%02X", value);
            serial.sendLine(hexStr);
            if (listener != null) {
                SwingUtilities.invokeLater(() -> listener.onTrace("Sent " + cmd + " -> " + hexStr));
            }
        } catch (IllegalArgumentException e) {
            if (listener != null) {
                SwingUtilities.invokeLater(() -> listener.onTrace("Invalid command: " + cmd + " (" + e.getMessage() + ")"));
            }
        }
    }

    // Keep the other methods (parseCommand, hexLine, tryDecodeUtf8, close) as they are

    /**
     * Convert H/S/I command to byte (0-255)
     */
    private byte parseCommand(String cmd) {
        if (cmd == null || cmd.length() < 2) throw new IllegalArgumentException("Invalid command");

        char type = Character.toUpperCase(cmd.charAt(cmd.length() - 1));
        double value;
        try {
            value = Double.parseDouble(cmd.substring(0, cmd.length() - 1));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid number in command");
        }

        byte byteValue = 0;

        switch (type) {
            case 'H': // Height: 2 - 5.3 cm
                if (value < 2) value = 2;
                if (value > 5.3) value = 5.3;
                byteValue = (byte) ((value - 2) / (5.3 - 2) * 255);
                break;

            case 'S': // Slide: 3 - 7.5 cm
                if (value < 3) value = 3;
                if (value > 7.5) value = 7.5;
                byteValue = (byte) ((value - 3) / (7.5 - 3) * 255);
                break;

            case 'I': // Incline: 67 - 105 degree
                if (value < 67) value = 67;
                if (value > 105) value = 105;
                byteValue = (byte) ((value - 67) / (105 - 67) * 255);
                break;

            default:
                throw new IllegalArgumentException("Unknown command type: " + type);
        }

        return byteValue;
    }

    /**
     * Convert byte array to hex string for logging
     */
    private String hexLine(byte[] data, int len) {
        StringBuilder sb = new StringBuilder(len * 3);
        for (int i = 0; i < len; i++) sb.append(String.format("%02X ", data[i]));
        return sb.toString().trim();
    }

    /**
     * Try decoding bytes as UTF-8 text
     */
    private String tryDecodeUtf8(byte[] data, int len) {
        try {
            String s = new String(data, 0, len, java.nio.charset.StandardCharsets.UTF_8);
            int printable = 0;
            for (int i = 0; i < s.length(); i++) {
                char c = s.charAt(i);
                if ((c >= 32 && c <= 126) || c == '\n' || c == '\r' || c == '\t') printable++;
            }
            if (printable * 2 >= s.length()) return s;
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public void close() {
        serial.disconnect();
    }
}
    