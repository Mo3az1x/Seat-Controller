#include <stdio.h>
#include "usart.h"  // provides huart2
#include "stm32f4xx_hal.h"

/* GCC/ARM newlib low-level write */
int _write(int file, char *ptr, int len)
{
    /* transmit via HAL UART blocking */
    HAL_StatusTypeDef status = HAL_UART_Transmit(&huart2, (uint8_t*)ptr, len, HAL_MAX_DELAY);
    return (status == HAL_OK) ? len : 0;
}
