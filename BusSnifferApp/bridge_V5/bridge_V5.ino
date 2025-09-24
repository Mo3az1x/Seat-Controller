#include <Wire.h>

// Configuration
#define I2C_SLAVE_ADDRESS     0x50
#define UART_BAUD_RATE        115200
#define LED_PIN               13
#define MAX_I2C_BUFFER        32
#define MAX_UART_BUFFER       64

// Global variables for I2C receive (STM32 -> Arduino -> PC)
uint8_t i2c_rx_buffer[MAX_I2C_BUFFER];
volatile uint8_t i2c_rx_length = 0;
volatile bool i2c_data_received = false;

// Global variables for I2C transmit (PC -> Arduino -> STM32)
uint8_t i2c_tx_buffer[MAX_I2C_BUFFER];
volatile uint8_t i2c_tx_length = 0;
volatile bool i2c_tx_ready = false;

// UART command buffer
char uart_command_buffer[MAX_UART_BUFFER];
uint8_t uart_buffer_pos = 0;

uint32_t message_counter = 0;

// Statistics
struct Statistics {
  uint32_t messages_received_from_stm32;
  uint32_t messages_sent_to_pc;
  uint32_t messages_received_from_pc;
  uint32_t messages_sent_to_stm32;
  uint32_t errors;
  uint32_t last_message_time;
} stats = {0};

void setup() {
  // Initialize Serial communication
  Serial.begin(UART_BAUD_RATE);
  
  // Initialize I2C as slave
  Wire.begin(I2C_SLAVE_ADDRESS);
  Wire.onReceive(onI2CReceive);
  Wire.onRequest(onI2CRequest);
  
  // Initialize LED
  pinMode(LED_PIN, OUTPUT);
  digitalWrite(LED_PIN, LOW);
  
  // Clear buffers
  memset(uart_command_buffer, 0, MAX_UART_BUFFER);
  uart_buffer_pos = 0;
  
  // Startup message
  Serial.println("=== Arduino Nano Bi-directional I2C-UART Bridge ===");
  Serial.println("Version 3.0 - Full duplex communication");
  Serial.print("I2C Address: 0x");
  Serial.println(I2C_SLAVE_ADDRESS, HEX);
  Serial.print("UART Baud: ");
  Serial.println(UART_BAUD_RATE);
  Serial.println("Commands:");
  Serial.println("  STATUS - Show bridge status");
  Serial.println("  RESET - Reset statistics");
  Serial.println("  PING - Test bridge");
  Serial.println("  INFO - Show system info");
  Serial.println("  SEND <hex> - Send hex data to STM32 (e.g., SEND 01 02 03)");
  Serial.println("Ready...");
  
  // Startup LED sequence
  for (int i = 0; i < 3; i++) {
    digitalWrite(LED_PIN, HIGH);
    delay(200);
    digitalWrite(LED_PIN, LOW);
    delay(200);
  }
}

void loop() {
  // Check if I2C data was received from STM32
  if (i2c_data_received) {
    noInterrupts();
    i2c_data_received = false;
    interrupts();
    
    // Process received I2C message and forward to PC
    processI2CMessage();
    
    // Activity LED
    digitalWrite(LED_PIN, HIGH);
    delay(50);
    digitalWrite(LED_PIN, LOW);
    
    stats.messages_received_from_stm32++;
    stats.last_message_time = millis();
  }
  
  // Check for incoming UART commands from PC
  if (Serial.available()) {
    processUARTInput();
  }
  
  // Status update every 10 seconds
  static uint32_t last_status = 0;
  if (millis() - last_status > 10000) {
    sendStatus();
    last_status = millis();
  }
  
  // Heartbeat LED if no activity
  static uint32_t last_heartbeat = 0;
  if (millis() - stats.last_message_time > 3000) {
    if (millis() - last_heartbeat > 1000) {
      digitalWrite(LED_PIN, !digitalRead(LED_PIN));
      last_heartbeat = millis();
    }
  }
}

/**
 * I2C receive event handler (STM32 -> Arduino)
 */
void onI2CReceive(int bytes) {
  i2c_rx_length = 0;
  
  // Read all available bytes
  while (Wire.available() && i2c_rx_length < MAX_I2C_BUFFER) {
    i2c_rx_buffer[i2c_rx_length++] = Wire.read();
  }
  
  // Set flag to process in main loop
  if (i2c_rx_length > 0) {
    i2c_data_received = true;
  }
}

/**
 * I2C request event handler (Arduino -> STM32)
 * STM32 is requesting data from Arduino
 */
void onI2CRequest() {
  if (i2c_tx_ready && i2c_tx_length > 0) {
    // Send queued data to STM32
    Wire.write(i2c_tx_buffer, i2c_tx_length);
    
    // Clear the buffer after sending
    i2c_tx_ready = false;
    i2c_tx_length = 0;
    stats.messages_sent_to_stm32++;
  } else {
    // Send status byte if no data queued
    uint8_t status = 0x00;
    if (stats.messages_received_from_stm32 > 0) status |= 0x01;
    if (millis() - stats.last_message_time < 1000) status |= 0x02;
    
    Wire.write(status);
  }
}

/**
 * Process I2C message received from STM32 and send to PC via UART
 */
void processI2CMessage() {
  if (i2c_rx_length < 1) {
    stats.errors++;
    return;
  }
  
  // Check message type based on first byte or length
  if (i2c_rx_length == 2 && i2c_rx_buffer[0] == 0x01) {
    // Mode message
    Serial.print("[STM32->PC MODE] ");
    Serial.print((char)i2c_rx_buffer[1]);
    Serial.println();
  }
  else if (i2c_rx_length >= 7) {
    // Seat status message
    uint16_t height = (i2c_rx_buffer[1] << 8) | i2c_rx_buffer[0];
    uint16_t slide = (i2c_rx_buffer[3] << 8) | i2c_rx_buffer[2];
    uint16_t incline = (i2c_rx_buffer[5] << 8) | i2c_rx_buffer[4];
    uint8_t mode = i2c_rx_buffer[6];
    uint8_t state = (i2c_rx_length > 7) ? i2c_rx_buffer[7] : 0;
    
    Serial.print("[STM32->PC SEAT_STATUS] H=");
    Serial.print(height);
    Serial.print(" S=");
    Serial.print(slide);
    Serial.print(" I=");
    Serial.print(incline);
    Serial.print(" Mode=");
    Serial.print((char)mode);
    Serial.print(" State=");
    Serial.println(state);
  }
  else {
    // Generic message - print as hex
    Serial.print("[STM32->PC DATA] ");
    for (int i = 0; i < i2c_rx_length; i++) {
      Serial.print("0x");
      if (i2c_rx_buffer[i] < 0x10) Serial.print("0");
      Serial.print(i2c_rx_buffer[i], HEX);
      Serial.print(" ");
    }
    Serial.println();
  }
  
  stats.messages_sent_to_pc++;
}

/**
 * Process incoming UART input from PC
 */
void processUARTInput() {
  while (Serial.available()) {
    char c = Serial.read();
    
    if (c == '\n' || c == '\r') {
      if (uart_buffer_pos > 0) {
        uart_command_buffer[uart_buffer_pos] = '\0';
        processUARTCommand();
        uart_buffer_pos = 0;
        memset(uart_command_buffer, 0, MAX_UART_BUFFER);
      }
    } else if (uart_buffer_pos < MAX_UART_BUFFER - 1) {
      uart_command_buffer[uart_buffer_pos++] = c;
    }
  }
}

/**
 * Process complete UART commands from PC
 */
void processUARTCommand() {
  // Convert to uppercase for easier parsing
  for (int i = 0; uart_command_buffer[i]; i++) {
    if (uart_command_buffer[i] >= 'a' && uart_command_buffer[i] <= 'z') {
      uart_command_buffer[i] = uart_command_buffer[i] - 'a' + 'A';
    }
  }
  
  if (strncmp(uart_command_buffer, "STATUS", 6) == 0) {
    sendStatus();
  }
  else if (strncmp(uart_command_buffer, "RESET", 5) == 0) {
    stats.messages_received_from_stm32 = 0;
    stats.messages_sent_to_pc = 0;
    stats.messages_received_from_pc = 0;
    stats.messages_sent_to_stm32 = 0;
    stats.errors = 0;
    Serial.println("[PC->STM32] Statistics reset");
  }
  else if (strncmp(uart_command_buffer, "PING", 4) == 0) {
    Serial.println("[PC->STM32] PONG - Bridge alive");
  }
  else if (strncmp(uart_command_buffer, "INFO", 4) == 0) {
    Serial.println("[PC->STM32] Arduino Nano I2C-UART Bridge v3.0");
    Serial.print("Uptime: ");
    Serial.print(millis() / 1000);
    Serial.println(" seconds");
    Serial.print("I2C Address: 0x");
    Serial.println(I2C_SLAVE_ADDRESS, HEX);
  }
  else if (strncmp(uart_command_buffer, "SEND ", 5) == 0) {
    processSendCommand(&uart_command_buffer[5]);
  }
  else {
    Serial.print("[ERROR] Unknown command: ");
    Serial.println(uart_command_buffer);
    Serial.println("Available commands: STATUS, RESET, PING, INFO, SEND <hex>");
  }
  
  stats.messages_received_from_pc++;
}

/**
 * Process SEND command to queue data for STM32
 */
void processSendCommand(char* hex_data) {
  // Check if previous message is still pending
  noInterrupts();
  bool tx_busy = i2c_tx_ready;
  interrupts();
  
  if (tx_busy) {
    Serial.println("[PC->STM32 ERROR] Previous message still pending, try again later");
    return;
  }
  
  // Parse hex data
  uint8_t temp_buffer[MAX_I2C_BUFFER];
  uint8_t temp_length = 0;
  
  char* token = hex_data;
  while (*token && temp_length < MAX_I2C_BUFFER) {
    // Skip whitespace
    while (*token == ' ' || *token == '\t') token++;
    
    if (!*token) break;
    
    // Parse hex byte
    uint8_t byte_val = 0;
    for (int i = 0; i < 2 && *token; i++) {
      byte_val <<= 4;
      if (*token >= '0' && *token <= '9') {
        byte_val |= (*token - '0');
      } else if (*token >= 'A' && *token <= 'F') {
        byte_val |= (*token - 'A' + 10);
      } else if (*token >= 'a' && *token <= 'f') {
        byte_val |= (*token - 'a' + 10);
      } else {
        Serial.print("[PC->STM32 ERROR] Invalid hex character: ");
        Serial.println(*token);
        return;
      }
      token++;
    }
    
    temp_buffer[temp_length++] = byte_val;
  }
  
  if (temp_length == 0) {
    Serial.println("[PC->STM32 ERROR] No valid hex data provided");
    return;
  }
  
  // Copy to I2C TX buffer with interrupt protection
  noInterrupts();
  memcpy(i2c_tx_buffer, temp_buffer, temp_length);
  i2c_tx_length = temp_length;
  i2c_tx_ready = true;
  interrupts();
  
  // Confirm queued
  Serial.print("[PC->STM32] Queued ");
  Serial.print(temp_length);
  Serial.print(" bytes: ");
  for (int i = 0; i < temp_length; i++) {
    Serial.print("0x");
    if (temp_buffer[i] < 0x10) Serial.print("0");
    Serial.print(temp_buffer[i], HEX);
    Serial.print(" ");
  }
  Serial.println("(waiting for STM32 to read)");
}

/**
 * Send status information
 */
void sendStatus() {
  Serial.println("=== Bi-directional Bridge Status ===");
  Serial.print("STM32->PC Messages: ");
  Serial.println(stats.messages_received_from_stm32);
  Serial.print("PC Messages sent: ");
  Serial.println(stats.messages_sent_to_pc);
  Serial.print("PC->STM32 Messages: ");
  Serial.println(stats.messages_received_from_pc);
  Serial.print("STM32 Messages sent: ");
  Serial.println(stats.messages_sent_to_stm32);
  Serial.print("Errors: ");
  Serial.println(stats.errors);
  Serial.print("Last STM32 message: ");
  if (stats.last_message_time > 0) {
    Serial.print(millis() - stats.last_message_time);
    Serial.println(" ms ago");
  } else {
    Serial.println("Never");
  }
  
  noInterrupts();
  bool tx_pending = i2c_tx_ready;
  uint8_t tx_len = i2c_tx_length;
  interrupts();
  
  Serial.print("TX Buffer: ");
  if (tx_pending) {
    Serial.print(tx_len);
    Serial.println(" bytes pending");
  } else {
    Serial.println("Empty");
  }
  
  Serial.print("Free RAM: ");
  Serial.println(getFreeRam());
  Serial.print("I2C Status: ");
  Serial.println((millis() - stats.last_message_time < 5000) ? "Active" : "Idle");
}

/**
 * Get available RAM
 */
int getFreeRam() {
  extern int __heap_start, *__brkval;
  int v;
  return (int) &v - (__brkval == 0 ? (int) &__heap_start : (int) __brkval);
}
