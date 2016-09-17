#include <SPI.h>
#include <EEPROM.h>
#include <TouchScreen.h>
#include "Probestate.h"
#include "Color.h"
#include "DigiPot.h"
#include "TouchUIController.h"
#include "BlueToothUIController.h"
#include "IOUtils.h"

#define DEBUG_MODE false
#define USE_HARDWARE_BUZZER false

const int PROBE_ACTIVE_SENSOR_PIN = 15;
const int BUZZER_IO_PIN = 44;
const int DAC_OUTPUT_PIN = 22;

const int MAX_VOLTAGE = 10;
const int MIN_KILL_TIME = 4;
const int BUZZER_PAUSE_TIME = 5;

ProbeState* state = new ProbeState();
IUIController* uiController = new BlueToothUIController(state);

DigiPot powerPot(DAC_OUTPUT_PIN);
long lastTickTime = 0;

void setup(void) {
	Serial.begin(9600);
	state->initialize();
	uiController->initialize();
}

void loop(void) {
	int activeVoltage = IOUtils::analogReadFiltered(PROBE_ACTIVE_SENSOR_PIN);
	bool ended = false;

	if (isInsertStart(activeVoltage)) {
		state->setIsProbeActive(true);
		state->setIsRefreshNeeded(true);
	} else if (activeVoltage == 0) {
		if (isInsertEnd()) {
			ended = true;
			state->setIsRefreshNeeded(true);

			state->incrementKillCount();
			state->incrementLifeTimeCount();
		}

		state->setIsProbeActive(false);
	}

	if (state->getIsProbeActive() && state->getActiveTime() != lastTickTime)
		state->setIsRefreshNeeded(true);

	if (millis() % 1000 > 600 && !uiController->useSlowRefresh)
		state->setIsRefreshNeeded(true);

	long time = state->getActiveTime();
	if (state->getIsProbeActive() && (time % BUZZER_PAUSE_TIME > 0 || time < BUZZER_PAUSE_TIME))
		playBuzzer();
	else
		stopBuzzer();

	double R1 = 480;
	double vIn = state->lastInputVoltage;
	double vOut = (5.0 * activeVoltage) / 1024.0;

	double buffer = (vIn / vOut) - 1;
	double R2 = (R1 * buffer);
	double current = vIn / (R2 + R1);

	double targetVoltage = getTargetCurrentForTime() * (R2 + R1);

	if (targetVoltage > MAX_VOLTAGE)
		targetVoltage = MAX_VOLTAGE;

	// initial read may be off so drop back down if voltage really high so no possible initial painful zap
	if (targetVoltage > 5 && state->getPreciseActiveTime() < .3)
		targetVoltage = 5;

	if (activeVoltage < 1) {
		powerPot.writeVolts(1);
		targetVoltage = 1;
		state->lastInputVoltage = 1;
		R2 = 0;
		current = 0;
	} 
	// only update dac if voltage change significant as dac updates are fairly slow
	else if (abs(state->lastInputVoltage - targetVoltage) >= .01) {
		// divide by 2 since opamp will double our target voltage
		powerPot.writeVolts(targetVoltage / 2.0);

		state->lastInputVoltage = targetVoltage;
	}

	if (state->resistance != R2)
		state->setIsRefreshNeeded(true);

	state->resistance = R2;

	uiController->readInput();
	uiController->update(ended);

	lastTickTime = state->getActiveTime();
	state->isFirstLoop = false;
}

bool isInsertStart(int activeVoltage) {
	return activeVoltage > 0 && !state->getIsProbeActive();
}

bool isInsertEnd() {
	return state->getIsProbeActive() && state->getActiveTime() >= MIN_KILL_TIME;
}

void playBuzzer() {
	if (!USE_HARDWARE_BUZZER)
		return;

	int hz = 110;
	tone(BUZZER_IO_PIN, hz);
}

void stopBuzzer() {
	if (!USE_HARDWARE_BUZZER)
		return;

	noTone(BUZZER_IO_PIN);
}

double getTargetCurrentForTime() {
	double targetCurrent = uaToAmps(state->getTargetMicroAmps());
	double time = state->getPreciseActiveTime();

	// ramp up current over first half second to reduce inital shock, have to have a decent initial
	// current though so the analog input for constant current control can measure the voltage drop
	if (time <= .15)
		return uaToAmps(150);
	else if (time <= 0.5)
		return time * 2.0 * targetCurrent;

	// very slowley add current for every second active to squeeze more current in
	// without a noticible pain increase
	else if (time < 10.0)
		return (uaToAmps(3) * (time - 0.5)) + targetCurrent;

	return targetCurrent;
}

double uaToAmps(double microAmps) {
	return 0.000001 * microAmps;
}