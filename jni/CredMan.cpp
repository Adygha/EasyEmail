/**
 * Some assumptions about charcode here are based on that Windows uses unicode UTF-16 little-endian and the
 * Windows-API specifying some data to be in unicode UTF-16.
 */

#ifdef UNICODE
#define tStrCat wcscat_s
#define ElevMain ElevMainW
#else
#define tStrCat strcat_s
#define ElevMain ElevMainA
#endif // UNICODE

#include <Windows.h>
#include "CredMan.h"
#include <wincred.h>
#include <unordered_map>
#include <vector>
#include "EasyJNI.h"

namespace ez {
	jclass JCredClass = nullptr; // Java's credential class
	jmethodID JCredConstr = nullptr; // Java's credential class constructor
	//jfieldID JCredFieldType = nullptr;
	jfieldID JCredFieldTargetName = nullptr; // Java credential's TargetName field
	jfieldID JCredFieldComment = nullptr; // Java credential's Comment field
	jfieldID JCredFieldBytePassword = nullptr; // Java credential's BytePassword field
	//jfieldID JCredFieldPersist = nullptr;
	jfieldID JCredFieldTargetAlias = nullptr; // Java credential's TargetAlias field
	jfieldID JCredFieldUserName = nullptr; // Java credential's UserName field
	jmethodID JCredMethodAttribs = nullptr; // Java credential's Attribs method
	jclass JAttribClass = nullptr; // Java's credential-attrib class
	jmethodID JAttribConstr = nullptr; // Java's credential-attrib class constructor
	jfieldID JAttribFieldKeyword = nullptr; // Java credential-attrib's Keyword field
	jfieldID JAttribFieldValue = nullptr; // Java credential-attrib's Value field
	jclass JNativeExceptionClass = nullptr; // Java's NativeException class
	jmethodID JNativeExceptionConstr = nullptr; // Java's NativeException class constructor

	const TCHAR *MEMORY_NAME = TEXT("SharedMemortEasy"); // The mamory mapping name that this implementation may use (may postfix it with a UUID later)
	const TCHAR *ELEV_FILE_OR_COMMAND = TEXT("rundll32.exe"); // The command/file passed to 'ShellExecuteEx' to run in elevated state
	const TCHAR *ELEV_PARAMS = TEXT("CredMan.dll,ElevMain "); // The unchanged part of the parameters passed to 'ShellExecuteEx' to run in elevated state

	/**
	 * Used to get a new Java NativeException object (to be thrown later by caller) using the specified error-code.
	 * @author	Janty Azmat
	 * @param javaEnv	pointer to the java-environment.
	 * @param errCode	the OS specific error-code.
	 * @returns			the newly created Java NativeException object.
	 */
	jthrowable newJNativeException(JNIEnv *javaEnv, DWORD errCode) {
		TCHAR *tmpErrMsg;
		FormatMessage( // Get the OS's error message
			FORMAT_MESSAGE_ALLOCATE_BUFFER | FORMAT_MESSAGE_FROM_SYSTEM | FORMAT_MESSAGE_IGNORE_INSERTS,
			nullptr, errCode, 0, reinterpret_cast<TCHAR *>(&tmpErrMsg), 0, nullptr
		);
		unsigned tmpPos = lstrlen(tmpErrMsg) - 2; // The possible position of the CR=13 tchar.
		if (tmpErrMsg[tmpPos] == '\r') {
			tmpErrMsg[tmpPos] = 0; // Just to drop the trailing CRLF
		}
		jstring tmpJErrMsg = tStrToJStr(javaEnv, tmpErrMsg);
		LocalFree(tmpErrMsg); // According to documentations
		return static_cast<jthrowable>(javaEnv->NewObject(JNativeExceptionClass, JNativeExceptionConstr, errCode, tmpJErrMsg));
	}

	/**
	 * Used to check if the specified content is previously protected/encrypted by the OS.
	 * @author	Janty Azmat
	 * @param javaEnv					pointer to the java-environment.
	 * @param contAddr					pointer to the start of the content.
	 * @param contByteLen (Optional)	length in byte of the content. If omitted or zero, the content is considered a null-terminated
	 *									'WCHAR*' string, otherwise the content is copied and checked.
	 * @returns							'true' if the content is previously protected/encrypted, otherwise 'false'.
	 */
	bool isContentProtected(JNIEnv *javaEnv, void *contAddr, unsigned contByteLen = 0) {
		WCHAR *tmpCont; // Will hold the content as 'WCHAR*' string
		CRED_PROTECTION_TYPE tmpProtType;
		if (contByteLen) { // In this case, the contenet is not null-terminated (the user should specify)
			unsigned tmpWCharLen = contByteLen / SIZE_WCHAR + 1;
			tmpCont = new WCHAR[tmpWCharLen];
			if (memcpy_s(tmpCont, contByteLen, contAddr, contByteLen)) { // Copy the passed content
				javaEnv->Throw(newJNativeException(javaEnv, ERROR_BAD_ARGUMENTS)); // Closest error-code to both 'memcpy_s' error return
				return false;
			}
			tmpCont[tmpWCharLen - 1] = 0; // Null-terminat it
		} else { // In this case, the contenet is null-terminated WCHAR*/LPWSTR (the user should specify)
			tmpCont = static_cast<WCHAR *>(contAddr); // No need to copy content (since it should be null-terminated in this case)
		}
		if (!CredIsProtectedW(tmpCont, &tmpProtType)) {
			javaEnv->Throw(newJNativeException(javaEnv, GetLastError())); // Closest error-code to both 'memcpy_s' error return
			return false;
		}
		if (contByteLen) { // Only delete if we create it
			delete[] tmpCont;
		}
		return tmpProtType != CRED_PROTECTION_TYPE::CredUnprotected;
	}

	/**
	 * Used to create a Java credential-attribute object based on a Windows CREDENTIAL_ATTRIBUTE.
	 * @author	Janty Azmat
	 * @param javaEnv		pointer to the java-environment.
	 * @param credAttrib	a pointer to Windows CREDENTIAL_ATTRIBUTE.
	 * @returns				a Java credential-attribute version of the Windows CREDENTIAL_ATTRIBUTE.
	 */
	jobject wCredAttrToJCredAttr(JNIEnv *javaEnv, const PCREDENTIAL_ATTRIBUTE credAttrib) {
		jbyteArray tmpVal = javaEnv->NewByteArray(credAttrib->ValueSize);
		javaEnv->SetByteArrayRegion(tmpVal, 0, credAttrib->ValueSize, reinterpret_cast<const jbyte *>(credAttrib->Value));
		return javaEnv->NewObject(JAttribClass, JAttribConstr, tStrToJStr(javaEnv, credAttrib->Keyword), tmpVal);
	}

	/**
	 * Used to create a Windows CREDENTIAL_ATTRIBUTE based on a Java credential-attribute object ('CredFree' should be used on
	 * the CREDENTIAL that holds the resulted CREDENTIAL_ATTRIBUTE when done with it).
	 * @author	Janty Azmat
	 * @param javaEnv			pointer to the java-environment.
	 * @param inJCredAttr		a Java credential-attribute object pointer.
	 * @param [out] outWinCredAttr	outputs a Windows CREDENTIAL_ATTRIBUTE version of the Java credential-attribute.
	 */
	void jCredAttrToWCredAttr(JNIEnv *javaEnv, jobject inJCredAttr, PCREDENTIAL_ATTRIBUTE outWinCredAttr) {
		outWinCredAttr->Keyword = jStrToTStr(javaEnv, static_cast<jstring>(javaEnv->GetObjectField(inJCredAttr, JAttribFieldKeyword)));
		jbyteArray tmpValArr = static_cast<jbyteArray>(javaEnv->GetObjectField(inJCredAttr, JAttribFieldValue));
		outWinCredAttr->ValueSize = javaEnv->GetArrayLength(tmpValArr);
		outWinCredAttr->Value = reinterpret_cast<LPBYTE>(javaEnv->GetByteArrayElements(tmpValArr, JNI_FALSE)); // Elements will/should be released later with 'CredFree'
		outWinCredAttr->Flags = 0;
	}

	/**
	 * Used to create a Java credential object based on a Windows CREDENTIAL.
	 * @author	Janty Azmat
	 * @param javaEnv		pointer to the java-environment.
	 * @param credStruct	a pointer to Windows CREDENTIAL.
	 * @returns				a Java credential version of the Windows CREDENTIAL.
	 */
	jobject wCredToJCred(JNIEnv *javaEnv, const PCREDENTIAL credStruct) {
		jbyteArray tmpPass = javaEnv->NewByteArray(credStruct->CredentialBlobSize);
		ULARGE_INTEGER tmpDateTime;
		jobjectArray tmpAttribs = javaEnv->NewObjectArray(credStruct->AttributeCount, JAttribClass, nullptr);
		for (DWORD i = 0; i < credStruct->AttributeCount; i++) {
			javaEnv->SetObjectArrayElement(tmpAttribs, i, wCredAttrToJCredAttr(javaEnv, &credStruct->Attributes[i]));
		}
		javaEnv->SetByteArrayRegion(tmpPass, 0, credStruct->CredentialBlobSize, reinterpret_cast<const jbyte *>(credStruct->CredentialBlob));
		tmpDateTime.LowPart = credStruct->LastWritten.dwLowDateTime;
		tmpDateTime.HighPart = credStruct->LastWritten.dwHighDateTime;
		return javaEnv->NewObject(
			JCredClass, JCredConstr, isContentProtected(javaEnv, credStruct->CredentialBlob, credStruct->CredentialBlobSize),
			tStrToJStr(javaEnv, credStruct->TargetName), tStrToJStr(javaEnv, credStruct->Comment), tmpDateTime, tmpPass, tmpAttribs,
			tStrToJStr(javaEnv, credStruct->TargetAlias), tStrToJStr(javaEnv, credStruct->UserName)
		);
	}

	/**
	 * Used to create a Windows CREDENTIAL based on a Java credential object ('CredFree' should be used on the resulted CREDENTIAL when done with it).
	 * @author	Janty Azmat
	 * @param javaEnv	pointer to the java-environment.
	 * @param winCred	a Java credential object pointer.
	 * @returns			a Windows CREDENTIAL version of the the Java credential.
	 */
	PCREDENTIAL jCredToWCred(JNIEnv *javaEnv, jobject winCred) {
		PCREDENTIAL outCred = new CREDENTIAL({0});
		//outCred->Type = javaEnv->GetIntField(winCred, JCredFieldType);
		outCred->Type = CRED_TYPE_GENERIC;
		outCred->TargetName = jStrToTStr(javaEnv, static_cast<jstring>(javaEnv->GetObjectField(winCred, JCredFieldTargetName)));
		outCred->Comment = jStrToTStr(javaEnv, static_cast<jstring>(javaEnv->GetObjectField(winCred, JCredFieldComment)));
		jbyteArray tmpPassArr = static_cast<jbyteArray>(javaEnv->GetObjectField(winCred, JCredFieldBytePassword));
		outCred->CredentialBlobSize = javaEnv->GetArrayLength(tmpPassArr);
		outCred->CredentialBlob = reinterpret_cast<LPBYTE>(javaEnv->GetByteArrayElements(tmpPassArr, JNI_FALSE));
		//outCred->Persist = javaEnv->GetIntField(winCred, JCredFieldPersist);
		outCred->Persist = CRED_PERSIST_LOCAL_MACHINE;
		outCred->TargetAlias = jStrToTStr(javaEnv, static_cast<jstring>(javaEnv->GetObjectField(winCred, JCredFieldTargetAlias)));
		outCred->UserName = jStrToTStr(javaEnv, static_cast<jstring>(javaEnv->GetObjectField(winCred, JCredFieldUserName)));
		jobjectArray tmpAttrArr = static_cast<jobjectArray>(javaEnv->CallObjectMethod(winCred, JCredMethodAttribs));
		outCred->AttributeCount = javaEnv->GetArrayLength(tmpAttrArr);
		outCred->Attributes = new CREDENTIAL_ATTRIBUTE[outCred->AttributeCount];
		for (DWORD i = 0; i < outCred->AttributeCount; i++) {
			jCredAttrToWCredAttr(javaEnv, javaEnv->GetObjectArrayElement(tmpAttrArr, i), &outCred->Attributes[i]);
		}
		return outCred;
	}

	/**
	 * Used to process (protect/unprotect) the specified content. When used with a mapped/joined memory region, only the first two
	 * parameters should be used (in this case the result will be outputted to the same mapped/joined memory region when success; or
	 * error data will be outputted to that same memory region when failed). In this case, as input, the memory region should be one
	 * unsigned-int that holds the size of the content and the rest is the content, while as output, the same on success; or on failure:
	 * the first unsigned-int will hold the zero value to indicate an error and another unsigned-int that holds the error-code. To get
	 * the required size (in bytes) of the processed data beforehand, only the last parameter should be omitted.
	 * @author	Janty Azmat
	 * @param isProtect								'true' to protect/encrypt the content, or 'false' to unprotect/decrypt.
	 * @param inContAddr							a pointer to the start of the content (whether string data or mapped memory region).
	 * @param inMaxBytes (Optional)					specifies the maximum number of bytes to protect (if omitted or zero, the content
	 *												pointer is considered a mapped memory region).
	 * @param [out] outProcessedCont (Optional)		outputs the required processed data (can be omitted alone to get the required size
	 *												in bytes of the processed data beforehand).
	 * @returns										the size (in bytes) of the processed data on success, or zero on failure. To get
	 * 												specific error data, 'GetLastError' can be used.
	 */
	unsigned processContent(bool isProtect, void *inContAddr, unsigned inMaxBytes = 0, jbyte *outProcessedCont = nullptr) {
		unsigned *tmpPtrMapMemLen = nullptr; // Pointer to (will point to) content length stored in mapped memory
		DWORD tmpMaxWCharLen, tmpNeededWCharLen = 0, tmpMaxByteLen, outNeededByteLen;
		if (inMaxBytes) {
			tmpMaxByteLen = inMaxBytes;
			tmpMaxWCharLen = tmpMaxByteLen / SIZE_WCHAR;
		} else {
			tmpPtrMapMemLen = static_cast<unsigned int *>(inContAddr); // Make it poit to content length stored in mapped memory
			tmpMaxByteLen = *tmpPtrMapMemLen;
			tmpMaxWCharLen = tmpMaxByteLen / SIZE_WCHAR;
			inContAddr = (tmpPtrMapMemLen + 1);
		}
		WCHAR *tmpCont = new WCHAR[static_cast<size_t>(tmpMaxWCharLen) + 1];
		if (memcpy_s(tmpCont, tmpMaxByteLen, inContAddr, tmpMaxByteLen)) { // Copy without the terminating null WCHAR
			if (!inMaxBytes) {
				*tmpPtrMapMemLen = 0; // To indicate an error for later
				*static_cast<unsigned int *>(inContAddr) = ERROR_BAD_ARGUMENTS; // Since 'memcpy_s' is not part of the Windows API, we fill owr own error to get it later
			} else {
				SetLastError(ERROR_BAD_ARGUMENTS); // Since 'memcpy_s' is not part of the Windows API, we put owr own error to be get later
			}
			delete[] tmpCont;
			return 0;
		}
		tmpCont[tmpMaxWCharLen] = 0;
		if (!(isProtect
			  ? CredProtectW(TRUE, tmpCont, tmpMaxWCharLen, nullptr, &tmpNeededWCharLen, nullptr)
			  : CredUnprotectW(TRUE, tmpCont, tmpMaxWCharLen, nullptr, &tmpNeededWCharLen)
			  ) && GetLastError() != ERROR_INSUFFICIENT_BUFFER) {
			if (!inMaxBytes) {
				*tmpPtrMapMemLen = 0; // To indicate an error for later
				*static_cast<unsigned int *>(inContAddr) = GetLastError(); // Fill the error-code
			}
			delete[] tmpCont;
			return 0;
		}
		outNeededByteLen = tmpNeededWCharLen * SIZE_WCHAR - (isProtect ? SIZE_WCHAR : 0); // The needed byte length (without the terminating null WCHAR when protecting)
		if (outProcessedCont) { // Here, we are required to fill 'outProtCont' with protected content
			WCHAR *tmpProcessedStr = new WCHAR[tmpNeededWCharLen]; // Will hold the protected content as WCHAR string (unicode/utf-16LE)
			if (!(isProtect
				  ? CredProtectW(TRUE, tmpCont, tmpMaxWCharLen, tmpProcessedStr, &tmpNeededWCharLen, nullptr)
				  : CredUnprotectW(TRUE, tmpCont, tmpMaxWCharLen, tmpProcessedStr, &tmpNeededWCharLen))) {
				delete[] tmpCont;
				return 0;
			}
			if (memcpy_s(outProcessedCont, outNeededByteLen, tmpProcessedStr, outNeededByteLen)) { // Copy without the terminating null WCHAR
				SetLastError(ERROR_BAD_ARGUMENTS); // Since 'memcpy_s' is not part of the Windows API, we put owr own error to be get later
				delete[] tmpCont;
				return 0;
			}
			delete[] tmpProcessedStr;
		} else if (!inMaxBytes) { // Here, filling the protected data in 'inContAddr' is required
			WCHAR *tmpProcessedStr = static_cast<WCHAR *>(inContAddr); // Will hold the protected content as WCHAR string (unicode/utf-16LE)
			*tmpPtrMapMemLen = outNeededByteLen; // Here, we but the byte length of the protected content in the mapped memory
			if (!(isProtect
					? CredProtectW(TRUE, tmpCont, tmpMaxWCharLen, tmpProcessedStr, &tmpNeededWCharLen, nullptr)
					: CredUnprotectW(TRUE, tmpCont, tmpMaxWCharLen, tmpProcessedStr, &tmpNeededWCharLen))) {
				*tmpPtrMapMemLen = 0; // To indicate an error for later
				*static_cast<unsigned int *>(inContAddr) = GetLastError(); // Fill the error-code
				delete[] tmpCont;
				return 0;
			}
		}
		delete[] tmpCont;
		return outNeededByteLen;
	}

	/**
	 * Used to run a process (and wait for it to finish) in an elevated/administrator state specifing a command-parameter (the two commands
	 * for now are 'protect' and 'unprotect').
	 * @author	Janty Azmat
	 * @param javaEnv			pointer to the java-environment.
	 * @param commandParams		a command to pass to the elevated process.
	 */
	void runElevated(JNIEnv *javaEnv, const TCHAR *commandParams) {
		unsigned tmpLen = GetFullPathName(TEXT(".\\"), 0, nullptr, nullptr); // Incluses terminating null character.
		if (!tmpLen) {
			javaEnv->Throw(newJNativeException(javaEnv, GetLastError()));
			return;
		}
		tmpLen += lstrlen(ELEV_PARAMS) + (commandParams ? lstrlen(commandParams) : 0);
		TCHAR *tmpParams = new TCHAR[tmpLen];
		if (!GetFullPathName(TEXT(".\\"), tmpLen, tmpParams, nullptr)) {
			delete[] tmpParams;
			javaEnv->Throw(newJNativeException(javaEnv, GetLastError()));
			return;
		}
		tStrCat(tmpParams, tmpLen, ELEV_PARAMS);
		if (commandParams) {
			tStrCat(tmpParams, tmpLen, commandParams);
		}
		SHELLEXECUTEINFO tmpShInfo = {0};
		tmpShInfo.cbSize = sizeof(tmpShInfo);
		tmpShInfo.fMask = SEE_MASK_NOCLOSEPROCESS;
		tmpShInfo.hwnd = 0;
		tmpShInfo.lpVerb = TEXT("runas");
		tmpShInfo.lpFile = ELEV_FILE_OR_COMMAND;
		tmpShInfo.lpParameters = tmpParams;
		tmpShInfo.lpDirectory = nullptr;
		tmpShInfo.nShow = SW_SHOWNA;
		tmpShInfo.hInstApp = nullptr;
		if (ShellExecuteEx(&tmpShInfo) && tmpShInfo.hProcess) {
			WaitForSingleObject(tmpShInfo.hProcess, INFINITE);
			CloseHandle(tmpShInfo.hProcess);
		} else {
			javaEnv->Throw(newJNativeException(javaEnv, GetLastError()));
		}
		delete[] tmpParams;
	}
}

/**
 * The Java VM calls 'JNI_OnLoad' when this native library is loaded.
 * @author	Janty Azmat
 * @param javaVm		The Java virtual machine instance.
 * @param parameter2	reserved.
 * @returns				the JNI version needed by the native library.
 */
jint JNICALL JNI_OnLoad(JavaVM *javaVm, void *) {
	JNIEnv *tmpEnv;
	javaVm->AttachCurrentThread(reinterpret_cast<void **>(&tmpEnv), nullptr); // Main thread is already attached (just to get original JNIEnv)
	ez::JCredClass = static_cast<jclass>(tmpEnv->NewGlobalRef(tmpEnv->FindClass("Lmodel/CredentialManager$WindowsCredential;")));
	ez::JCredConstr = tmpEnv->GetMethodID(
		ez::JCredClass, "<init>", "(ZLjava/lang/String;Ljava/lang/String;J[B[Lmodel/CredentialManager$Attribute;Ljava/lang/String;Ljava/lang/String;)V");
	//ez::JCredFieldType = tmpEnv->GetFieldID(ez::JCredClass, "meType", "I");
	ez::JCredFieldTargetName = tmpEnv->GetFieldID(ez::JCredClass, "meTargetName", "Ljava/lang/String;");
	ez::JCredFieldComment = tmpEnv->GetFieldID(ez::JCredClass, "meComment", "Ljava/lang/String;");
	ez::JCredFieldBytePassword = tmpEnv->GetFieldID(ez::JCredClass, "meBytePassword", "[B");
	//ez::JCredFieldPersist = tmpEnv->GetFieldID(ez::JCredClass, "mePersist", "I");
	ez::JCredFieldTargetAlias = tmpEnv->GetFieldID(ez::JCredClass, "meTargetAlias", "Ljava/lang/String;");
	ez::JCredFieldUserName = tmpEnv->GetFieldID(ez::JCredClass, "meUserName", "Ljava/lang/String;");
	ez::JCredMethodAttribs = tmpEnv->GetMethodID(ez::JCredClass, "getAttributes", "()[Lmodel/CredentialManager$Attribute;");
	ez::JAttribClass = static_cast<jclass>(tmpEnv->NewGlobalRef(tmpEnv->FindClass("Lmodel/CredentialManager$Attribute;")));
	ez::JAttribConstr = tmpEnv->GetMethodID(ez::JAttribClass, "<init>", "(Ljava/lang/String;[B)V");
	ez::JAttribFieldKeyword = tmpEnv->GetFieldID(ez::JAttribClass, "meKeyword", "Ljava/lang/String;");
	ez::JAttribFieldValue = tmpEnv->GetFieldID(ez::JAttribClass, "meValue", "[B");
	ez::JNativeExceptionClass = static_cast<jclass>(tmpEnv->NewGlobalRef(tmpEnv->FindClass("Lmodel/CredentialManager$NativeException;")));
	ez::JNativeExceptionConstr = tmpEnv->GetMethodID(ez::JNativeExceptionClass, "<init>", "(ILjava/lang/String;)V");
	return tmpEnv->GetVersion(); // TODO: Change to mach minimum required.
}

//void JNICALL JNI_OnUnload(JavaVM *javaVm, void *) { // Wasn't being called
//	JNIEnv *tmpEnv;
//	javaVm->AttachCurrentThread(reinterpret_cast<void **>(&tmpEnv), nullptr); // Main thread is already attached (just to get original JNIEnv)
//	tmpEnv->DeleteGlobalRef(ez::JCredClass);
//	ez::JCredClass = nullptr;
//	ez::JCredConstr = nullptr;
//	//ez::JCredFieldType = nullptr;
//	ez::JCredFieldTargetName = nullptr;
//	ez::JCredFieldComment = nullptr;
//	ez::JCredFieldBytePassword = nullptr;
//	//ez::JCredFieldPersist = nullptr;
//	ez::JCredFieldTargetAlias = nullptr;
//	ez::JCredFieldUserName = nullptr;
//	tmpEnv->DeleteGlobalRef(ez::JAttribClass);
//	ez::JAttribClass = nullptr;
//	ez::JAttribConstr = nullptr;
//	tmpEnv->DeleteGlobalRef(ez::JNativeExceptionClass);
//	ez::JNativeExceptionClass = nullptr;
//	ez::JNativeExceptionConstr = nullptr;
//}

/**
 * The 'rundll32.exe' (may change later) calls ElevMain, on request, to run a specific command in an elevated/administrator state.
 * @author	Janty Azmat
 * @param ownerWindow	the window handle (if any) that should be used as the owner window for any windows created here.
 * @param myInstHdl		this DLL's instance handl.
 * @param cmdLine		the passed command line (will hold 'protect' or 'unprotect' for now).
 * @param howShow		specified how windows created here (if any) should be displayed.
 */
void CALLBACK ElevMain(HWND ownerWindow, HINSTANCE myInstHdl, LPTSTR cmdLine, int howShow) {
	HANDLE tmpMemHandle;
	void *tmpMemPos = ez::memoryJoin(tmpMemHandle, ez::MEMORY_NAME);
	if (!lstrcmp(cmdLine, TEXT("protect"))) {
		ez::processContent(true, tmpMemPos);
	} else if (!lstrcmp(cmdLine, TEXT("unprotect"))) {
		ez::processContent(false, tmpMemPos);
	} else {
		// TODO: What error.
	}
	ez::memoryUnjoin(tmpMemPos, tmpMemHandle);
}

void JNICALL Java_model_CredentialManager_unloadLibrary(JNIEnv *javaEnv, jclass) {
	javaEnv->DeleteGlobalRef(ez::JCredClass);
	javaEnv->DeleteGlobalRef(ez::JAttribClass);
	javaEnv->DeleteGlobalRef(ez::JNativeExceptionClass);
}

jobjectArray JNICALL Java_model_CredentialManager_getCreds(JNIEnv *javaEnv, jclass, jstring credFilter, jboolean isAll) {
	TCHAR *tmpFilter = credFilter ? ez::jStrToTStr(javaEnv, credFilter) : nullptr;
	DWORD tmpCnt = 0;
	PCREDENTIAL *tmpCreds;
#pragma warning(suppress:6388)
	BOOL tmpIsOk = CredEnumerate(isAll ? nullptr : tmpFilter, isAll ? CRED_ENUMERATE_ALL_CREDENTIALS : 0, &tmpCnt, &tmpCreds);
	jobjectArray outCreds = javaEnv->NewObjectArray(tmpCnt, ez::JCredClass, nullptr);
	delete[] tmpFilter;
	if (tmpIsOk) {
		for (DWORD i = 0; i < tmpCnt; i++) {
			javaEnv->SetObjectArrayElement(outCreds, i, ez::wCredToJCred(javaEnv, tmpCreds[i]));
		}
	} else {
		javaEnv->Throw(ez::newJNativeException(javaEnv, GetLastError()));
	}
	CredFree(tmpCreds);
	return outCreds;
}

jobject JNICALL Java_model_CredentialManager_getCred(JNIEnv *javaEnv, jclass, jstring credTarget, jint credType) {
	TCHAR *tmpTarg = ez::jStrToTStr(javaEnv, credTarget);
	PCREDENTIAL tmpCred;
	jobject outCred = nullptr;
	if (CredRead(tmpTarg, credType, 0, &tmpCred)) {
		outCred = ez::wCredToJCred(javaEnv, tmpCred);
		CredFree(tmpCred);
	} else {
		javaEnv->Throw(ez::newJNativeException(javaEnv, GetLastError()));
	}
	delete[] tmpTarg;
	return outCred;
}

void JNICALL Java_model_CredentialManager_saveCred(JNIEnv *javaEnv, jclass, jobject winCred) {
	PCREDENTIAL tmpWriteCred = ez::jCredToWCred(javaEnv, winCred);
	//if (!CredWrite(tmpWriteCred, CRED_PRESERVE_CREDENTIAL_BLOB)) {
	if (!CredWrite(tmpWriteCred, 0)) {
		javaEnv->Throw(ez::newJNativeException(javaEnv, GetLastError()));
	}
	CredFree(tmpWriteCred);
}

void JNICALL Java_model_CredentialManager_deleteCred(JNIEnv *javaEnv, jclass, jstring credTarget, jint credType) {
	TCHAR *tmpTarg = ez::jStrToTStr(javaEnv, credTarget);
	if (!CredDelete(tmpTarg, credType, 0)) {
		javaEnv->Throw(ez::newJNativeException(javaEnv, GetLastError()));
	}
	delete[] tmpTarg;
}

jbyteArray JNICALL Java_model_CredentialManager_processBlob(JNIEnv *javaEnv, jclass, jbyteArray theBlob, jboolean isProtect, jboolean isEleveted) {
	jbyte *tmpCont = javaEnv->GetByteArrayElements(theBlob, JNI_FALSE);
	jbyteArray outBlob; // The array to be returned
	unsigned tmpByteLen = javaEnv->GetArrayLength(theBlob); // Length of original content in bytes
	unsigned tmpProcessedByteLen;
	if (isProtect) {
		tmpProcessedByteLen = ez::processContent(true, tmpCont, tmpByteLen); // Length of processed content in bytes
		if (!tmpProcessedByteLen) {
			javaEnv->ReleaseByteArrayElements(theBlob, tmpCont, JNI_ABORT);
			javaEnv->Throw(ez::newJNativeException(javaEnv, GetLastError()));
			return nullptr;
		}
	}
	if (isEleveted) {
		HANDLE tmpMemHandle;
		void *tmpMemPos = ez::memoryMap(// Map memory with size (+1 for null WCHAR)
										tmpMemHandle, sizeof(unsigned int) + (isProtect ? tmpProcessedByteLen : tmpByteLen) + ez::SIZE_WCHAR, ez::MEMORY_NAME);
		if (!tmpMemPos) {
			javaEnv->ReleaseByteArrayElements(theBlob, tmpCont, JNI_ABORT);
			javaEnv->Throw(ez::newJNativeException(javaEnv, GetLastError()));
			return nullptr;
		}
		*static_cast<unsigned int *>(tmpMemPos) = tmpByteLen; // Put the length in bytes of the content in the mapped memory
		void *tmpContPos = (static_cast<unsigned int *>(tmpMemPos) + 1); // The content position in the mapped memory (in a pushed position)
		if (memcpy_s(tmpContPos, tmpByteLen, tmpCont, tmpByteLen)) { // Copy content to mapped memory (to pushed position)
			javaEnv->ReleaseByteArrayElements(theBlob, tmpCont, JNI_ABORT);
			javaEnv->Throw(ez::newJNativeException(javaEnv, ERROR_BAD_ARGUMENTS)); // Closest error-code to both 'memcpy_s' error return
			return nullptr;
		}
		ez::runElevated(javaEnv, isProtect ? TEXT("protect") : TEXT("unprotect")); // Run in elevated state with 'protect'/'unprotect' command
		tmpProcessedByteLen = *static_cast<unsigned int *>(tmpMemPos); // extract the result from mapped memory after running elevated is done
		if (!tmpProcessedByteLen) { // In case an error happened while in elevated state
			unsigned tmpErrCode = *static_cast<unsigned int *>(tmpContPos); // Extract the error-code
			ez::memoryUnmap(tmpMemPos, tmpMemHandle); // Unmap the mapped memory
			javaEnv->ReleaseByteArrayElements(theBlob, tmpCont, JNI_ABORT);
			javaEnv->Throw(ez::newJNativeException(javaEnv, tmpErrCode));
			return nullptr;
		}
		outBlob = javaEnv->NewByteArray(tmpProcessedByteLen);
		javaEnv->SetByteArrayRegion(outBlob, 0, tmpProcessedByteLen, static_cast<jbyte *>(tmpContPos)); // Fill the array to be returned
		ez::memoryUnmap(tmpMemPos, tmpMemHandle); // Unmap the mapped memory
	} else {
		if (!isProtect) {
			tmpProcessedByteLen = ez::processContent(false, tmpCont, tmpByteLen); // Length of processed content in bytes
			if (!tmpProcessedByteLen) {
				javaEnv->ReleaseByteArrayElements(theBlob, tmpCont, JNI_ABORT);
				javaEnv->Throw(ez::newJNativeException(javaEnv, GetLastError()));
				return nullptr;
			}
		}
		outBlob = javaEnv->NewByteArray(tmpProcessedByteLen);
		jbyte *tmpProcessedCont = new jbyte[tmpProcessedByteLen]; // The processed content
		if (!ez::processContent(isProtect, tmpCont, tmpByteLen, tmpProcessedCont)) {
			delete[] tmpProcessedCont;
			javaEnv->ReleaseByteArrayElements(theBlob, tmpCont, JNI_ABORT);
			javaEnv->Throw(ez::newJNativeException(javaEnv, GetLastError()));
			return nullptr;
		}
		javaEnv->SetByteArrayRegion(outBlob, 0, tmpProcessedByteLen, tmpProcessedCont); // Fill the array to be returned
		delete[] tmpProcessedCont;
	}
	javaEnv->ReleaseByteArrayElements(theBlob, tmpCont, JNI_ABORT);
	return outBlob;
}

//unsigned glbIII = 1;
//FILE *glbFile = nullptr;
//BOOL DllMain(_In_ HINSTANCE hinstDLL, _In_ DWORD fdwReason, _In_ LPVOID lpvReserved) {
//	if (fdwReason == DLL_PROCESS_ATTACH) {
//		char tmpStr[256]{0};
//		bool tmpIsElev = ez::isCurrentProcessElevated();
//		sprintf_s(tmpStr, 256, "D:\\tmp\\[%d]output[%s].txt", glbIII, tmpIsElev ? "true" : "false");
//		fopen_s(&glbFile, tmpStr, "w");
//		GetModuleFileNameA(nullptr, tmpStr, 256);
//		fprintf_s(glbFile, "Path: [%s]\n", tmpStr);
//		//_fullpath(tmpStr, ".\\", 256);
//		fprintf_s(glbFile, "Path: [%s]\n", _fullpath(nullptr, ".\\", 0));
//	} else if (fdwReason == DLL_PROCESS_DETACH) {
//		fprintf_s(glbFile, ">>>>>>>>>>>>>>>>>>>>>>>>> hinstDLL: [%p], fdwReason: [%u]. lpvReserved: [%p]. glbIII [%d].\n", hinstDLL, fdwReason, lpvReserved, glbIII++);
//		fclose(glbFile);
//	}
//	if (glbFile) {
//		fprintf_s(glbFile, ">>>>>>>>>>>>>>>>>>>>>>>>> hinstDLL: [%p], fdwReason: [%u]. lpvReserved: [%p]. glbIII [%d].\n", hinstDLL, fdwReason, lpvReserved, glbIII++);
//		fflush(glbFile);
//	}
//	return TRUE;
//}

#undef ElevMain
#undef tStrCat
