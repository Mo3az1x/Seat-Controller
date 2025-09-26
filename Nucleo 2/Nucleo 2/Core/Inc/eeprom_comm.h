/**
  * EEPROM Communication Protocol Header
  * Handles communication between Java GUI, STM32, and Arduino EEPROM emulator
  */

#ifndef EEPROM_COMM_H
#define EEPROM_COMM_H

#include "main.h"
#include "cmsis_os.h"
#include <stdint.h>

/* Forward-declare SeatProfile to avoid circular includes.
   The actual SeatProfile struct is defined in freertos.c */
struct SeatProfile;

/* Frame Protocol Constants */
#define FRAME_HEADER             0x7E
#define FRAME_TAIL              0x7F
#define MAX_FRAME_SIZE          256

/* UART Commands (from Java GUI) */
#define CMD_READ_BYTE           0x01
#define CMD_WRITE_BYTE          0x02
#define CMD_READ_ALL            0x03
#define CMD_WRITE_ALL           0x04
#define CMD_CLEAR               0x05
#define CMD_VERIFY              0x06
#define CMD_SAVE_PROFILE        0x07
#define CMD_LOAD_PROFILE        0x08

/* UART Response Codes (to Java GUI) */
#define RES_ACK                 0x10
#define RES_NACK                0x11
#define RES_READ_BYTE           0x12
#define RES_READ_ALL            0x13
#define RES_VERIFY_OK           0x14
#define RES_VERIFY_FAIL         0x15
#define RES_ERROR               0x1F

/* SPI Commands (to Arduino) */
#define SPI_CMD_READ            0x03
#define SPI_CMD_WRITE           0x02

/* Error Codes */
#define ERR_INVALID_COMMAND     0x01
#define ERR_TIMEOUT             0x02
#define ERR_CHECKSUM            0x03
#define ERR_SPI_FAILURE         0x04

/* Profile Management */
#define PROFILE_SIZE            64
#define MAX_PROFILES            4
#define PROFILE_BASE_ADDR       0x100  // Base address for profiles in EEPROM

/* Function Prototypes */
void EEPROM_Init(void);
void UART_ReceiveTask(void *argument);
void SPI_TransmitTask(void *argument);
void ProcessCommand(uint8_t *frame, uint16_t length);

/* Frame Handling */
uint16_t CreateResponseFrame(uint8_t *buffer, uint8_t cmd, uint8_t *data, uint16_t dataLen);
uint8_t CalculateChecksum(uint8_t *data, uint16_t length);
void SendResponse(uint8_t *buffer, uint16_t length);

/* SPI Operations */
HAL_StatusTypeDef SPI_ReadByte(uint16_t address, uint8_t *data);
HAL_StatusTypeDef SPI_WriteByte(uint16_t address, uint8_t data);
HAL_StatusTypeDef SPI_ReadBlock(uint16_t address, uint8_t *buffer, uint16_t length);
HAL_StatusTypeDef SPI_WriteBlock(uint16_t address, uint8_t *buffer, uint16_t length);

/* Profile Management */
HAL_StatusTypeDef SaveProfile(uint8_t profile_id, struct SeatProfile *p);
HAL_StatusTypeDef LoadProfile(uint8_t profile_id, struct SeatProfile *p);

#endif /* EEPROM_COMM_H */