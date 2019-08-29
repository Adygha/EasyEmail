package model;

import java.nio.charset.Charset;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.AbstractMap.SimpleEntry;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.StringJoiner;
import model.SystemCredentialsManager.WindowsCredential.CredentialType;

/**
 * @author Janty Azmat
 */
public class SystemCredentialsManager {
	static {
		System.loadLibrary("CredMan");
	}

	public static class WindowsCredential {

		public static enum CredentialFlag {
//			CRED_FLAGS_NONE(0),
			CRED_FLAGS_PROMPT_NOW(2),
			CRED_FLAGS_USERNAME_TARGET(4),
			CRED_FLAGS_UNDOCUMENTED(8192); // TODO: The '8196' value passed as flags (this one + CRED_FLAGS_USERNAME_TARGET) on some site
			public static final EnumSet<CredentialFlag> CRED_FLAGS_NONE = EnumSet.noneOf(CredentialFlag.class);
//			public static final EnumSet<CredentialFlag> CRED_FLAGS_COMB_UNDOCUMENTED = EnumSet.of(CredentialFlag.CRED_FLAGS_USERNAME_TARGET, CredentialFlag.CRED_FLAGS_UNDOCUMENTED);

			private static final Map<Integer, CredentialFlag> stVals = new HashMap<>(CredentialFlag.values().length);
			static {
				for (var val : CredentialFlag.values()) {
					CredentialFlag.stVals.put(val.getFlagValue(), val);
				}
			}
			private int meVal;

			private CredentialFlag(int theVal) {
				this.meVal = theVal;
			}

			public int getFlagValue() {
				return this.meVal;
			}

			public static EnumSet<CredentialFlag> of(int bitFlags) {
				var outFlags = CredentialFlag.CRED_FLAGS_NONE;
				for (var flg : CredentialFlag.values()) {
					if ((bitFlags & flg.getFlagValue()) == flg.getFlagValue()) {
						outFlags.add(flg);
					}
				}
				return outFlags;
			}

//			public static int getBitFlags(EnumSet<CredentialFlag> credFlags) {
//				int outFlags = 0;
//				for (var flg : credFlags) {
//					outFlags |= flg.meVal;
//				}
//				return outFlags;
//			}

//			public static CredentialFlag of(int flagVal) {
//				CredentialFlag outFlag = CredentialFlag.stVals.get(flagVal);
////				return outFlag == null ? CredentialFlag.CRED_FLAGS_NONE : outFlag;
//				if (outFlag == null) {
//					throw new IllegalArgumentException("Provided flag value is invalid.");
//				}
//				return outFlag;
//			}
		}

		public static enum CredentialType {
			CRED_TYPE_GENERIC(1),
			CRED_TYPE_DOMAIN_PASSWORD(2),
			CRED_TYPE_DOMAIN_CERTIFICATE(3),
			CRED_TYPE_DOMAIN_VISIBLE_PASSWORD(4),
			CRED_TYPE_GENERIC_CERTIFICATE(5),
			CRED_TYPE_DOMAIN_EXTENDED(6),
			CRED_TYPE_MAXIMUM(7),
			CRED_TYPE_MAXIMUM_EX(CRED_TYPE_MAXIMUM.meVal + 1000);

			private static final Map<Integer, CredentialType> stVals = new HashMap<>(CredentialType.values().length);
			static {
				for (var val : CredentialType.values()) {
					CredentialType.stVals.put(val.meVal, val);
				}
			}
			private int meVal;

			private CredentialType(int theVal) {
				this.meVal = theVal;
			}

//			public int getTypeValue() {
//				return this.meVal;
//			}

			public static CredentialType of(int typeValue) {
				CredentialType outType = CredentialType.stVals.get(typeValue);
				if (outType == null) {
					throw new IllegalArgumentException("Provided type value is invalid.");
				}
				return outType;
			}
		}

		public static enum CredentialPersist {
			CRED_PERSIST_SESSION(1),
			CRED_PERSIST_LOCAL_MACHINE(2),
			CRED_PERSIST_ENTERPRISE(3);

			private static final Map<Integer, CredentialPersist> stVals = new HashMap<>(CredentialPersist.values().length);
			static {
				for (var val : CredentialPersist.values()) {
					CredentialPersist.stVals.put(val.meVal, val);
				}
			}
			private int meVal;

			private CredentialPersist(int theVal) {
				this.meVal = theVal;
			}

//			public int getPersistValue() {
//				return this.meVal;
//			}

			public static CredentialPersist of(int typeValue) {
				CredentialPersist outPers = CredentialPersist.stVals.get(typeValue);
				if (outPers == null) {
					throw new IllegalArgumentException("Provided type value is invalid.");
				}
				return outPers;
			}
		}

		// Fields
		private static final boolean me_IS_PRETTY_TO_STRING = true;
		private static final Charset me_CHARSET = Charset.forName("UTF-16LE");
		private EnumSet<CredentialFlag> meFlags;
		private int meType;
		private String meTargetName;
		private String meComment;
		private Instant meLastModified;
		private byte[] meBytePassword;
		private int mePersist;
		private Map<String, byte[]> meAttributes;
		private String meTargetAlias;
		private String meUserName;
		private boolean meIsChanged;

		public WindowsCredential(EnumSet<CredentialFlag> credFlags, CredentialType credType, String targetName, String credComment, String credPassword, CredentialPersist credPersist, String credAlias, String userName) {
			this(targetName, credComment, credAlias, userName);
			this.meFlags = credFlags;
			this.meType = credType.meVal;
			this.meLastModified = Instant.now();
			this.setPassword(credPassword);
			this.mePersist = credPersist.meVal;
			this.meIsChanged = false;
		}

		@SuppressWarnings("unused")
		private WindowsCredential(int bitFlags, int credType, String targetName, String credComment, long lastModified, byte[] credPassword, int credPersist, Entry<String, byte[]>[] credAttribs, String credTargetAlias, String userName) {
			this(targetName, credComment, credTargetAlias, userName);
			this.meFlags = CredentialFlag.of(bitFlags);
			this.meType = credType;
			this.meLastModified = Instant.ofEpochMilli((lastModified  - 116444736000000000L) / 10000L);
			this.meBytePassword = credPassword == null ? new byte[0] : credPassword;
			this.mePersist = credPersist;
			for (var entry : credAttribs) {
				this.meAttributes.put(entry.getKey(), entry.getValue());
			}
			this.meIsChanged = false;
		}

		private WindowsCredential(String targetName, String credComment, String credTargetAlias, String userName) {
			this.meAttributes = new LinkedHashMap<>();
			this.meTargetName = targetName;
			this.meComment = credComment;
			this.meTargetAlias = credTargetAlias;
			this.meUserName = userName;
		}

		public WindowsCredential addAttribute(Entry<String, String> newAttrib) {
			return this.addAttribute(newAttrib.getKey(), newAttrib.getValue());
		}

		public WindowsCredential addAttribute(String newAttribKey, String newAttribValue) {
			Objects.requireNonNull(newAttribKey, "New attribute key cannot be null.");
			Objects.requireNonNull(newAttribValue, "New attribute valuey cannot be null.");
			this.meAttributes.put(newAttribKey, newAttribValue.getBytes());
			this.meIsChanged = true;
			return this;
		}

		/**
		 * @return the flags
		 */
		public EnumSet<CredentialFlag> getFlags() {
			return this.meFlags;
		}

		/**
		 * @return the type
		 */
		public CredentialType getType() {
			return CredentialType.of(this.meType);
		}

		/**
		 * @return the targetName
		 */
		public String getTargetName() {
			return this.meTargetName;
		}

		/**
		 * @return the comment
		 */
		public String getComment() {
			return this.meComment;
		}

		/**
		 * @param comment the comment to set
		 */
		public void setComment(String newComment) {
			if (!this.meComment.equals(newComment)) {
				this.meComment = newComment;
				this.meIsChanged = true;
			}
		}

		/**
		 * @return the lastModifiedEpoch
		 */
		public ZonedDateTime getLastModified() {
//			return LocalDateTime.ofEpochSecond((this.meLastModifiedEpoch - 116444736000000000L) / 10000000, 0, OffsetDateTime.now().getOffset());
			return ZonedDateTime.ofInstant(this.meLastModified, ZoneId.systemDefault());
		}

		/**
		 * @return the bytePassword
		 */
		public byte[] getBytePassword() {
			return this.meBytePassword;
		}

		/**
		 * @return the password
		 */
		public String getPassword() {
			return this.meBytePassword.length == 0 ? null : new String(this.meBytePassword, WindowsCredential.me_CHARSET);
		}

		/**
		 * @param newPassword
		 */
		public void setPassword(String newPassword) {
			if (newPassword == null || newPassword.isEmpty()) {
				if (this.meBytePassword == null || this.meBytePassword.length != 0) {
					this.meBytePassword = new byte[0];
					this.meIsChanged = true;
				}
			} else if (this.meBytePassword == null || !newPassword.equals(this.getPassword())) {
				this.meBytePassword = newPassword.getBytes(WindowsCredential.me_CHARSET);
				this.meIsChanged = true;
			}
		}

		/**
		 * @return the persist
		 */
		public CredentialPersist getPersist() {
			return CredentialPersist.of(this.mePersist);
		}

		/**
		 * @param persist the persist to set
		 */
		public void setPersist(CredentialPersist newPersist) {
			if (this.mePersist != newPersist.meVal) {
				this.mePersist = newPersist.meVal;
				this.meIsChanged = true;
			}
		}

		/**
		 * @return the attributes
		 */
		@SuppressWarnings("unchecked")
		public Entry<String, String>[] getAttributes() {
			return this.meAttributes.entrySet().stream()
					.map(attr -> new SimpleEntry<String, String>(attr.getKey(), new String(attr.getValue()))).toArray(Entry[]::new);
//			this.meAttributes.entrySet().stream().toArray(attr -> new SimpleEntry<String, String>(attr.getKey(), new String(attr.getValue())));
		}

		/**
		 * @return the byteAttributes
		 */
		@SuppressWarnings("unchecked")
		public Entry<String, byte[]>[] getByteAttributes() {
			return this.meAttributes.entrySet().toArray(new Entry[0]);
		}

		/**
		 * @return	the targetAlias
		 */
		public String getTargetAlias() {
			return this.meTargetAlias;
		}

		/**
		 * @param newTargetAlias	the targetAlias to set
		 */
		public void setTargetAlias(String newTargetAlias) {
			this.meTargetAlias = newTargetAlias;
			if (!this.meTargetAlias.equals(newTargetAlias)) {
				this.meTargetAlias = newTargetAlias;
				this.meIsChanged = true;
			}
		}

		/**
		 * @return the userName
		 */
		public String getUserName() {
			return this.meUserName;
		}

		/**
		 * @param userName the userName to set
		 */
		public void setUserName(String newUserName) {
			this.meUserName = newUserName;
			if (!this.meUserName.equals(newUserName)) {
				this.meUserName = newUserName;
				this.meIsChanged = true;
			}
		}

		@Override
		public String toString() {
			StringJoiner outJoin = WindowsCredential.me_IS_PRETTY_TO_STRING ? new StringJoiner(",\n\t", "{\n\t", "\n}") : new StringJoiner(", ", "{", "}");
			outJoin.add("Target Name: " + this.meTargetName);
			outJoin.add("Target Alias: " + this.meTargetAlias);
			outJoin.add("Flags: " + this.meFlags);
			outJoin.add("Type: " + this.getType());
			outJoin.add("Comment: " + this.meComment);
			outJoin.add("Modified DateTime: " + this.getLastModified()
					.format(DateTimeFormatter.ofPattern("yyyy-MM-dd '['hh:mm:ss a']' '['Z 'GMT]'")));
			outJoin.add("Persist: " + this.getPersist());
			outJoin.add("User Name: " + this.meUserName);
			outJoin.add("BytePassword: " + Arrays.toString(this.meBytePassword));
			outJoin.add("Password: " + this.getPassword());
			outJoin.add("Attributes: " + Arrays.toString(this.getAttributes()));
			return outJoin.toString();
		}
	}

	// Fields
//	private String mePrefix;
//
//	public SystemCredentialsManager(String credPrefix) {
//		this.mePrefix = credPrefix;
//	}

	private static native WindowsCredential[] getCreds(String credFilter, boolean isAll);

	private static native WindowsCredential getCred(String credTarget, int credType);

	private static native int newCred(WindowsCredential newCred);

	private static native int updateCred(WindowsCredential updatedCred);

	private static native int deleteCred(String credTarget, int credType);

	private static native String getErrMsg(int errCode);

	public static WindowsCredential[] getWindowsCredentials() {
		return SystemCredentialsManager.getCreds(null, false);
	}

	public static WindowsCredential[] getWindowsCredentials(String credFilter) {
		return SystemCredentialsManager.getCreds(credFilter, false);
	}

	public static WindowsCredential[] getWindowsCredentialsAll() {
		return SystemCredentialsManager.getCreds(null, true);
	}

	public static WindowsCredential getWindowsCredential(String credTarget, CredentialType credType) {
		return SystemCredentialsManager.getCred(credTarget, credType.meVal);
	}

	public static int newCredential(WindowsCredential newCred) {
		return SystemCredentialsManager.newCred(newCred);
	}

	public static int updateCredential(WindowsCredential updatedCred) {
		int outExitCode = 0;
		if (updatedCred.meIsChanged) {
			outExitCode = SystemCredentialsManager.updateCred(updatedCred);
			updatedCred.meIsChanged = false;
		}
		return outExitCode;
	}

	public static int deleteCredential(WindowsCredential updatedCred) {
		return SystemCredentialsManager.deleteCred(updatedCred.meTargetName, updatedCred.meType);
	}

	public static int deleteCredential(String credTarget, CredentialType credType) {
		return SystemCredentialsManager.deleteCred(credTarget, credType.meVal);
	}

	public static String getErrorMessage(int errorCode) {
		return SystemCredentialsManager.getErrMsg(errorCode);
	}

//	/**
//	 * @return	the prefix
//	 */
//	public String getPrefix() {
//		return this.mePrefix;
//	}
//
//	/**
//	 * @param newPrefix		the new prefix
//	 */
//	public void setPrefix(String newPrefix) {
//		this.mePrefix = newPrefix;
//	}
}
