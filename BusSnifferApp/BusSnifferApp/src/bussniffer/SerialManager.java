package bussniffer;

import com.fazecast.jSerialComm.SerialPort;

import java.io.IOException;
import java.util.function.Consumer;

public class SerialManager {
    private SerialPort port;
    private Consumer<byte[]> callback;
    private volatile boolean running = false;

    public SerialManager(String portName, int baud, Consumer<byte[]> cb) {
        this.port = SerialPort.getCommPort(portName);
        this.port.setBaudRate(baud);
        this.callback = cb;
        this.port.setComPortTimeouts(SerialPort.TIMEOUT_NONBLOCKING, 0, 0);
    }

    public boolean open() {
        if (!port.openPort()) return false;
        running = true;
        new Thread(this::readLoop, "SerialReadThread").start();
        return true;
    }

    public boolean isOpen() { return port.isOpen(); }

    public void close() {
        running = false;
        try { Thread.sleep(50); } catch(Exception e){}
        if (port.isOpen()) port.closePort();
    }

    public void send(byte[] data) {
        if (port.isOpen()) {
            try {
                port.getOutputStream().write(data);
                port.getOutputStream().flush();
            } catch (IOException e) { e.printStackTrace(); }
        }
    }

    private void readLoop() {
        try {
            byte[] buffer = new byte[1024];
            int idx = 0;
            while (running && port.isOpen()) {
                int available = port.bytesAvailable();
                if (available <= 0) {
                    Thread.sleep(10);
                    continue;
                }
                byte[] tmp = new byte[available];
                int read = port.readBytes(tmp, tmp.length);
                for (int i = 0; i < read; i++) {
                    byte b = tmp[i];
                    buffer[idx++] = b;
                    if (idx >= 3 && buffer[0]==BusProtocol.HEADER && buffer[idx-1]==BusProtocol.TAIL) {
                        byte[] frame = new byte[idx];
                        System.arraycopy(buffer, 0, frame, 0, idx);
                        callback.accept(frame);
                        idx = 0;
                    }
                    if (idx == buffer.length) idx = 0; // reset safety
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
