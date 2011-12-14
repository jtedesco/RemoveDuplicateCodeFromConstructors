package edu.illinois.canistelCassabanana;

/**
 * Contains UI constants for the refactoring, including error messages and text constants to appear in the UI.
 * 
 * @author jon
 */
public class RemoveDuplicateCodeInConstructorsConstants {

	// Error messages for refactoring
	public static final String ERROR_REFACTORING_MESSAGE = "Error performing refactoring. ";
	public static final String NO_DUPLICATE_CODE_MESSAGE = ERROR_REFACTORING_MESSAGE + "No duplicate code found in constructors. "
			+ "This refactoring must be performed on code with two or more constructors that contain duplicate code.";
	public static final String ONE_CONSTRUCTOR_MESSAGE = ERROR_REFACTORING_MESSAGE + "Only one constructor found. "
			+ "This refactoring must be performed on code containing two or more constructors.";
	public static final String NO_CONSTRUCTORS_MESSAGE = ERROR_REFACTORING_MESSAGE + "No constructors found to refactor. "
			+ "This refactoring must be performed on code containing two or more constructors.";
	public static final String UNEXPECTED_ERROR_MESSAGE = "An unexpected error occurred. See the error log for more details.";
	public static final String NULL_CONSTRUCTORS_MESSAGE = UNEXPECTED_ERROR_MESSAGE;
	public static final String CHECKING_PRECONDITIONS_MESSAGE = "Checking preconditions for refactoring ...";
	public static final String UNABLE_TO_REFACTOR = ERROR_REFACTORING_MESSAGE + UNEXPECTED_ERROR_MESSAGE;
	public static final String HELPER_FUNCTION_BAD_NAME_MESSAGE = ERROR_REFACTORING_MESSAGE + "Invalid name for helper function.";
	public static final String HELPER_FUNCTION_EXISTING_NAME_MESSAGE = ERROR_REFACTORING_MESSAGE + "Invalid name for helper function. Using an existing name for helper function not allowed.";
	public static final String ASSIGNMENT_DEPENDENCY_MESSAGE = ERROR_REFACTORING_MESSAGE + "Unable to resolve variable dependencies.";

	// UI text constants for this refactoring
	public static final String REFACTORING_ID = "RemoveDuplicateCodeInConstructorsRefactoring";
	public static final String REFACTORING_NAME = "Remove Duplicate Code In Constructors";
	public static final String REFACTORING_DESCRIPTION = "Remove duplicate code from constructors by delegating "
			+ "common construction code to as few constructors or as possible.";
}
