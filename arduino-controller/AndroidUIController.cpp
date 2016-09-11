#include <Arduino.h>
#include <SPI.h>
#include "AndroidUIController.h"
#include "SoftModem.h"

const int ANDROID_INPUT_PIN = 14;
const int ANDROID_OUTPUT_PIN = 8;

const int HIGH_FREQ = 2000;
const int LOW_FREQ = 100;
const int TONE_DELAY = 90;

const int VOLTAGE_DATA_ID = 1;
const int RESISTANCE_DATA_ID = 2;
const int CURRENT_DATA_ID = 3;
const int ACTIVE_TIME_DATA_ID = 4;

const int INT_BITS = sizeof(int) * 8;
// SoftModem modem = SoftModem();
bool booted = false;

AndroidUIController::AndroidUIController(ProbeState* state) {
	this->state = state;
}

void AndroidUIController::initialize() {
}
void AndroidUIController::update(bool ended) {
	// if (state->getIsRefreshNeeded()) {

	// if (Serial.available()) {
	// 	modem.write(0xff);
	// 	while (Serial.available()) {
	// 		char c = Serial.read();
	// 		modem.write(c);
	// 		Serial.println("written ");
	// 		Serial.println(c);
	// 	}
	// }


	// sendTone(HIGH_FREQ);
	// // sendInt(5, VOLTAGE_DATA_ID);
	// // sendInt(state->lastInputVoltage, VOLTAGE_DATA_ID);
	// sendInt(state->getTargetMicroAmps(), CURRENT_DATA_ID);
	// // sendInt(state->resistance, RESISTANCE_DATA_ID);
	// sendInt(state->getActiveTime(), ACTIVE_TIME_DATA_ID);

	// sendTone(LOW_FREQ);
	// }
}

void AndroidUIController::readInput() {
	// tone(ANDROID_OUTPUT_PIN, 400);
	// modem.write(0xff);
	// modem.write('R');
	// modem.write('1');
	// modem.write('2');
	// modem.write('3');
	// modem.write('4');
	// delay(100);
	// Serial.println("written");

	// if (!booted) {
	// 	booted = true;
	// 	Serial.begin(9600);
	// 	Serial.println("Booting Modem");
	// 	delay(100);
	// 	modem.begin();
	// }
	// if (Serial.available()) {
	// 	modem.write(0xff);
	// 	while (Serial.available()) {
	// 		char c = Serial.read();
	// 		modem.write(c);
	// 	}
	// }
}

void AndroidUIController::sendData (int* data, int size) {
	for (int i = 0; i < size; i++) {
		int hz = 200 + (data[i] * 100);
		sendTone(hz);
	}

}

void AndroidUIController::sendTone(int hz) {
	// tone(ANDROID_OUTPUT_PIN, hz);
	// delay(TONE_DELAY);
	// noTone(ANDROID_OUTPUT_PIN);
	// delay(TONE_DELAY);
}

void AndroidUIController::sendInt(int value, int id) {
	int idBinary[1];
	int4ToBinary(id, idBinary);
	sendData(idBinary, 1);

	int binary[INT_BITS / 4];
	intToBinary(value, binary);
	sendData(binary, 4);
}

void AndroidUIController::intToBinary(int number, int* tones) {
	Serial.print("value: ");
	Serial.print(number);
	Serial.print(" bin: ");

	tones[0] =  number & 0x000f;
	tones[1] = (number & 0x00f0) >> 4;
	tones[2] = (number & 0x0f00) >> 8;
	tones[3] = (number & 0xf000) >> 12;

	for (int i = 0; i < 4; i ++) {
		Serial.print(tones[i]);
		Serial.print(" ");
	}


	Serial.println();
}

void AndroidUIController::int4ToBinary(int number, int* tones) {

	Serial.print("id: ");
	Serial.print(number);
	Serial.print(" bin: ");

	tones[0] =  number & 0x000f;
	Serial.print(tones[0]);

	Serial.println();
}