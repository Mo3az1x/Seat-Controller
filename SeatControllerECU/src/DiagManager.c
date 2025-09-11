#include "Rte.h"
#include <stdio.h>

void Runnable_DiagManager(void) {
    if (Rte_Read_SeatPosition() > 100 || Rte_Read_SeatPosition() < 0) {
        Rte_Write_FaultFlag(true);
        printf("[DiagManager] Fault detected!\n");
    }
}
