package application;

import com.fazecast.jSerialComm.*;
import java.nio.charset.StandardCharsets;

public class SerialComm {

    public interface DataSink {
        void onBytes(byte[] data, int len);
    }

    private SerialPort comPort;
    private DataSink sink;

    /**
     * Connect to serial port
     */
    public boolean connect(String portName, int baudRate) {
        comPort = SerialPort.getCommPort(portName);
        comPort.setBaudRate(baudRate);
        comPort.setNumDataBits(8);
        comPort.setNumStopBits(SerialPort.ONE_STOP_BIT);
        comPort.setParity(SerialPort.NO_PARITY);

        boolean ok = comPort.openPort();
        if (!ok) return false;

        comPort.addDataListener(new SerialPortDataListener() {
            @Override
            public int getListeningEvents() {
                return SerialPort.LISTENING_EVENT_DATA_AVAILABLE;
            }

            @Override
            public void serialEvent(SerialPortEvent event) {
                if (event.getEventType() != SerialPort.LISTENING_EVENT_DATA_AVAILABLE) return;
                int available = comPort.bytesAvailable();
                if (available <= 0) return;

                byte[] buffer = new byte[available];
                int n = comPort.readBytes(buffer, buffer.length);

                if (sink != null && n > 0) {
                    sink.onBytes(buffer, n);
                }
            }
        });

        return true;
    }

    /**
     * Set callback for incoming bytes
     */
    public void setSink(DataSink sink) {
        this.sink = sink;
    }

    /**
     * Disconnect serial port
     */
    public void disconnect() {
        if (comPort != null) {
            try {
                comPort.removeDataListener();
            } catch (Exception ignored) {}
            if (comPort.isOpen()) comPort.closePort();
            comPort = null;
        }
    }

    /**
     * Check if connected
     */
    public boolean isConnected() {
        return comPort != null && comPort.isOpen();
    }

    /**
     * Send raw bytes (for Frames)
     */
    public void send(byte[] data) {
        if (isConnected()) {
            comPort.writeBytes(data, data.length);
        }
    }

    /**
     * Send line (UTF-8 string + newline) — useful for debugging
     */
    public void sendLine(String s) {
        send((s + "\n").getBytes(StandardCharsets.UTF_8));
    }
}