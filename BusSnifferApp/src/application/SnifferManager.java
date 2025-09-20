package application;

public class SnifferManager {
    private final SeatControllerSnifferManager manager;

    public SnifferManager(TraceListener traceListener) {
        this.manager = new SeatControllerSnifferManager(traceListener);
    }

    public boolean start(String port, int baud) {
        return manager.start(port, baud);
    }

    public void close() {
        manager.close();
    }

    public void sendCommand(String command) {
        manager.sendCommand(command);
    }

    public void sendReadByte(int address) {
        manager.sendReadByte(address);
    }

    public void sendWriteByte(int address, int value) {
        manager.sendWriteByte(address, value);
    }

    public void sendWriteAll(byte[] pattern) {
        manager.sendWriteAll(pattern);
    }

    public void saveProfile(int profileId) {
        manager.saveProfile(profileId);
    }

    public void loadProfile(int profileId) {
        manager.loadProfile(profileId);
    }

    public void saveProfile(int profileId, double heightCm, double slideCm, double inclineDeg) {
        manager.saveProfile(profileId, heightCm, slideCm, inclineDeg);
    }

    public void saveProfileAt(int address, double heightCm, double slideCm, double inclineDeg) {
        manager.saveProfileAt(address, heightCm, slideCm, inclineDeg);
    }

    public void loadProfileAt(int address) {
        manager.loadProfileAt(address);
    }
}
