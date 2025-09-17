package eeprom;

import javax.swing.SwingUtilities;

public class SnifferManager implements SerialComm.DataSink, AutoCloseable {
    private final SerialComm serial = new SerialComm();
    private TraceListener listener;

    private static final byte HEADER = 0x7E;
    private static final byte TAIL   = 0x7F;

    private static final byte CMD_READ_BYTE   = 0x01;
    private static final byte CMD_WRITE_BYTE  = 0x02;
    private static final byte CMD_READ_ALL    = 0x03;
    private static final byte CMD_WRITE_ALL   = 0x04;

    private static final byte RES_READ_BYTE   = (byte)0x81;
    private static final byte RES_WRITE_BYTE  = (byte)0x82;
    private static final byte RES_READ_ALL    = (byte)0x83;
    private static final byte RES_WRITE_ALL   = (byte)0x84;

    public SnifferManager(TraceListener listener) {
        this.listener = listener;
    }

    public boolean start(String portName, int baud) {
        serial.setSink(this);
        boolean ok = serial.connect(portName, baud);
        if (!ok) {
            log("❌ Failed to open " + portName);
            return false;
        }
        log("✅ Connected to " + portName + " @ " + baud);
        return true;
    }

    public void sendReadByte(int addr) {
        serial.send(buildFrame(CMD_READ_BYTE, addr, 0));
        log("➡ Sent READ_BYTE addr=" + addr);
    }

    public void sendWriteByte(int addr, int val) {
        serial.send(buildFrame(CMD_WRITE_BYTE, addr, val));
        log("➡ Sent WRITE_BYTE addr=" + addr + " val=" + val);
    }

    public void sendReadAll(int size) {
        serial.send(buildFrame(CMD_READ_ALL, size, 0));
        log("➡ Sent READ_ALL size=" + size);
    }

    public void sendWriteAll(byte[] data) {
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
        serial.send(frame);
        log("➡ Sent WRITE_ALL size=" + len);
    }

    private byte[] buildFrame(byte cmd, int p1, int p2) {
        byte[] frame = new byte[12];
        frame[0] = HEADER;
        frame[1] = 0; frame[2] = 0; frame[3] = 0; frame[4] = 5;
        frame[5] = cmd;
        frame[6] = (byte)((p1 >> 24) & 0xFF);
        frame[7] = (byte)((p1 >> 16) & 0xFF);
        frame[8] = (byte)((p1 >> 8) & 0xFF);
        frame[9] = (byte)(p1 & 0xFF);
        frame[10] = (byte)(p2 & 0xFF);
        frame[11] = TAIL;
        return frame;
    }

    @Override
    public void onBytes(byte[] data, int len) {
        if (len < 6) return;
        byte cmdId = data[5];
        String msg;
        switch (cmdId) {
            case RES_READ_BYTE:
                int addr = (data[6] & 0xFF);
                int val = data[7] & 0xFF;
                msg = "⬅ READ_BYTE Response: addr=" + addr + " val=" + val;
                break;
            case RES_WRITE_BYTE:
                msg = "⬅ WRITE_BYTE Response: " + (data[6] == 1 ? "OK" : "FAIL");
                break;
            case RES_READ_ALL:
                msg = "⬅ READ_ALL Response (" + (len - 7) + " bytes)";
                break;
            case RES_WRITE_ALL:
                msg = "⬅ WRITE_ALL Response: " + (data[6] == 1 ? "OK" : "FAIL");
                break;
            default:
                msg = "⬅ HEX: " + hexLine(data, len);
        }
        final String finalMsg = msg;
        SwingUtilities.invokeLater(() -> listener.onTrace(finalMsg));
    }

    private void log(String s) {
        SwingUtilities.invokeLater(() -> listener.onTrace(s));
    }

    private String hexLine(byte[] data, int len) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < len; i++) sb.append(String.format("%02X ", data[i]));
        return sb.toString().trim();
    }

    @Override
    public void close() {
        serial.disconnect();
    }
}
