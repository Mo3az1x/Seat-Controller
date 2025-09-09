#include "UART.h"
#include <stdio.h>

void UART_Init(void) {
    // TODO: Configure UART baud rate, frame format, etc.
    printf("UART initialized.\n");
}

void UART_SendChar(char c) {
    // TODO: Send a character via UART
    printf("UART send: %c\n", c);
}

char UART_ReceiveChar(void) {
    // TODO: Receive a character via UART
    printf("UART receive called.\n");
    return 0; // dummy
}
