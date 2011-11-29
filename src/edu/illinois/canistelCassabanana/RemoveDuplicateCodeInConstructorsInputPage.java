package edu.illinois.canistelCassabanana;

import java.util.List;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.ILocalVariable;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.Signature;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.ui.refactoring.UserInputWizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.TableEditor;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

/**
 * The input page for the refactoring wizard. This class will set the fields corresponding to user input
 * 	on the refactoring object.
 * 
 * @author jon
 */
public class RemoveDuplicateCodeInConstructorsInputPage extends UserInputWizardPage {

	/**
	 * The list of selected constructors, to pass to the refactoring
	 */
	List<IMethod> constructors;

	/**
	 * The refactoring to launch
	 */
	RemoveDuplicateCodeInConstructorsRefactoring refactoring;
	
	/**
	 * Combo box to select the access modifier for the new constructor or helper method
	 */
	Combo accessModifierComboBox;

	/**
	 * Check box to specify whether or not the user would prefer to create a helper method
	 * 	over a new constructor, if it is necessary.
	 */
	Button helperMethodCheckBox;
	
	/**
	 * Text box to specify the name of the helper method to create instead of a constructor,
	 * 	if the checkbox option has been set.
	 */
	Text helperMethodTextBox;

	/**
	 * Create this input page, initializing the refactoring object and list of constructors.
	 * 
	 * 	@param name			The name to display on the input page
	 * 	@param constructors	The constructors to selected to refactor
	 */
	public RemoveDuplicateCodeInConstructorsInputPage(String name, List<IMethod> constructors) {
		super(name);
		this.constructors = constructors;
		this.refactoring = getRemoveDuplicateCodeInConstructorsRefactoring();
	}

	
	/**
	 * Create the GUI controls for the input page
	 * 
	 * 	@param parent	The parent composite object to which to attach all controls	
	 */
	@Override
	public void createControl(Composite parent) {

		// Create an object to hold all of the controls
		Composite composite = new Composite(parent, SWT.NONE);
		setControl(composite);

		// Lay out controls in three columns
		GridLayout layout = new GridLayout();
		layout.numColumns = 3;
		composite.setLayout(layout);

		//
		buildAccessModifierOptions(composite);

		//
		buildHelperMethodOptions(composite);

		// Prepare the interface and handle the user input
		composite.pack();
		handleInput();
	}


	/**
	 * Create the GUI components for the helper method options, and add them to the parent 
	 * 	controls GUI element.
	 * 
	 * @param composite	The parent for these controls
	 */
	private void buildHelperMethodOptions(Composite composite) {
		
		// Create the helper method check box
		GridData checkBoxWrapper = new GridData(SWT.HORIZONTAL);
		checkBoxWrapper.horizontalAlignment = GridData.FILL;
		checkBoxWrapper.horizontalSpan = 2;
		checkBoxWrapper.grabExcessHorizontalSpace = true;
		helperMethodCheckBox = new Button(composite, SWT.CHECK);
		helperMethodCheckBox.setText("Create &helper method instead of constructor");
		helperMethodCheckBox.setLayoutData(checkBoxWrapper);
		helperMethodCheckBox.addSelectionListener(getHelperMethodCheckBoxListener());

		// Create the text box for specifying the helper method name
		GridData helperMethodTextBoxWrapper = new GridData(SWT.HORIZONTAL);
		helperMethodTextBoxWrapper.grabExcessHorizontalSpace = true;
		helperMethodTextBox = new Text(composite, SWT.BORDER);
		helperMethodTextBox.setEnabled(false);
		helperMethodTextBox.setLayoutData(helperMethodTextBoxWrapper);
	}


	/**
	 * Create the GUI components for the access modifier options, and add them to the parent 
	 * 	controls GUI element.
	 * 
	 * @param composite	The parent for these controls
	 */
	private void buildAccessModifierOptions(Composite composite) {
		
		// Create the access modifier label
		GridData accessModifierLabelWrapper = new GridData(SWT.HORIZONTAL);
		accessModifierLabelWrapper.horizontalAlignment = GridData.FILL;
		accessModifierLabelWrapper.horizontalSpan = 2;
		Label label = new Label(composite, SWT.NONE);
		label.setText("&Access modifier:");
		label.setLayoutData(accessModifierLabelWrapper);

		// Create the access modifier combo box
		GridData accessModifierComboBoxWrapper = new GridData(SWT.HORIZONTAL);
		String accessModifiers[] = { "public", "private", "protected", "package" };
		accessModifierComboBoxWrapper.grabExcessHorizontalSpace = true;
		accessModifierComboBox = new Combo(composite, SWT.READ_ONLY);
		accessModifierComboBox.setItems(accessModifiers);
		accessModifierComboBox.select(0);
		accessModifierComboBox.setLayoutData(accessModifierComboBoxWrapper);
	}

	
	/**
	 * Handle the user input, setting the corresponding options on the refactoring object.
	 */
	private void handleInput() {

		// Skip if the refactoring has not yet been initialized
		if (refactoring != null) {
			
			// Grab the helper method options
			boolean useHelperMethod = helperMethodCheckBox.getSelection();
			if (useHelperMethod) {
				refactoring.setUseHelperFunction(useHelperMethod);
				refactoring.setHelperFunctionName(helperMethodTextBox.getText());
			} else {
				refactoring.setHelperFunctionName(helperMethodTextBox.getText());
			}
			
			// Grab the access modifier option
			refactoring.setAccessModifier(accessModifierComboBox.getText());
		}
	}

	
	/**
	 * Gets the refactoring associated with this input page from the parent input page.
	 * 
	 * @return The refactoring associated with this input page
	 */
	private RemoveDuplicateCodeInConstructorsRefactoring getRemoveDuplicateCodeInConstructorsRefactoring() {
		return (RemoveDuplicateCodeInConstructorsRefactoring) getRefactoring();
	}

	
	/**
	 * Enables or disable the helper method text box based on the check box state.
	 */
	private void enableDisableHelperMethodTextBox() {
		if (helperMethodCheckBox.getSelection()) {
			helperMethodTextBox.setEnabled(true);
		} else {
			helperMethodTextBox.setEnabled(false);
		}
	}
	

	/**
	 * Gets the listener for the helper method check box, which enables/disables the corresponding input 
	 * 	text box field.
	 * 
	 * 	@return	The selection listener to trigger enabling/disabling the helper method input box 
	 */
	private SelectionListener getHelperMethodCheckBoxListener() {
		return new SelectionListener() {

			@Override
			public void widgetSelected(SelectionEvent event) {
				enableDisableHelperMethodTextBox();
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent event) {
				enableDisableHelperMethodTextBox();
			}
		};
	}
}
