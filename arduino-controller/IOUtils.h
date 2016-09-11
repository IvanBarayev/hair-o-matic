#ifndef IOUTILS_INCLUDED
#define IOUTILS_INCLUDED
#include <EEPROM.h>
class IOUtils {
#define NUM_READS 100
	
public:
	static void saveEepromInt(int value, int position) {
		int a = value / 256;
		int b = value % 256;

		EEPROM.write(position, a);
		EEPROM.write(position + 1, b);
	}

	static int readEepromInt(int position) {
		int a = EEPROM.read(position);
		int b = EEPROM.read(position + 1);

		return a * 256 + b;
	}

	static void bubbleSort(int* array, int size)
	{
		int i, j, swap;
		for (i = 0 ; i < ( size - 1 ); i++)
		{
			for (j = 0 ; j < size - i - 1; j++)
			{
				if (array[j] > array[j + 1])
				{
					swap = array[j];
					array[j] = array[j + 1];
					array[j + 1] = swap;
				}
			}
		}
	}

	static float scaledMode(int* array, int scale) {
		float returnval = 0;
		for (int i = NUM_READS / 2 - (scale / 2); i < (NUM_READS / 2 + (scale / 2)); i++)
			returnval += array[i];

		return returnval / scale;
	}

	static float analogReadFiltered(int sensorpin) {
		int sortedValues[NUM_READS];
		for (int i = 0; i < NUM_READS; i++) {
			int value = analogRead(sensorpin);
			sortedValues[i] = value;
		}

		bubbleSort(sortedValues, NUM_READS);

		float mode = scaledMode(sortedValues, 10);
		if (mode < 1 && mode > 0)
			return 1;
		
		return mode;
	}
};
#endif
