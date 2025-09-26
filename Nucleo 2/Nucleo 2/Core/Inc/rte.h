/*
 * rte.h - Lightweight AUTOSAR-style RTE
 */

#ifndef RTE_H
#define RTE_H

#include <stdint.h>
#include <stdarg.h>
#include "rte_types.h"
#include "rte_cfg.h"

#include "i2c.h"
#include "usart.h"

#ifdef __cplusplus
extern "C" {
#endif

// Initialization
void Rte_Init(I2C_HandleTypeDef *i2c, UART_HandleTypeDef *uart);

// Write request API
HAL_StatusTypeDef Rte_Write_SeatControlReq(uint16_t height, uint16_t slide, uint16_t incline);

// Read cached sensor values
HAL_StatusTypeDef Rte_Read_SeatHeight_Current(uint16_t *value);
HAL_StatusTypeDef Rte_Read_SeatSlide_Current(uint16_t *value);
HAL_StatusTypeDef Rte_Read_SeatIncline_Current(uint16_t *value);

// Update cached sensor values (to be called by drivers/tasks)
void Rte_Update_SeatHeight(uint16_t newVal);
void Rte_Update_SeatSlide(uint16_t newVal);
void Rte_Update_SeatIncline(uint16_t newVal);

// Trace over UART2
void Rte_Trace(const char *fmt, ...);

#ifdef __cplusplus
}
#endif

#endif /* RTE_H */
