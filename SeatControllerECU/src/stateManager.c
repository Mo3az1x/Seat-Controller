#include "rte_seatcontroller.h"
#include <stdio.h>

void Runnable_StateManager(void)
{
    ECU_StateType state;
    ECU_EventType event;

    Rte_Read_ECU_State(&state);
    Rte_Read_ECU_Event(&event);

    switch (state)
    {
    case ECU_OFF:
        if (event == EVENT_RESET)
        {
            Rte_Write_ECU_State(ECU_IDLE);
        }
        break;

    case ECU_IDLE:
        if (event == EVENT_BTN_PRESSED)
        {
            Rte_Write_ECU_State(ECU_BUSY);
        }
        else if (event == EV_FLT_DETECTED)
        {
            Rte_Write_ECU_State(ECU_ERROR);
        }
        break;

    case ECU_BUSY:
        if (event == EVENT_MOTION_COMPLETED)
        {
            Rte_Write_ECU_State(ECU_IDLE);
        }
        else if (event == EVENT_DRIVING_STARTED)
        {
            Rte_Write_ECU_State(ECU_LOCKED);
        }
        break;

    case ECU_LOCKED:
        if (event == EVENT_STOP)
        {
            Rte_Write_ECU_State(ECU_IDLE);
        }
        else if (event == EV_FLT_DETECTED)
        {
            Rte_Write_ECU_State(ECU_ERROR);
        }
        break;

    case ECU_ERROR:
        if (event == EVENT_PERM_ERROR)
        {
            Rte_Write_ECU_State(ECU_PERM_ERROR);
        }
        else if (event == EVENT_RESET)
        {
            Rte_Write_ECU_State(ECU_IDLE);
        }
        break;

    case ECU_PERM_ERROR:
        // stays until hard reset
        break;

    default:
        break;
    }

    Rte_Read_ECU_State(&state);
    printf("[StateManager] ECU State=%d Event=%d\n", state, event);

    // clear event after processing
    Rte_Write_ECU_Event(EVENT_NONE);
}
