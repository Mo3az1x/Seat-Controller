import time

# === بروتوكول ثابت ===
HEADER = 0x7E
TAIL   = 0x7F

MSG_ALIVE       = 0x10
MSG_CONTROL_REQ = 0x14
MSG_STATUS      = 0x20
MSG_ACK         = 0x30
MSG_NACK        = 0x31

# === Helper ===
def build_frame(msg_id, payload=None):
    if payload is None:
        payload = []
    return bytes([HEADER, msg_id] + payload + [TAIL])

def send(frame, desc=""):
    # بدل Serial، هنطبع فقط
    print(f"[SIM] Sent {desc}: {frame.hex(' ')}")

# === main loop ===
def main():
    print("[SIM] Starting STM Simulator without Serial Port...")

    counter = 0
    while True:
        # 1. Send Alive every 2 sec
        ts = int(time.time()) & 0xFFFF
        alive_payload = [(ts >> 8) & 0xFF, ts & 0xFF,
                         (counter >> 8) & 0xFF, counter & 0xFF]
        send(build_frame(MSG_ALIVE, alive_payload), "Alive")
        counter += 1

        # 2. Simulate Idle state
        send(build_frame(MSG_STATUS, [0, 0, 0, 0]), "Status IDLE")
        time.sleep(3)

        # 3. Simulate Moving
        send(build_frame(MSG_STATUS, [1, 0, 0, 0]), "Status MOVING")
        time.sleep(2)

        # 4. Send ACK for ControlReq (pretend user sent one)
        send(build_frame(MSG_ACK, [MSG_CONTROL_REQ]), "ACK ControlReq")
        time.sleep(2)

        # 5. Back to Idle
        send(build_frame(MSG_STATUS, [0, 0, 0, 0]), "Status IDLE")
        time.sleep(3)

        # 6. Simulate Error
        send(build_frame(MSG_STATUS, [2, 5, 0, 0]), "Status ERROR (code=5)")
        time.sleep(4)

        # Back to Idle after error clear
        send(build_frame(MSG_STATUS, [0, 0, 0, 0]), "Status IDLE after clear")
        time.sleep(5)

if __name__ == "__main__":
    main()
