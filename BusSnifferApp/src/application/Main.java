package application;

import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        System.out.println("=== Bus Sniffer App ===");

        // 1) عرض كل الـ COM Ports
        PortUtil.listPorts();

        // 2) اختيار port من المستخدم
        try (Scanner sc = new Scanner(System.in)) {
            System.out.print("Select port index: ");
            int idx = sc.nextInt();

            String portName = PortUtil.getPortNameByIndex(idx);
            if (portName == null) {
                System.out.println("Invalid index.");
                return;
            }

            // 3) إدخال Baud Rate
            System.out.print("Enter baud rate (مثلاً 9600): ");
            int baud = sc.nextInt();

            // 4) تشغيل Sniffer
            try (SnifferManager sniffer = new SnifferManager()) {
                if (!sniffer.start(portName, baud)) return;

                System.out.println("Type commands to send (or 'exit' to quit):");
                sc.nextLine(); // consume endline
                while (true) {
                    String line = sc.nextLine();
                    if ("exit".equalsIgnoreCase(line)) break;
                    if (!line.isBlank()) sniffer.sendCommand(line);
                }
            }
        }

        System.out.println("Sniffer closed.");
    }
}
