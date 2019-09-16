#include <Windows.h>
#include <jni.h>
#include "EasyJNI.h"

#ifdef UNICODE
#define tStrCopy wcscpy_s
#define GetJavaStringChars GetStringChars
#define ReleaseJavaStringChars(AA, BB) ReleaseStringChars(AA, reinterpret_cast<const jchar *>(BB))
#else
#define tStrCopy strcpy_s
#define GetJavaStringChars GetStringUTFChars
#define ReleaseJavaStringChars ReleaseStringUTFChars
#endif // UNICODE

namespace ez {

	/**
	 * Used to get a 'jstring' version of the specified 'TCHAR*' string.
	 * @author	Janty Azmat
	 * @param javaEnv		pointer to the java-environment which is used to create the 'jstring' object.
	 * @param origStr		specifies the original 'TCHAR*' string to convert.
	 * @returns				a 'jstring' version of the specified 'TCHAR*' string or null if original specified string is null.
	 */
	jstring tStrToJStr(JNIEnv *javaEnv, const TCHAR *origStr) {
#ifdef UNICODE
		return origStr ? javaEnv->NewString(reinterpret_cast<const jchar *>(origStr), lstrlen(origStr)) : nullptr;
#else
		return origStr ? javaEnv->NewStringUTF(origStr) : nullptr;
#endif // UNICODE
	}

	/**
	 * Used to get a 'TCHAR*' string version of the specified 'jstring'. The caller is responsible for freeing/deleting the returned string.
	 * @author	Janty Azmat
	 * @param javaEnv		pointer to the java-environment which is used to retrieve the 'jstring' object.
	 * @param origString	specifies the original 'jstring' to convert.
	 * @returns				a 'TCHAR*' string version of the specified 'jstring' or null if original specified 'jstring' is null.
	 */
	TCHAR *jStrToTStr(JNIEnv *javaEnv, const jstring origString) {
		if (!origString) {
			return nullptr;
		}
		size_t tmpLen = javaEnv->GetStringLength(origString);
		TCHAR *outStr = new TCHAR[tmpLen + 1];
		const TCHAR *tmpOrigStr = reinterpret_cast<const TCHAR *>(javaEnv->GetJavaStringChars(origString, JNI_FALSE));
		tStrCopy(outStr, tmpLen + 1, tmpOrigStr);
		javaEnv->ReleaseJavaStringChars(origString, tmpOrigStr); // Said to be a must even if not a copy
		outStr[tmpLen] = 0;
		return outStr;
	}

	/**
	 * Used to maps a memory region with the specified size and name.
	 * @author	Janty Azmat
	 * @param [out] outMemHandle	outputs the the map HANDLE of the memory region.
	 * @param memSize				specifies the required size in bytes of the memory region.
	 * @param memName				specifies the name that the OS will assign to the memory region.
	 * @returns						a pointer to the start of the mapped memory region, or null on failure.
	 */
	void *memoryMap(HANDLE &memHandle, unsigned memSize, const TCHAR *memName) {
		void *outMemStart = nullptr;
		memHandle = CreateFileMapping(INVALID_HANDLE_VALUE, nullptr, PAGE_READWRITE, 0, memSize, memName);
		if (GetLastError() == ERROR_ALREADY_EXISTS) {
			CloseHandle(memHandle);
			memHandle = INVALID_HANDLE_VALUE;
		} else if (!memHandle) {
			memHandle = INVALID_HANDLE_VALUE;
		} else if (!(outMemStart = MapViewOfFile(memHandle, FILE_MAP_ALL_ACCESS, 0, 0, memSize))) {
			CloseHandle(memHandle);
			memHandle = INVALID_HANDLE_VALUE;
		}
		return outMemStart;
	}

	/**
	 * Used to unmaps a mapped memory region with the specified address and HANDLE.
	 * @author	Janty Azmat
	 * @param memStart		specifies the pointer to the start of the mapped memory region.
	 * @param memHandle		specifies the map HANDLE of the mapped memory region.
	 */
	void memoryUnmap(void *memStart, HANDLE memHandle) {
		UnmapViewOfFile(memStart);
		CloseHandle(memHandle);
	}

	/**
	 * Used to join in on an alredy mapped memory region with the specified name.
	 * @author	Janty Azmat
	 * @param [out] memHandle	outputs the join HANDLE of the memory region.
	 * @param memName			specifies the name of the mapped memory region to join.
	 * @returns					a pointer to the start of the mapped memory region, or null on failure.
	 */
	void *memoryJoin(HANDLE &memHandle, const TCHAR *memName) {
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

	/**
	 * Used to unjoin from an alredy joined memory region with the specified  address and HANDLE.
	 * @author	Janty Azmat
	 * @param memStart		specifies the pointer to the start of the joined memory region.
	 * @param memHandle		specifies the join HANDLE of the joined memory region.
	 */
	void memoryUnjoin(void *memStart, HANDLE memHandle) {
		UnmapViewOfFile(memStart);
		CloseHandle(memHandle);
	}
}

#undef ReleaseJavaStringChars
#undef GetJavaStringChars
#undef tStrCopy
