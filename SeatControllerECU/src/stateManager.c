#include "Rte.h"
#include <stdio.h>

void Runnable_StateManager(void) {
    ECU_StateType st = Rte_Read_ECUState();

    switch(st) {
        case ECU_OFF:
            Rte_Write_ECUState(ECU_IDLE);
            break;
        case ECU_IDLE:
            if (Rte_Read_SeatPosition() > 0) 
                Rte_Write_ECUState(ECU_LOCKED);
            break;
        case ECU_LOCKED:
            if (Rte_Read_FaultFlag())
                Rte_Write_ECUState(ECU_ERROR);
            break;
        default:
            break;
    }

    printf("[StateManager] ECU State=%d\n", Rte_Read_ECUState());
}
