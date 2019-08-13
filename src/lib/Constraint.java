package lib;

import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.StringJoiner;
import java.util.function.Predicate;

/**
 * An class that represents a type data constraint to validate the type data and its needed limits/validation (if any).
 * @author Janty Azmat
 */
public class Constraint<X> {
	// Fields
	private X meVal;
	private Class<X> meClass;
	private String meName;
	private boolean meIsConfid;
	private boolean meIsMand;
	private Predicate<X> meValidator;
	private String meInvMsg;

	/**
	 * A constructor the takes the type and name as parameters (the new Constraint object is not confidential, not mandatory, and its
	 * validator only checks for 'null').
	 * @param constraintType			the type (class) of the value of this new Constraint object.
	 * @param constraintName			the name for the new Constraint object.
	 * @throws NullPointerException		when the specified type or name for the new Constraint object is 'null'.
	 */
	public Constraint(Class<X> constraintType, String constraintName) throws NullPointerException {
		this(constraintType, constraintName, false, false, null, null);
	}

	/**
	 * A constructor the takes the type and name as parameters (the new Constraint object is not mandatory and its validator only checks for 'null').
	 * @param constraintType			the type (class) of the value of this new Constraint object.
	 * @param constraintName			the name for the new Constraint object.
	 * @param isConfidential			specifies if the value for the new Constraint object is confidential (and should be hidden like in passwords).
	 * @throws NullPointerException		when the specified type or name for the new Constraint object is 'null'.
	 */
	public Constraint(Class<X> constraintType, String constraintName, boolean isConfidential) throws NullPointerException {
		this(constraintType, constraintName, isConfidential, false, null, null);
	}

	/**
	 * A constructor the takes the type, name, and mandatory-check as parameters (the new Constraint object's validator only checks for 'null').
	 * @param constraintType			the type (class) of the value of this new Constraint object.
	 * @param constraintName			the name for the new Constraint object.
	 * @param isConfidential			specifies if the value for the new Constraint object is confidential (and should be hidden like in passwords).
	 * @param isMandatory				specifies if setting a value for the new Constraint object is mandatory.
	 * @throws NullPointerException		when the specified type or name for the new Constraint object is 'null'.
	 */
	public Constraint(Class<X> constraintType, String constraintName, boolean isConfidential, boolean isMandatory) throws NullPointerException {
		this(constraintType, constraintName, isConfidential, isMandatory, null, null);
	}

	/**
	 * A constructor the takes the type, name, confidentiality, mandatory-check, validation-message, and validator as parameters.
	 * @param constraintType			the type (class) of the value of this new Constraint object.
	 * @param constraintName			the name for the new Constraint object.
	 * @param isConfidential			specifies if the value for the new Constraint object is confidential (and should be hidden like in passwords).
	 * @param isMandatory				specifies if setting a value for the new Constraint object is mandatory.
	 * @param invalidMsg				the message to display when the value for the new Constraint object is invalid.
	 * @param valueValidator			a validator callback the Constraint object's value.
	 * @throws NullPointerException		when the specified type or name for the new Constraint object is 'null'.
	 */
	public Constraint(Class<X> constraintType, String constraintName, boolean isConfidential, boolean isMandatory, String invalidMsg, Predicate<X> valueValidator) throws NullPointerException {
		Objects.requireNonNull(constraintName, "The name for the new Constraint object cannot be 'null'.");
		this.meClass = constraintType;
		this.meName = constraintName;
		this.meIsConfid = isConfidential;
		this.meIsMand = isMandatory;
		if (valueValidator == null && invalidMsg == null) {
			this.meInvMsg = "Privided value cannot be null.";
		} else if (invalidMsg == null) {
			this.meInvMsg = "";
		} else {
			this.meInvMsg = invalidMsg;
		}
		this.meValidator = valueValidator == null ? (val) -> val != null : valueValidator; // Default it to a 'null-not-accepted' validation
	}

	/**
	 * Used to get the Constraint type's class.
	 * @return	the class of the Constraint type.
	 */
	public Class<X> getConstraintTypeClass() {
		return this.meClass;
	}

	/**
	 * Used to get the Constraint name.
	 * @return	the Constraint name.
	 */
	public String getName() {
		return this.meName;
	}

	/**
	 * Checks if the value for this Constraint object is confidential (and should be hidden like in passwords).
	 * @return	'true' if the value for this Constraint object is confidential.
	 */
	public boolean isConfidential() {
		return meIsConfid;
	}

	/**
	 * Checks if it is mandatory to set a value for this Constraint object.
	 * @return	'true' if it is mandatory to set a value for this Constraint object.
	 */
	public boolean isMandatory() {
		return this.meIsMand;
	}

	/**
	 * Used to get the message displayed when invalid data provided.
	 * @return	the message displayed when invalid data provided.
	 */
	public String getInvalidMessage() {
		return meInvMsg;
	}

	/**
	 * Validates if the specified value is accepted to be assigned to this Constraint.
	 * @param valueToValidate	the value to be validated.
	 * @return					'true' if the specified value is accepted.
	 */
	public boolean validate(X valueToValidate) {
		return this.meValidator.test(valueToValidate);
	}

	/**
	 * Checks if a value is set for this Constraint object.
	 * @return	'true' if the value for this Constraint object is set.
	 */
	public boolean isValuePresent() {
		return this.meVal != null;
	}

	/**
	 * Used to get the value of this Constraint object.
	 * @return							the value of this Constraint object.
	 * @throws NoSuchElementException	when the value of this Constraint object is not set yet.
	 */
	public X getValue() throws NoSuchElementException {
		if (this.meVal == null) {
			throw new NoSuchElementException("This Constraint object's value is not set.");
		}
		return this.meVal;
	}

	/**
	 * Used to set the value of this Constraint object.
	 * @param newValue						the new value to be set.
	 * @throws NullPointerException			when the specified new value is 'null'.
	 * @throws IllegalArgumentException		when the specified new value is not validated.
	 */
	public void setValue(X newValue) throws NullPointerException, IllegalArgumentException {
		Objects.requireNonNull(newValue);
		if (!this.validate(newValue)) {
			throw new IllegalArgumentException("The specified new value doesn't pass validation");
		}
		this.meVal = newValue;
	}

	/**
	 * Resets this Constraint object and clears its value ('null' cannot be used with 'setValue').
	 */
	public void reset() {
		this.meVal = null;
	}

	@Override
	public String toString() {
		StringJoiner outJoin = new StringJoiner(", ", "{", "}");
		outJoin.add(this.getName());
		outJoin.add(this.getConstraintTypeClass().getSimpleName());
		outJoin.add(this.isValuePresent() ? this.getValue().toString() : "[null]");
		return outJoin.toString();
	}
}
