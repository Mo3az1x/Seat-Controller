#ifndef RTE_SEATCONTROLLER_H
#define RTE_SEATCONTROLLER_H

#include <stdint.h>
#include <stdbool.h>

/* ===========================================================
 * Return Type
 * =========================================================== */
typedef int Std_ReturnType;
#define E_OK     0
#define E_NOT_OK 1

/* ===========================================================
 * ECU State Type
 * =========================================================== */
typedef enum {
    ECU_OFF,
    ECU_IDLE,
    ECU_LOCKED,
    ECU_ERROR,
    ECU_SHUTDOWN
} ECU_StateType;

/* ===========================================================
 * Sender/Receiver Ports
 * =========================================================== */

/* Buttons */
extern Std_ReturnType Rte_Write_Btn_Up(bool val);
extern Std_ReturnType Rte_Read_Btn_Up(bool* val);

extern Std_ReturnType Rte_Write_Btn_Down(bool val);
extern Std_ReturnType Rte_Read_Btn_Down(bool* val);

extern Std_ReturnType Rte_Write_Btn_Save(bool val);
extern Std_ReturnType Rte_Read_Btn_Save(bool* val);

extern Std_ReturnType Rte_Write_Btn_Load(bool val);
extern Std_ReturnType Rte_Read_Btn_Load(bool* val);

/* Seat Position Channels */
extern Std_ReturnType Rte_Write_Seat_Height(uint8_t h);
extern Std_ReturnType Rte_Read_Seat_Height(uint8_t* h);

extern Std_ReturnType Rte_Write_Seat_Slide(uint8_t s);
extern Std_ReturnType Rte_Read_Seat_Slide(uint8_t* s);

extern Std_ReturnType Rte_Write_Seat_Incline(uint8_t i);
extern Std_ReturnType Rte_Read_Seat_Incline(uint8_t* i);

/* ECU State */
extern Std_ReturnType Rte_Write_ECU_State(ECU_StateType state);
extern Std_ReturnType Rte_Read_ECU_State(ECU_StateType* state);

/* Fault Flag */
extern Std_ReturnType Rte_Write_FaultFlag(bool val);
extern Std_ReturnType Rte_Read_FaultFlag(bool* val);

/* ===========================================================
 * Client/Server Ports
 * =========================================================== */
extern uint8_t Rte_Call_Eeprom_Read(uint16_t address);
extern void    Rte_Call_Eeprom_Write(uint16_t address, uint8_t data);

extern bool Rte_Call_CommBus_Read(uint8_t msgId, uint8_t* data, uint8_t len);
extern void Rte_Call_CommBus_Send(uint8_t msgId, const uint8_t* data, uint8_t len);

#endif /* RTE_SEATCONTROLLER_H */
