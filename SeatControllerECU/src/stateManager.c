#include "rte_seatcontroller.h"
#include <stdio.h>

void Runnable_StateManager(void)
{
    ECU_StateType state;
    Rte_Read_ECU_State(&state);

    switch (state)
    {
    case ECU_OFF:
        Rte_Write_ECU_State(ECU_IDLE);
        break;
    case ECU_IDLE:
        Rte_Write_ECU_State(ECU_LOCKED);
        break;
    default:
        break;
    }

    Rte_Read_ECU_State(&state);
    printf("[StateManager] ECU State=%d\n", state);
}
