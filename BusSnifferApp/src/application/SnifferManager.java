package application;

public class SnifferManager implements SerialComm.DataSink, AutoCloseable {
    private final SerialComm serial = new SerialComm();

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

    @Override
    public void onBytes(byte[] data, int len) {
        // اطبع البيانات المستلمة بالهيكس
        System.out.println(hexLine(data, len));
    }

    private String hexLine(byte[] data, int len) {
        StringBuilder sb = new StringBuilder(len * 3);
        for (int i = 0; i < len; i++) sb.append(String.format("%02X ", data[i]));
        return sb.toString().trim();
    }

    public void sendCommand(String line) {
        serial.sendLine(line); // لو عايز تبعت أوامر للـ ECU
    }

    @Override
    public void close() {
        serial.disconnect();
    }
}
