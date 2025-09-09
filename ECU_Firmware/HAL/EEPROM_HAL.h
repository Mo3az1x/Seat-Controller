#ifndef EEPROM_HAL_H
#define EEPROM_HAL_H

void EEPROM_Init(void);
void EEPROM_ReadAll(void* buffer);
void EEPROM_WriteAll(const void* buffer);

#endif
