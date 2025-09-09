#include "DIO.h"
#include <stdio.h>  // for debug printing

void DIO_Init(void) {
    // TODO: Initialize ports/pins
    printf("DIO initialized.\n");
}

int DIO_ReadChannel(int channel) {
    // TODO: Read pin value (return 0 or 1)
    printf("DIO read channel %d.\n", channel);
    return 0;
}

void DIO_WriteChannel(int channel, int value) {
    // TODO: Write pin value (0 or 1)
    printf("DIO write channel %d = %d.\n", channel, value);
}
