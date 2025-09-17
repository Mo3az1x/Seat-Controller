package application;

import java.util.Scanner;
import java.io.*;

public class Main {
    public static void main(String[] args) throws FileNotFoundException, IOException {
        System.out.println("=== EEPROM Sniffer App (Console) ===");

        PortUtil.listPorts();

        try (Scanner sc = new Scanner(System.in)) {
            System.out.print("Select port index: ");
            int idx = sc.nextInt();

            String portName = PortUtil.getPortNameByIndex(idx);
            if (portName == null) {
                System.out.println("Invalid index.");
                return;
            }

            System.out.print("Enter baud rate (e.g., 115200): ");
            int baud = sc.nextInt();
            sc.nextLine();

            try (SnifferManager sniffer = new SnifferManager(new TraceListener() {
                @Override
                public void onTrace(String message) {
                    System.out.println(message);
                }

                @Override
                public void onFrame(byte[] data, int len) {
                    System.out.println("Frame received, len=" + len);
                }
            })) {
                if (!sniffer.start(portName, baud)) return;

                while (true) {
                    System.out.print("\nCommand (1=ReadByte,2=WriteByte,3=ReadAll,4=WriteAll,save,load,exit): ");
                    String line = sc.nextLine().trim();

                    if ("exit".equalsIgnoreCase(line)) break;

                    switch (line) {
                        case "1":
                            System.out.print("Enter address: ");
                            int addr = sc.nextInt(); sc.nextLine();
                            sniffer.sendReadByte(addr);
                            break;

                        case "2":
                            System.out.print("Enter address: ");
                            addr = sc.nextInt();
                            System.out.print("Enter value (0-255): ");
                            int val = sc.nextInt(); sc.nextLine();
                            sniffer.sendWriteByte(addr, val);
                            break;

                        case "3":
                            System.out.print("Enter size: ");
                            int size = sc.nextInt(); sc.nextLine();
                            sniffer.sendReadAll(size);
                            break;

                        case "4":
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

                        case "save":
                            byte[] dump = sniffer.getLastReadAll();
                            if (dump == null) {
                                System.out.println("No data to save (do a ReadAll first).");
                            } else {
                                try (FileOutputStream fos = new FileOutputStream("profile.bin")) {
                                    fos.write(dump);
                                    System.out.println("Profile saved (" + dump.length + " bytes).");
                                }
                            }
                            break;

                        case "load":
                            File f = new File("profile.bin");
                            if (!f.exists()) {
                                System.out.println("No profile.bin found.");
                            } else {
                                try {
                                    byte[] buf = java.nio.file.Files.readAllBytes(f.toPath());
                                    sniffer.sendWriteAll(buf);
                                    System.out.println("Profile loaded (" + buf.length + " bytes).");
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            }
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
