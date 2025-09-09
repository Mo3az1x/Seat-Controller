#include "ModeManager.h"
#include <stdio.h>

static Mode_t currentMode = MODE_MANUAL;

void ModeManager_Init(void) {
    currentMode = MODE_MANUAL;
    printf("ModeManager initialized (default: MANUAL).\n");
}

void ModeManager_Switch(Mode_t mode) {
    currentMode = mode;
    printf("Mode switched to %s.\n", mode == MODE_MANUAL ? "MANUAL" : "AUTOMATIC");
}

Mode_t ModeManager_GetCurrent(void) {
    return currentMode;
}
