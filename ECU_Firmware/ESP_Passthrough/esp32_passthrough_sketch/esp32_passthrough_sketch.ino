#include <SPI.h>

// Volatile variable to store the received byte
volatile byte receivedByte; 
// Flag to indicate if a new byte has been received
volatile bool newData = false;

void setup() {
  Serial.begin(9600); // Initialize serial communication for debugging
  
  // Set MISO pin as output, as the slave will send data back to the master
  pinMode(MISO, OUTPUT);
  
  // Enable SPI in slave mode
  SPCR |= _BV(SPE); 
  
  // Enable SPI interrupts
  SPI.attachInterrupt(); 
  
  Serial.println("Arduino SPI Slave Ready.");
}

void loop() {
  // Check if a new byte has been received
  if (newData) {
    Serial.print("Received from Master: ");
    Serial.println(receivedByte, HEX); // Print the received byte in hexadecimal
    newData = false; // Reset the flag
  }
}

// SPI Interrupt Service Routine (ISR)
ISR(SPI_STC_vect) {
  // Read the received byte from the SPI Data Register (SPDR)
  receivedByte = SPDR; 
  
  // Prepare the byte to send back to the master (e.g., increment it)
  SPDR = receivedByte + 1; 
  
  newData = true; // Set the flag to indicate new data is available
}