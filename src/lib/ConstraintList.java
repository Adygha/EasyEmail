package lib;

import java.util.LinkedList;

/**
 * A class that represents a list of Constraint objects with some added functionality to a normal list.
 * @author Janty Azmat
 */
public class ConstraintList extends LinkedList<Constraint<? extends Object>> {
	// Fields
	private static final long serialVersionUID = 2617382012712090285L; // Just to stop the warning
	private boolean meIsFilled;

	public boolean isMandatoryDataPresent() {
		return !this.stream().anyMatch(cons -> cons.isMandatory() && !cons.isValuePresent());
	}

	/**
	 * Checks if the data is filled.
	 * @return	'true' if the data is filled.
	 */
	public boolean isFilled() {
		return this.meIsFilled;
	}

	/**
	 * Sets whether the data is filled.
	 * @param newIsFilled					new value for whether the data is filled.
	 * @throws IllegalArgumentException		when setting this ConstraintData object as filled while there are still mandatory data not filled.
	 */
	public void setFilled(boolean newIsFilled) throws IllegalArgumentException {
		if (newIsFilled && !this.isMandatoryDataPresent()) {
			throw new IllegalArgumentException("All mandatory data should be filled before setting this ConstraintData object as filled.");
		}
		this.meIsFilled = newIsFilled;
	}

	/**
	 * Resets all contained Constraint objects.
	 */
	public void resetAll() {
		this.stream().forEach(Constraint::reset);
	}
}
