#ifndef EEPROM_H
#define EEPROM_H

void EEPROM_WriteByte(unsigned int address, unsigned char data);
unsigned char EEPROM_ReadByte(unsigned int address);

#endif
