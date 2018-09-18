#include <jni.h>
#include <stdio.h>
#include <string.h>
#include "Application.h"
#include "dwf.h"
HDWF hdwf;				// variable represent a object of device



JNIEXPORT jboolean JNICALL Java_Application_sayHallo(JNIEnv *env, jobject thisObj)
{
	if(!FDwfDeviceOpen(-1, &hdwf))
	{
		return JNI_FALSE;	// device did not connect
	}
	else
	{
		return JNI_TRUE;	// device connected
	}
}




JNIEXPORT void JNICALL Java_Application_sayTschus(JNIEnv *env, jobject thisObj)
{
	FDwfDigitalIOOutputSet(hdwf, 0);				// set output to 0
	FDwfDigitalIOOutputEnableSet(hdwf, 0);			// disable setting digital output
	FDwfAnalogIOChannelNodeSet(hdwf, 0, 0, 0);		// set chanel0(FIXED SUPPLY), node0(enable/disable) to 0
	FDwfAnalogIOEnableSet(hdwf, JNI_FALSE);			// disable setting analog I/O
	FDwfDeviceClose(hdwf);
	FDwfDeviceCloseAll();							// close all instruments
}



JNIEXPORT void JNICALL Java_Application_autoConfig(JNIEnv *env, jobject thisObj)
{
	FDwfDigitalIOReset(hdwf);						//auto default config all DigitalIO instrument parameters
	FDwfAnalogIOReset(hdwf);						// auto default config all AnalogIO instrument parameters
	FDwfDigitalIOOutputEnableSet(hdwf, 65535);		// enable output pin 0-15
	FDwfAnalogIOEnableSet(hdwf, JNI_TRUE);  		// set the master enable switch
}



JNIEXPORT jint JNICALL Java_Application_getInputValue(JNIEnv *env, jobject thisObj)
{
	unsigned int inputValue;
	FDwfDigitalIOStatus(hdwf);						// read DIGITAL IO value from device
	FDwfDigitalIOInputStatus(hdwf, &inputValue);	// return
	return inputValue;
}



JNIEXPORT jint JNICALL Java_Application_getOutputSetValue(JNIEnv *env, jobject thisObj)
{
	unsigned int pfsOutput;
	FDwfDigitalIOOutputGet(hdwf, &pfsOutput);		// returns the currently set output values across all output pins.
	return pfsOutput;
}



JNIEXPORT void JNICALL Java_Application_setOutputValue(JNIEnv *env, jobject thisObj, jint val)
{
	FDwfDigitalIOOutputSet(hdwf, val);
}



JNIEXPORT void JNICALL Java_Application_setVoltageSupply(JNIEnv *env, jobject thisObj, jdouble val2)
{
	FDwfAnalogIOChannelNodeSet(hdwf, 0, 1, val2);	// set chanel0(FIXED SUPPLY), node1(voltage)
}



JNIEXPORT jdouble JNICALL Java_Application_getVoltage(JNIEnv *env, jobject thisObj)
{
	jdouble voltageVal;
	FDwfAnalogIOStatus(hdwf);									// read ANALOG IO value from device
	FDwfAnalogIOChannelNodeStatus(hdwf, 0, 1, &voltageVal);		// return currently voltage
	return voltageVal;
}



JNIEXPORT jdouble JNICALL Java_Application_getCurrent(JNIEnv *env, jobject thisObj)
{
	jdouble currentVal;
	FDwfAnalogIOStatus(hdwf);									// read ANALOG IO value from device
	FDwfAnalogIOChannelNodeStatus(hdwf, 0, 2, &currentVal);		// return currently current
	return currentVal;
}



JNIEXPORT void JNICALL Java_Application_toggleSupply(JNIEnv *env, jobject thisObj, jint val3)
{
	FDwfAnalogIOChannelNodeSet(hdwf, 0, 0, val3);				// enable power pin
}



JNIEXPORT jstring JNICALL Java_Application_getNameSN(JNIEnv *env, jobject thisObj)
{
	int cDevice, i;
	char name[32];
	char sn[32];
	char nameSN[64];
	BOOL fIsInUse;


	FDwfEnum(enumfilterEExplorer, &cDevice);				// finding all ee board which are connecting
	for(i = 0; i < cDevice; i++)
	{
        FDwfEnumDeviceIsOpened(i, &fIsInUse);

		if(fIsInUse)
		{
			FDwfEnumDeviceName (i, name);					// get name of device
			FDwfEnumSN(i, sn);								// get series number of device
			strcpy(nameSN, name);
			strcat(nameSN, " ");
			strcat(nameSN, sn);
			return (*env)->NewStringUTF(env, nameSN);			// Convert the C-string (char*) into JNI String (jstring) and return sn+name of a ee board that has used
        }
    }
}
