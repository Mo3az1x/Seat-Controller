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

    public void saveProfile(int profileId) {
        manager.saveProfile(profileId);
    }

    public void loadProfile(int profileId) {
        manager.loadProfile(profileId);
    }

    public void saveProfile(int profileId, double heightCm, double slideCm, double inclineDeg) {
        manager.saveProfile(profileId, heightCm, slideCm, inclineDeg);
    }
}
