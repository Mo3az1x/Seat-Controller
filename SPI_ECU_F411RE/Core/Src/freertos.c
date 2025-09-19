/* USER CODE BEGIN Header */
/**
  ******************************************************************************
  * File Name          : freertos.c
  * Description        : Code for freertos applications
  ******************************************************************************
  * @attention
  *
  * Copyright (c) 2025 STMicroelectronics.
  * All rights reserved.
  *
  * This software is licensed under terms that can be found in the LICENSE file
  * in the root directory of this software component.
  * If no LICENSE file comes with this software, it is provided AS-IS.
  *
  ******************************************************************************
  */
/* USER CODE END Header */

/* Includes ------------------------------------------------------------------*/
#include "FreeRTOS.h"
#include "task.h"
#include "main.h"
#include "cmsis_os.h"

/* Private includes ----------------------------------------------------------*/
/* USER CODE BEGIN Includes */

#include "spi.h"
#include "usart.h"
#include <string.h>
#include <stdio.h>

/* USER CODE END Includes */

/* Private typedef -----------------------------------------------------------*/
/* USER CODE BEGIN PTD */
typedef struct __attribute__((packed)) {
  uint16_t height;
  uint16_t slide;
  uint16_t incline;
} SeatProfile;
/* USER CODE END PTD */

/* Private define ------------------------------------------------------------*/
/* USER CODE BEGIN PD */
#define CMD_SAVE     0x01
#define CMD_LOAD     0x02
#define MAX_PROFILES 5
/* USER CODE END PD */

/* Private macro -------------------------------------------------------------*/
/* USER CODE BEGIN PM */

/* USER CODE END PM */

/* Private variables ---------------------------------------------------------*/
/* USER CODE BEGIN Variables */
osMutexId_t spiMutexHandle;
/* USER CODE END Variables */

/* Definitions for defaultTask */
osThreadId_t defaultTaskHandle;
const osThreadAttr_t defaultTask_attributes = {
  .name = "defaultTask",
  .stack_size = 128 * 4,
  .priority = (osPriority_t) osPriorityNormal,
};

/* Definitions for SaveTask */
osThreadId_t saveTaskHandle;
const osThreadAttr_t saveTask_attributes = {
  .name = "saveTask",
  .stack_size = 256 * 4,
  .priority = (osPriority_t) osPriorityBelowNormal,
};

/* Definitions for LoadTask */
osThreadId_t loadTaskHandle;
const osThreadAttr_t loadTask_attributes = {
  .name = "loadTask",
  .stack_size = 256 * 4,
  .priority = (osPriority_t) osPriorityLow,
};

/* Private function prototypes -----------------------------------------------*/
/* USER CODE BEGIN FunctionPrototypes */
void StartSaveTask(void *argument);
void StartLoadTask(void *argument);
/* USER CODE END FunctionPrototypes */

void StartDefaultTask(void *argument);

void MX_FREERTOS_Init(void); /* (MISRA C 2004 rule 8.1) */

/**
  * @brief  FreeRTOS initialization
  * @param  None
  * @retval None
  */
void MX_FREERTOS_Init(void) {
  /* USER CODE BEGIN Init */

  /* USER CODE END Init */

 /* Create the mutex(es) */
 /* definition and creation of spiMutex */
 spiMutexHandle = osMutexNew(NULL);


  /* USER CODE BEGIN RTOS_MUTEX */
  /* add mutexes, ... */
  /* USER CODE END RTOS_MUTEX */

  /* USER CODE BEGIN RTOS_SEMAPHORES */
  /* add semaphores, ... */
  /* USER CODE END RTOS_SEMAPHORES */

  /* USER CODE BEGIN RTOS_TIMERS */
  /* start timers, add new ones, ... */
  /* USER CODE END RTOS_TIMERS */

  /* USER CODE BEGIN RTOS_QUEUES */
  /* add queues, ... */
  /* USER CODE END RTOS_QUEUES */

  /* Create the thread(s) */
  /* creation of defaultTask */
  defaultTaskHandle = osThreadNew(StartDefaultTask, NULL, &defaultTask_attributes);
  saveTaskHandle    = osThreadNew(StartSaveTask, NULL, &saveTask_attributes);
  loadTaskHandle    = osThreadNew(StartLoadTask, NULL, &loadTask_attributes);


  /* USER CODE BEGIN RTOS_THREADS */
  /* add threads, ... */
  /* USER CODE END RTOS_THREADS */

  /* USER CODE BEGIN RTOS_EVENTS */
  /* add events, ... */
  /* USER CODE END RTOS_EVENTS */

}

/* USER CODE BEGIN Header_StartDefaultTask */
/**
  * @brief  Function implementing the defaultTask thread.
  * @param  argument: Not used
  * @retval None
  */
/* USER CODE END Header_StartDefaultTask */
void StartDefaultTask(void *argument)
{
  /* USER CODE BEGIN StartDefaultTask */
  /* Infinite loop */
  for(;;)
  {
    osDelay(1);
  }
  /* USER CODE END StartDefaultTask */
}

/* USER CODE BEGIN 0 */
/* Retarget printf to USART2 */
int __io_putchar(int ch) {
  uint8_t c = ch & 0xFF;
  HAL_UART_Transmit(&huart2, &c, 1, HAL_MAX_DELAY);
  return ch;
}

/* Chip Select helpers */
static inline void CS_Select(void) {
  HAL_GPIO_WritePin(SPI_CS_GPIO_Port, SPI_CS_Pin, GPIO_PIN_RESET);
}
static inline void CS_Deselect(void) {
  HAL_GPIO_WritePin(SPI_CS_GPIO_Port, SPI_CS_Pin, GPIO_PIN_SET);
}

/* SPI save/load functions */
HAL_StatusTypeDef SaveProfileSPI(uint8_t id, SeatProfile *p) {
  HAL_StatusTypeDef st;
  uint8_t cmd[2] = { CMD_SAVE, id };

  osMutexAcquire(spiMutexHandle, osWaitForever);
  CS_Select();
  st = HAL_SPI_Transmit(&hspi1, cmd, 2, HAL_MAX_DELAY);
  if (st == HAL_OK) {
    st = HAL_SPI_Transmit(&hspi1, (uint8_t*)p, sizeof(SeatProfile), HAL_MAX_DELAY);
  }
  CS_Deselect();
  osMutexRelease(spiMutexHandle);

  return st;
}

HAL_StatusTypeDef LoadProfileSPI(uint8_t id, SeatProfile *p) {
  HAL_StatusTypeDef st;
  uint8_t cmd[2] = { CMD_LOAD, id };
  uint8_t rxbuf[sizeof(SeatProfile)];
  uint8_t txDummy[sizeof(SeatProfile)];
  memset(txDummy, 0xFF, sizeof(txDummy));

  osMutexAcquire(spiMutexHandle, osWaitForever);
  CS_Select();
  st = HAL_SPI_Transmit(&hspi1, cmd, 2, HAL_MAX_DELAY);
  if (st == HAL_OK) {
    st = HAL_SPI_TransmitReceive(&hspi1, txDummy, rxbuf, sizeof(SeatProfile), HAL_MAX_DELAY);
  }
  CS_Deselect();
  osMutexRelease(spiMutexHandle);

  if (st == HAL_OK) memcpy(p, rxbuf, sizeof(SeatProfile));
  return st;
}
/* USER CODE END 0 */

/* USER CODE BEGIN 4 */
void StartSaveTask(void *argument)
{
  SeatProfile p;
  uint8_t id = 0;

  for(;;)
  {
    p.height  = 100 + id * 10;
    p.slide   = 200 + id * 5;
    p.incline = 10  + id;

    if (SaveProfileSPI(id, &p) == HAL_OK) {
      printf("[SAVE] Profile %u saved: H=%u S=%u I=%u\r\n", id, p.height, p.slide, p.incline);
    } else {
      printf("[SAVE] Error saving profile %u\r\n", id);
    }

    id = (id + 1) % MAX_PROFILES;
    osDelay(5000); // every 5s
  }
}

void StartLoadTask(void *argument)
{
  SeatProfile p;

  for(;;)
  {
    for (uint8_t i = 0; i < MAX_PROFILES; i++) {
      if (LoadProfileSPI(i, &p) == HAL_OK) {
        printf("[LOAD] ID=%u -> H=%u S=%u I=%u\r\n", i, p.height, p.slide, p.incline);
      } else {
        printf("[LOAD] Error loading ID=%u\r\n", i);
      }
      osDelay(100); // small gap
    }
    osDelay(8000);
  }
}
/* USER CODE END 4 */

/* Private application code --------------------------------------------------*/
/* USER CODE BEGIN Application */

/* USER CODE END Application */

