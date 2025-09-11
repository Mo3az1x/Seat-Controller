#include <stdio.h>
#include <windows.h>
#include "../inc/Rte.h"


// Global RTE Variables
bool Rte_Btn_Up = false;
bool Rte_Btn_Down = false;
bool Rte_Btn_Save = false;
bool Rte_Btn_Load = false;

int Rte_SeatPosition = 0;
int Rte_TargetPosition = 0;
ECU_StateType Rte_ECUState = ECU_OFF;
bool Rte_FaultFlag = false;

int main(void) {
    for (int i = 0; i < 50; i++) {
        Runnable_BtnCtrl();
        Runnable_ProfileManager();
        Runnable_SeatCtrl();
        Runnable_DiagManager();
        Runnable_StateManager();
        Runnable_NvmService();
        Runnable_CommIf();

        Sleep(100); // 100ms cycle
    }
    return 0;
}
