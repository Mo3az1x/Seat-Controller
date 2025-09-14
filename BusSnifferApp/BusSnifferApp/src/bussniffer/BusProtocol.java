package bussniffer;

public class BusProtocol {
    public static final byte HEADER = 0x7E;
    public static final byte TAIL   = 0x7F;

    public static final byte MSG_ALIVE        = 0x10;
    public static final byte MSG_CONTROL_REQ  = 0x14;
    public static final byte MSG_STATUS       = 0x20;
    public static final byte MSG_ACK          = 0x30;
    public static final byte MSG_NACK         = 0x31;
    public static final byte MSG_PROFILE_SAVE = 0x40;
    public static final byte MSG_PROFILE_LOAD = 0x41;
    public static final byte MSG_MODE_CHANGE  = 0x50;

    // build control request
    public static byte[] buildControlRequest(int h, int s, int i) {
        byte[] frame = new byte[9]; // HDR + ID + 6bytes payload + TAIL
        frame[0] = HEADER;
        frame[1] = MSG_CONTROL_REQ;
        frame[2] = (byte)((h >> 8) & 0xFF);
        frame[3] = (byte)(h & 0xFF);
        frame[4] = (byte)((s >> 8) & 0xFF);
        frame[5] = (byte)(s & 0xFF);
        frame[6] = (byte)((i >> 8) & 0xFF);
        frame[7] = (byte)(i & 0xFF);
        frame[8] = TAIL;
        return frame;
    }

    public static byte[] buildProfileSave(int profileId, int h, int s, int i) {
        byte[] frame = new byte[9];
        frame[0] = HEADER;
        frame[1] = MSG_PROFILE_SAVE;
        frame[2] = (byte)(profileId & 0xFF);
        frame[3] = (byte)((h >> 8) & 0xFF);
        frame[4] = (byte)(h & 0xFF);
        frame[5] = (byte)((s >> 8) & 0xFF);
        frame[6] = (byte)(s & 0xFF);
        frame[7] = (byte)((i >> 8) & 0xFF);
        // note: using fixed 9 length; last payload byte overlapped -> use 10 bytes if need more
        frame[8] = TAIL;
        return frame;
    }

    public static byte[] buildProfileLoad(int profileId) {
        byte[] frame = new byte[4];
        frame[0] = HEADER;
        frame[1] = MSG_PROFILE_LOAD;
        frame[2] = (byte)(profileId & 0xFF);
        frame[3] = TAIL;
        return frame;
    }

    public static byte[] buildModeChange(int mode) {
        byte[] f = new byte[4];
        f[0] = HEADER; f[1] = MSG_MODE_CHANGE; f[2] = (byte)(mode & 0xFF); f[3] = TAIL;
        return f;
    }

    // parse frames (robust: accepts variable length)
    public static ParsedMessage parseRawFrame(byte[] frame) {
        if (frame == null || frame.length < 3) return new ParsedMessage(false, "Frame too short");
        if (frame[0] != HEADER || frame[frame.length-1] != TAIL) return new ParsedMessage(false, "Bad header/tail");
        byte id = frame[1];
        switch (id) {
            case MSG_ALIVE: {
                if (frame.length < 7) return new ParsedMessage(false, "Alive too short");
                int ts = ((frame[2]&0xFF)<<8) | (frame[3]&0xFF);
                int counter = ((frame[4]&0xFF)<<8) | (frame[5]&0xFF);
                return new ParsedMessage(true, "Alive: ts="+ts+" cnt="+counter);
            }
            case MSG_CONTROL_REQ: {
                if (frame.length < 9) return new ParsedMessage(false, "ControlReq too short");
                int h = ((frame[2]&0xFF)<<8)|(frame[3]&0xFF);
                int s = ((frame[4]&0xFF)<<8)|(frame[5]&0xFF);
                int i = ((frame[6]&0xFF)<<8)|(frame[7]&0xFF);
                return new ParsedMessage(true, "ControlReq H="+h+" S="+s+" I="+i);
            }
            case MSG_STATUS: {
                if (frame.length < 6) return new ParsedMessage(false, "Status too short");
                int state = frame[2] & 0xFF;
                int err = frame[3] & 0xFF;
                return new ParsedMessage(true, "Status state="+state+" err="+err, state, err);
            }
            case MSG_ACK: {
                int cmd = frame.length>2? frame[2]&0xFF : -1;
                return new ParsedMessage(true, "ACK for cmd="+cmd);
            }
            case MSG_NACK: {
                int cmd = frame.length>2? frame[2]&0xFF : -1;
                int reason = frame.length>3? frame[3]&0xFF : -1;
                return new ParsedMessage(true, "NACK for cmd="+cmd+" reason="+reason);
            }
            case MSG_PROFILE_SAVE:
                return new ParsedMessage(true, "Profile Save Resp");
            case MSG_PROFILE_LOAD:
                return new ParsedMessage(true, "Profile Load Resp");
            default:
                return new ParsedMessage(true, "Unknown Msg ID="+String.format("%02X", id));
        }
    }

    public static class ParsedMessage {
        public final boolean ok;
        public final String text;
        public final int state; // used for status messages
        public final int err;

        public ParsedMessage(boolean ok, String text) { this(ok,text,-1,-1); }
        public ParsedMessage(boolean ok, String text, int state, int err) { this.ok=ok; this.text=text; this.state=state; this.err=err; }
    }
}
