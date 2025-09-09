#include "Application/SeatControlManager.h"
#include "Application/ModeManager.h"
#include "Application/ProfileManager.h"
#include "Application/FaultManager.h"

int main(void) {
    // Initialize managers
    SeatControlManager_Init();
    ModeManager_Init();
    ProfileManager_Init();
    FaultManager_Init();

    // Example usage
    SeatControlManager_Task();
    ModeManager_Switch(MODE_AUTOMATIC);
    ProfileManager_Save(1);
    ProfileManager_Load(1);
    FaultManager_Monitor();

    while (1) {
        // TODO: Periodic loop
    }

    return 0;
}
