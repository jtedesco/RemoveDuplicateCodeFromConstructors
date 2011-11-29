package edu.illinois.canistelCassabanana;

import java.util.List;

import org.eclipse.jdt.core.IMethod;
import org.eclipse.ltk.ui.refactoring.RefactoringWizard;

/**
 * The refactoring wizard class, which displays the input page and passes data on to the refactoring.
 * 
 * @author jon
 */
public class RemoveDuplicateCodeInConstructorsWizard extends RefactoringWizard {

	/**
	 * The constructors to be refactored
	 */
	List<IMethod> constructors;
	
	/**
	 * Builds the refactoring wizard.
	 * 
	 * 	@param refactoring	The refactoring object to launch
	 * 	@param pageTitle	The title to be displayed on the wizard
	 * 	@param constructors	The constructors selected to be refactored. If the class was selected to be refactored,
	 * 						all constructors from the compilation unit will be passed. 
	 */
	public RemoveDuplicateCodeInConstructorsWizard(RemoveDuplicateCodeInConstructorsRefactoring refactoring, 
			String pageTitle, List<IMethod> constructors) {
		super(refactoring, DIALOG_BASED_USER_INTERFACE | PREVIEW_EXPAND_FIRST_NODE);
		this.constructors = constructors;
		setDefaultPageTitle(pageTitle);
	}

	/**
	 * Add the only input page for this refactoring wizard
	 */
	@Override
	protected void addUserInputPages() {
		addPage(new RemoveDuplicateCodeInConstructorsInputPage("RemoveDuplicateCodeInConstructorsInputPage", constructors));
	}
}
