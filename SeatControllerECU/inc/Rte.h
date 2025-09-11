#ifndef RTE_H
#define RTE_H

#include <stdbool.h>

// ECU States
typedef enum {
    ECU_OFF,
    ECU_IDLE,
    ECU_LOCKED,
    ECU_ERROR,
    ECU_SHUTDOWN
} ECU_StateType;

// ------------------- Inputs -------------------
extern bool Rte_Btn_Up;
extern bool Rte_Btn_Down;
extern bool Rte_Btn_Save;
extern bool Rte_Btn_Load;

extern int Rte_SeatPosition;   // current seat pos (0–100)
extern int Rte_TargetPosition; // desired position
extern ECU_StateType Rte_ECUState;

extern bool Rte_FaultFlag;

// ------------------- Write APIs -------------------
#define Rte_Write_BtnUp(val)      (Rte_Btn_Up = (val))
#define Rte_Write_BtnDown(val)    (Rte_Btn_Down = (val))
#define Rte_Write_BtnSave(val)    (Rte_Btn_Save = (val))
#define Rte_Write_BtnLoad(val)    (Rte_Btn_Load = (val))

#define Rte_Write_SeatPosition(val)  (Rte_SeatPosition = (val))
#define Rte_Write_TargetPosition(val) (Rte_TargetPosition = (val))
#define Rte_Write_ECUState(val) (Rte_ECUState = (val))
#define Rte_Write_FaultFlag(val) (Rte_FaultFlag = (val))

// ------------------- Read APIs -------------------
#define Rte_Read_BtnUp()      (Rte_Btn_Up)
#define Rte_Read_BtnDown()    (Rte_Btn_Down)
#define Rte_Read_BtnSave()    (Rte_Btn_Save)
#define Rte_Read_BtnLoad()    (Rte_Btn_Load)

#define Rte_Read_SeatPosition()   (Rte_SeatPosition)
#define Rte_Read_TargetPosition() (Rte_TargetPosition)
#define Rte_Read_ECUState()       (Rte_ECUState)
#define Rte_Read_FaultFlag()      (Rte_FaultFlag)

// ------------------- Runnables -------------------
void Runnable_BtnCtrl(void);
void Runnable_SeatCtrl(void);
void Runnable_ProfileManager(void);
void Runnable_StateManager(void);
void Runnable_DiagManager(void);
void Runnable_NvmService(void);
void Runnable_CommIf(void);

#endif
