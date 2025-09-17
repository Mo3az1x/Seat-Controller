package application;

import com.fazecast.jSerialComm.SerialPort;

public class PortUtil {

    public static void listPorts() {
        SerialPort[] ports = SerialPort.getCommPorts();
        if (ports.length == 0) {
            System.out.println("No serial ports found.");
            return;
        }

        System.out.println("Available COM Ports:");
        for (int i = 0; i < ports.length; i++) {
            System.out.printf("[%d] %s (%s)%n", i,
                ports[i].getSystemPortName(),
                ports[i].getDescriptivePortName());
        }
    }

    public static String getPortNameByIndex(int index) {
        SerialPort[] ports = SerialPort.getCommPorts();
        if (index < 0 || index >= ports.length) return null;
        return ports[index].getSystemPortName();
    }

    // 🆕 دي الإضافة
    public static String[] getAllPorts() {
        SerialPort[] ports = SerialPort.getCommPorts();
        String[] names = new String[ports.length];
        for (int i = 0; i < ports.length; i++) {
            names[i] = ports[i].getSystemPortName();
        }
        return names;
    }
}

