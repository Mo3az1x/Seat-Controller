#include "rte_seatcontroller.h"
#include <stdio.h>

void Runnable_ProfileManager(void)
{
    bool save, load;
    uint8_t height, slide, incline;

    Rte_Read_Btn_Save(&save);
    Rte_Read_Btn_Load(&load);

    if (save)
    {
        Rte_Read_Seat_Height(&height);
        Rte_Read_Seat_Slide(&slide);
        Rte_Read_Seat_Incline(&incline);

        Rte_Call_Eeprom_Write(0x00, height);
        Rte_Call_Eeprom_Write(0x01, slide);
        Rte_Call_Eeprom_Write(0x02, incline);

        printf("[ProfileManager] Profile saved\n");
    }

    if (load)
    {
        height = Rte_Call_Eeprom_Read(0x00);
        slide = Rte_Call_Eeprom_Read(0x01);
        incline = Rte_Call_Eeprom_Read(0x02);

        Rte_Write_Seat_Height(height);
        Rte_Write_Seat_Slide(slide);
        Rte_Write_Seat_Incline(incline);

        printf("[ProfileManager] Profile loaded\n");
    }
}
