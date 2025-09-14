package bussniffer;

import com.fazecast.jSerialComm.SerialPort;
import java.util.ArrayList;
import java.util.List;

public class PortUtil {
    public static List<String> getAvailablePortNames() {
        List<String> ports = new ArrayList<>();
        for (SerialPort p : SerialPort.getCommPorts()) {
            ports.add(p.getSystemPortName());
        }
        return ports;
    }
}
