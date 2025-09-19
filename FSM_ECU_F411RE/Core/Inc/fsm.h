#ifndef FSM_H
#define FSM_H

#include "stdint.h"
#include "FreeRTOS.h"
#include "queue.h"
extern QueueHandle_t xEventQueue;

typedef enum {
    STATE_OFF = 0,
    STATE_IDLE,
    STATE_BUSY,
    STATE_LOCKED,
    STATE_ERROR,
    STATE_PERM_ERROR
} SystemState_t;

typedef enum {
    EVENT_NONE = 0,
    EVENT_BTN_PRESSED,
    EVENT_RESET,
    EVENT_MOTION_COMPLETED,
    EVENT_DRIVING_STARTED,
    EVENT_STOP,
    EVENT_FLT_DETECTED,
    EVENT_PERM_ERROR
} SystemEvent_t;

extern QueueHandle_t xEventQueue;   // declaration only, no definition here!

void FSM_Init(void);
void vFSMTask(void *pvParameters);
#endif // FSM_H
