/* USER CODE BEGIN Header */
/**
  ******************************************************************************
  * @file    gpio.c
  * @brief   This file provides code for the configuration
  *          of all used GPIO pins.
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
#include "gpio.h"

/* USER CODE BEGIN 0 */

/* USER CODE END 0 */

/*----------------------------------------------------------------------------*/
/* Configure GPIO                                                             */
/*----------------------------------------------------------------------------*/
/* USER CODE BEGIN 1 */

/* USER CODE END 1 */

/** Configure pins as
        * Analog
        * Input
        * Output
        * EVENT_OUT
        * EXTI
*/
void MX_GPIO_Init(void)
{

  GPIO_InitTypeDef GPIO_InitStruct = {0};

  /* GPIO Ports Clock Enable */
  __HAL_RCC_GPIOC_CLK_ENABLE();
  __HAL_RCC_GPIOH_CLK_ENABLE();
  __HAL_RCC_GPIOA_CLK_ENABLE();
  __HAL_RCC_GPIOB_CLK_ENABLE();

  /*Configure GPIO pin Output Level */
  HAL_GPIO_WritePin(LD2_GPIO_Port, LD2_Pin, GPIO_PIN_RESET);

  /*Configure GPIO pin Output Level */
  HAL_GPIO_WritePin(SPI_CS_GPIO_Port, SPI_CS_Pin, GPIO_PIN_RESET);

  /*Configure GPIO pin Output Level */
  HAL_GPIO_WritePin(GPIOC, GPIO_PIN_8|save_led_Pin, GPIO_PIN_RESET);

  /*Configure GPIO pin Output Level */
  HAL_GPIO_WritePin(ERROR_LED_GPIO_Port, ERROR_LED_Pin, GPIO_PIN_RESET);

  /*Configure GPIO pin Output Level */
  HAL_GPIO_WritePin(IDLE_LED_GPIO_Port, IDLE_LED_Pin, GPIO_PIN_RESET);

  /*Configure GPIO pin Output Level */
  HAL_GPIO_WritePin(MOVING_LED_GPIO_Port, MOVING_LED_Pin, GPIO_PIN_RESET);

  /*Configure GPIO pins : switch_mode_Pin Height_dwn_Pin Slide_dwn_Pin */
  GPIO_InitStruct.Pin = switch_mode_Pin|Height_dwn_Pin|Slide_dwn_Pin;
  GPIO_InitStruct.Mode = GPIO_MODE_INPUT;
  GPIO_InitStruct.Pull = GPIO_NOPULL;
  HAL_GPIO_Init(GPIOC, &GPIO_InitStruct);

  /*Configure GPIO pins : reset_Pin PA1 Height_up_Pin Slide_up_Pin
                           Incline_up_Pin PA8 PA9 BUTTON_LOAD_Pin
                           BUTTON_SAVE_Pin */
  GPIO_InitStruct.Pin = reset_Pin|GPIO_PIN_1|Height_up_Pin|Slide_up_Pin
                          |Incline_up_Pin|GPIO_PIN_8|GPIO_PIN_9|BUTTON_LOAD_Pin
                          |BUTTON_SAVE_Pin;
  GPIO_InitStruct.Mode = GPIO_MODE_INPUT;
  GPIO_InitStruct.Pull = GPIO_NOPULL;
  HAL_GPIO_Init(GPIOA, &GPIO_InitStruct);

  /*Configure GPIO pin : LD2_Pin */
  GPIO_InitStruct.Pin = LD2_Pin;
  GPIO_InitStruct.Mode = GPIO_MODE_OUTPUT_PP;
  GPIO_InitStruct.Pull = GPIO_NOPULL;
  GPIO_InitStruct.Speed = GPIO_SPEED_FREQ_LOW;
  HAL_GPIO_Init(LD2_GPIO_Port, &GPIO_InitStruct);

  /*Configure GPIO pin : incline_dwn_Pin */
  GPIO_InitStruct.Pin = incline_dwn_Pin;
  GPIO_InitStruct.Mode = GPIO_MODE_INPUT;
  GPIO_InitStruct.Pull = GPIO_NOPULL;
  HAL_GPIO_Init(incline_dwn_GPIO_Port, &GPIO_InitStruct);

  /*Configure GPIO pin : SPI_CS_Pin */
  GPIO_InitStruct.Pin = SPI_CS_Pin;
  GPIO_InitStruct.Mode = GPIO_MODE_OUTPUT_PP;
  GPIO_InitStruct.Pull = GPIO_NOPULL;
  GPIO_InitStruct.Speed = GPIO_SPEED_FREQ_LOW;
  HAL_GPIO_Init(SPI_CS_GPIO_Port, &GPIO_InitStruct);

  /*Configure GPIO pins : PC8 save_led_Pin */
  GPIO_InitStruct.Pin = GPIO_PIN_8|save_led_Pin;
  GPIO_InitStruct.Mode = GPIO_MODE_OUTPUT_PP;
  GPIO_InitStruct.Pull = GPIO_NOPULL;
  GPIO_InitStruct.Speed = GPIO_SPEED_FREQ_LOW;
  HAL_GPIO_Init(GPIOC, &GPIO_InitStruct);

  /*Configure GPIO pin : ERROR_LED_Pin */
  GPIO_InitStruct.Pin = ERROR_LED_Pin;
  GPIO_InitStruct.Mode = GPIO_MODE_OUTPUT_PP;
  GPIO_InitStruct.Pull = GPIO_NOPULL;
  GPIO_InitStruct.Speed = GPIO_SPEED_FREQ_LOW;
  HAL_GPIO_Init(ERROR_LED_GPIO_Port, &GPIO_InitStruct);

  /*Configure GPIO pin : IDLE_LED_Pin */
  GPIO_InitStruct.Pin = IDLE_LED_Pin;
  GPIO_InitStruct.Mode = GPIO_MODE_OUTPUT_PP;
  GPIO_InitStruct.Pull = GPIO_NOPULL;
  GPIO_InitStruct.Speed = GPIO_SPEED_FREQ_LOW;
  HAL_GPIO_Init(IDLE_LED_GPIO_Port, &GPIO_InitStruct);

  /*Configure GPIO pin : MOVING_LED_Pin */
  GPIO_InitStruct.Pin = MOVING_LED_Pin;
  GPIO_InitStruct.Mode = GPIO_MODE_OUTPUT_PP;
  GPIO_InitStruct.Pull = GPIO_NOPULL;
  GPIO_InitStruct.Speed = GPIO_SPEED_FREQ_LOW;
  HAL_GPIO_Init(MOVING_LED_GPIO_Port, &GPIO_InitStruct);

}

/* USER CODE BEGIN 2 */
void ErrorLED_On(void)
{
    HAL_GPIO_WritePin(ERROR_LED_GPIO_Port, ERROR_LED_Pin, GPIO_PIN_SET);
}

void ErrorLED_Off(void)
{
    HAL_GPIO_WritePin(ERROR_LED_GPIO_Port, ERROR_LED_Pin, GPIO_PIN_RESET);
}

void ErrorLED_Toggle(void)
{
    HAL_GPIO_TogglePin(ERROR_LED_GPIO_Port, ERROR_LED_Pin);
}

void IdleLED_On(void)
{
    HAL_GPIO_WritePin(IDLE_LED_GPIO_Port, IDLE_LED_Pin, GPIO_PIN_SET);
}

void IdleLED_Off(void)
{
    HAL_GPIO_WritePin(IDLE_LED_GPIO_Port, IDLE_LED_Pin, GPIO_PIN_RESET);
}

void IdleLED_Toggle(void)
{
    HAL_GPIO_TogglePin(IDLE_LED_GPIO_Port, IDLE_LED_Pin);
}

void MovingLED_On(void)
{
    HAL_GPIO_WritePin(MOVING_LED_GPIO_Port, MOVING_LED_Pin, GPIO_PIN_SET);
}

void MovingLED_Off(void)
{
    HAL_GPIO_WritePin(MOVING_LED_GPIO_Port, MOVING_LED_Pin, GPIO_PIN_RESET);
}

void MovingLED_Toggle(void)
{
    HAL_GPIO_TogglePin(MOVING_LED_GPIO_Port, MOVING_LED_Pin);
}
/* USER CODE END 2 */
