#ifndef UART_H
#define UART_H

void UART_Init(void);
void UART_SendChar(char c);
char UART_ReceiveChar(void);

#endif
