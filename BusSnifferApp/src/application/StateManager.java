package application;

/**
 * Simple state manager for Seat Controller states.
 */
public class StateManager {

    public enum State { OFF, IDLE, BUSY, LOCKED, ERROR, SHUTDOWN }

    private State current = State.OFF;
    private int errorCount = 0;

    public StateManager() {
    }

    public synchronized State getCurrent() {
        return current;
    }

    public synchronized void setCurrent(State s) {
        current = s;
    }

    /**
     * Apply an event name (string) to attempt a transition.
     * This is intentionally simple — you can expand to more structured events later.
     */
    public synchronized void transition(String event) {
        switch (current) {
            case OFF:
                if ("IGN_ON".equals(event)) current = State.IDLE;
                break;
            case IDLE:
                if ("BTN_PRESS".equals(event)) current = State.BUSY;
                if ("DRV_START".equals(event)) current = State.LOCKED;
                break;
            case BUSY:
                if ("BTN_RELEASE".equals(event)) current = State.IDLE;
                if ("DRV_START".equals(event)) current = State.LOCKED;
                break;
            case LOCKED:
                if ("DRV_STOP".equals(event)) current = State.IDLE;
                break;
            case ERROR:
                if ("RESET".equals(event)) {
                    errorCount = 0;
                    current = State.OFF;
                }
                break;
            case SHUTDOWN:
                // remain until power cycle or explicit reset
                break;
        }
    }

    public synchronized void setError() {
        errorCount++;
        current = State.ERROR;
    }

    public synchronized int getErrorCount() {
        return errorCount;
    }

    public synchronized boolean isChangeAllowed() {
        // Changes from UI allowed only in IDLE (you can extend logic)
        return current == State.IDLE;
    }
}
