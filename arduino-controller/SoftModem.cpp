#include "SoftModem.h"

SoftModem *SoftModem::activeObject = 0;

SoftModem::SoftModem() {
}

SoftModem::~SoftModem() {
	end();
}

#if F_CPU == 16000000
#if SOFT_MODEM_BAUD_RATE <= 126
#define TIMER_CLOCK_SELECT	   (7)
#define MICROS_PER_TIMER_COUNT   (clockCyclesToMicroseconds(1024))
#elif SOFT_MODEM_BAUD_RATE <= 315
#define TIMER_CLOCK_SELECT	   (4)
#define MICROS_PER_TIMER_COUNT   (clockCyclesToMicroseconds(256))
#elif SOFT_MODEM_BAUD_RATE <= 630
#define TIMER_CLOCK_SELECT	   (5)
#define MICROS_PER_TIMER_COUNT   (clockCyclesToMicroseconds(128))
#elif SOFT_MODEM_BAUD_RATE <= 1225
#define TIMER_CLOCK_SELECT	   (4)
#define MICROS_PER_TIMER_COUNT   (clockCyclesToMicroseconds(64))
#else
#define TIMER_CLOCK_SELECT	   (3)
#define MICROS_PER_TIMER_COUNT   (clockCyclesToMicroseconds(32))
#endif
#else
#if SOFT_MODEM_BAUD_RATE <= 126
#define TIMER_CLOCK_SELECT	   (6)
#define MICROS_PER_TIMER_COUNT   (clockCyclesToMicroseconds(256))
#elif SOFT_MODEM_BAUD_RATE <= 315
#define TIMER_CLOCK_SELECT	   (5)
#define MICROS_PER_TIMER_COUNT   (clockCyclesToMicroseconds(128))
#elif SOFT_MODEM_BAUD_RATE <= 630
#define TIMER_CLOCK_SELECT	   (4)
#define MICROS_PER_TIMER_COUNT   (clockCyclesToMicroseconds(64))
#else
#define TIMER_CLOCK_SELECT	   (3)
#define MICROS_PER_TIMER_COUNT   (clockCyclesToMicroseconds(32))
#endif
#endif

#define BIT_PERIOD            (1000000/SOFT_MODEM_BAUD_RATE)
#define HIGH_FREQ_MICROS      (1000000/SOFT_MODEM_HIGH_FREQ)
#define LOW_FREQ_MICROS       (1000000/SOFT_MODEM_LOW_FREQ)

#define HIGH_FREQ_CNT         (BIT_PERIOD/HIGH_FREQ_MICROS)
#define LOW_FREQ_CNT          (BIT_PERIOD/LOW_FREQ_MICROS)

#define MAX_CARRIR_BITS	      (40000/BIT_PERIOD)

#define TCNT_BIT_PERIOD		  (BIT_PERIOD/MICROS_PER_TIMER_COUNT)
#define TCNT_HIGH_FREQ		  (HIGH_FREQ_MICROS/MICROS_PER_TIMER_COUNT)
#define TCNT_LOW_FREQ		  (LOW_FREQ_MICROS/MICROS_PER_TIMER_COUNT)

#define TCNT_HIGH_TH_L		  (TCNT_HIGH_FREQ * 0.90)
#define TCNT_HIGH_TH_H		  (TCNT_HIGH_FREQ * 1.15)
#define TCNT_LOW_TH_L		  (TCNT_LOW_FREQ * 0.85)
#define TCNT_LOW_TH_H		  (TCNT_LOW_FREQ * 1.10)

#if SOFT_MODEM_DEBUG_ENABLE
static volatile uint8_t *_portLEDReg;
static uint8_t _portLEDMask;
#endif

#if defined(__AVR_ATmega1280__) || defined(__AVR_ATmega2560__)
#define OCR_B  OCR3B
#define TIFR  TIFR3
#define OCF_B  OCF3B
#define TCCR_A TCCR3A
#define TCCR_B  TCCR3B
#define TCNT  TCNT3
#define TIMER_COMPA_vect TIMER2_COMPA_vect
#define OCR_A OCR3A
#endif

enum { START_BIT = 0, DATA_BIT = 8, STOP_BIT = 9, INACTIVE = 0xff };

void SoftModem::begin(void)
{
	pinMode(_rxPin1, INPUT);
	digitalWrite(_rxPin1, LOW);
	
	pinMode(_rxPin2, INPUT);
	digitalWrite(_rxPin2, LOW);
	
	pinMode(_txPin, OUTPUT);
	digitalWrite(_txPin, LOW);
	
	_txPortReg = portOutputRegister(digitalPinToPort(_txPin));
	_txPortMask = digitalPinToBitMask(_txPin);
	
#if SOFT_MODEM_DEBUG_ENABLE
	_portLEDReg = portOutputRegister(digitalPinToPort(13));
	_portLEDMask = digitalPinToBitMask(13);
	pinMode(13, OUTPUT);
#endif
	
	_recvStat = INACTIVE;
	_recvBufferHead = _recvBufferTail = 0;
	
	SoftModem::activeObject = this;
	
	_lastTCNT = TCNT;
	_lastDiff = _lowCount = _highCount = 0;
	
	TCCR_A = 0;
	TCCR_B = (1 << WGM32)| (1 << CS12)|(1 << CS10);
	Serial.println(TIMER_CLOCK_SELECT);

	ACSR   = _BV(ACIE) | _BV(ACIS1);
	DIDR1  = _BV(AIN1D) | _BV(AIN0D);
}

void SoftModem::end(void)
{
	ACSR   &= ~(_BV(ACIE));
	TIMSK2 &= ~(_BV(OCIE2A));
	DIDR1  &= ~(_BV(AIN1D) | _BV(AIN0D));
	SoftModem::activeObject = 0;
}

void SoftModem::demodulate(void)
{
	uint8_t t = TCNT2;
	uint8_t diff;
	
	diff = t - _lastTCNT;
	
	if(diff < 4)
		return;
	
	_lastTCNT = t;
	
	if(diff > (uint8_t)(TCNT_LOW_TH_H))
		return;
	
	// Calculating the moving average
#if SOFT_MODEM_MOVING_AVERAGE_ENABLE
	_lastDiff = (diff >> 1) + (diff >> 2) + (_lastDiff >> 2);
#else
	_lastDiff = diff;
#endif

	if(_lastDiff >= (uint8_t)(TCNT_LOW_TH_L)){
		_lowCount += _lastDiff;
		if(_recvStat == INACTIVE){
			// Start bit detection
			if(_lowCount >= (uint8_t)(TCNT_BIT_PERIOD * 0.5)){
				_recvStat = START_BIT;
				_highCount = 0;
				_recvBits  = 0;
				OCR2A = t + (uint8_t)(TCNT_BIT_PERIOD) - _lowCount;
				TIFR2 |= _BV(OCF2A);
				TIMSK2 |= _BV(OCIE2A);
			}
		}
	}
	else if(_lastDiff <= (uint8_t)(TCNT_HIGH_TH_H)){
		if(_recvStat == INACTIVE){
			_lowCount = 0;
			_highCount = 0;
		}
		else{
			_highCount += _lastDiff;
		}
	}
}

// Analog comparator interrupt
ISR(ANALOG_COMP_vect)
{
	SoftModem::activeObject->demodulate();
}

void SoftModem::recv(void)
{
	uint8_t high;
	
	// Bit logic determination
	if(_highCount > _lowCount){
		_highCount = 0;
		high = 0x80;
	}
	else{
		_lowCount = 0;
		high = 0x00;
	}
	
	// Start bit reception
	if(_recvStat == START_BIT){
		if(!high){
			_recvStat++;
		}
		else{
			goto end_recv;
		}
	}
	// Data bit reception
	else if(_recvStat <= DATA_BIT) {
		_recvBits >>= 1;
		_recvBits |= high;
		_recvStat++;
	}
	// Stop bit reception
	else if(_recvStat == STOP_BIT){
		if(high){
			// Stored in the receive buffer
			uint8_t new_tail = (_recvBufferTail + 1) & (SOFT_MODEM_RX_BUF_SIZE - 1);
			if(new_tail != _recvBufferHead){
				_recvBuffer[_recvBufferTail] = _recvBits;
				_recvBufferTail = new_tail;
			}
			else{
				;// Overrun error detection
			}
		}
		else{
			;// Fleming error detection
		}
		goto end_recv;
	}
	else{
	end_recv:
		_recvStat = INACTIVE;
		TIMSK2 &= ~_BV(OCIE2A);
	}
}

// Timer 2 compare match interrupt A
ISR(TIMER_COMPA_vect)
{
	OCR_A += (uint8_t)TCNT_BIT_PERIOD;
	SoftModem::activeObject->recv();
#if SOFT_MODEM_DEBUG_ENABLE
	*_portLEDReg ^= _portLEDMask;
#endif
}

int SoftModem::available()
{
	return (_recvBufferTail + SOFT_MODEM_RX_BUF_SIZE - _recvBufferHead) & (SOFT_MODEM_RX_BUF_SIZE - 1);
}

int SoftModem::read()
{
	if(_recvBufferHead == _recvBufferTail)
		return -1;
	int d = _recvBuffer[_recvBufferHead];
	_recvBufferHead = (_recvBufferHead + 1) & (SOFT_MODEM_RX_BUF_SIZE - 1);
	return d;
}

int SoftModem::peek()
{
	if(_recvBufferHead == _recvBufferTail)
		return -1;
	return _recvBuffer[_recvBufferHead];
}

void SoftModem::flush()
{
}

void SoftModem::modulate(uint8_t b)
{
	uint8_t cnt,tcnt,tcnt2;
	if(b){
		cnt = (uint8_t)(HIGH_FREQ_CNT);
		tcnt2 = (uint8_t)(TCNT_HIGH_FREQ / 2);
		tcnt = (uint8_t)(TCNT_HIGH_FREQ) - tcnt2;
	}else{
		cnt = (uint8_t)(LOW_FREQ_CNT);
		tcnt2 = (uint8_t)(TCNT_LOW_FREQ / 2);
		tcnt = (uint8_t)(TCNT_LOW_FREQ) - tcnt2;
	}
// cnt = cnt * 2;
			OCR_B = tcnt;

	do {

		cnt--;
		{
			OCR_B = tcnt;
			TCNT = 0;
			TIFR |= _BV(OCF_B);
			while(!(TIFR & _BV(OCF_B)));
		}
		*_txPortReg ^= _txPortMask;
		{
			OCR_B +=tcnt;

			TIFR |= _BV(OCF_B);
			while(!(TIFR & _BV(OCF_B)));
		}
		*_txPortReg ^= _txPortMask;
	} while (cnt);
}

//  Preamble bit before transmission
//  1 start bit (LOW)
//  8 data bits, LSB first
//  1 stop bit (HIGH)
//  ...
//  Postamble bit after transmission

size_t SoftModem::write(const uint8_t *buffer, size_t size)
{
	// To calculate the preamble bit length
	uint16_t cnt = ((micros() - _lastWriteTime) / BIT_PERIOD) + 1;
	if(cnt > MAX_CARRIR_BITS){
		cnt = MAX_CARRIR_BITS;
	}
	// Preamble bit transmission
	for(uint16_t i = 0; i<cnt; i++){
		modulate(HIGH);
	}
	size_t n = size;
	while (size--) {
		uint8_t data = *buffer++;
		// Start bit transmission
		modulate(LOW);
		// Data bit transmission
		for(uint8_t mask = 1; mask; mask <<= 1){
			if(data & mask){
				modulate(HIGH);
			}
			else{
				modulate(LOW);
			}
		}
		// Stop bit transmission
		modulate(HIGH);
		// Serial.println("here1");

	}
	// Postamble bit transmission
	modulate(HIGH);
	_lastWriteTime = micros();
	return n;
}

size_t SoftModem::write(uint8_t data)
{
	return write(&data, 1);
}

void SoftModem::setTxPin(uint8_t txPin) {
	_txPin = txPin;
}

void SoftModem::setRxPins(uint8_t rxPin1, uint8_t rxPin2) {
	_rxPin1 = rxPin1;
	_rxPin2 = rxPin2;
}
