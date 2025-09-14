# Makefile for SeatControllerECU
CC = gcc
CFLAGS = -Iinc
SRC = $(wildcard src/*.c)
OUT = SeatECU

all:
	$(CC) $(SRC) $(CFLAGS) -o $(OUT)

run: all
	./$(OUT)

clean:
	rm -f $(OUT)
