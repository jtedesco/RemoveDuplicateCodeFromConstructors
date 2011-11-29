package edu.illinois.canistelCassabanana;

import java.util.Map;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.ltk.core.refactoring.Refactoring;
import org.eclipse.ltk.core.refactoring.RefactoringDescriptor;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;

/**
 * Descriptor object to allow refactoring to be used in refactoring scripts.
 * 
 * @author jon
 */
public class RemoveDuplicateCodeInConstructorsDescriptor extends RefactoringDescriptor {
	
	public static final String REFACTORING_ID= "org.eclipse.remove.duplicate.code.in.constructors";
	
	private Map arguments;

	public Map getArguments() {
		return arguments;
	}

	protected RemoveDuplicateCodeInConstructorsDescriptor(String project, String description, String comment, Map arguments) {
		super(REFACTORING_ID, project, description, comment, RefactoringDescriptor.STRUCTURAL_CHANGE | RefactoringDescriptor.MULTI_CHANGE);
		this.arguments = arguments;
	}

	@Override
	public Refactoring createRefactoring(RefactoringStatus status) throws CoreException {
		RemoveDuplicateCodeInConstructorsRefactoring refactoring = new RemoveDuplicateCodeInConstructorsRefactoring();
		return refactoring;
	}
	
	

}
