package edu.illinois.canistelCassabanana;

import java.util.LinkedList;
import java.util.List;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.internal.ui.refactoring.actions.RefactoringStarter;
import org.eclipse.jdt.ui.refactoring.RefactoringSaveHelper;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeSelection;
import org.eclipse.ltk.ui.refactoring.RefactoringWizard;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;

import edu.illinois.canistelCassabanana.utility.Utility;

/**
 * Action invoked by clicking on the menu item corresponding to this
 * refactoring. This should perform the
 * refactoring on the given code.
 * 
 * @author jon
 */
public class RemoveDuplicateCodeInConstructorsAction implements IWorkbenchWindowActionDelegate {

	/**
	 * The workbench window, or <code>null</code>
	 */
	private IWorkbenchWindow window;

	/**
	 * The top-level compilation unit
	 */
	private ICompilationUnit unit;

	/**
	 * {@inheritDoc}
	 */
	public void dispose() {
		// Do nothing
	}

	/**
	 * {@inheritDoc}
	 */
	public void init(final IWorkbenchWindow window) {
		this.window = window;
	}

	/**
	 * {@inheritDoc}
	 */
	public void run(final IAction action) {

		if (window != null) {

			// Get the selection in the active window
			ISelection selection = window.getSelectionService().getSelection();

			// If this is selected from the outline or package structure
			if (selection instanceof IStructuredSelection) {

				IStructuredSelection structuredSelection = (IStructuredSelection) selection;

				// Check to make sure something really is selected
				if (structuredSelection.size() > 0) {

					// Get the compilation unit out of this, if possible
					Object firstElement = structuredSelection.getFirstElement();
					if (firstElement instanceof ICompilationUnit) {

						// Create and launch the refactoring
						unit = (ICompilationUnit) firstElement;
						RemoveDuplicateCodeInConstructorsRefactoring refactoring = new RemoveDuplicateCodeInConstructorsRefactoring(
								unit);
						List<IMethod> constructors = Utility
								.getConstructors(this.unit);
						launchRefactoring(refactoring, constructors);

						// Else, check if it is a tree selection (if the user
						// has selected constructors to refactor from project)
					} else if (structuredSelection instanceof ITreeSelection) {

						refactorTreeSelection(action, (ITreeSelection) structuredSelection);

					} else {
						action.setEnabled(false);
					}

					// Return so that we don't disable the refactoring
					return;
				}
			}
		}

		// If any of these conditions failed, disable the refactoring
		action.setEnabled(false);
	}

	/**
	 * @param action
	 * @param structuredSelection
	 */
	private void refactorTreeSelection(final IAction action, ITreeSelection treeSelection) {
		List<Object> selectionList = treeSelection.toList();
		List<IMethod> selectedConstructors = new LinkedList<IMethod>();

		// Collect the list of selected items that are actually constructors
		for (Object selectedObject : selectionList) {
			try {

				// Check that each element is a constructor
				if (selectedObject instanceof IMethod && ((IMethod) selectedObject).isConstructor()) {
					selectedConstructors.add((IMethod) selectedObject);
				} else {
					break;
				}

			} catch (JavaModelException exception) {

				// If we get any strange Java errors, abort the refactoring
				System.out.println("Exception collecting selected constructors: " + exception.getLocalizedMessage());
				return;
			}
		}

		// If everything was a constructor, create and launch the refactoring
		if (selectedConstructors.size() == selectionList.size()) {
			RemoveDuplicateCodeInConstructorsRefactoring refactoring = new RemoveDuplicateCodeInConstructorsRefactoring(
					selectedConstructors);
			launchRefactoring(refactoring, selectedConstructors);
		}
	}

	/**
	 * Launches the refactoring.
	 */
	private void launchRefactoring(RemoveDuplicateCodeInConstructorsRefactoring refactoring, List<IMethod> constructors) {

		// Launch the refactoring
		RefactoringStarter refactoringStarter = new RefactoringStarter();
		RefactoringWizard wizard = new RemoveDuplicateCodeInConstructorsWizard(refactoring,
				RemoveDuplicateCodeInConstructorsConstants.REFACTORING_NAME, constructors);
		refactoringStarter.activate(wizard, window.getShell(),
				RemoveDuplicateCodeInConstructorsConstants.REFACTORING_NAME, RefactoringSaveHelper.SAVE_REFACTORING);
	}

	/**
	 * {@inheritDoc}
	 */
	public void selectionChanged(final IAction action, final ISelection selection) {
		if (selection instanceof IStructuredSelection) {
			action.setEnabled(true);
		} else {
			action.setEnabled(false);
		}
	}
}
