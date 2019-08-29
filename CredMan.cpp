/**
 * Some assumptions about charcode here are based on that Windows uses unicode UTF-16 little-endian and the
 * Windows-API specifying some data to be in unicode UTF-16.
 */

#ifdef UNICODE
#define tStrCopy wcscpy_s
#define tStrCat wcscat_s
#define ElevMain ElevMainW
#define GetJavaStringChars GetStringChars
#define ReleaseJavaStringChars(AA, BB) ReleaseStringChars(AA, reinterpret_cast<const jchar *>(BB))
#else
#define tStrCopy strcpy_s
#define tStrCat strcat_s
#define ElevMain ElevMainA
#define GetJavaStringChars GetStringUTFChars
#define ReleaseJavaStringChars ReleaseStringUTFChars
#endif // UNICODE

#include <Windows.h>
#include "CredMan.h"
#include <wincred.h>
#include <unordered_map>
#include <vector>

namespace ez {
	jclass JCredClass = nullptr;
	jmethodID JCredConstr = nullptr;
	jfieldID JCredFieldType = nullptr;
	jfieldID JCredFieldTargetName = nullptr;
	jfieldID JCredFieldComment = nullptr;
	jfieldID JCredFieldBytePassword = nullptr;
	jfieldID JCredFieldPersist = nullptr;
	jfieldID JCredFieldTargetAlias = nullptr;
	jfieldID JCredFieldUserName = nullptr;
	jmethodID JCredMethodByteAttribs = nullptr;
	jclass JAttribClass = nullptr;
	jmethodID JAttribConstr = nullptr;
	jmethodID JAttribMethodKey = nullptr;
	jmethodID JAttribMethodValue = nullptr;

	const unsigned TCHAR_SIZE = sizeof(TCHAR);
	const TCHAR *MEMORY_NAME = TEXT("SharedMemortEasy");
	const TCHAR *ELEV_PARAMS = TEXT("C:\\PubDtop\\Projects\\Java\\Git\\EasyEmail\\CredMan.dll,ElevMain "); // TODO: Change it.

	struct DeepCredAttrib : CREDENTIAL_ATTRIBUTE {
		DeepCredAttrib() : CREDENTIAL_ATTRIBUTE{0} {};
		//DeepCredAttrib(DeepCredAttrib &) = delete;
		//DeepCredAttrib(DeepCredAttrib &&) = delete;
		DeepCredAttrib(CREDENTIAL_ATTRIBUTE &origAttrib) : DeepCredAttrib(&origAttrib) {}
		DeepCredAttrib(PCREDENTIAL_ATTRIBUTE origAttrib) : CREDENTIAL_ATTRIBUTE(*origAttrib) {}

		DeepCredAttrib *deepCopy(void *&inOutNextAddr) {
			DeepCredAttrib *outCredAttr = static_cast<DeepCredAttrib *>(inOutNextAddr);
			unsigned tmpSz;
			*(static_cast<DeepCredAttrib *>(inOutNextAddr)) = *this; // Copy the strtucture content
			//outCredAttr->Flags = 0;
			inOutNextAddr = (this + 1); // Seek to after strtucture content
			tmpSz = (lstrlen(this->Keyword) + 1) * TCHAR_SIZE;
			memcpy_s(inOutNextAddr, tmpSz, this->Keyword, tmpSz); // Copy the keyword after strtucture content
			outCredAttr->Keyword = static_cast<TCHAR *>(inOutNextAddr); // Now the copy points to it
			inOutNextAddr = ((static_cast<char *>(inOutNextAddr)) + tmpSz); // Update seek
			memcpy_s(inOutNextAddr, this->ValueSize, this->Value, this->ValueSize); // Then copy the value after that
			outCredAttr->Value = static_cast<BYTE *>(inOutNextAddr); // Now the copy points to it
			inOutNextAddr = ((static_cast<char *>(inOutNextAddr)) + this->ValueSize); // Final seek update for output
		}

		unsigned getDeepSize() {
			return static_cast<unsigned>(sizeof(DeepCredAttrib)) + ((lstrlen(this->Keyword) + 1) * TCHAR_SIZE) + this->ValueSize;
		}
	};

	struct DeepCred : CREDENTIAL {
		DeepCred() : CREDENTIAL{0} {}
		//DeepCred(DeepCred &) = delete;
		//DeepCred(DeepCred &&) = delete;

		DeepCred(CREDENTIAL &origCred, bool doFreeOrig = true) : DeepCred(&origCred, doFreeOrig) {}

		DeepCred(PCREDENTIAL origCred, bool doFreeOrig = true) : CREDENTIAL(*origCred) {
			DeepCredAttrib *tmpAttribs = new DeepCredAttrib[origCred->AttributeCount];
			for (unsigned i = 0; i < origCred->AttributeCount; i++) {
				tmpAttribs[i] = new DeepCredAttrib(this->Attributes[i]);
			}
			this->Attributes = tmpAttribs;
			if (doFreeOrig) {
				CredFree(origCred);
			}
		}

		~DeepCred() {
			delete[] this->Attributes;	//
			this->AttributeCount = 0;	// Delete the custom defined attributes
			this->Attributes = nullptr;	//
			CredFree(this); // Let Windows handle the original credential without the attributes
		}

		DeepCred *deepCopy(void *&inOutNextAddr) {
			DeepCred *outCred = static_cast<DeepCred *>(inOutNextAddr);
			unsigned tmpSz;
			*(static_cast<DeepCred *>(inOutNextAddr)) = *this; // Copy the strtucture content
			inOutNextAddr = (this + 1);
			outCred->Attributes = static_cast<DeepCredAttrib *>(inOutNextAddr);
			for (unsigned i = 0; i < this->AttributeCount; i++) {
				static_cast<DeepCredAttrib>(Attributes[i]).deepCopy(inOutNextAddr);
			}
			tmpSz = (lstrlen(this->TargetName) + 1) * TCHAR_SIZE;
			memcpy_s(inOutNextAddr, tmpSz, this->TargetName, tmpSz); // Copy TargetName after attributes stuff
			outCred->TargetName = static_cast<TCHAR *>(inOutNextAddr);
			inOutNextAddr = ((static_cast<char *>(inOutNextAddr)) + tmpSz);
			tmpSz = (lstrlen(this->Comment) + 1) * TCHAR_SIZE;
			memcpy_s(inOutNextAddr, tmpSz, this->Comment, tmpSz);
			outCred->Comment = static_cast<TCHAR *>(inOutNextAddr);
			inOutNextAddr = ((static_cast<char *>(inOutNextAddr)) + tmpSz);
			tmpSz = (lstrlen(this->TargetAlias) + 1) * TCHAR_SIZE;
			memcpy_s(inOutNextAddr, tmpSz, this->TargetAlias, tmpSz);
			outCred->TargetAlias = static_cast<TCHAR *>(inOutNextAddr);
			inOutNextAddr = ((static_cast<char *>(inOutNextAddr)) + tmpSz);
			tmpSz = (lstrlen(this->UserName) + 1) * TCHAR_SIZE;
			memcpy_s(inOutNextAddr, tmpSz, this->UserName, tmpSz);
			outCred->UserName = static_cast<TCHAR *>(inOutNextAddr);
			inOutNextAddr = ((static_cast<char *>(inOutNextAddr)) + tmpSz);
			memcpy_s(inOutNextAddr, this->CredentialBlobSize, this->Comment, this->CredentialBlobSize);
			outCred->CredentialBlob = static_cast<BYTE *>(inOutNextAddr);
			inOutNextAddr = ((static_cast<char *>(inOutNextAddr)) + this->CredentialBlobSize);
			return outCred;
		}

		unsigned getDeepSize() {
			unsigned outSize = sizeof(DeepCred);
			for (unsigned i = 0; i < this->AttributeCount; i++) {
				outSize += static_cast<DeepCredAttrib>(this->Attributes[i]).getDeepSize();
			}
			outSize += (lstrlen(this->TargetName) + 1) * TCHAR_SIZE;
			outSize += (lstrlen(this->Comment) + 1) * TCHAR_SIZE;
			outSize += (lstrlen(this->TargetAlias) + 1) * TCHAR_SIZE;
			outSize += (lstrlen(this->UserName) + 1) * TCHAR_SIZE;
			return outSize + this->CredentialBlobSize;
		}
	};

	void *memoryMap(HANDLE &memHandle, unsigned memSize, const TCHAR *memName = MEMORY_NAME) {
		void *outMemStart = nullptr;
		memHandle = CreateFileMapping(INVALID_HANDLE_VALUE, nullptr, PAGE_READWRITE, 0, memSize, memName);
		if (memHandle) {
			outMemStart = MapViewOfFile(memHandle, FILE_MAP_ALL_ACCESS, 0, 0, memSize);
			if (!outMemStart) {
				CloseHandle(memHandle);
			}
		} else {
		}
		return outMemStart;
	}

	void memoryUnmap(void *memStart, HANDLE &memHandle) {
		UnmapViewOfFile(memStart);
		CloseHandle(memHandle);
	}

	void *memoryJoin(HANDLE &memHandle, const TCHAR *memName = MEMORY_NAME) {
		void *outMemStart = nullptr;
		memHandle = OpenFileMapping(FILE_MAP_ALL_ACCESS, FALSE, memName);
		if (memHandle) {
			outMemStart = MapViewOfFile(memHandle, FILE_MAP_ALL_ACCESS, 0, 0, 0);
			if (!outMemStart) {
				CloseHandle(memHandle);
			}
		}
		return outMemStart;
	}

	void memoryUnjoin(void *memStart, HANDLE memHandle) {
		UnmapViewOfFile(memStart);
		CloseHandle(memHandle);
	}

	void pushCreds(void *memAddr, ez::DeepCred *credsToMap, unsigned credCount) {
		for (unsigned i = 0; i < credCount; i++) {
			credsToMap[i].deepCopy(memAddr);
		}
	}

	ez::DeepCred *pullCreds(void *memAddr, unsigned &outCredCount) {
		outCredCount = *static_cast<unsigned int *>(memAddr);
		void *tmpStart = (static_cast<unsigned int *>(memAddr) + 1);
		return static_cast<ez::DeepCred *>(tmpStart);
	}

	bool isCurrentProcessElevated() {
		bool outBool = false;
		HANDLE tmpTokHandle = nullptr;
		if (OpenProcessToken(GetCurrentProcess(), TOKEN_QUERY, &tmpTokHandle)) {
			TOKEN_ELEVATION tmpElev;
			DWORD tmpElevSize = sizeof(tmpElev);
			if (GetTokenInformation(tmpTokHandle, TokenElevation, &tmpElev, tmpElevSize, &tmpElevSize)) {
				return tmpElev.TokenIsElevated;
			}
		}
		if (tmpTokHandle) {
			CloseHandle(tmpTokHandle);
		}
		return outBool;
	}

	jstring tStrToJStr(JNIEnv *javaEnv, const TCHAR *origStr) {
#ifdef UNICODE
		return origStr ? javaEnv->NewString(reinterpret_cast<const jchar *>(origStr), lstrlen(origStr)) : nullptr;
#else
		return origStr ? javaEnv->NewStringUTF(origStr) : nullptr;
#endif // UNICODE
	}

	TCHAR *jStrToTStr(JNIEnv *javaEnv, const jstring origString) {
		size_t tmpLen = javaEnv->GetStringLength(origString);
		TCHAR *outStr = new TCHAR[tmpLen + 1];
		const TCHAR *tmpOrigStr = reinterpret_cast<const TCHAR *>(javaEnv->GetJavaStringChars(origString, JNI_FALSE));
		tStrCopy(outStr, tmpLen + 1, tmpOrigStr);
		javaEnv->ReleaseJavaStringChars(origString, tmpOrigStr); // Said to be a must even if not a copy
		outStr[tmpLen] = 0;
		return outStr;
	}

	jobject wCredAttrToJCredAttr(JNIEnv *javaEnv, const PCREDENTIAL_ATTRIBUTE credAttrib) {
		jbyteArray tmpVal = javaEnv->NewByteArray(credAttrib->ValueSize);
		javaEnv->SetByteArrayRegion(tmpVal, 0, credAttrib->ValueSize, reinterpret_cast<const jbyte *>(credAttrib->Value));
		return javaEnv->NewObject(ez::JAttribClass, ez::JAttribConstr, tStrToJStr(javaEnv, credAttrib->Keyword), tmpVal);
	}

	PCREDENTIAL_ATTRIBUTE jCredAttrToWCredAttr(JNIEnv *javaEnv, jobject winCredAttr) {
		PCREDENTIAL_ATTRIBUTE outAttr = new CREDENTIAL_ATTRIBUTE({0});
		outAttr->Keyword = jStrToTStr(javaEnv, static_cast<jstring>(javaEnv->CallObjectMethod(winCredAttr, ez::JAttribMethodKey)));
		jbyteArray tmpValArr = static_cast<jbyteArray>(javaEnv->CallObjectMethod(winCredAttr, ez::JAttribMethodValue));
		outAttr->ValueSize = javaEnv->GetArrayLength(tmpValArr);
		outAttr->Value = reinterpret_cast<LPBYTE>(javaEnv->GetByteArrayElements(tmpValArr, JNI_FALSE));
		outAttr->Flags = 0UL;
		return outAttr;
	}

	jobject wCredToJCred(JNIEnv *javaEnv, const PCREDENTIAL credStruct) {
		jbyteArray tmpPass = javaEnv->NewByteArray(credStruct->CredentialBlobSize);
		ULARGE_INTEGER tmpDateTime;
		jobjectArray tmpAttribs = javaEnv->NewObjectArray(credStruct->AttributeCount, ez::JAttribClass, nullptr);
		for (DWORD i = 0; i < credStruct->AttributeCount; i++) {
			javaEnv->SetObjectArrayElement(tmpAttribs, i, wCredAttrToJCredAttr(javaEnv, &credStruct->Attributes[i]));
		}
		javaEnv->SetByteArrayRegion(tmpPass, 0, credStruct->CredentialBlobSize, reinterpret_cast<const jbyte *>(credStruct->CredentialBlob));
		tmpDateTime.LowPart = credStruct->LastWritten.dwLowDateTime;
		tmpDateTime.HighPart = credStruct->LastWritten.dwHighDateTime;
		return javaEnv->NewObject(
			ez::JCredClass, ez::JCredConstr, credStruct->Flags, credStruct->Type,
			tStrToJStr(javaEnv, credStruct->TargetName), tStrToJStr(javaEnv, credStruct->Comment), tmpDateTime, tmpPass,
			credStruct->Persist, tmpAttribs, tStrToJStr(javaEnv, credStruct->TargetAlias), tStrToJStr(javaEnv, credStruct->UserName)
		);
	}

	PCREDENTIAL jCredToWCred(JNIEnv *javaEnv, jobject winCred) {
		PCREDENTIAL outCred = new CREDENTIAL({0});
		outCred->Type = javaEnv->GetIntField(winCred, ez::JCredFieldType);
		outCred->TargetName = jStrToTStr(javaEnv, static_cast<jstring>(javaEnv->GetObjectField(winCred, ez::JCredFieldTargetName)));
		outCred->Comment = jStrToTStr(javaEnv, static_cast<jstring>(javaEnv->GetObjectField(winCred, ez::JCredFieldComment)));
		jbyteArray tmpPassArr = static_cast<jbyteArray>(javaEnv->GetObjectField(winCred, ez::JCredFieldBytePassword));
		outCred->CredentialBlobSize = javaEnv->GetArrayLength(tmpPassArr);
		outCred->CredentialBlob = reinterpret_cast<LPBYTE>(javaEnv->GetByteArrayElements(tmpPassArr, JNI_FALSE));
		outCred->Persist = javaEnv->GetIntField(winCred, ez::JCredFieldPersist);
		outCred->TargetAlias = jStrToTStr(javaEnv, static_cast<jstring>(javaEnv->GetObjectField(winCred, ez::JCredFieldTargetAlias)));
		outCred->UserName = jStrToTStr(javaEnv, static_cast<jstring>(javaEnv->GetObjectField(winCred, ez::JCredFieldUserName)));
		jarray tmpAttrArr = static_cast<jarray>(javaEnv->CallObjectMethod(winCred, ez::JCredMethodByteAttribs));
		outCred->AttributeCount = javaEnv->GetArrayLength(tmpAttrArr);
		PCREDENTIAL_ATTRIBUTE *tmpAttr = new PCREDENTIAL_ATTRIBUTE[outCred->AttributeCount];
		for (DWORD i = 0; i < outCred->AttributeCount; i++) {
			tmpAttr[i] = jCredAttrToWCredAttr(javaEnv, &tmpAttrArr[i]);
		}
		return outCred;
	}
}

jint JNI_OnLoad(JavaVM *javaVm, void *) {
	JNIEnv *tmpEnv;
	javaVm->AttachCurrentThread((void **)& tmpEnv, nullptr); // Main thread is already attached (just to get original JNIEnv)
	ez::JCredClass = static_cast<jclass>(tmpEnv->NewGlobalRef(tmpEnv->FindClass("Lmodel/SystemCredentialsManager$WindowsCredential;")));
	ez::JCredConstr = tmpEnv->GetMethodID(
		ez::JCredClass, "<init>", "(IILjava/lang/String;Ljava/lang/String;J[BI[Ljava/util/Map$Entry;Ljava/lang/String;Ljava/lang/String;)V");
	ez::JCredFieldType = tmpEnv->GetFieldID(ez::JCredClass, "meType", "I");
	ez::JCredFieldTargetName = tmpEnv->GetFieldID(ez::JCredClass, "meTargetName", "Ljava/lang/String;");
	ez::JCredFieldComment = tmpEnv->GetFieldID(ez::JCredClass, "meComment", "Ljava/lang/String;");
	ez::JCredFieldBytePassword = tmpEnv->GetFieldID(ez::JCredClass, "meBytePassword", "[B");
	ez::JCredFieldPersist = tmpEnv->GetFieldID(ez::JCredClass, "mePersist", "I");
	ez::JCredFieldTargetAlias = tmpEnv->GetFieldID(ez::JCredClass, "meTargetAlias", "Ljava/lang/String;");
	ez::JCredFieldUserName = tmpEnv->GetFieldID(ez::JCredClass, "meUserName", "Ljava/lang/String;");
	ez::JCredMethodByteAttribs = tmpEnv->GetMethodID(ez::JCredClass, "getByteAttributes", "()[Ljava/util/Map$Entry;");
	ez::JAttribClass = static_cast<jclass>(tmpEnv->NewGlobalRef(tmpEnv->FindClass("Ljava/util/AbstractMap$SimpleEntry;")));
	ez::JAttribConstr = tmpEnv->GetMethodID(ez::JAttribClass, "<init>", "(Ljava/lang/Object;Ljava/lang/Object;)V");
	ez::JAttribMethodKey = tmpEnv->GetMethodID(ez::JAttribClass, "getKey", "()Ljava/lang/Object;");
	ez::JAttribMethodValue = tmpEnv->GetMethodID(ez::JAttribClass, "getValue", "()Ljava/lang/Object;");
	return tmpEnv->GetVersion(); // TODO: Change to mach minimum required.
}

void JNI_OnUnload(JavaVM *javaVm, void *) {
	JNIEnv *tmpEnv;
	javaVm->AttachCurrentThread((void **)& tmpEnv, nullptr); // Main thread is already attached (just to get original JNIEnv)
	tmpEnv->DeleteGlobalRef(ez::JCredClass);
	ez::JCredClass = nullptr;
	ez::JCredConstr = nullptr;
	ez::JCredFieldType = nullptr;
	ez::JCredFieldTargetName = nullptr;
	ez::JCredFieldComment = nullptr;
	ez::JCredFieldBytePassword = nullptr;
	ez::JCredFieldPersist = nullptr;
	ez::JCredFieldTargetAlias = nullptr;
	ez::JCredFieldUserName = nullptr;
	tmpEnv->DeleteGlobalRef(ez::JAttribClass);
	ez::JAttribClass = nullptr;
	ez::JAttribConstr = nullptr;
}

jobjectArray Java_model_SystemCredentialsManager_getCreds(JNIEnv *javaEnv, jclass, jstring credFilter, jboolean isAll) {
	TCHAR *tmpFilter = credFilter ? ez::jStrToTStr(javaEnv, credFilter) : nullptr;
	DWORD tmpCnt;
	PCREDENTIAL *tmpCreds;
#pragma warning(suppress:6388)
	BOOL tmpIsOk = CredEnumerate(tmpFilter, isAll ? CRED_ENUMERATE_ALL_CREDENTIALS : 0, &tmpCnt, &tmpCreds);
	delete[] tmpFilter;
	jobjectArray outCreds = javaEnv->NewObjectArray(tmpCnt, ez::JCredClass, nullptr);
	if (tmpIsOk) {
		for (DWORD i = 0; i < tmpCnt; i++) {
			javaEnv->SetObjectArrayElement(outCreds, i, ez::wCredToJCred(javaEnv, tmpCreds[i]));
		}
	}
	CredFree(tmpCreds);
	return outCreds;
}

void runElevated(const TCHAR *commandParams) {
	unsigned tmpLen = lstrlen(ez::ELEV_PARAMS) + lstrlen(commandParams) + 1;
	TCHAR *tmpParams = new TCHAR[tmpLen];
	tStrCopy(tmpParams, tmpLen, ez::ELEV_PARAMS);
	if (commandParams) {
		tStrCat(tmpParams, tmpLen, commandParams);
	}
	SHELLEXECUTEINFO tmpShInfo = {0};
	tmpShInfo.cbSize = sizeof(tmpShInfo);
	tmpShInfo.fMask = SEE_MASK_NOCLOSEPROCESS;
	tmpShInfo.hwnd = 0;
	tmpShInfo.lpVerb = TEXT("runas");
	tmpShInfo.lpFile = TEXT("rundll32.exe");
	tmpShInfo.lpParameters = tmpParams;
	tmpShInfo.lpDirectory = nullptr;
	tmpShInfo.nShow = SW_SHOWNORMAL;
	tmpShInfo.hInstApp = nullptr;
	if (ShellExecuteEx(&tmpShInfo) && tmpShInfo.hProcess) {
		WaitForSingleObject(tmpShInfo.hProcess, INFINITE);
		CloseHandle(tmpShInfo.hProcess);
	}
	delete[] tmpParams;
}

void runIt() {
	//CREDENTIAL tmpWriteCred = {0};
	//tmpWriteCred.Type = CRED_TYPE_GENERIC;
	//tmpWriteCred.TargetName = (TCHAR *)TEXT("InTheNameOfALLAH");
	//tmpWriteCred.Comment = (TCHAR *)TEXT("The Comment");
	//tmpWriteCred.CredentialBlobSize = 12;
	//tmpWriteCred.CredentialBlob = (LPBYTE)TEXT("Janty");
	//tmpWriteCred.Persist = CRED_PERSIST_LOCAL_MACHINE;
	//tmpWriteCred.TargetAlias = (TCHAR *)TEXT("ItnoALLAH");
	//tmpWriteCred.UserName = (TCHAR *)TEXT("LoginUser");
	////DWORD outErr = CredWrite(tmpWriteCred, CRED_PRESERVE_CREDENTIAL_BLOB) ? 0 : GetLastError();
	//DWORD outErr = CredWrite(&tmpWriteCred, 0) ? 0 : GetLastError();

	TCHAR tmpTestPass[256]{TEXT("Janty")};
	TCHAR tmpStr[256]{0};
	TCHAR tmpOut[256]{0};
	DWORD tmpSize = 256;
	CRED_PROTECTION_TYPE tmpType = CRED_PROTECTION_TYPE::CredForSystemProtection;
	//CredProtectEx(CRED_PROTECT_VALID_FLAGS, tmpTestPass, lstrlen(tmpTestPass), tmpOut, &tmpSz, &tmpType);
	CredProtect(TRUE, tmpTestPass, lstrlen(tmpTestPass), tmpOut, &tmpSize, &tmpType);
	tmpOut[tmpSize] = 0;
	unsigned tmpSz = tmpSize;
	wsprintf(tmpStr, TEXT("Pass [%s]. Size [%u]"), tmpOut, tmpSz);
	MessageBox(nullptr, tmpStr, tmpOut, MB_ICONWARNING | MB_APPLMODAL);

	////CredProtect(TRUE, tmpTestPass, lstrlen(tmpTestPass), tmpOut, &tmpSz, &tmpType);
	////tmpOut[tmpSz] = 0;
	//wsprintf(tmpStr, TEXT("Pass [%s]. is protected [%S]"), tmpOut, CredIsProtected(tmpOut, &tmpType) ? "yes" : "no");
	//MessageBox(nullptr, tmpStr, tmpOut, MB_ICONWARNING | MB_APPLMODAL);

	//CredUnprotect(FALSE, tmpOut, lstrlen(tmpTestPass), tmpTestPass, &tmpSz);
	//wsprintf(tmpStr, TEXT("Pass [%s]. encrypted [%s]"), tmpTestPass, tmpOut);
	//MessageBox(nullptr, tmpStr, tmpOut, MB_ICONWARNING | MB_APPLMODAL);

	HANDLE tmpMemHandle;
	void *tmpMemPos, *tmpSeek;
	tmpMemPos = tmpSeek = ez::memoryJoin(tmpMemHandle);
	tmpSz = (lstrlen(tmpOut) + 1) * ez::TCHAR_SIZE;
	*static_cast<unsigned int *>(tmpSeek) = tmpSz;
	wsprintf(tmpStr, TEXT("Size [%u]"), tmpSz);
	MessageBox(nullptr, tmpStr, L"XXX", MB_ICONWARNING | MB_APPLMODAL);
	tmpSeek = (static_cast<unsigned int *>(tmpSeek) + 1);
	memcpy_s(tmpSeek, tmpSz, tmpOut, tmpSz);
	ez::memoryUnjoin(tmpMemPos, tmpMemHandle);
}

jobject Java_model_SystemCredentialsManager_getCred(JNIEnv *javaEnv, jclass, jstring credTarget, jint credType) {

	HANDLE tmpMemHandle;
	void *tmpMemPos, *tmpSeek;
	tmpMemPos = tmpSeek = ez::memoryMap(tmpMemHandle, 512);

	runElevated(nullptr);

	unsigned tmpLen = *static_cast<unsigned int *>(tmpSeek);
	printf_s("Size: [%u]\n\n", tmpLen);
	tmpSeek = (static_cast<unsigned int *>(tmpSeek) + 1);
	TCHAR tmpPrPass[256]{0};
	TCHAR tmpStr[256]{0};
	memcpy_s(tmpPrPass, tmpLen, tmpSeek, tmpLen);
	for (unsigned i = 0; i < tmpLen; i++) {
		printf_s("[%d][%c]", tmpPrPass[i], tmpPrPass[i]);
	}
	printf_s("\nStrlen: [%d]. Size: [%u]\n", lstrlen(tmpPrPass), tmpLen);

	ez::memoryUnmap(tmpMemPos, tmpMemHandle);
	CRED_PROTECTION_TYPE tmpType;
	//wprintf(tmpStr, TEXT("Pass [%s]. is protected [%S]"), tmpPrPass, CredIsProtected(tmpPrPass, &tmpType) ? "yes" : "no");
	//wprintf_s(L"Pass [%s]. is protected [%S]\n", tmpPrPass, CredIsProtected(tmpPrPass, &tmpType) ? "yes" : "no");
	//printf_s("Pass [%S]. is protected [%s]\n", tmpPrPass, CredIsProtected(tmpPrPass, &tmpType) ? "yes" : "no");
	DWORD tmpWlen = tmpLen;
	BOOL isOk = CredUnprotect(FALSE, tmpPrPass, lstrlen(tmpPrPass), tmpStr, &tmpWlen);
	if (!isOk) {
		printf_s("NoT OK: Is_ERROR_NOT_CAPABLE: [%s]\n", GetLastError() == ERROR_NOT_CAPABLE ? "yes" : "no");
	}
	printf_s("Unprotected: [%S]. Size: [%u]\n\n", tmpStr, tmpWlen);

	for (unsigned i = 0; i < tmpWlen; i++) {
		printf_s("[%d][%c]", tmpStr[i], tmpStr[i]);
	}

	printf_s("\nIs java process elevated: [%s]\n", ez::isCurrentProcessElevated() ? "Yes" : "No");

	TCHAR *tmpTarg = ez::jStrToTStr(javaEnv, credTarget);
	PCREDENTIAL tmpCred;
	jobject outCred = nullptr;
	BOOL tmpIsOk = CredRead(tmpTarg, credType, 0, &tmpCred);
	delete[] tmpTarg;
	if (tmpIsOk) {
		outCred = ez::wCredToJCred(javaEnv, tmpCred);
		CredFree(tmpCred);
	}
	return outCred;
}

jint Java_model_SystemCredentialsManager_newCred(JNIEnv *javaEnv, jclass, jobject winCred) {
	PCREDENTIAL tmpWriteCred = ez::jCredToWCred(javaEnv, winCred);
	//DWORD outErr = CredWrite(tmpWriteCred, CRED_PRESERVE_CREDENTIAL_BLOB) ? 0 : GetLastError();
	DWORD outErr = CredWrite(tmpWriteCred, 0) ? 0 : GetLastError();
	CredFree(tmpWriteCred);
	return outErr;
}

jint Java_model_SystemCredentialsManager_updateCred(JNIEnv *javaEnv, jclass, jobject winCred) {
	return 0;
}

jint Java_model_SystemCredentialsManager_deleteCred(JNIEnv *javaEnv, jclass, jstring credTarget, jint credType) {
	TCHAR *tmpTarg = ez::jStrToTStr(javaEnv, credTarget);
	DWORD outErr = CredDelete(tmpTarg, credType, 0) ? 0 : GetLastError();
	delete[] tmpTarg;
	printf_s("Passed Deleted [%lu]\n", outErr);
	return outErr;
}

jstring Java_model_SystemCredentialsManager_getErrMsg(JNIEnv *javaEnv, jclass, jint errCode) {
	LPTSTR tmpErrMsg = nullptr;
	FormatMessage(FORMAT_MESSAGE_ALLOCATE_BUFFER | FORMAT_MESSAGE_FROM_SYSTEM | FORMAT_MESSAGE_IGNORE_INSERTS, nullptr, errCode, MAKELANGID(LANG_NEUTRAL, SUBLANG_DEFAULT), (LPTSTR)&tmpErrMsg, 0, nullptr);
	jstring outMsg = ez::tStrToJStr(javaEnv, tmpErrMsg);
	LocalFree(tmpErrMsg);
	return outMsg;
}

void ElevMain(HWND ownerWindow, HINSTANCE myInstHdl, LPTSTR cmdLine, int howShow) {
	runIt();

	//MessageBox(nullptr, TEXT("In The Name Of ALLAH RR"), cmdLine, MB_ICONWARNING);
	//wprintf_s(cmdLine);
	//int *tmpInt = reinterpret_cast<int *>(strtoull(cmdLine, NULL, 16));
	//strtoll(cmdLine, nullptr, 16);
	//TCHAR tmpCmd[512]{0};
	//sprintf_s(tmpCmd, "Number: [%d]", *tmpInt);

	//MessageBox(nullptr, outStr, cmdLine, MB_ICONWARNING);
}
