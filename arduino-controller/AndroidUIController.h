#ifndef AndroidUIController_INCLUDED
#define AndroidUIController_INCLUDED
#include "Color.h"
#include "ProbeState.h"
#include "IUIController.h"

class AndroidUIController : public IUIController {
public :
	AndroidUIController(ProbeState* state);
	void initialize ();
	void update(double R2, double targetVoltage, bool ended);
	void readInput();

protected :
	void sendData (bool* data, int size);
	void sendTone(int hz);
	void intToBinary(int number, bool* binary);
};

#endif

