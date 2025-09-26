/*
 * rte.c - Lightweight AUTOSAR-style RTE implementation
 */

#include "rte.h"
#include <stdio.h>
#include <string.h>
#if RTE_USE_FREERTOS
#include "cmsis_os.h"
#endif

static I2C_HandleTypeDef *rte_i2c = NULL;
static UART_HandleTypeDef *rte_uart = NULL;

static Rte_SensorCache_t rte_cache = {0};

#if RTE_USE_FREERTOS
static osMutexId_t rteMutexHandle;
#endif

void Rte_Init(I2C_HandleTypeDef *i2c, UART_HandleTypeDef *uart)
{
	rte_i2c = i2c;
	rte_uart = uart;

#if RTE_USE_FREERTOS
	const osMutexAttr_t rteMutexAttr = { .name = "rteMutex" };
	rteMutexHandle = osMutexNew(&rteMutexAttr);
#endif
}

HAL_StatusTypeDef Rte_Write_SeatControlReq(uint16_t height, uint16_t slide, uint16_t incline)
{
	if (rte_i2c == NULL) return HAL_ERROR;

	uint8_t frame[7];
	frame[0] = 0x10; // Message ID for SeatControlReq
	frame[1] = (uint8_t)(height >> 8);
	frame[2] = (uint8_t)(height & 0xFF);
	frame[3] = (uint8_t)(slide >> 8);
	frame[4] = (uint8_t)(slide & 0xFF);
	frame[5] = (uint8_t)(incline >> 8);
	frame[6] = (uint8_t)(incline & 0xFF);

	return HAL_I2C_Master_Transmit(rte_i2c,
								  (RTE_I2C_SLAVE_ADDR << 1),
								  frame,
								  (uint16_t)sizeof(frame),
								  (uint32_t)RTE_I2C_TIMEOUT_MS);
}

HAL_StatusTypeDef Rte_Read_SeatHeight_Current(uint16_t *value)
{
	if (value == NULL) return HAL_ERROR;
#if RTE_USE_FREERTOS
	if (rteMutexHandle) osMutexAcquire(rteMutexHandle, osWaitForever);
#endif
	*value = rte_cache.seatHeight;
#if RTE_USE_FREERTOS
	if (rteMutexHandle) osMutexRelease(rteMutexHandle);
#endif
	return HAL_OK;
}

HAL_StatusTypeDef Rte_Read_SeatSlide_Current(uint16_t *value)
{
	if (value == NULL) return HAL_ERROR;
#if RTE_USE_FREERTOS
	if (rteMutexHandle) osMutexAcquire(rteMutexHandle, osWaitForever);
#endif
	*value = rte_cache.seatSlide;
#if RTE_USE_FREERTOS
	if (rteMutexHandle) osMutexRelease(rteMutexHandle);
#endif
	return HAL_OK;
}

HAL_StatusTypeDef Rte_Read_SeatIncline_Current(uint16_t *value)
{
	if (value == NULL) return HAL_ERROR;
#if RTE_USE_FREERTOS
	if (rteMutexHandle) osMutexAcquire(rteMutexHandle, osWaitForever);
#endif
	*value = rte_cache.seatIncline;
#if RTE_USE_FREERTOS
	if (rteMutexHandle) osMutexRelease(rteMutexHandle);
#endif
	return HAL_OK;
}

void Rte_Update_SeatHeight(uint16_t newVal)
{
#if RTE_USE_FREERTOS
	if (rteMutexHandle) osMutexAcquire(rteMutexHandle, osWaitForever);
#endif
	rte_cache.seatHeight = newVal;
#if RTE_USE_FREERTOS
	if (rteMutexHandle) osMutexRelease(rteMutexHandle);
#endif
}

void Rte_Update_SeatSlide(uint16_t newVal)
{
#if RTE_USE_FREERTOS
	if (rteMutexHandle) osMutexAcquire(rteMutexHandle, osWaitForever);
#endif
	rte_cache.seatSlide = newVal;
#if RTE_USE_FREERTOS
	if (rteMutexHandle) osMutexRelease(rteMutexHandle);
#endif
}

void Rte_Update_SeatIncline(uint16_t newVal)
{
#if RTE_USE_FREERTOS
	if (rteMutexHandle) osMutexAcquire(rteMutexHandle, osWaitForever);
#endif
	rte_cache.seatIncline = newVal;
#if RTE_USE_FREERTOS
	if (rteMutexHandle) osMutexRelease(rteMutexHandle);
#endif
}

void Rte_Trace(const char *fmt, ...)
{
	if (rte_uart == NULL || fmt == NULL) return;
	char buf[RTE_TRACE_BUF_SIZE];
	va_list args;
	va_start(args, fmt);
	int n = vsnprintf(buf, sizeof(buf), fmt, args);
	va_end(args);
	if (n < 0) return;
	if (n > (int)(sizeof(buf) - 2)) n = (int)(sizeof(buf) - 2);
	buf[n++] = '\r';
	buf[n++] = '\n';
	HAL_UART_Transmit(rte_uart, (uint8_t*)buf, (uint16_t)n, 100);
}
