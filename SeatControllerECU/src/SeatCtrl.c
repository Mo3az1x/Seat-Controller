#include "Rte.h"
#include <stdio.h>

void Runnable_SeatCtrl(void) {
    int pos = Rte_Read_SeatPosition();
    int target = Rte_Read_TargetPosition();

    if (Rte_Read_BtnUp()) target = pos + 1;
    if (Rte_Read_BtnDown()) target = pos - 1;

    if (target != pos) {
        if (target > pos) pos++;
        else pos--;
    }

    Rte_Write_SeatPosition(pos);
    Rte_Write_TargetPosition(target);

    printf("[SeatCtrl] Position=%d Target=%d\n", pos, target);
}
