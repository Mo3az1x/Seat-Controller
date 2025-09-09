#include "ADC.h"
#include <stdio.h>

void ADC_Init(void) {
    // TODO: Configure ADC registers
    printf("ADC initialized.\n");
}

int ADC_ReadChannel(int channel) {
    // TODO: Read analog value from channel
    printf("ADC read channel %d.\n", channel);
    return 0; // dummy value
}
