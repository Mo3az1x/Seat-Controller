#include "rte_seatcontroller.h"
#include <stdio.h>

void Runnable_CommIf(void) {
    uint8_t h, s, i;
    Rte_Read_Seat_Height(&h);
    Rte_Read_Seat_Slide(&s);
    Rte_Read_Seat_Incline(&i);

    printf("[CommIf] Sending Seat Status -> H=%d S=%d I=%d\n", h, s, i);
}
