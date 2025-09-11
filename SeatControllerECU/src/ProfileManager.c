#include "Rte.h"
#include <stdio.h>

static int savedProfile = 50;

void Runnable_ProfileManager(void) {
    if (Rte_Read_BtnSave()) {
        savedProfile = Rte_Read_SeatPosition();
        printf("[ProfileManager] Saved profile: %d\n", savedProfile);
    }
    if (Rte_Read_BtnLoad()) {
        Rte_Write_TargetPosition(savedProfile);
        printf("[ProfileManager] Loaded profile: %d\n", savedProfile);
    }
}
