#ifndef AudioUIController_INCLUDED
#define AudioUIController_INCLUDED
#include "Color.h"
#include "ProbeState.h"
#include "IUIController.h"

class AudioUIController : public IUIController {
public :
	AudioUIController(ProbeState* state);
	void initialize ();
	void update(bool ended);
	void readInput();

protected :
	void sendData (bool* data, int size);
	void sendTone(int hz);
	void intToBinary(int number, bool* binary);
	void int4ToBinary(int number, bool* binary);
	void sendInt(int value, int id);
};

#endif

