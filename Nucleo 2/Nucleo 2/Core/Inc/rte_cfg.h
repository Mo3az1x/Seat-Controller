/*
 * rte_cfg.h - RTE configuration
 */

#ifndef RTE_CFG_H
#define RTE_CFG_H

#include <stdint.h>

// I2C slave address for Seat ECU peer (7-bit)
#ifndef RTE_I2C_SLAVE_ADDR
#define RTE_I2C_SLAVE_ADDR        (0x3A)
#endif

// I2C timeout (ms)
#ifndef RTE_I2C_TIMEOUT_MS
#define RTE_I2C_TIMEOUT_MS        (200)
#endif

// UART trace buffer size
#ifndef RTE_TRACE_BUF_SIZE
#define RTE_TRACE_BUF_SIZE        (128)
#endif

// Optional: enable FreeRTOS safe locking if mutex is available
#ifndef RTE_USE_FREERTOS
#define RTE_USE_FREERTOS          (1)
#endif

#endif /* RTE_CFG_H */
