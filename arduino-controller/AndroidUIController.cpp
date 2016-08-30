#include <Arduino.h>
#include <SPI.h>
#include "AndroidUIController.h"

const int ANDROID_INPUT_PIN = 14;
const int ANDROID_OUTPUT_PIN = 8;

const int PACKET_START_TONE = 1500;
const int PACKET_END_TONE = 1000;
const int OFF_TONE = 100;
const int ON_TONE = 500;
const int INT_BITS = sizeof(int) * 8;

AndroidUIController::AndroidUIController(ProbeState* state) {
	this->state = state;
}

void AndroidUIController::initialize() {
}

void AndroidUIController::update(double R2, double targetVoltage, bool ended) {
	if (state->getIsRefreshNeeded()) {

	}
}

void AndroidUIController::readInput() {
	bool binary[INT_BITS];
	intToBinary(6666, binary);

	sendData(binary, INT_BITS);

	delay(500);
}


void AndroidUIController::sendData (bool* data, int size) {
	sendTone(PACKET_START_TONE);

	for (int i = 0; i < size; i++) {
		Serial.print(data[i]);

		if (data[i])
			sendTone(ON_TONE);
		else
			sendTone(OFF_TONE);
	}

	Serial.println();
	sendTone(PACKET_END_TONE);
}

void AndroidUIController::sendTone(int hz) {
	tone(ANDROID_OUTPUT_PIN, hz);
	delay(200);
	noTone(ANDROID_OUTPUT_PIN);
	delay(200);
}

void AndroidUIController::intToBinary(int number, bool* binary) {
	for (int i = 0; i < INT_BITS; i++)
    	binary[i] = (number & (1 << i)) ? 1 : 0;
}