package eeprom;

import com.fazecast.jSerialComm.SerialPort;

public class SerialComm {
    private SerialPort port;
    private DataSink sink;

    public interface DataSink {
        void onBytes(byte[] data, int len);
    }

    public void setSink(DataSink sink) {
        this.sink = sink;
    }

    public boolean connect(String portName, int baud) {
        port = SerialPort.getCommPort(portName);
        port.setBaudRate(baud);
        port.setComPortTimeouts(SerialPort.TIMEOUT_READ_SEMI_BLOCKING, 100, 100);
        if (!port.openPort()) return false;

        Thread t = new Thread(() -> {
            byte[] buffer = new byte[1024];
            while (port.isOpen()) {
                int n = port.readBytes(buffer, buffer.length);
                if (n > 0 && sink != null) {
                    sink.onBytes(buffer, n);
                }
            }
        });
        t.setDaemon(true);
        t.start();
        return true;
    }

    public void send(byte[] data) {
        if (port != null && port.isOpen()) {
            port.writeBytes(data, data.length);
        }
    }

    public void disconnect() {
        if (port != null) port.closePort();
    }
}
