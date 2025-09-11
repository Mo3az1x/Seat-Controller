package application;
import javax.swing.SwingUtilities;

public class SnifferManager implements SerialComm.DataSink, AutoCloseable {
    private final SerialComm serial = new SerialComm();
    private TraceListener listener; // listener for trace messages

    // Frame format constants
    private static final byte HEADER = 0x7E;
    private static final byte TAIL   = 0x7F;

    // Command IDs
    private static final byte CMD_READ_BYTE   = 0x01;
    private static final byte CMD_WRITE_BYTE  = 0x02;
    private static final byte CMD_READ_ALL    = 0x03;
    private static final byte CMD_WRITE_ALL   = 0x04;

    // Response IDs (example: request + 0x80)
    private static final byte RES_READ_BYTE   = (byte)0x81;
    private static final byte RES_WRITE_BYTE  = (byte)0x82;
    private static final byte RES_READ_ALL    = (byte)0x83;
    private static final byte RES_WRITE_ALL   = (byte)0x84;

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

    // ======= EEPROM COMMANDS =======
    public void sendReadByte(int addr) {
        byte[] frame = buildFrame(CMD_READ_BYTE, addr, 0);
        serial.send(frame);
        log("Sent READ_BYTE addr=" + addr);
    }

    public void sendWriteByte(int addr, int value) {
        byte[] frame = buildFrame(CMD_WRITE_BYTE, addr, value);
        serial.send(frame);
        log("Sent WRITE_BYTE addr=" + addr + " val=" + value);
    }

    public void sendReadAll(int size) {
        byte[] frame = buildFrame(CMD_READ_ALL, size, 0);
        serial.send(frame);
        log("Sent READ_ALL size=" + size);
    }

    public void sendWriteAll(byte[] data) {
        // هنا ببساطة: HEADER | len | CMD | data[] | TAIL
        int len = data.length;
        byte[] frame = new byte[7 + len]; // 1H + 4L + 1ID + data + 1T
        frame[0] = HEADER;
        frame[1] = (byte)((len >> 24) & 0xFF);
        frame[2] = (byte)((len >> 16) & 0xFF);
        frame[3] = (byte)((len >> 8) & 0xFF);
        frame[4] = (byte)(len & 0xFF);
        frame[5] = CMD_WRITE_ALL;
        System.arraycopy(data, 0, frame, 6, len);
        frame[6 + len - 0] = TAIL;

        serial.send(frame);
        log("Sent WRITE_ALL size=" + len);
    }

    // Build small frames with 2 params (addr, val)
    private byte[] buildFrame(byte cmd, int p1, int p2) {
        byte[] frame = new byte[12]; // 1H + 4L + 1CMD + 4P1 + 1P2 + 1T
        frame[0] = HEADER;
        frame[1] = 0x00; frame[2] = 0x00; frame[3] = 0x00; frame[4] = 0x05; // length=5 bytes payload
        frame[5] = cmd;
        frame[6] = (byte)((p1 >> 24) & 0xFF);
        frame[7] = (byte)((p1 >> 16) & 0xFF);
        frame[8] = (byte)((p1 >> 8) & 0xFF);
        frame[9] = (byte)(p1 & 0xFF);
        frame[10] = (byte)(p2 & 0xFF); // second param (value)
        frame[11] = TAIL;
        return frame;
    }

    /**
     * Handle incoming bytes from Arduino
     */
    @Override
    public void onBytes(byte[] data, int len) {
        if (len < 3) return;

        byte cmdId = data[5]; // assume fixed frame position
        String msg = "";

        switch (cmdId) {
            case RES_READ_BYTE:
                int addr = ((data[6] & 0xFF) << 24) | ((data[7] & 0xFF) << 16) |
                           ((data[8] & 0xFF) << 8) | (data[9] & 0xFF);
                int val = data[10] & 0xFF;
                msg = "READ_BYTE Response: addr=" + addr + " val=" + val;
                break;

            case RES_WRITE_BYTE:
                msg = "WRITE_BYTE Response: " + (data[6] == 1 ? "OK" : "FAIL");
                break;

            case RES_READ_ALL:
                msg = "READ_ALL Response: size=" + (len - 7);
                break;

            case RES_WRITE_ALL:
                msg = "WRITE_ALL Response: " + (data[6] == 1 ? "OK" : "FAIL");
                break;

            default:
                msg = "HEX: " + hexLine(data, len);
                break;
        }

        final String finalMsg = msg;
        if (listener != null) {
            SwingUtilities.invokeLater(() -> listener.onTrace(finalMsg));
        }
    }

    private void log(String s) {
        if (listener != null) {
            SwingUtilities.invokeLater(() -> listener.onTrace(s));
        }
    }

    private String hexLine(byte[] data, int len) {
        StringBuilder sb = new StringBuilder(len * 3);
        for (int i = 0; i < len; i++) sb.append(String.format("%02X ", data[i]));
        return sb.toString().trim();
    }

    @Override
    public void close() {
        serial.disconnect();
    }

    public void sendCommand(String line) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'sendCommand'");
    }
}