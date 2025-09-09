package application;

import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        System.out.println("=== Bus Sniffer App ===");

        // 1) List all COM ports
        PortUtil.listPorts();

        // 2) Ask user to select a port
        try (Scanner sc = new Scanner(System.in)) {
            System.out.print("Select port index: ");
            int idx = sc.nextInt();

            String portName = PortUtil.getPortNameByIndex(idx);
            if (portName == null) {
                System.out.println("Invalid index.");
                return;
            }

            // 3) Ask for baud rate
            System.out.print("Enter baud rate (e.g., 9600): ");
            int baud = sc.nextInt();

            // 4) Start Sniffer with a TraceListener that prints to console
            try (SnifferManager sniffer = new SnifferManager(message -> System.out.println(message))) {
                if (!sniffer.start(portName, baud)) return;

                System.out.println("Type commands to send (or 'exit' to quit):");
                sc.nextLine(); // consume the leftover newline

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
