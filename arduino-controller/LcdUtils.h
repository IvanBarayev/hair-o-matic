#include <Adafruit_GFX.h>
#include <Adafruit_TFTLCD.h>
#include <Arduino.h>
#ifndef LCD_UTILS_INCLUDED
#define LCD_UTILS_INCLUDED

void initializeLcd (Adafruit_TFTLCD& tft) {
	Serial.begin(9600);
	Serial.print("TFT size is "); Serial.print(tft.width()); Serial.print("x"); Serial.println(tft.height());

	tft.reset();

	uint16_t identifier = tft.readID();
	if (identifier == 0x9325) {
		Serial.println(F("Found ILI9325 LCD driver"));
	} else if (identifier == 0x9328) {
		Serial.println(F("Found ILI9328 LCD driver"));
	} else if (identifier == 0x4535) {
		Serial.println(F("Found LGDP4535 LCD driver"));
	} else if (identifier == 0x7575) {
		Serial.println(F("Found HX8347G LCD driver"));
	} else if (identifier == 0x9341) {
		Serial.println(F("Found ILI9341 LCD driver"));
	} else if (identifier == 0x8357) {
		Serial.println(F("Found HX8357D LCD driver"));
	} else if (identifier == 0x0101)
		identifier = 0x9341;
	else
		identifier = 0x9341;

	tft.begin(identifier);
}
#endif