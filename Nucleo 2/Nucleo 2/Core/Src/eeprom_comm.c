/**
  * EEPROM Communication Protocol Implementation
  * Handles communication between Java GUI, STM32, and Arduino EEPROM emulator
  */

#include <eeprom_comm.h>
#include "usart.h"
#include "spi.h"
#include "gpio.h"
#include <string.h>
#include <stdio.h>

/* Private variables */
static uint8_t rxBuffer[MAX_FRAME_SIZE];
static uint8_t txBuffer[MAX_FRAME_SIZE];
static uint8_t dataBuffer[MAX_FRAME_SIZE];
static volatile uint16_t rxIndex = 0;
static volatile uint8_t frameReceived = 0;

/* Mutex handles */
osMutexId_t spiMutexHandle;
osMutexId_t uartMutexHandle;

/* Queue handles */
osMessageQueueId_t spiQueueHandle;

/* Task handles */
osThreadId_t uartReceiveTaskHandle;
osThreadId_t spiTransmitTaskHandle;

/* Private function prototypes */
static void ProcessFrame(void);
static uint8_t ValidateFrame(uint8_t *frame, uint16_t length);

/**
  * @brief  Initialize EEPROM communication components
  * @param  None
  * @retval None
  */
void EEPROM_Init(void)
{
  /* Create mutexes */
  const osMutexAttr_t spiMutexAttr = {
    .name = "spiMutex"
  };
  spiMutexHandle = osMutexNew(&spiMutexAttr);
  
  const osMutexAttr_t uartMutexAttr = {
    .name = "uartMutex"
  };
  uartMutexHandle = osMutexNew(&uartMutexAttr);
  
  /* Create message queue for SPI commands */
  const osMessageQueueAttr_t spiQueueAttr = {
    .name = "spiQueue"
  };
  spiQueueHandle = osMessageQueueNew(10, sizeof(uint8_t) * MAX_FRAME_SIZE, &spiQueueAttr);
  
  /* Start UART reception in interrupt mode */
  HAL_UART_Receive_IT(&huart2, &rxBuffer[rxIndex], 1);
}

/**
  * @brief  UART receive task
  * @param  argument: Not used
  * @retval None
  */
void UART_ReceiveTask(void *argument)
{
  /* Infinite loop */
  for(;;)
  {
    /* Check if a complete frame has been received */
    if(frameReceived)
    {
      /* Process the received frame */
      ProcessFrame();
      
      /* Reset reception variables */
      rxIndex = 0;
      frameReceived = 0;
      
      /* Restart UART reception */
      HAL_UART_Receive_IT(&huart2, &rxBuffer[rxIndex], 1);
    }
    
    /* Task delay */
    osDelay(10);
  }
}

/**
  * @brief  SPI transmit task
  * @param  argument: Not used
  * @retval None
  */
void SPI_TransmitTask(void *argument)
{
  uint8_t spiFrame[MAX_FRAME_SIZE];
  
  /* Infinite loop */
  for(;;)
  {
    /* Wait for SPI command in queue */
    if(osMessageQueueGet(spiQueueHandle, spiFrame, NULL, osWaitForever) == osOK)
    {
      /* Get mutex for SPI access */
      if(osMutexAcquire(spiMutexHandle, osWaitForever) == osOK)
      {
        /* Process SPI command based on first byte */
        switch(spiFrame[0])
        {
          case SPI_CMD_READ:
            {
              uint16_t address = (spiFrame[1] << 8) | spiFrame[2];
              uint16_t length = (spiFrame[3] << 8) | spiFrame[4];
              uint8_t *dataPtr = &spiFrame[5];
              
              /* Select SPI slave (Arduino) */
              HAL_GPIO_WritePin(SPI_CS_GPIO_Port, SPI_CS_Pin, GPIO_PIN_RESET);
              
              /* Send read command */
              uint8_t cmd[3] = {SPI_CMD_READ, (address >> 8) & 0xFF, address & 0xFF};
              HAL_SPI_Transmit(&hspi2, cmd, 3, HAL_MAX_DELAY);
              
              /* Receive data */
              HAL_SPI_Receive(&hspi2, dataPtr, length, HAL_MAX_DELAY);
              
              /* Deselect SPI slave */
              HAL_GPIO_WritePin(SPI_CS_GPIO_Port, SPI_CS_Pin, GPIO_PIN_SET);
            }
            break;
            
          case SPI_CMD_WRITE:
            {
              uint16_t address = (spiFrame[1] << 8) | spiFrame[2];
              uint16_t length = (spiFrame[3] << 8) | spiFrame[4];
              uint8_t *dataPtr = &spiFrame[5];
              
              /* Select SPI slave (Arduino) */
              HAL_GPIO_WritePin(SPI_CS_GPIO_Port, SPI_CS_Pin, GPIO_PIN_RESET);
              
              /* Send write command */
              uint8_t cmd[3] = {SPI_CMD_WRITE, (address >> 8) & 0xFF, address & 0xFF};
              HAL_SPI_Transmit(&hspi2, cmd, 3, HAL_MAX_DELAY);
              
              /* Send data */
              HAL_SPI_Transmit(&hspi2, dataPtr, length, HAL_MAX_DELAY);
              
              /* Deselect SPI slave */
              HAL_GPIO_WritePin(SPI_CS_GPIO_Port, SPI_CS_Pin, GPIO_PIN_SET);
              
              /* Small delay to allow EEPROM write to complete */
              osDelay(5);
            }
            break;
            
          default:
            /* Invalid SPI command */
            break;
        }
        
        /* Release SPI mutex */
        osMutexRelease(spiMutexHandle);
      }
    }
  }
}

/**
  * @brief  Process received UART frame
  * @param  None
  * @retval None
  */
static void ProcessFrame(void)
{
  /* Check if we have a valid frame */
  if(rxIndex < 4)
  {
    /* Not enough data for a valid frame */
    return;
  }
  
  /* Process the received frame */
  ProcessCommand(rxBuffer, rxIndex);
  
  /* Reset receive buffer */
  rxIndex = 0;
}

/**
  * @brief  Process command from UART frame
  * @param  frame: Pointer to frame buffer
  * @param  length: Length of frame
  * @retval None
  */
void ProcessCommand(uint8_t *frame, uint16_t length)
{
  uint8_t cmd = frame[1];
  uint8_t *data = &frame[2];
  uint16_t address;
  uint8_t value;
  uint16_t size;
  uint8_t profileNum;
  uint16_t responseLen;
  uint8_t errorCode;
  uint8_t *writeData;
  uint8_t *expectedData;
  uint8_t verifyResult;
  
  switch(cmd)
  {
    case CMD_READ_BYTE:
      /* Read single byte from EEPROM */
      address = (data[0] << 8) | data[1];
      if(SPI_ReadByte(address, &value) != HAL_OK) {
        /* Handle error if needed */
      }
      
      /* Create response frame */
      responseLen = CreateResponseFrame(txBuffer, RES_READ_BYTE, &value, 1);
      SendResponse(txBuffer, responseLen);
      break;
      
    case CMD_WRITE_BYTE:
      /* Write single byte to EEPROM */
      address = (data[0] << 8) | data[1];
      value = data[2];
      SPI_WriteByte(address, value);
      
      /* Send ACK response */
      responseLen = CreateResponseFrame(txBuffer, RES_ACK, NULL, 0);
      SendResponse(txBuffer, responseLen);
      break;
      
    case CMD_READ_ALL:
      /* Read multiple bytes from EEPROM */
      address = (data[0] << 8) | data[1];
      size = (data[2] << 8) | data[3];
      
      /* Limit size to prevent buffer overflow */
      if(size > MAX_FRAME_SIZE - 10)
        size = MAX_FRAME_SIZE - 10;
      
      /* Read data block */
      SPI_ReadBlock(address, dataBuffer, size);
      
      /* Create response frame */
      responseLen = CreateResponseFrame(txBuffer, RES_READ_ALL, dataBuffer, size);
      SendResponse(txBuffer, responseLen);
      break;
      
    case CMD_WRITE_ALL:
      /* Write multiple bytes to EEPROM */
      address = (data[0] << 8) | data[1];
      size = (data[2] << 8) | data[3];
      writeData = &data[4];
      
      /* Write data block */
      SPI_WriteBlock(address, writeData, size);
      
      /* Send ACK response */
      responseLen = CreateResponseFrame(txBuffer, RES_ACK, NULL, 0);
      SendResponse(txBuffer, responseLen);
      break;
      
    case CMD_CLEAR:
      /* Clear EEPROM (write 0xFF to all cells) */
      size = (data[0] << 8) | data[1]; /* Size to clear */
      
      /* Fill buffer with 0xFF */
      memset(dataBuffer, 0xFF, size);
      
      /* Write to EEPROM */
      SPI_WriteBlock(0, dataBuffer, size);
      
      /* Send ACK response */
      responseLen = CreateResponseFrame(txBuffer, RES_ACK, NULL, 0);
      SendResponse(txBuffer, responseLen);
      break;
      
    case CMD_VERIFY:
      /* Verify EEPROM contents match expected data */
      address = (data[0] << 8) | data[1];
      size = (data[2] << 8) | data[3];
      expectedData = &data[4];
      verifyResult = 1; /* 1 = OK, 0 = Fail */
      
      /* Read data for verification */
      SPI_ReadBlock(address, dataBuffer, size);
      
      /* Compare data */
      for(uint16_t i = 0; i < size; i++)
      {
        if(dataBuffer[i] != expectedData[i])
        {
          verifyResult = 0;
          break;
        }
      }
      
      /* Send verification result */
      if(verifyResult)
        responseLen = CreateResponseFrame(txBuffer, RES_VERIFY_OK, NULL, 0);
      else
        responseLen = CreateResponseFrame(txBuffer, RES_VERIFY_FAIL, NULL, 0);
      
      SendResponse(txBuffer, responseLen);
      break;
      
    case CMD_SAVE_PROFILE:
      /* Save profile to EEPROM */
      profileNum = data[0];
      if(profileNum >= MAX_PROFILES)
      {
        /* Invalid profile number */
        errorCode = ERR_INVALID_COMMAND;
        responseLen = CreateResponseFrame(txBuffer, RES_NACK, &errorCode, 1);
      }
      else
      {
        /* Save profile data */
        SaveProfile(profileNum, &data[1]);
        responseLen = CreateResponseFrame(txBuffer, RES_ACK, NULL, 0);
      }
      
      SendResponse(txBuffer, responseLen);
      break;
      
    case CMD_LOAD_PROFILE:
      /* Load profile from EEPROM */
      profileNum = data[0];
      if(profileNum >= MAX_PROFILES)
      {
        /* Invalid profile number */
        errorCode = ERR_INVALID_COMMAND;
        responseLen = CreateResponseFrame(txBuffer, RES_NACK, &errorCode, 1);
      }
      else
      {
        /* Load profile data */
        LoadProfile(profileNum, dataBuffer);
        responseLen = CreateResponseFrame(txBuffer, RES_READ_ALL, dataBuffer, PROFILE_SIZE);
      }
      
      SendResponse(txBuffer, responseLen);
      break;
      
    default:
      /* Invalid command */
      errorCode = ERR_INVALID_COMMAND;
      responseLen = CreateResponseFrame(txBuffer, RES_NACK, &errorCode, 1);
      SendResponse(txBuffer, responseLen);
      break;
  }
}

/**
  * @brief  Create response frame
  * @param  buffer: Pointer to output buffer
  * @param  cmd: Response command
  * @param  data: Pointer to data
  * @param  dataLen: Length of data
  * @retval Total frame length
  */
uint16_t CreateResponseFrame(uint8_t *buffer, uint8_t cmd, uint8_t *data, uint16_t dataLen)
{
  uint16_t index = 0;
  
  /* Add header */
  buffer[index++] = FRAME_HEADER;
  
  /* Add command */
  buffer[index++] = cmd;
  
  /* Add data if present */
  if(data != NULL && dataLen > 0)
  {
    memcpy(&buffer[index], data, dataLen);
    index += dataLen;
  }
  
  /* Add checksum (simple XOR of all bytes) */
  buffer[index] = CalculateChecksum(buffer, index);
  index++;
  
  /* Add tail */
  buffer[index++] = FRAME_TAIL;
  
  return index;
}

/**
  * @brief  Calculate checksum for frame
  * @param  data: Pointer to data
  * @param  length: Length of data
  * @retval Checksum value
  */
uint8_t CalculateChecksum(uint8_t *data, uint16_t length)
{
  uint8_t checksum = 0;
  
  for(uint16_t i = 0; i < length; i++)
  {
    checksum ^= data[i];
  }
  
  return checksum;
}

/**
  * @brief  Send response frame via UART
  * @param  buffer: Pointer to response buffer
  * @param  length: Length of response
  * @retval None
  */
void SendResponse(uint8_t *buffer, uint16_t length)
{
  /* Acquire UART mutex */
  if(osMutexAcquire(uartMutexHandle, osWaitForever) == osOK)
  {
    /* Send response via UART */
    HAL_UART_Transmit(&huart2, buffer, length, HAL_MAX_DELAY);
    
    /* Release UART mutex */
    osMutexRelease(uartMutexHandle);
  }
}

/**
  * @brief  Validate received frame
  * @param  frame: Pointer to frame buffer
  * @param  length: Length of frame
  * @retval 1 if valid, 0 if invalid
  */
static uint8_t ValidateFrame(uint8_t *frame, uint16_t length)
{
  /* Check minimum length */
  if(length < 4) /* Header + CMD + Checksum + Tail */
    return 0;
  
  /* Check header and tail */
  if(frame[0] != FRAME_HEADER || frame[length-1] != FRAME_TAIL)
    return 0;
  
  /* Check checksum */
  uint8_t calculatedChecksum = CalculateChecksum(frame, length - 2);
  if(calculatedChecksum != frame[length-2])
    return 0;
  
  return 1;
}

/**
  * @brief  Read a single byte from EEPROM
  * @param  address: EEPROM address
  * @param  data: Pointer to store the read byte
  * @retval HAL status
  */
HAL_StatusTypeDef SPI_ReadByte(uint16_t address, uint8_t *data)
{
  HAL_StatusTypeDef status = HAL_OK;
  
  /* Acquire SPI mutex */
  if(osMutexAcquire(spiMutexHandle, osWaitForever) == osOK)
  {
    /* Select SPI slave (Arduino) */
    HAL_GPIO_WritePin(SPI_CS_GPIO_Port, SPI_CS_Pin, GPIO_PIN_RESET);
    
    /* Send read command */
    uint8_t cmd[3] = {SPI_CMD_READ, (address >> 8) & 0xFF, address & 0xFF};
    status = HAL_SPI_Transmit(&hspi2, cmd, 3, HAL_MAX_DELAY);
    
    if(status == HAL_OK)
    {
      /* Receive data */
      status = HAL_SPI_Receive(&hspi2, data, 1, HAL_MAX_DELAY);
    }
    
    /* Deselect SPI slave */
    HAL_GPIO_WritePin(SPI_CS_GPIO_Port, SPI_CS_Pin, GPIO_PIN_SET);
    
    /* Release SPI mutex */
    osMutexRelease(spiMutexHandle);
  }
  else
  {
    status = HAL_ERROR;
  }
  
  return status;
}

/**
  * @brief  Write a single byte to EEPROM
  * @param  address: EEPROM address
  * @param  data: Byte to write
  * @retval HAL status
  */
HAL_StatusTypeDef SPI_WriteByte(uint16_t address, uint8_t data)
{
  HAL_StatusTypeDef status = HAL_OK;
  
  /* Acquire SPI mutex */
  if(osMutexAcquire(spiMutexHandle, osWaitForever) == osOK)
  {
    /* Select SPI slave (Arduino) */
    HAL_GPIO_WritePin(SPI_CS_GPIO_Port, SPI_CS_Pin, GPIO_PIN_RESET);
    
    /* Send write command */
    uint8_t cmd[4] = {SPI_CMD_WRITE, (address >> 8) & 0xFF, address & 0xFF, data};
    status = HAL_SPI_Transmit(&hspi2, cmd, 4, HAL_MAX_DELAY);
    
    /* Deselect SPI slave */
    HAL_GPIO_WritePin(SPI_CS_GPIO_Port, SPI_CS_Pin, GPIO_PIN_SET);
    
    /* Small delay to allow EEPROM write to complete */
    osDelay(5);
    
    /* Release SPI mutex */
    osMutexRelease(spiMutexHandle);
  }
  else
  {
    status = HAL_ERROR;
  }
  
  return status;
}

/**
  * @brief  Read a block of data from EEPROM
  * @param  address: EEPROM address
  * @param  buffer: Buffer to store read data
  * @param  length: Number of bytes to read
  * @retval HAL status
  */
HAL_StatusTypeDef SPI_ReadBlock(uint16_t address, uint8_t *buffer, uint16_t length)
{
  HAL_StatusTypeDef status = HAL_OK;
  
  /* Acquire SPI mutex */
  if(osMutexAcquire(spiMutexHandle, osWaitForever) == osOK)
  {
    /* Select SPI slave (Arduino) */
    HAL_GPIO_WritePin(SPI_CS_GPIO_Port, SPI_CS_Pin, GPIO_PIN_RESET);
    
    /* Send read command */
    uint8_t cmd[3] = {SPI_CMD_READ, (address >> 8) & 0xFF, address & 0xFF};
    status = HAL_SPI_Transmit(&hspi2, cmd, 3, HAL_MAX_DELAY);
    
    if(status == HAL_OK)
    {
      /* Receive data */
      status = HAL_SPI_Receive(&hspi2, buffer, length, HAL_MAX_DELAY);
    }
    
    /* Deselect SPI slave */
    HAL_GPIO_WritePin(SPI_CS_GPIO_Port, SPI_CS_Pin, GPIO_PIN_SET);
    
    /* Release SPI mutex */
    osMutexRelease(spiMutexHandle);
  }
  else
  {
    status = HAL_ERROR;
  }
  
  return status;
}

/**
  * @brief  Write multiple bytes to EEPROM
  * @param  address: Starting EEPROM address
  * @param  buffer: Pointer to data buffer
  * @param  length: Number of bytes to write
  * @retval HAL status
  */
HAL_StatusTypeDef SPI_WriteBlock(uint16_t address, uint8_t *buffer, uint16_t length)
{
  HAL_StatusTypeDef status = HAL_OK;
  
  /* Acquire SPI mutex */
  if(osMutexAcquire(spiMutexHandle, osWaitForever) == osOK)
  {
    /* Write data byte by byte */
    for(uint16_t i = 0; i < length; i++)
    {
      /* Select SPI slave (Arduino) */
      HAL_GPIO_WritePin(SPI_CS_GPIO_Port, SPI_CS_Pin, GPIO_PIN_RESET);
      
      /* Send write command */
      uint8_t cmd[4] = {SPI_CMD_WRITE, ((address+i) >> 8) & 0xFF, (address+i) & 0xFF, buffer[i]};
      status = HAL_SPI_Transmit(&hspi2, cmd, 4, HAL_MAX_DELAY);
      
      if(status != HAL_OK)
      {
        break;
      }
      
      /* Deselect SPI slave */
      HAL_GPIO_WritePin(SPI_CS_GPIO_Port, SPI_CS_Pin, GPIO_PIN_SET);
      
      /* Small delay to allow EEPROM write to complete */
      osDelay(5);
    }
    
    /* Release SPI mutex */
    osMutexRelease(spiMutexHandle);
  }
  else
  {
    status = HAL_ERROR;
  }
  
  return status;
}

/**
  * @brief  Save profile to EEPROM
  * @param  profileNum: Profile number (0-3)
  * @param  data: Pointer to profile data
  * @retval HAL status
  */
HAL_StatusTypeDef SaveProfile(uint8_t profile_id, struct SeatProfile *p)
{
  /* Calculate profile address */
  uint16_t profileAddress = PROFILE_BASE_ADDR + (profile_id * PROFILE_SIZE);
  
  /* Write profile data to EEPROM */
  return SPI_WriteBlock(profileAddress, (uint8_t*)p, PROFILE_SIZE);
}

/**
  * @brief  Load profile from EEPROM
  * @param  profileNum: Profile number (0-3)
  * @param  buffer: Pointer to output buffer
  * @retval HAL status
  */
HAL_StatusTypeDef LoadProfile(uint8_t profile_id, struct SeatProfile *p)
{
  /* Calculate profile address */
  uint16_t profileAddress = PROFILE_BASE_ADDR + (profile_id * PROFILE_SIZE);
  
  /* Read profile data from EEPROM */
  return SPI_ReadBlock(profileAddress, (uint8_t*)p, PROFILE_SIZE);
}

/**
  * @brief  UART RX complete callback
  * @param  huart: UART handle
  * @retval None
  */
void HAL_UART_RxCpltCallback(UART_HandleTypeDef *huart)
{
  if(huart->Instance == USART2)
  {
    /* Check for frame header */
    if(rxIndex == 0 && rxBuffer[0] != FRAME_HEADER)
    {
      /* Invalid header, restart reception */
      HAL_UART_Receive_IT(&huart2, &rxBuffer[0], 1);
      return;
    }
    
    /* Increment index */
    rxIndex++;
    
    /* Check for frame tail */
    if(rxBuffer[rxIndex-1] == FRAME_TAIL && rxIndex >= 4)
    {
      /* Complete frame received */
      frameReceived = 1;
    }
    else if(rxIndex < MAX_FRAME_SIZE)
    {
      /* Continue reception */
      HAL_UART_Receive_IT(&huart2, &rxBuffer[rxIndex], 1);
    }
    else
    {
      /* Buffer overflow, restart reception */
      rxIndex = 0;
      HAL_UART_Receive_IT(&huart2, &rxBuffer[0], 1);
    }
  }
}