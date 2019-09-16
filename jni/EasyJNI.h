#ifndef EASY_JNI
#define EASY_JNI

namespace ez {
	const unsigned SIZE_TCHAR = sizeof(TCHAR); // TCHAR's size (char/WCHAR depending on project's charset choice) that is needed several times
	const unsigned SIZE_WCHAR = sizeof(WCHAR); // WCHAR size that is needed several times

	/**
	 * Used to get a 'jstring' version of the specified 'TCHAR*' string.
	 * @author	Janty Azmat
	 * @param javaEnv		pointer to the java-environment which is used to create the 'jstring' object.
	 * @param origStr		specifies the original 'TCHAR*' string to convert.
	 * @returns				a 'jstring' version of the specified 'TCHAR*' string or null if original specified string is null.
	 */
	jstring tStrToJStr(JNIEnv *javaEnv, const TCHAR *origStr);

	/**
	 * Used to get a 'TCHAR*' string version of the specified 'jstring'. The caller is responsible for freeing/deleting the returned string.
	 * @author	Janty Azmat
	 * @param javaEnv		pointer to the java-environment which is used to retrieve the 'jstring' object.
	 * @param origString	specifies the original 'jstring' to convert.
	 * @returns				a 'TCHAR*' string version of the specified 'jstring' or null if original specified 'jstring' is null.
	 */
	TCHAR *jStrToTStr(JNIEnv *javaEnv, const jstring origString);

	/**
	 * Used to maps a memory region with the specified size and name.
	 * @author	Janty Azmat
	 * @param [out] outMemHandle	outputs the the map HANDLE of the memory region.
	 * @param memSize				specifies the required size in bytes of the memory region.
	 * @param memName				specifies the name that the OS will assign to the memory region.
	 * @returns						a pointer to the start of the mapped memory region, or null on failure.
	 */
	void *memoryMap(HANDLE &outMemHandle, unsigned memSize, const TCHAR *memName);

	/**
	 * Used to unmaps a mapped memory region with the specified address and HANDLE.
	 * @author	Janty Azmat
	 * @param memStart		specifies the pointer to the start of the mapped memory region.
	 * @param memHandle		specifies the map HANDLE of the mapped memory region.
	 */
	void memoryUnmap(void *memStart, HANDLE memHandle);

	/**
	 * Used to join in on an alredy mapped memory region with the specified name.
	 * @author	Janty Azmat
	 * @param [out] memHandle	outputs the join HANDLE of the memory region.
	 * @param memName			specifies the name of the mapped memory region to join.
	 * @returns					a pointer to the start of the mapped memory region, or null on failure.
	 */
	void *memoryJoin(HANDLE &memHandle, const TCHAR *memName);

	/**
	 * Used to unjoin from an alredy joined memory region with the specified  address and HANDLE.
	 * @author	Janty Azmat
	 * @param memStart		specifies the pointer to the start of the joined memory region.
	 * @param memHandle		specifies the join HANDLE of the joined memory region.
	 */
	void memoryUnjoin(void *memStart, HANDLE memHandle);
}

#endif // !EASY_JNI
