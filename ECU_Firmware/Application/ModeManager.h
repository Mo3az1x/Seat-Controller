#ifndef MODE_MANAGER_H
#define MODE_MANAGER_H

typedef enum {
    MODE_MANUAL,
    MODE_AUTOMATIC
} Mode_t;

void ModeManager_Init(void);
void ModeManager_Switch(Mode_t mode);
Mode_t ModeManager_GetCurrent(void);

#endif
