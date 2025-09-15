package application;

import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        System.out.println("=== EEPROM Sniffer App ===");

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
            System.out.print("Enter baud rate (e.g., 115200): ");
            int baud = sc.nextInt();

            // 4) Start Sniffer with a TraceListener that prints to console
            try (SnifferManager sniffer = new SnifferManager(new TraceListener() {
                @Override
                public void onTrace(String message) {
                    System.out.println(message);
                }

                @Override
                public void onFrame(byte[] data, int len) {
                    // TODO Auto-generated method stub
                    throw new UnsupportedOperationException("Unimplemented method 'onFrame'");
                }
            })) {
                if (!sniffer.start(portName,baud)) return;

                System.out.println("\n=== Commands Menu ===");
                System.out.println("1 - Read Byte");
                System.out.println("2 - Write Byte");
                System.out.println("3 - Read All");
                System.out.println("4 - Write All");
                System.out.println("exit - Quit");
                sc.nextLine(); // consume leftover newline

                while (true) {
                    System.out.print("\nEnter command: ");
                    String line = sc.nextLine().trim();

                    if ("exit".equalsIgnoreCase(line)) break;

                    switch (line) {
                        case "1": // Read Byte
                            System.out.print("Enter address: ");
                            int addr = sc.nextInt();
                            sc.nextLine();
                            sniffer.sendReadByte(addr);
                            break;

                        case "2": // Write Byte
                            System.out.print("Enter address: ");
                            addr = sc.nextInt();
                            System.out.print("Enter value (0-255): ");
                            int val = sc.nextInt();
                            sc.nextLine();
                            sniffer.sendWriteByte(addr, val);
                            break;

                        case "3": // Read All
                            System.out.print("Enter size: ");
                            int size = sc.nextInt();
                            sc.nextLine();
                            sniffer.sendReadAll(size);
                            break;

                        case "4": // Write All
                            System.out.print("Enter number of bytes: ");
                            int len = sc.nextInt();
                            byte[] data = new byte[len];
                            for (int i = 0; i < len; i++) {
                                System.out.print("Byte[" + i + "]: ");
                                data[i] = (byte) sc.nextInt();
                            }
                            sc.nextLine();
                            sniffer.sendWriteAll(data);
                            break;

                        default:
                            System.out.println("Unknown command.");
                    }
                }
            }
        }

        System.out.println("Sniffer closed.");
    }
}
