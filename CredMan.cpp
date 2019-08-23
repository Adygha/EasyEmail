/**
 * Some assumptions about charcode here are based on that Windows uses unicode UTF-16 little-endian and the
 * Windows-API specifying some data to be in unicode UTF-16.
 */

#include <Windows.h>
#include <wincred.h>
#include "CredMan.h"

#ifdef UNICODE
#define GetJavaStringChars GetStringChars
#define ReleaseJavaStringChars(AA, BB) ReleaseStringChars(AA, (jchar *)BB)
#else
#define GetJavaStringChars GetStringUTFChars
#define ReleaseJavaStringChars ReleaseStringUTFChars
#endif // UNICODE

namespace credManConsts {
	//const unsigned BUF_SIZE = 100U;
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
}

jint JNI_OnLoad(JavaVM *javaVm, void *) {
	JNIEnv *tmpEnv;
	javaVm->AttachCurrentThread((void **)& tmpEnv, nullptr); // Main thread is already attached (just to get original JNIEnv)
	credManConsts::JCredClass = (jclass)tmpEnv->NewGlobalRef(tmpEnv->FindClass("Lmodel/SystemCredentialsManager$WindowsCredential;"));
	credManConsts::JCredConstr = tmpEnv->GetMethodID(
		credManConsts::JCredClass, "<init>", "(IILjava/lang/String;Ljava/lang/String;J[BI[Ljava/util/Map$Entry;Ljava/lang/String;Ljava/lang/String;)V");
	credManConsts::JCredFieldType = tmpEnv->GetFieldID(credManConsts::JCredClass, "meType", "I");
	credManConsts::JCredFieldTargetName = tmpEnv->GetFieldID(credManConsts::JCredClass, "meTargetName", "Ljava/lang/String;");
	credManConsts::JCredFieldComment = tmpEnv->GetFieldID(credManConsts::JCredClass, "meComment", "Ljava/lang/String;");
	credManConsts::JCredFieldBytePassword = tmpEnv->GetFieldID(credManConsts::JCredClass, "meBytePassword", "[B");
	credManConsts::JCredFieldPersist = tmpEnv->GetFieldID(credManConsts::JCredClass, "mePersist", "I");
	credManConsts::JCredFieldTargetAlias = tmpEnv->GetFieldID(credManConsts::JCredClass, "meTargetAlias", "Ljava/lang/String;");
	credManConsts::JCredFieldUserName = tmpEnv->GetFieldID(credManConsts::JCredClass, "meUserName", "Ljava/lang/String;");
	credManConsts::JCredMethodByteAttribs = tmpEnv->GetMethodID(credManConsts::JCredClass, "getByteAttributes", "()[Ljava/util/Map$Entry;");
	credManConsts::JAttribClass = (jclass)tmpEnv->NewGlobalRef(tmpEnv->FindClass("Ljava/util/AbstractMap$SimpleEntry;"));
	credManConsts::JAttribConstr = tmpEnv->GetMethodID(credManConsts::JAttribClass, "<init>", "(Ljava/lang/Object;Ljava/lang/Object;)V");
	credManConsts::JAttribMethodKey = tmpEnv->GetMethodID(credManConsts::JAttribClass, "getKey", "()Ljava/lang/Object;");
	credManConsts::JAttribMethodValue = tmpEnv->GetMethodID(credManConsts::JAttribClass, "getValue", "()Ljava/lang/Object;");
	return tmpEnv->GetVersion(); // TODO: Change to mach minimum required.
}

void JNI_OnUnload(JavaVM *javaVm, void *) {
	JNIEnv *tmpEnv;
	javaVm->AttachCurrentThread((void **)& tmpEnv, nullptr); // Main thread is already attached (just to get original JNIEnv)
	tmpEnv->DeleteGlobalRef(credManConsts::JCredClass);
	credManConsts::JCredClass = nullptr;
	credManConsts::JCredConstr = nullptr;
	credManConsts::JCredFieldType = nullptr;
	credManConsts::JCredFieldTargetName = nullptr;
	credManConsts::JCredFieldComment = nullptr;
	credManConsts::JCredFieldBytePassword = nullptr;
	credManConsts::JCredFieldPersist = nullptr;
	credManConsts::JCredFieldTargetAlias = nullptr;
	credManConsts::JCredFieldUserName = nullptr;
	tmpEnv->DeleteGlobalRef(credManConsts::JAttribClass);
	credManConsts::JAttribClass = nullptr;
	credManConsts::JAttribConstr = nullptr;
}

jstring tStrToJStr(JNIEnv *javaEnv, const TCHAR *origStr) {
#ifdef UNICODE
	return origStr ? javaEnv->NewString((jchar *)origStr, lstrlen(origStr)) : nullptr;
#else
	return origStr ? javaEnv->NewStringUTF(origStr) : nullptr;
#endif // UNICODE
}

TCHAR *jStrToTStr(JNIEnv *javaEnv, const jstring origString) {
	size_t tmpLen = javaEnv->GetStringLength(origString);
	TCHAR *outStr = new TCHAR[tmpLen + 1];
	const TCHAR *tmpOrigStr = (TCHAR *)javaEnv->GetJavaStringChars(origString, JNI_FALSE);
	lstrcpy(outStr, tmpOrigStr);
	javaEnv->ReleaseJavaStringChars(origString, tmpOrigStr); // Said to be a must even if not a copy
	outStr[tmpLen] = 0;
	return outStr;
}

jobject wCredAttrToJCredAttr(JNIEnv *javaEnv, const PCREDENTIAL_ATTRIBUTE credAttrib) {
	jbyteArray tmpVal = javaEnv->NewByteArray(credAttrib->ValueSize);
	javaEnv->SetByteArrayRegion(tmpVal, 0, credAttrib->ValueSize, (const jbyte *)credAttrib->Value);
	return javaEnv->NewObject(credManConsts::JAttribClass, credManConsts::JAttribConstr, tStrToJStr(javaEnv, credAttrib->Keyword), tmpVal);
}

PCREDENTIAL_ATTRIBUTE jCredAttrToWCredAttr(JNIEnv *javaEnv, jobject winCredAttr) {
	PCREDENTIAL_ATTRIBUTE outAttr = new CREDENTIAL_ATTRIBUTE({0});
	outAttr->Keyword = jStrToTStr(javaEnv, (jstring)javaEnv->CallObjectMethod(winCredAttr, credManConsts::JAttribMethodKey));
	jbyteArray tmpValArr = (jbyteArray)javaEnv->CallObjectMethod(winCredAttr, credManConsts::JAttribMethodValue);
	outAttr->ValueSize = javaEnv->GetArrayLength(tmpValArr);
	//outAttr->Value = (LPBYTE)tmpValArr;
	outAttr->Value = (LPBYTE)javaEnv->GetByteArrayElements(tmpValArr, JNI_FALSE);
	outAttr->Flags = 0UL;
	return outAttr;
}

jobject wCredToJCred(JNIEnv *javaEnv, const PCREDENTIAL credStruct) {
	jbyteArray tmpPass = javaEnv->NewByteArray(credStruct->CredentialBlobSize);
	ULARGE_INTEGER tmpDateTime;
	jobjectArray tmpAttribs = javaEnv->NewObjectArray(credStruct->AttributeCount, credManConsts::JAttribClass, nullptr);
	for (DWORD i = 0; i < credStruct->AttributeCount; i++) {
		javaEnv->SetObjectArrayElement(tmpAttribs, i, wCredAttrToJCredAttr(javaEnv, &credStruct->Attributes[i]));
	}
	javaEnv->SetByteArrayRegion(tmpPass, 0, credStruct->CredentialBlobSize, (const jbyte *)credStruct->CredentialBlob);
	tmpDateTime.LowPart = credStruct->LastWritten.dwLowDateTime;
	tmpDateTime.HighPart = credStruct->LastWritten.dwHighDateTime;
	//SYSTEMTIME tmpSysTime, tmpLocTime;
	//FileTimeToSystemTime(&credStruct->LastWritten, &tmpSysTime);
	//SystemTimeToTzSpecificLocalTime(nullptr, &tmpSysTime, &tmpLocTime);
	//printf_s("%02d-%02d-%d  %02d:%02d:%02d\n", tmpLocTime.wDay, tmpLocTime.wMonth, tmpLocTime.wYear, tmpLocTime.wHour, tmpLocTime.wMinute, tmpLocTime.wSecond);
	return javaEnv->NewObject(
		credManConsts::JCredClass, credManConsts::JCredConstr, credStruct->Flags, credStruct->Type,
		tStrToJStr(javaEnv, credStruct->TargetName), tStrToJStr(javaEnv, credStruct->Comment), tmpDateTime, tmpPass,
		credStruct->Persist, tmpAttribs, tStrToJStr(javaEnv, credStruct->TargetAlias), tStrToJStr(javaEnv, credStruct->UserName)
	);
}

PCREDENTIAL jCredToWCred(JNIEnv *javaEnv, jobject winCred) {
	PCREDENTIAL outCred = new CREDENTIAL({0});
	outCred->Type = javaEnv->GetIntField(winCred, credManConsts::JCredFieldType);
	outCred->TargetName = jStrToTStr(javaEnv, (jstring)javaEnv->GetObjectField(winCred, credManConsts::JCredFieldTargetName));
	outCred->Comment = jStrToTStr(javaEnv, (jstring)javaEnv->GetObjectField(winCred, credManConsts::JCredFieldComment));
	jbyteArray tmpPassArr = (jbyteArray)javaEnv->GetObjectField(winCred, credManConsts::JCredFieldBytePassword);
	outCred->CredentialBlobSize = javaEnv->GetArrayLength(tmpPassArr);
	//outCred->CredentialBlob = (LPBYTE)tmpPassArr;
	outCred->CredentialBlob = (LPBYTE)javaEnv->GetByteArrayElements(tmpPassArr, JNI_FALSE);
	outCred->Persist = javaEnv->GetIntField(winCred, credManConsts::JCredFieldPersist);
	outCred->TargetAlias = jStrToTStr(javaEnv, (jstring)javaEnv->GetObjectField(winCred, credManConsts::JCredFieldTargetAlias));
	outCred->UserName = jStrToTStr(javaEnv, (jstring)javaEnv->GetObjectField(winCred, credManConsts::JCredFieldUserName));
	jarray tmpAttrArr = (jarray)javaEnv->CallObjectMethod(winCred, credManConsts::JCredMethodByteAttribs);
	outCred->AttributeCount = javaEnv->GetArrayLength(tmpAttrArr);
	PCREDENTIAL_ATTRIBUTE *tmpAttr = new PCREDENTIAL_ATTRIBUTE[outCred->AttributeCount];
	for (DWORD i = 0; i < outCred->AttributeCount; i++) {
		tmpAttr[i] = jCredAttrToWCredAttr(javaEnv, &tmpAttrArr[i]);
	}
	return outCred;
}

jobjectArray Java_model_SystemCredentialsManager_getCreds(JNIEnv *javaEnv, jclass, jstring credFilter, jboolean isAll) {
	TCHAR *tmpFilter = credFilter ? jStrToTStr(javaEnv, credFilter) : nullptr;
	DWORD tmpCnt;
	PCREDENTIAL *tmpCreds;
#pragma warning(suppress:6388)
	BOOL tmpIsOk = CredEnumerate(tmpFilter, isAll ? CRED_ENUMERATE_ALL_CREDENTIALS : 0, &tmpCnt, &tmpCreds);
	delete[] tmpFilter;
	jobjectArray outCreds = javaEnv->NewObjectArray(tmpCnt, credManConsts::JCredClass, nullptr);
	if (tmpIsOk) {
		for (DWORD i = 0; i < tmpCnt; i++) {
			javaEnv->SetObjectArrayElement(outCreds, i, wCredToJCred(javaEnv, tmpCreds[i]));
		}
	}
	CredFree(tmpCreds);
	return outCreds;
}

jobject Java_model_SystemCredentialsManager_getCred(JNIEnv *javaEnv, jclass, jstring credTarget, jint credType) {
	TCHAR *tmpTarg = jStrToTStr(javaEnv, credTarget);
	PCREDENTIAL tmpCred;
	jobject outCred = nullptr;
	BOOL tmpIsOk = CredRead(tmpTarg, credType, 0, &tmpCred);
	delete[] tmpTarg;
	if (tmpIsOk) {
		outCred = wCredToJCred(javaEnv, tmpCred);
		CredFree(tmpCred);
	}
	return outCred;
}

jint Java_model_SystemCredentialsManager_newCred(JNIEnv *javaEnv, jclass, jobject winCred) {
	PCREDENTIAL tmpWriteCred = jCredToWCred(javaEnv, winCred);
	//CredWrite(tmpWriteCred, CRED_PRESERVE_CREDENTIAL_BLOB);
	if (CredWrite(tmpWriteCred, 0)) {
		return 0;
	} else {
		return GetLastError();
	}
//#ifdef UNICODE
//	TCHAR *tmpCredName = (TCHAR *)javaEnv->GetStringChars(credName, 0);
//	printf_s("Input tmpCredName [%llu][%S]\n", wcslen(tmpCredName), tmpCredName);
//	TCHAR *tmpName = (TCHAR *)javaEnv->GetStringChars(userName, 0);
//	printf_s("Input tmpName [%llu][%S]\n", wcslen(tmpName), tmpName);
//	TCHAR *tmpPass = (TCHAR *)javaEnv->GetStringChars(userPassword, 0);
//	printf_s("Input tmpPass [%llu][%S]\n", wcslen(tmpPass), tmpPass);
//	DWORD tmByteSize = (DWORD)((wcslen(tmpPass) + 1) * sizeof(TCHAR));
//	printf_s("Input tmByteSize  [%lu]\n", tmByteSize);
//#else
//	TCHAR *tmpCredName = (TCHAR *)javaEnv->GetStringUTFChars(credName, 0);
//	printf_s("Input tmpCredName [%llu][%s]\n", strlen(tmpCredName), tmpCredName);
//	TCHAR *tmpName = (TCHAR *)javaEnv->GetStringUTFChars(userName, 0);
//	printf_s("Input tmpName [%llu][%s]\n", strlen(tmpName), tmpName);
//	TCHAR *tmpPass = (TCHAR *)javaEnv->GetStringUTFChars(userPassword, 0);
//	printf_s("Input tmpPass [%llu][%s]\n", strlen(tmpPass), tmpPass);
//	DWORD tmByteSize = (DWORD)(strlen(tmpPass) + 1);
//	printf_s("Input tmByteSize  [%lu]\n", tmByteSize);
//#endif // UNICODE
//	//std::wstring tmpPass(userPassword);
//	//const MyChar *tmpUser = "MYUSER";
//	//const MyChar *tmpPass = "MYPASS";
//	CREDENTIAL tmpWriteCred = {0};
//	tmpWriteCred.Type = CRED_TYPE_GENERIC;
//	tmpWriteCred.TargetName = tmpCredName;
//	tmpWriteCred.CredentialBlobSize = tmByteSize;
//	tmpWriteCred.CredentialBlob = (LPBYTE)tmpPass;
//	tmpWriteCred.Persist = CRED_PERSIST_LOCAL_MACHINE;
//	tmpWriteCred.UserName = (TCHAR *)tmpName;
//	CredWrite(&tmpWriteCred, 0);
}

jint Java_model_SystemCredentialsManager_updateCred(JNIEnv *, jclass, jobject winCred) {
	return 0;
}

#undef GetJavaStringChars
#undef ReleaseJavaStringChars
