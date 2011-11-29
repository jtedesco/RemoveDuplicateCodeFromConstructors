package edu.illinois.canistelCassabanana;

/**
 * Contains UI constants for the refactoring, including error messages and text constants to appear in the UI.
 * 
 * @author jon
 */
public class RemoveDuplicateCodeInConstructorsConstants {

	// Error messages for refactoring
	public static final String NO_DUPLICATE_CODE_MESSAGE = "No duplicate code found in constructors. "
			+ "This refactoring must be performed on containing two or more constructors that contain duplicate code.";
	public static final String ONE_CONSTRUCTOR_MESSAGE = "Only one constructor found. "
			+ "This refactoring must be performed on containing two or more constructors.";
	public static final String NO_CONSTRUCTORS_MESSAGE = "No constructors found to refactor. "
			+ "This refactoring must be performed on containing two or more constructors.";
	public static final String NULL_CONSTRUCTORS_MESSAGE = "Error finding constructors to refactor.";
	public static final String CHECKING_PRECONDITIONS_MESSAGE = "Checking preconditions for refactoring ...";
	public static final String UNEXPECTED_ERROR_MESSAGE = "An unexpected exception occurred. See the error log for more details.";
	private static final String UNABLE_TO_REFACTOR = "Preconditions satisfied, but unable to refactor.";

	// UI text constants for this refactoring
	public static final String REFACTORING_ID = "RemoveDuplicateCodeInConstructorsRefactoring";
	public static final String REFACTORING_NAME = "Remove Duplicate Code In Constructors";
	public static final String REFACTORING_DESCRIPTION = "Remove duplicate code from constructors by delegating "
			+ "common construction code to as few constructors or as possible.";
}
