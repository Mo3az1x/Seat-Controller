#include "Rte.h"
#include <stdio.h>

// Simulate button press (in real case from GPIO)
void Runnable_BtnCtrl(void) {
    static int tick = 0;
    tick++;

    if (tick == 10) Rte_Write_BtnUp(true);
    if (tick == 20) Rte_Write_BtnUp(false);

    if (tick == 30) Rte_Write_BtnSave(true);
    if (tick == 31) Rte_Write_BtnSave(false);
}
