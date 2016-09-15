#ifndef BlueToothUIController_INCLUDED
#define BlueToothUIController_INCLUDED
#include "Color.h"
#include "ProbeState.h"
#include "IUIController.h"
#include <SoftwareSerial.h>

class BlueToothUIController : public IUIController {
public :
	BlueToothUIController(ProbeState* state);
	void initialize ();
	void update(bool ended);
	void readInput();
protected :
	String getBtCommandResponse();
	String getBtInputCommand();
	void appendJsonElement(String name, double value);
	void appendJsonElement(String name, long value);
	void appendJsonElement(String name, int value);
	void appendJsonElement(String name, String value);
	
	String statusJson;
};

#endif

