#include "EEPROM.h"
#include <stdio.h>

void EEPROM_WriteByte(unsigned int address, unsigned char data) {
    // TODO: Write a byte to internal EEPROM
    printf("EEPROM write: addr=%u, data=%u\n", address, data);
}

unsigned char EEPROM_ReadByte(unsigned int address) {
    // TODO: Read a byte from internal EEPROM
    printf("EEPROM read: addr=%u\n", address);
    return 0; // dummy
}
