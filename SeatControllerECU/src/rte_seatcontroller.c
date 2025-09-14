#include "rte_seatcontroller.h"
#include <stdio.h>

/* ===========================================================
 * Internal Global State (RTE Buffers)
 * =========================================================== */
static bool g_btn_up, g_btn_down, g_btn_save, g_btn_load;
static uint8_t g_seat_height, g_seat_slide, g_seat_incline;
static ECU_StateType g_ecu_state = ECU_OFF;
static bool g_fault_flag = false;

/* ===========================================================
 * Sender/Receiver Implementations
 * =========================================================== */
Std_ReturnType Rte_Write_Btn_Up(bool val)
{
    g_btn_up = val;
    return E_OK;
}
Std_ReturnType Rte_Read_Btn_Up(bool *val)
{
    *val = g_btn_up;
    return E_OK;
}

Std_ReturnType Rte_Write_Btn_Down(bool val)
{
    g_btn_down = val;
    return E_OK;
}
Std_ReturnType Rte_Read_Btn_Down(bool *val)
{
    *val = g_btn_down;
    return E_OK;
}

Std_ReturnType Rte_Write_Btn_Save(bool val)
{
    g_btn_save = val;
    return E_OK;
}
Std_ReturnType Rte_Read_Btn_Save(bool *val)
{
    *val = g_btn_save;
    return E_OK;
}

Std_ReturnType Rte_Write_Btn_Load(bool val)
{
    g_btn_load = val;
    return E_OK;
}
Std_ReturnType Rte_Read_Btn_Load(bool *val)
{
    *val = g_btn_load;
    return E_OK;
}

Std_ReturnType Rte_Write_Seat_Height(uint8_t h)
{
    g_seat_height = h;
    return E_OK;
}
Std_ReturnType Rte_Read_Seat_Height(uint8_t *h)
{
    *h = g_seat_height;
    return E_OK;
}

Std_ReturnType Rte_Write_Seat_Slide(uint8_t s)
{
    g_seat_slide = s;
    return E_OK;
}
Std_ReturnType Rte_Read_Seat_Slide(uint8_t *s)
{
    *s = g_seat_slide;
    return E_OK;
}

Std_ReturnType Rte_Write_Seat_Incline(uint8_t i)
{
    g_seat_incline = i;
    return E_OK;
}
Std_ReturnType Rte_Read_Seat_Incline(uint8_t *i)
{
    *i = g_seat_incline;
    return E_OK;
}

Std_ReturnType Rte_Write_ECU_State(ECU_StateType state)
{
    g_ecu_state = state;
    return E_OK;
}
Std_ReturnType Rte_Read_ECU_State(ECU_StateType *state)
{
    *state = g_ecu_state;
    return E_OK;
}

Std_ReturnType Rte_Write_FaultFlag(bool val)
{
    g_fault_flag = val;
    return E_OK;
}
Std_ReturnType Rte_Read_FaultFlag(bool *val)
{
    *val = g_fault_flag;
    return E_OK;
}

/* ===========================================================
 * Client/Server Ports
 * =========================================================== */
uint8_t Rte_Call_Eeprom_Read(uint16_t address)
{
    printf("[RTE] EEPROM Read at 0x%04X\n", address);
    return 42; // dummy data
}

void Rte_Call_Eeprom_Write(uint16_t address, uint8_t data)
{
    printf("[RTE] EEPROM Write at 0x%04X = %u\n", address, data);
}

bool Rte_Call_CommBus_Read(uint8_t msgId, uint8_t *data, uint8_t len)
{
    printf("[RTE] CommBus Read msgId=%d len=%d\n", msgId, len);
    return false;
}

void Rte_Call_CommBus_Send(uint8_t msgId, const uint8_t *data, uint8_t len)
{
    printf("[RTE] CommBus Send msgId=%d len=%d\n", msgId, len);
}
