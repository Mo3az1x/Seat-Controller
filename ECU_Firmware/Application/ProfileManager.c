#include "ProfileManager.h"
#include <stdio.h>

void ProfileManager_Init(void) {
    printf("ProfileManager initialized.\n");
}

void ProfileManager_Save(int profileId) {
    // TODO: Save to EEPROM
    printf("Profile %d saved.\n", profileId);
}

void ProfileManager_Load(int profileId) {
    // TODO: Load from EEPROM
    printf("Profile %d loaded.\n", profileId);
}
