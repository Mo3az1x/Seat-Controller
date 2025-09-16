package eeprom;

public class NvmManager {
    public static final int STARTUP_START = 0x0000;
    public static final int STARTUP_END   = 0x0003;
    public static final int CALIB_START   = 0x0004;
    public static final int CALIB_END     = 0x03FF;
    public static final int DIAG_START    = 0x0400;
    public static final int DIAG_END      = 0x07FF; // مثال لحد افتراضي

    public String readStartup() {
        return "[NVM] READ_STARTUP | Addr=0x0000–0x0003";
    }

    public String readCalibration() {
        return "[NVM] READ_CALIBRATION | Addr=0x0004–0x03FF";
    }

    public String readDiagnostic() {
        return "[NVM] READ_DIAGNOSTIC | Addr=0x0400–0x07FF";
    }

    public String writeStartup(byte[] data) {
        return "[NVM] WRITE_STARTUP | Addr=0x0000–0x0003 | Data=" + format(data);
    }

    public String writeCalibration(byte[] data) {
        return "[NVM] WRITE_CALIBRATION | Addr=0x0004–0x03FF | Size=" + data.length + " | Data=" + format(data);
    }

    public String writeDiagnostic(byte[] data) {
        return "[NVM] WRITE_DIAGNOSTIC | Addr=0x0400–0x07FF | Size=" + data.length + " | Data=" + format(data);
    }

    private String format(byte[] data) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < data.length; i++) {
            sb.append(String.format("0x%02X", data[i]));
            if (i < data.length - 1) sb.append(", ");
        }
        sb.append("]");
        return sb.toString();
    }
}
