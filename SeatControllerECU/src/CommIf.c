#include "Rte.h"
#include <stdio.h>

void Runnable_CommIf(void) {
    // In real ECU: UART/I2C TX/RX
    printf("[CommIf] Seat=%d State=%d\n", 
        Rte_Read_SeatPosition(), Rte_Read_ECUState());
}
