package application;

public interface TraceListener {
    
    void onTrace(String message);

    void onFrame(byte[] data, int len);
 
}
