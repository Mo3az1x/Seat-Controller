/* USER CODE BEGIN Header */
/**
  ******************************************************************************
  * @file    gpio.h
  * @brief   This file contains all the function prototypes for
  *          the gpio.c file
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
/* Define to prevent recursive inclusion -------------------------------------*/
#ifndef __GPIO_H__
#define __GPIO_H__

#ifdef __cplusplus
extern "C" {
#endif

/* Includes ------------------------------------------------------------------*/
#include "main.h"

/* USER CODE BEGIN Includes */

/* USER CODE END Includes */

/* USER CODE BEGIN Private defines */
#define ERROR_LED_GPIO_Port GPIOB
#define ERROR_LED_Pin GPIO_PIN_0
#define IDLE_LED_GPIO_Port GPIOB
#define IDLE_LED_Pin GPIO_PIN_1
#define MOVING_LED_GPIO_Port GPIOB
#define MOVING_LED_Pin GPIO_PIN_2
/* USER CODE END Private defines */

void MX_GPIO_Init(void);

/* USER CODE BEGIN Prototypes */
void ErrorLED_On(void);
void ErrorLED_Off(void);
void ErrorLED_Toggle(void);
void IdleLED_On(void);
void IdleLED_Off(void);
void IdleLED_Toggle(void);
void MovingLED_On(void);
void MovingLED_Off(void);
void MovingLED_Toggle(void);
/* USER CODE END Prototypes */

#ifdef __cplusplus
}
#endif
#endif /*__ GPIO_H__ */
