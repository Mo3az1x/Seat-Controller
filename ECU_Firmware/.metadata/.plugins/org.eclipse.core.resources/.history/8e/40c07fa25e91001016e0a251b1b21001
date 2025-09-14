#include "rte_seatcontroller.h"
#include <stdio.h>

void Runnable_SeatCtrl(void)
{
    uint8_t height, slide, incline;
    Rte_Read_Seat_Height(&height);
    Rte_Read_Seat_Slide(&slide);
    Rte_Read_Seat_Incline(&incline);

    bool btnUp, btnDown;
    Rte_Read_Btn_Up(&btnUp);
    Rte_Read_Btn_Down(&btnDown);

    if (btnUp && height < 100)
        height++;
    if (btnDown && height > 0)
        height--;

    Rte_Write_Seat_Height(height);
    Rte_Write_Seat_Slide(slide + 1); // dummy auto-slide
    Rte_Write_Seat_Incline(incline); // unchanged for now

    printf("[SeatCtrl] Height=%d Slide=%d Incline=%d\n", height, slide, incline);
}
