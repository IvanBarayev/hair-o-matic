#include <Arduino.h>
#include <SPI.h>
#include "DigiPot.h"
// const word outputValue = 0; // a word is a 16-bit number
// byte data = 0; // and a byte is an 8-bit number

DigiPot::DigiPot (int ioPin) {
	outputPin = ioPin;

	pinMode(outputPin, OUTPUT);
	// wake up the SPI bus.
	SPI.begin(); 
	SPI.setBitOrder(MSBFIRST);
}

void DigiPot::write (word outputValue)
{
	digitalWrite(outputPin, LOW);
	byte data = highByte(outputValue);
	data = 0b00001111 & data;
	data = 0b00110000 | data;
	SPI.transfer(data);
	data = lowByte(outputValue);
	SPI.transfer(data);
	digitalWrite(outputPin, HIGH);

}

