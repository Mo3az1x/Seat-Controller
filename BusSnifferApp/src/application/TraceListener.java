package application;

/**
 * Callback interface for trace messages and raw frame events.
 */
public interface TraceListener {
    /**
     * Human readable trace line (text).
     */
    void onTrace(String message);

    /**
     * Raw frame bytes received from the bus (already validated as a frame).
     * @param data frame buffer
     * @param len  length
     */
    void onFrame(byte[] data, int len);
}
