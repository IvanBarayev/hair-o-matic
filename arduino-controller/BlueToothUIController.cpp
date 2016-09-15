#include <Arduino.h>
#include <SPI.h>
#include "BlueToothUIController.h"

#define BLUETOOTH_RX 10
#define BLUETOOTH_TX 11

SoftwareSerial BT(BLUETOOTH_RX, BLUETOOTH_TX);

const String BT_DEVICE_NAME = "Hair-o-matic";

const String VOLTAGE_STATUS_ID = "voltage";
const String CURRENT_STATUS_ID = "current";
const String TIMER_STATUS_ID = "timer";
const String RESISTANCE_STATUS_ID = "resistance";
const String LIFETIME_KILLS_ID = "lifetimeKills";
const String SESSION_KILLS_ID = "sessionKills";
const String INC_CURRENT_COMMAND = "[increase_current]";
const String DEC_CURRENT_COMMAND = "[decrease_current]";

BlueToothUIController::BlueToothUIController(ProbeState* state) {
	this->state = state;
}

void BlueToothUIController::initialize() {
	BT.begin(19200);
	delay(1000);

	BT.print("AT");
	delay(500);

	BT.print("AT+VERSION");
	delay(500);

	// BT.print("AT+BAUD4");
	// delay(500);

	String nameCommand = "AT+NAME" + BT_DEVICE_NAME;
	BT.print(nameCommand);
	delay(500);

	Serial.println(getBtCommandResponse());
}

void BlueToothUIController::update(bool ended) {
	delay(50);

	if (state->getIsRefreshNeeded()) {
		statusJson = "{";

		appendJsonElement(VOLTAGE_STATUS_ID, state->lastInputVoltage);
		appendJsonElement(CURRENT_STATUS_ID, state->getTargetMicroAmps());
		appendJsonElement(RESISTANCE_STATUS_ID, state->resistance);

		appendJsonElement(SESSION_KILLS_ID, state->getKillCount());
		appendJsonElement(LIFETIME_KILLS_ID, state->getLifeTimeCount());
		appendJsonElement(TIMER_STATUS_ID, state->getActiveTime());

		statusJson += "}";

		BT.print(statusJson);
		BT.flush();
	}
}

void BlueToothUIController::readInput() {
	// don't allow changes while probe inserted to avoid dangerous increases.. unless testing
	if (state->getIsProbeActive() && state->resistance > 2)
		return;

	delay(50);
	BT.flush();

	String command = getBtInputCommand();
	if (command == "") 
		return;
	
	Serial.println(command);
	if (command.endsWith(INC_CURRENT_COMMAND))
		state->increaseTargetCurrent();
	else if (command.endsWith(DEC_CURRENT_COMMAND))
		state->decreaseTargetCurrent();
}

String BlueToothUIController::getBtCommandResponse()
{
	if (BT.available() > 0)
		return BT.readString();

	return "";
}

String BlueToothUIController::getBtInputCommand()
{
	if (BT.available() > 0)
		return BT.readStringUntil(']') + ']';

	return "";
}

void BlueToothUIController::appendJsonElement(String name, double value) {
	appendJsonElement(name, String(value));
}

void BlueToothUIController::appendJsonElement(String name, long value) {
	if (statusJson != "{") statusJson += ", ";
	statusJson += "\"" + name + "\"" + " : " + value;
}

void BlueToothUIController::appendJsonElement(String name, int value) {
	if (statusJson != "{") statusJson += ", ";
	statusJson += "\"" + name + "\"" + " : " + value;
}

void BlueToothUIController::appendJsonElement(String name, String value) {
	if (statusJson != "{")
		statusJson += ", ";

	statusJson += "\"" + name + "\"" + " : " + value;
}
