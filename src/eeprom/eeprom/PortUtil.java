package eeprom;

import com.fazecast.jSerialComm.SerialPort;
import java.util.ArrayList;
import java.util.List;

public class PortUtil {
    public static List<String> getAvailablePortNames() {
        SerialPort[] ports = SerialPort.getCommPorts();
        List<String> names = new ArrayList<>();
        for (SerialPort port : ports) {
            names.add(port.getSystemPortName());
        }
        return names;
    }
}
