#include "Rte.h"
#include <stdio.h>

void Runnable_NvmService(void) {
    // In real ECU: EEPROM Read/Write
    // Here: just print when saving
    if (Rte_Read_BtnSave())
        printf("[NvmService] Writing to EEPROM...\n");
}
