#include "rte_seatcontroller.h"
#include <stdio.h>

void Runnable_DiagManager(void)
{
    uint8_t height;
    Rte_Read_Seat_Height(&height);

    if (height > 100)
    {
        Rte_Write_FaultFlag(true);
        printf("[DiagManager] Fault detected: Height too high!\n");
    }
}
