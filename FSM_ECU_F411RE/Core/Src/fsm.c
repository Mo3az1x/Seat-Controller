#include "fsm.h"
#include "stm32f4xx_hal.h"
#include "cmsis_os.h"   // optional if you like cmsis wrappers
#include <stdio.h>
#include <string.h>
#include "main.h"
/* These symbols come from CubeMX generated files: */
extern UART_HandleTypeDef huart2;   // provided by usart.c

/* queue handle declared in header */


static SystemState_t currentState = STATE_OFF;

void vFSMTask(void *pvParameters)
{
    SystemEvent_t evt;

    printf("FSM: Task started. State = STATE_OFF\r\n");

    for (;;) {
        if (xQueueReceive(xEventQueue, &evt, portMAX_DELAY) == pdPASS) {
            printf("FSM: Received event %d in state %d\r\n", (int)evt, (int)currentState);

            switch (currentState) {
                case STATE_OFF:
                    if (evt == EVENT_RESET) {
                        currentState = STATE_IDLE;
                        printf("FSM: -> STATE_IDLE\r\n");
                        HAL_GPIO_WritePin(LD2_GPIO_Port, LD2_Pin, GPIO_PIN_SET); // LED on = idle
                    }
                    break;

                case STATE_IDLE:
                    if (evt == EVENT_BTN_PRESSED || evt == EVENT_DRIVING_STARTED) {
                        currentState = STATE_BUSY;
                        printf("FSM: -> STATE_BUSY\r\n");
                        HAL_GPIO_WritePin(LD2_GPIO_Port, LD2_Pin, GPIO_PIN_RESET); // LED off = busy
                    } else if (evt == EVENT_FLT_DETECTED) {
                        currentState = STATE_ERROR;
                        printf("FSM: -> STATE_ERROR\r\n");
                    }
                    break;

                case STATE_BUSY:
                    if (evt == EVENT_MOTION_COMPLETED) {
                        currentState = STATE_IDLE;
                        printf("FSM: -> STATE_IDLE\r\n");
                        HAL_GPIO_WritePin(LD2_GPIO_Port, LD2_Pin, GPIO_PIN_SET);
                    } else if (evt == EVENT_DRIVING_STARTED) {
                        currentState = STATE_LOCKED;
                        printf("FSM: -> STATE_LOCKED\r\n");
                    } else if (evt == EVENT_FLT_DETECTED) {
                        currentState = STATE_ERROR;
                        printf("FSM: -> STATE_ERROR\r\n");
                    }
                    break;

                case STATE_LOCKED:
                    if (evt == EVENT_STOP) {
                        currentState = STATE_IDLE;
                        printf("FSM: -> STATE_IDLE\r\n");
                        HAL_GPIO_WritePin(LD2_GPIO_Port, LD2_Pin, GPIO_PIN_SET);
                    } else if (evt == EVENT_FLT_DETECTED) {
                        currentState = STATE_ERROR;
                        printf("FSM: -> STATE_ERROR\r\n");
                    }
                    break;

                case STATE_ERROR:
                    if (evt == EVENT_PERM_ERROR) {
                        currentState = STATE_PERM_ERROR;
                        printf("FSM: -> STATE_PERM_ERROR\r\n");
                    } else if (evt == EVENT_RESET) {
                        currentState = STATE_IDLE;
                        printf("FSM: -> STATE_IDLE (recovered)\r\n");
                        HAL_GPIO_WritePin(LD2_GPIO_Port, LD2_Pin, GPIO_PIN_SET);
                    }
                    break;

                case STATE_PERM_ERROR:
                    // Stay here until board reset
                    printf("FSM: PERM ERROR - manual reset needed\r\n");
                    break;
            }
        }
    }
}

QueueHandle_t xEventQueue = NULL;   // single definition here

/* initializer to be called from main before vTaskStartScheduler */
void FSM_Init(void) {
    xEventQueue = xQueueCreate(10, sizeof(SystemEvent_t));
    xTaskCreate(vFSMTask, "FSM", 256, NULL, 2, NULL);
}
