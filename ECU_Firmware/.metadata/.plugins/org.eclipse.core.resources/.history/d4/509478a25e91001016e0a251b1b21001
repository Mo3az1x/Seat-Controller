#include "rte_seatcontroller.h"
#include <stdio.h>

void Runnable_BtnCtrl(void)
{
    static int tick = 0;
    tick++;

    if (tick == 5)
        Rte_Write_Btn_Up(true);
    if (tick == 6)
        Rte_Write_Btn_Up(false);

    if (tick == 10)
        Rte_Write_Btn_Save(true);
    if (tick == 11)
        Rte_Write_Btn_Save(false);

    if (tick == 15)
        Rte_Write_Btn_Load(true);
    if (tick == 16)
        Rte_Write_Btn_Load(false);
}
