#ifndef DIO_H
#define DIO_H

void DIO_Init(void);
int DIO_ReadChannel(int channel);
void DIO_WriteChannel(int channel, int value);

#endif
