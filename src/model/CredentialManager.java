package model;

import java.nio.charset.Charset;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.StringJoiner;

/**
 * A class that represents a manager for credentials saved into the operating system (OS).
 * @author Janty Azmat
 */
public class CredentialManager {
	static {
		System.loadLibrary("CredMan"); // Load the library
		// Since 'JNI_OnUnload' doesn't always work (almost never), used this next line instead
		Runtime.getRuntime().addShutdownHook(new Thread(CredentialManager::unloadLibrary));
	}

	/**
	 * An exception class that represents native code errors (thrown in native code).
	 * @author Janty Azmat
	 */
	public static class NativeException extends RuntimeException {
		// Fields
		private static final long serialVersionUID = -2077346525481389173L; // For serialization purposes
		private int meErrorCode; // Stores the native error-code

		/**
		 * A Constructor that takes the native error code and message.
		 * @param errCode	the native error-code.
		 * @param errMsg	the native error-message.
		 */
		public NativeException(int errCode, String errMsg) {
			super(errMsg); // Just pass the message
			this.meErrorCode = errCode;
		}

		/**
		 * Used to get the native error-code.
		 * @return	the native error-code.
		 */
		public int getErrorCode() {
			return meErrorCode;
		}
	}

	/**
	 * A class that represents an attribute (name/keyword and value) that can optionally be attached with an OS's credential
	 * @author Janty Azmat
	 */
	public static class Attribute {
 		// Field
 		private String meKeyword; // The name/keyword for the attribute
 		private String meStringValue; // The String version of the attribute's value (may need to remove it later)
 		private byte[] meValue; // The byte-array version of the attribute's value

 		/**
 		 * A Constructor that takes the attribute's value as a String.
 		 * @param attribKeyword					the attribute's name/keyword.
 		 * @param attribValue					the attribute's value as a String.
 		 * @throws IllegalArgumentException		when either the passed name/keyword or the value is null or empty.
 		 */
		public Attribute(String attribKeyword, String attribValue) throws IllegalArgumentException {
			if (attribKeyword == null || attribValue == null || attribKeyword.isEmpty() || attribValue.isEmpty()) {
				throw new IllegalArgumentException("The attribute's name/keyword and value cannot be null or empty.");
			}
			this.meKeyword = attribKeyword;
			this.meStringValue = attribValue;
			this.meValue = attribValue.getBytes();
		}

		/**
		 * A Constructor that takes the attribute's value as a byte array.
		 * @param attribKeyword					the attribute's name/keyword.
		 * @param attribValue					the attribute's value as a byte array.
		 * @throws IllegalArgumentException		when either the passed name/keyword or the value is null or empty.
		 */
		public Attribute(String attribKeyword, byte[] attribValue) throws IllegalArgumentException {
			if (attribKeyword == null || attribValue == null || attribKeyword.isEmpty() || attribValue.length < 1) {
				throw new IllegalArgumentException("The attribute's name/keyword and value cannot be null or empty.");
			}
			this.meKeyword = attribKeyword;
			this.meStringValue = new String(attribValue);
			this.meValue = attribValue;
		}

		/**
		 * A Constructor that takes the attribute's data in an Entry form. The Entry object's value can be either a
		 * String or a byte array, or else an IllegalArgumentException will be thrown.
		 * @param attribEntry					an Entry object that holds the attribute's data.
		 * @throws IllegalArgumentException		if the passed Entry object or its content is null or empty, or when
		 *										the Entry object's value is not a String or a byte array.
		 */
		public Attribute(Entry<String, ?> attribEntry) throws IllegalArgumentException {
			if (attribEntry == null || attribEntry.getKey() == null || attribEntry.getValue() == null || attribEntry.getKey().isEmpty()) {
				throw new IllegalArgumentException("The passed attribute entry and its content (key/value) cannot be null or empty.");
			}
			this.meKeyword = attribEntry.getKey();
			if (attribEntry.getValue() instanceof String) {
				String tmpVal = (String)attribEntry.getValue();
				if (tmpVal.isEmpty()) {
					throw new IllegalArgumentException("The passed attribute's value cannot be empty.");
				}
				this.meStringValue = tmpVal;
				this.meValue = tmpVal.getBytes();
			} else if (attribEntry.getValue() instanceof byte[]) {
				byte[] tmpVal = (byte[])attribEntry.getValue();
				if (tmpVal.length < 1) {
					throw new IllegalArgumentException("The passed attribute's value cannot be empty.");
				}
				this.meStringValue = new String(tmpVal);
				this.meValue = tmpVal;
			} else {
				throw new IllegalArgumentException("The passed attribute entry does not comply with the required data type.");
			}
		}

		/**
		 * Used to get the attribute's name/keyword.
		 * @return	the attribute's name/keyword.
		 */
		public String getKeyword() {
			return this.meKeyword;
		}

		/**
		 * Used to get the String version of the attribute's value.
		 * @return	the String version of the attribute's value.
		 */
		public byte[] getValue() {
			return this.meValue;
		}

		/**
		 * Used to get the byte-array version of the attribute's value.
		 * @return	the byte-array version of the attribute's value.
		 */
		public String getStringValue() {
			return this.meStringValue;
		}

		@Override
		public String toString() {
			return "[\"" + this.meKeyword + "\": \"" + this.meStringValue + "\"]";
		}
	}

	/**
	 * A class that represents Windows OS credential that can be stored/retrieved from Windows OS.
	 * @author Janty Azmat
	 */
 	public static class WindowsCredential {

// 		// vvvvvvvvvvvv Unneeded Now. May Need Later. vvvvvvvvvvvv //
//		private static enum CredentialFlag {
////			CRED_FLAGS_NONE(0),
////			CRED_FLAGS_PROMPT_NOW(2),
//
//			CRED_FLAGS_USERNAME_TARGET(4),
//
//			/**
//			 * Undocumented. This value (0x2000 + CRED_FLAGS_USERNAME_TARGET) is passed as flags (according to:
//			 * "https://stackoverflow.com/a/40718712" ) to avoid asking for user credentials when running as different user.
//			 */
//			CRED_FLAGS_USERNAME_TARGET_NO_PROMPT(CRED_FLAGS_USERNAME_TARGET.meVal + 8192); // As flags (0x2000 + CRED_FLAGS_USERNAME_TARGET).
//			public static final EnumSet<CredentialFlag> CRED_FLAGS_NONE = EnumSet.noneOf(CredentialFlag.class);
//
//			private static final Map<Integer, CredentialFlag> stVals = new HashMap<>(CredentialFlag.values().length);
//			static {
//				for (var val : CredentialFlag.values()) {
//					CredentialFlag.stVals.put(val.getFlagValue(), val);
//				}
//			}
//			private int meVal;
//
//			private CredentialFlag(int theVal) {
//				this.meVal = theVal;
//			}
//
//			public int getFlagValue() {
//				return this.meVal;
//			}
//
//			public static EnumSet<CredentialFlag> of(int bitFlags) {
//				var outFlags = CredentialFlag.CRED_FLAGS_NONE;
//				for (var flg : CredentialFlag.values()) {
//					if ((bitFlags & flg.getFlagValue()) == flg.getFlagValue()) {
//						outFlags.add(flg);
//					}
//				}
//				return outFlags;
//			}
//
////			public static int getBitFlags(EnumSet<CredentialFlag> credFlags) {
////				int outFlags = 0;
////				for (var flg : credFlags) {
////					outFlags |= flg.meVal;
////				}
////				return outFlags;
////			}
//
////			public static CredentialFlag of(int flagVal) {
////				CredentialFlag outFlag = CredentialFlag.stVals.get(flagVal);
//////				return outFlag == null ? CredentialFlag.CRED_FLAGS_NONE : outFlag;
////				if (outFlag == null) {
////					throw new IllegalArgumentException("Provided flag value is invalid.");
////				}
////				return outFlag;
////			}
//		}
//
//		public static enum CredentialType {
//			CRED_TYPE_GENERIC(1),
//			CRED_TYPE_DOMAIN_PASSWORD(2),
//			CRED_TYPE_DOMAIN_CERTIFICATE(3),
//			CRED_TYPE_DOMAIN_VISIBLE_PASSWORD(4),
//			CRED_TYPE_GENERIC_CERTIFICATE(5),
//			CRED_TYPE_DOMAIN_EXTENDED(6),
//			CRED_TYPE_MAXIMUM(7),
//			CRED_TYPE_MAXIMUM_EX(CRED_TYPE_MAXIMUM.meVal + 1000);
//
//			private static final Map<Integer, CredentialType> stVals = new HashMap<>(CredentialType.values().length);
//			static {
//				for (var val : CredentialType.values()) {
//					CredentialType.stVals.put(val.meVal, val);
//				}
//			}
//			private int meVal;
//
//			private CredentialType(int theVal) {
//				this.meVal = theVal;
//			}
//
////			public int getTypeValue() {
////				return this.meVal;
////			}
//
//			public static CredentialType of(int typeValue) {
//				CredentialType outType = CredentialType.stVals.get(typeValue);
//				if (outType == null) {
//					throw new IllegalArgumentException("Provided type value is invalid.");
//				}
//				return outType;
//			}
//		}
//
//		public static enum CredentialPersist {
//			CRED_PERSIST_SESSION(1),
//			CRED_PERSIST_LOCAL_MACHINE(2),
//			CRED_PERSIST_ENTERPRISE(3);
//
//			private static final Map<Integer, CredentialPersist> stVals = new HashMap<>(CredentialPersist.values().length);
//			static {
//				for (var val : CredentialPersist.values()) {
//					CredentialPersist.stVals.put(val.meVal, val);
//				}
//			}
//			private int meVal;
//
//			private CredentialPersist(int theVal) {
//				this.meVal = theVal;
//			}
//
////			public int getPersistValue() {
////				return this.meVal;
////			}
//
//			public static CredentialPersist of(int typeValue) {
//				CredentialPersist outPers = CredentialPersist.stVals.get(typeValue);
//				if (outPers == null) {
//					throw new IllegalArgumentException("Provided type value is invalid.");
//				}
//				return outPers;
//			}
//		}

		// Fields
		private static final boolean me_IS_PRETTY_TO_STRING = true; // To specify if 'toString' will be in a pretty shape
//		private EnumSet<CredentialFlag> meFlags;
//		private int meType;
		private String meTargetName; // The credential's target-/purpose-name for this credential
		private String meComment; // Optional credential's comment
		private Instant meLastModified; // The credential's last modification instance
		private byte[] meBytePassword; // The byte representation of the credential's designated password (can be encrypted/protected)
//		private int mePersist;
		private Map<String, byte[]> meAttributes; // Optional credential's attributes
		private String meTargetAlias; // Optional alias for the credential's name/target-name
		private String meUserName; // The credential's designated user-name
		private boolean meIsProtected; // To specify that the credential's password is encrypted/protected by the OS
		private boolean meIsChanged; // To check if the credential content is changed and needs to be saved to OS

		/**
		 * A Constructor for the user to create a credential by the specified parameters.
		 * @param isProtected					specifies if the credential is encrypted/protected (by the OS with an elevated protection).
		 * @param targetName					specifies the target-/purpose-name of this credential (cannot be null or empty).
		 * @param userName						specifies the credential's designated user-name (cannot be null or empty).
		 * @param credPassword					specifies the credential's designated password (can be null or empty -might change later-).
		 * @param credComment					specifies any comments associated with the credential (can be null or empty).
		 * @param targetAlias					specifies an alias for the target-/purpose-name (can be null or empty).
		 * @throws NativeException				when encrypted/protected credential is specified and the user cancels elevated protection.
		 * @throws IllegalArgumentException		when target-/purpose-name or user-name is null or empty.
		 */
		public WindowsCredential(boolean isProtected, String targetName, String userName, String credPassword, String credComment, String targetAlias) throws NativeException, IllegalArgumentException {
//		public WindowsCredential(boolean isProtected, EnumSet<CredentialFlag> credFlags, CredentialType credType, String targetName, String credComment, String credPassword, CredentialPersist credPersist, String credAlias, String userName) {
			this(isProtected, targetName, credComment, targetAlias, userName);
//			this.meFlags = credFlags;
//			this.meType = credType.meVal;
			this.meLastModified = Instant.now(); // May need to move this to getter
			this.setPassword(credPassword);
//			this.mePersist = credPersist.meVal;
			this.meIsChanged = true; // It is not saved to system (so, its status is changed from the system)
		}

		/**
		 * A private constructor that is used by native code.
		 * @param isProtected		specifies if the credential is protected (by the system with an elevated protection).
		 * @param targetName		specifies the target/purpose name of this credential.
		 * @param credComment		specifies any comments associated with the credential.
		 * @param lastModified		specifies the date and time when the credential was last modified.
		 * @param credPassword		specifies the credential's designated password.
		 * @param credAttribs		specified the credential's attributes (collected by JNI).
		 * @param targetAlias		specifies an alias for the target/purpose name.
		 * @param userName			specifies the credential's designated user-name.
		 */
		@SuppressWarnings("unused")
		private WindowsCredential(boolean isProtected, String targetName, String credComment, long lastModified, byte[] credPassword, Attribute[] credAttribs, String targetAlias, String userName) {
//		private WindowsCredential(boolean isProtected, int bitFlags, int credType, String targetName, String credComment, long lastModified, byte[] credPassword, int credPersist, Entry<String, byte[]>[] credAttribs, String credTargetAlias, String userName) {
			this(isProtected, targetName, credComment, targetAlias, userName);
//			this.meFlags = CredentialFlag.of(bitFlags);
//			this.meType = credType;
			this.meLastModified = Instant.ofEpochMilli((lastModified  - 116444736000000000L) / 10000L); // (# - epoch difference) / (milli-to-hundredth-nano)
			this.meBytePassword = credPassword == null ? new byte[0] : credPassword;
//			this.mePersist = credPersist;
			for (var attr : credAttribs) {
				this.meAttributes.put(attr.meKeyword, attr.meValue);
			}
			this.meIsChanged = false; // It matches the system
		}

		/**
		 * A private constructor (just for code re-factoring).
		 * @param isProtected					specifies if the credential is protected (by the system with an elevated protection).
		 * @param targetName					specifies the target/purpose name of this credential.
		 * @param credComment					specifies any comments associated with the credential.
		 * @param targetAlias					specifies an alias for the target/purpose name.
		 * @param userName						specifies the credential's designated user-name.
		 * @throws IllegalArgumentException		when target-/purpose-name or user-name is null or empty.
		 */
		private WindowsCredential(boolean isProtected, String targetName, String credComment, String targetAlias, String userName) throws IllegalArgumentException {
			if (targetName == null || userName == null || targetName.isEmpty() || userName.isEmpty()) {
				throw new IllegalArgumentException("The credential's target-/purpose-name and user-name cannot be null or empty.");
			}
			this.meIsProtected = isProtected;
			this.meAttributes = new LinkedHashMap<>();
			this.meTargetName = targetName;
			this.meComment = credComment;
			this.meTargetAlias = targetAlias;
			this.meUserName = userName;
		}

		/**
		 * Adds the specified attribute to this credential object.
		 * @param newAttrib						the attribute to be added.
		 * @return								this credential object (to be able to chain-add).
		 * @throws IllegalArgumentException		when the passed attribute is null, or one of its content is null or empty.
		 */
		public WindowsCredential addAttribute(Attribute newAttrib) throws IllegalArgumentException {
			if (newAttrib == null) {
				throw new IllegalArgumentException("The passed attribute cannot be null.");
			}
			return this.addAttribute(newAttrib.meKeyword, newAttrib.meStringValue);
		}

		/**
		 * Adds the specified attribute (as a separate name/keyword and value) to this credential object.
		 * @param newAttribKeyword				the attribute's name/keyword.
		 * @param newAttribValue				the attribute's value.
		 * @return								this credential object (to be able to chain-add).
		 * @throws IllegalArgumentException		when attribute's name/keyword or value is null or empty.
		 */
		public WindowsCredential addAttribute(String newAttribKeyword, String newAttribValue) throws IllegalArgumentException {
			if (newAttribKeyword == null || newAttribValue == null || newAttribKeyword.isEmpty() || newAttribValue.isEmpty()) {
				throw new IllegalArgumentException("The attribute's name/keyword and value cannot be null or empty.");
			}
			this.meAttributes.put(newAttribKeyword, newAttribValue.getBytes());
			this.meIsChanged = true;
			return this;
		}

//		public EnumSet<CredentialFlag> getFlags() {
//			return this.meFlags;
//		}
//
//		public CredentialType getType() {
//			return CredentialType.of(this.meType);
//		}

		/**
		 * Used to get this credential's target-/purpose-name.
		 * @return	this credential's target-/purpose-name.
		 */
		public String getTargetName() {
			return this.meTargetName;
		}

		/**
		 * Used to get this credential's optional comment (if any).
		 * @return	this credential's comment (if any).
		 */
		public String getComment() {
			return this.meComment;
		}

		/**
		 * Used to set this credential's comment.
		 * @param comment	the new credential's comment.
		 */
		public void setComment(String newComment) {
			if (this.meComment == null || !this.meComment.equals(newComment)) {
				this.meComment = newComment;
				this.meIsChanged = true;
			}
		}

		/**
		 * Used to get this credential's last modification zoned date and time.
		 * @return	this credential's last modification zoned date and time.
		 */
		public ZonedDateTime getLastModified() {
			return ZonedDateTime.ofInstant(this.meLastModified, ZoneId.systemDefault());
		}

		/**
		 * Used to get this credential's designated password as a byte-array with an option to unprotection/decryption the password data.
		 * @param isUnprotect		specifies if unprotection/decryption should be applied first.
		 * @return					this credential's designated password as a byte-array.
		 * @throws NativeException	when an error happens in the native code during unprotection/decryption of password data.
		 */
		public byte[] getBytePassword(boolean isUnprotect) throws NativeException {
			if (!isUnprotect || this.meBytePassword.length == 0 || !this.meIsProtected) {
				return this.meBytePassword;
			}
			return CredentialManager.processBlob(this.meBytePassword, false, this.meIsProtected);
		}

		/**
		 * Used to get this credential's designated password as a byte-array (no unprotection/decryption is applied).
		 * @return	this credential's designated password as a byte-array.
		 */
		public byte[] getBytePassword() {
			return this.getBytePassword(false);
		}

		/**
		 * Used to get this credential's designated password as a String with an option to unprotection/decryption the password data.
		 * @param isUnprotect		specifies if unprotection/decryption should be applied first.
		 * @return					this credential's designated password as a String.
		 * @throws NativeException	when an error happens in the native code during unprotection/decryption of password data.
		 */
		public String getPassword(boolean isUnprotect) throws NativeException {
			return this.meBytePassword.length == 0 ? "" : new String(this.getBytePassword(isUnprotect), CredentialManager.me_CHARSET);
		}

		/**
		 * Used to get this credential's designated password as a String (no unprotection/decryption is applied).
		 * @return	this credential's designated password as a String.
		 */
		public String getPassword() {
			return this.getPassword(false);
		}

		/**
		 * Used to set this credential's designated password.
		 * @param newPassword		the new password.
		 * @throws NativeException	when an error happens in the native code during protection/encryption of
		 *							password data (if this credential object is protected/encrypted).
		 */
		public void setPassword(String newPassword) throws NativeException {
			if (newPassword == null || newPassword.isEmpty()) {
				if (this.meBytePassword == null || this.meBytePassword.length != 0) {
					this.meBytePassword = new byte[0];
					this.meIsChanged = true;
				}
			} else if (this.meBytePassword == null || !newPassword.equals(this.getPassword())) {
				this.meBytePassword = this.meIsProtected
						? CredentialManager.processBlob(newPassword.getBytes(CredentialManager.me_CHARSET), true, this.meIsProtected)
						: newPassword.getBytes(CredentialManager.me_CHARSET);
				this.meIsChanged = true;
			}
		}

//		public CredentialPersist getPersist() {
//			return CredentialPersist.of(this.mePersist);
//		}
//
//		public void setPersist(CredentialPersist newPersist) {
//			if (this.mePersist != newPersist.meVal) {
//				this.mePersist = newPersist.meVal;
//				this.meIsChanged = true;
//			}
//		}

		/**
		 * Used to get this credential's optional attributes.
		 * @return	an array of this credential's attributes.
		 */
		public Attribute[] getAttributes() {
			return this.meAttributes.entrySet().stream().map(Attribute::new).toArray(Attribute[]::new);
		}

		/**
		 * Used to get the optional alias for this credential's name/target-name.
		 * @return	the optional alias for this credential's name/target-name.
		 */
		public String getTargetAlias() {
			return this.meTargetAlias;
		}

		/**
		 * Used to set a new alias for this credential's name/target-name.
		 * @param newTargetAlias	the new alias for this credential's name/target-name.
		 */
		public void setTargetAlias(String newTargetAlias) {
			this.meTargetAlias = newTargetAlias;
			if (!this.meTargetAlias.equals(newTargetAlias)) {
				this.meTargetAlias = newTargetAlias;
				this.meIsChanged = true;
			}
		}

		/**
		 * Used to get this credential's designated user-name.
		 * @return	this credential's designated user-name.
		 */
		public String getUserName() {
			return this.meUserName;
		}

		/**
		 * Used to set this credential's designated user-name.
		 * @param userName the new user-name.
		 */
		public void setUserName(String newUserName) {
			if (!this.meUserName.equals(newUserName)) {
				this.meUserName = newUserName;
				this.meIsChanged = true;
			}
		}

		/**
		 * A customized 'toString' with the ability to specify whether to show this credential's password in the returned String, and
		 * whether to unprotect/decrypt the shown password (not recommended, but maybe for testing purposes).
		 * @param isShowPassword	specifies whether to show the password in the returned String.
		 * @param isUnprotect		specifies whether to unprotect/decrypt the shown password.
		 * @return					a string representation of this credential object.
		 * @throws NativeException	when an error happens in the native code during unprotection/decryption of password
		 *							data (if unprotection/decryption is requested).
		 */
		public String toString(boolean isShowPassword, boolean isUnprotect) throws NativeException {
			StringJoiner outJoin = WindowsCredential.me_IS_PRETTY_TO_STRING
					? new StringJoiner(",\n\t", "{\n\t", "\n}")
					: new StringJoiner(", ", "{", "}");
			outJoin.add("Target Name: \"" + this.meTargetName + '\"');
			outJoin.add("Target Alias: \"" + this.meTargetAlias + '\"');
//			outJoin.add("Flags: " + this.meFlags);
//			outJoin.add("Type: " + this.getType());
			outJoin.add("Comment: \"" + this.meComment + '\"');
			outJoin.add("Modified DateTime: \"" + this.getLastModified()
					.format(DateTimeFormatter.ofPattern("yyyy-MM-dd '['hh:mm:ss a']' '['Z 'GMT]'")) + '\"');
//			outJoin.add("Persist: " + this.getPersist());
			outJoin.add("User Name: \"" + this.meUserName + '\"');
			if (isShowPassword) {
				if (isUnprotect) {
					outJoin.add("Password: \"" + this.getPassword(isUnprotect) + '\"');
				} else {
					outJoin.add("BytePassword: \"" + Arrays.toString(this.getBytePassword(isUnprotect)) + '\"');
				}
			}
			outJoin.add("Attributes: " + Arrays.toString(this.getAttributes()));
			return outJoin.toString();
		}

		/**
		 * Used to get a string representation of this credential object. The representation will not show this credential's password.
		 */
		@Override
		public String toString() {
			return this.toString(false, false);
		}
	}


	// Fields
 	private static final Charset me_CHARSET = Charset.forName("UTF-16LE"); // Used to make the password always in UTF-16LE form.
	private static final int me_CRED_TYPE_GENERIC = 1; // We'll only use generic credentials for now
	private String mePrefix; // A prefix for every credential's target-/purpose-name

	/**
	 * A Constructor that takes the prefix that will be prepended to every credential's target-/purpose-name.
	 * @param credPrefix	the prefix that will be prepended to every credential's target-/purpose-name.
	 */
	public CredentialManager(String credPrefix) {
		this.mePrefix = credPrefix + (credPrefix.endsWith("_") ? "" : "_"); // Just an underscore
	}

	/**
	 * Used instead of 'JNI_OnUnload' in native code since 'JNI_OnUnload' doesn't always work (almost never).
	 */
	private static native void unloadLibrary();

	/**
	 * Used to get an array of credential objects based on a search filter. The filter is a prefix/suffix followed/preceded by an
	 * asterisk (*). For example, "AA*" will return all credentials with a target-name beginning with "AA".
	 * @param credFilter		the filter to search based on.
	 * @param isAll				'true' to get all of the credentials in the OS user's credential set. If this was 'true', then
	 *							'credFilter' should be 'null' or else a NativeException might be thrown.
	 * @return					an array of credential objects based on the search filter.
	 * @throws NativeException	when an error in the native code happens. The documented error-codes held by 'NativeException'
	 *							here are: '1168' when no credential exists matching the specified search filter, '1312' when the
	 *							logon session does not exist or there is no credential set associated with this logon session,
	 *							or '1004' when a search filter that is not valid was specified or 'isAll' is specified as 'true'
	 *							and the search filter is not 'null'. These are the common native code errors for this action but
	 *							other types of native code errors may occur.
	 */
	private static native WindowsCredential[] getCreds(String credFilter, boolean isAll) throws NativeException;

	/**
	 * Used to get the credential object with the specified target-/purpose-name and type.
	 * @param credTarget		the credential's target-/purpose-name (will be prepended with this credential-manager object's
	 *							prefix if it is not).
	 * @param credType			an integer for the credential's type (for now only '1' would be used for generic credentials).
	 * @return					the requested credentials object.
	 * @throws NativeException	when an error in the native code happens. The documented error-codes held by 'NativeException'
	 *							here are: '1168' when no credential exists matching the specified target-/purpose-name and type,
	 *							or '1312' when the logon session does not exist or there is no credential set associated with this
	 *							logon session. These are the common native code errors for this action but other types of native
	 *							code errors may occur.
	 */
	private static native WindowsCredential getCred(String credTarget, int credType) throws NativeException;

	/**
	 * Used to save the specified credential object to OS user's credential set.
	 * @param savedCred			the credential object to be saved.
	 * @throws NativeException	when an error in the native code happens. The documented error-codes held by 'NativeException'
	 *							here are: '1312' when the logon session does not exist or there is no credential set associated
	 *							with this logon session, or '2202'when the credential's user-name is not accepted by OS. These
	 *							are the common native code errors for this action but other types of native code errors may occur.
	 */
	private static native void saveCred(WindowsCredential savedCred) throws NativeException;

	/**
	 * Used to delete the credential with the specified target-/purpose-name and type from the OS user's credential set.
	 * @param credTarget		the credential's target-/purpose-name.
	 * @param credType			an integer for the credential's type (for now only '1' would be used for generic credentials).
	 * @throws NativeException	when an error in the native code happens. The documented error-codes held by 'NativeException'
	 *							here are: '1168' when no credential exists matching the specified target-/purpose-name and type,
	 *							'1312' when the logon session does not exist or there is no credential set associated with this
	 *							logon session. These are the common native code errors for this action but other types of native
	 *							code errors may occur.
	 */
	private static native void deleteCred(String credTarget, int credType) throws NativeException;

//	/**
//	 * Used to ask the OS's services to protect/encrypt the specified data, with an option to make the protection/encryption at
//	 * an elevated/administrator level.
//	 * @param theBlob			the data to be protected/encrypted in a byte-array form.
//	 * @param isEleveted		'true' to make the protection/encryption at an elevated/administrator level.
//	 * @return					the protected/encrypted form of the data in a byte-array form.
//	 * @throws NativeException	when elevated/administrator protection/encryption is specified and the user cancels elevated
//	 *							protection/encryption or when an error in the native code happens.
//	 */
//	private static native byte[] protectBlob(byte[] theBlob, boolean isEleveted) throws NativeException;
//
//	/**
//	 * Used to ask the OS's services to unprotect/decrypt the specified protected/encrypted data, with an option to make the
//	 * protection/encryption at an elevated/administrator level.
//	 * @param theBlob			the protected/encrypted data to be unprotected/decrypted in a byte-array form.
//	 * @param isEleveted		'true' to make the unprotection/decryption at an elevated/administrator level.
//	 * @return					the unprotected/decrypted form of the data in a byte-array form.
//	 * @throws NativeException	when elevated/administrator unprotection/decryption is specified and the user cancels elevated
//	 *							unprotection/decryption or when an error in the native code happens.
//	 */
//	private static native byte[] unprotectBlob(byte[] theBlob, boolean isEleveted) throws NativeException;

	/**
	 * Used to ask the OS's services to protect/encrypt or unprotect/decrypt the specified data, with an option to make the
	 * processing at an elevated/administrator level.
	 * @param theBlob			the data to be processed in a byte-array form.
	 * @param isProtect			'true' to protect/encrypt the specified data, or 'false' to unprotect/decrypt.
	 * @param isEleveted		'true' to make the processing at an elevated/administrator level.
	 * @return					the processed form of the data in a byte-array form.
	 * @throws NativeException	when elevated processing is specified and the user cancels the elevated processing or when an
	 *							error in the native code happens.
	 */
	private static native byte[] processBlob(byte[] theBlob, boolean isProtect, boolean isEleveted) throws NativeException;

	/**
	 * Used to get an array of all the credentials stored by OS that are related to this credential-manager object (that their
	 * target-/purpose-name prefixed with this credential-manager object's prefix).
	 * @return					all the credentials stored by OS that are related to this credential-manager object.
	 * @throws NativeException	when an error in the native code happens. The documented error-codes held by 'NativeException'
	 *							here are: '1168' when no credential exists matching this credential-manager object's prefix, or
	 *							'1312' when the logon session does not exist or there is no credential set associated with this
	 *							logon session. These are the common native code errors for this action but other types of native
	 *							code errors may occur.
	 */
	public WindowsCredential[] getCredentials() throws NativeException {
		return CredentialManager.getCreds(this.mePrefix + '*', false);
	}

	/**
	 * Used to get the credential object with the specified target-/purpose-name that is related to this credential-manager
	 * object (that its target-/purpose-name prefixed with this credential-manager object's prefix).
	 * @param credTarget		the credential's target-/purpose-name (will be prepended with this credential-manager object's
	 *							prefix if it is not).
	 * @return					the requested credentials object.
	 * @throws NativeException	when an error in the native code happens. The documented error-codes held by 'NativeException'
	 *							here are: '1168' when no credential exists matching the specified target-/purpose-name, or '1312'
	 *							when the logon session does not exist or there is no credential set associated with this logon
	 *							session. These are the common native code errors for this action but other types of native code
	 *							errors may occur.
	 */
	public WindowsCredential getCredential(String credTarget) throws NativeException {
		if (!credTarget.startsWith(this.mePrefix)) { // If the target is not prefixed (most likely the cred is new)
			credTarget = this.mePrefix + credTarget;
		}
		return CredentialManager.getCred(credTarget, CredentialManager.me_CRED_TYPE_GENERIC);
	}

	/**
	 * Used to save the specified credential object to OS user's credential set.
	 * @param savedCred			the credential object to be saved.
	 * @throws NativeException	when an error in the native code happens. The documented error-codes held by 'NativeException'
	 *							here are: '1312' when the logon session does not exist or there is no credential set associated
	 *							with this logon session, or '2202'when the credential's user-name is not accepted by OS. These
	 *							are the common native code errors for this action but other types of native code errors may occur.
	 */
	public void saveCredential(WindowsCredential savedCred) throws NativeException {
		if (savedCred.meIsChanged) {
			if (!savedCred.meTargetName.startsWith(this.mePrefix)) { // If the target is not prefixed (most likely the cred is new)
				savedCred.meTargetName = this.mePrefix + savedCred.meTargetName;
			}
			CredentialManager.saveCred(savedCred);
			savedCred.meIsChanged = false;
		}
	}

	/**
	 * Used to delete the specified credential object from the OS user's credential set (its target-/purpose-name will be prepended
	 * with this credential-manager object's prefix).
	 * @param deletedCred		the credential object to be deleted.
	 * @throws NativeException	when an error in the native code happens. The documented error-codes held by 'NativeException'
	 *							here are: '1168' when no credential exists matching the specified target-/purpose-name and type,
	 *							'1312' when the logon session does not exist or there is no credential set associated with this
	 *							logon session. These are the common native code errors for this action but other types of native
	 *							code errors may occur.
	 */
	public void deleteCredential(WindowsCredential deletedCred) throws NativeException {
		CredentialManager.deleteCred(
				(deletedCred.meTargetName.startsWith(this.mePrefix) ? deletedCred.meTargetName : this.mePrefix + deletedCred.meTargetName),
				CredentialManager.me_CRED_TYPE_GENERIC);
	}

	/**
	 * Used to delete the credential with the specified target-/purpose-name and type from the OS user's credential set. (the
	 * specified target-/purpose-name will be prepended with this credential-manager object's prefix).
	 * @param credTarget		the credential's target-/purpose-name.
	 * @throws NativeException	when an error in the native code happens. The documented error-codes held by 'NativeException'
	 *							here are: '1168' when no credential exists matching the specified target-/purpose-name and type,
	 *							'1312' when the logon session does not exist or there is no credential set associated with this
	 *							logon session. These are the common native code errors for this action but other types of native
	 *							code errors may occur.
	 */
	public void deleteCredential(String credTarget) throws NativeException {
		CredentialManager.deleteCred(
				(credTarget.startsWith(this.mePrefix) ? credTarget : this.mePrefix + credTarget), CredentialManager.me_CRED_TYPE_GENERIC);
	}

	/**
	 * Used to create and directly save a new credential object into OS user's credential set using the credential's specified parameters.
	 * @param isProtected					specifies if the credential is encrypted/protected (by the OS with an elevated protection).
	 * @param targetName					specifies the target-/purpose-name of this credential (cannot be null or empty).
	 * @param userName						specifies the credential's designated user-name (cannot be null or empty).
	 * @param credPassword					specifies the credential's designated password (can be null or empty -might change later-).
	 * @param credComment					specifies any comments associated with the credential (can be null or empty).
	 * @param targetAlias					specifies an alias for the target-/purpose-name (can be null or empty).
	 * @param credAttribs					specifies any optional attributes that are related to the new credential.
	 * @return								the newly created (and saved) credential object.
	 * @throws NativeException				when encrypted/protected credential is specified and the user cancels elevated protection or
	 *										when an error in the native code happens. The documented error-codes held by 'NativeException'
	 *										here are: '1312' when the logon session does not exist or there is no credential set associated
	 *										with this logon session, or '2202'when the credential's user-name is not accepted by OS. These
	 *										are the common native code errors for this action but other types of native code errors may occur.
	 * @throws IllegalArgumentException		when target-/purpose-name or user-name is null or empty.
	 */
	public WindowsCredential newCredential(boolean isProtected, String targetName, String userName, String credPassword,
			String credComment, String targetAlias, Attribute ...credAttribs) throws NativeException, IllegalArgumentException {
		var outCred = new WindowsCredential(isProtected, targetName, credComment, targetAlias, userName);
		if (credAttribs != null && credAttribs.length > 0) {
			for (var attr : credAttribs) {
				outCred.addAttribute(attr);
			}
		}
		this.saveCredential(outCred);
		return outCred;
	}

	/**
	 * Used to create and save a new credential object into OS user's credential set using the credential's essential and attribute parameters.
	 * @param isProtected					specifies if the credential is encrypted/protected (by the OS with an elevated protection).
	 * @param targetName					specifies the target-/purpose-name of this credential (cannot be null or empty).
	 * @param userName						specifies the credential's designated user-name (cannot be null or empty).
	 * @param credPassword					specifies the credential's designated password (can be null or empty -might change later-).
	 * @param credAttribs					specifies any optional attributes that are related to the new credential.
	 * @return								the newly created (and saved) credential object.
	 * @throws NativeException				when encrypted/protected credential is specified and the user cancels elevated protection or
	 *										when an error in the native code happens. The documented error-codes held by 'NativeException'
	 *										here are: '1312' when the logon session does not exist or there is no credential set associated
	 *										with this logon session, or '2202'when the credential's user-name is not accepted by OS. These
	 *										are the common native code errors for this action but other types of native code errors may occur.
	 * @throws IllegalArgumentException		when target-/purpose-name or user-name is null or empty.
	 */
	public WindowsCredential newCredential(boolean isProtected, String targetName,
			String userName, String credPassword, Attribute ...credAttribs) throws NativeException, IllegalArgumentException {
		return this.newCredential(isProtected, targetName, userName, credPassword, "", "", credAttribs);
	}

	/**
	 * Used to create and directly save a new credential object into OS user's credential set using the credential's essential parameters.
	 * @param isProtected					specifies if the credential is encrypted/protected (by the OS with an elevated protection).
	 * @param targetName					specifies the target-/purpose-name of this credential (cannot be null or empty).
	 * @param userName						specifies the credential's designated user-name (cannot be null or empty).
	 * @param credPassword					specifies the credential's designated password (can be null or empty -might change later-).
	 * @return								the newly created (and saved) credential object.
	 * @throws NativeException				when encrypted/protected credential is specified and the user cancels elevated protection or
	 *										when an error in the native code happens. The documented error-codes held by 'NativeException'
	 *										here are: '1312' when the logon session does not exist or there is no credential set associated
	 *										with this logon session, or '2202'when the credential's user-name is not accepted by OS. These
	 *										are the common native code errors for this action but other types of native code errors may occur.
	 * @throws IllegalArgumentException		when target-/purpose-name or user-name is null or empty.
	 */
	public WindowsCredential newCredential(boolean isProtected,
			String targetName, String userName, String credPassword) throws NativeException, IllegalArgumentException {
		return this.newCredential(isProtected, targetName, userName, credPassword, "", "");
	}

//	public byte[] protect(String theStr, boolean isEleveted) {
//		byte[] tmpArr = CredentialManager.protectBlob(theStr.getBytes(CredentialManager.me_CHARSET), isEleveted);
//		System.out.println(Arrays.toString(tmpArr) + "\n" + tmpArr.length);
//		return tmpArr;
//	}
//
//	public String unprotect(byte[] theCont, boolean isEleveted) {
//		byte[] tmpArr = CredentialManager.unprotectBlob(theCont, isEleveted);
//		System.out.println(Arrays.toString(tmpArr) + "\n" + tmpArr.length);
//		return new String(tmpArr, CredentialManager.me_CHARSET);
//	}

	/**
	 * The prefix that prepends all target-/purpose-names of the credentials that belong to this credential-manager object.
	 * @return	the prefix
	 */
	public String getPrefix() {
		return this.mePrefix;
	}
}
