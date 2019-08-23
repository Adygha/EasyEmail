/* DO NOT EDIT THIS FILE - it is machine generated */
#include <jni.h>
/* Header for class model_SystemCredentialsManager */

#ifndef _Included_model_SystemCredentialsManager
#define _Included_model_SystemCredentialsManager
#ifdef __cplusplus
extern "C" {
#endif
	/*
	 * Class:     model_SystemCredentialsManager
	 * Method:    getCreds
	 * Signature: (Ljava/lang/String;Z)[Lmodel/SystemCredentialsManager/WindowsCredential;
	 */
	JNIEXPORT jobjectArray JNICALL Java_model_SystemCredentialsManager_getCreds(JNIEnv *, jclass, jstring, jboolean);

	/*
	 * Class:     model_SystemCredentialsManager
	 * Method:    getCred
	 * Signature: (Ljava/lang/String;I)Lmodel/SystemCredentialsManager/WindowsCredential;
	 */
	JNIEXPORT jobject JNICALL Java_model_SystemCredentialsManager_getCred(JNIEnv *, jclass, jstring, jint);

	/*
	 * Class:     model_SystemCredentialsManager
	 * Method:    newCred
	 * Signature: (Lmodel/SystemCredentialsManager/WindowsCredential;)I
	 */
	JNIEXPORT jint JNICALL Java_model_SystemCredentialsManager_newCred(JNIEnv *, jclass, jobject);

	/*
	 * Class:     model_SystemCredentialsManager
	 * Method:    updateCred
	 * Signature: (Lmodel/SystemCredentialsManager/WindowsCredential;)I
	 */
	JNIEXPORT jint JNICALL Java_model_SystemCredentialsManager_updateCred(JNIEnv *, jclass, jobject);

#ifdef __cplusplus
}
#endif
#endif
