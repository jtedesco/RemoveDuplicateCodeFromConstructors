package edu.illinois.canistelCassabanana.visitors;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.FieldAccess;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.MethodRefParameter;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.Statement;

/**
 * Collects the field variables found in the AST subtree from the starting root
 * node.
 * 
 * @author jon
 */
public class FieldVariableVisitor extends ASTVisitor {

	/**
	 * The fields that appear in any simple name nodes we visit.
	 */
	private Set<IVariableBinding> fields;

	/**
	 * The fields assigned by parameters
	 */
	private Set<IVariableBinding> fieldsAssigningParameters;

	/**
	 * The parameters of this constructor (this is modified as we visit)
	 */
	List<SimpleName> parameterNames;

	/**
	 *  The map of parameter bindings to field bindings
	 */
	Map<IVariableBinding, IVariableBinding> parameterToFieldBindings;

	/**
	 * Builds the visitor with the list of parameter names, used to visit a
	 * constructor and resolve bindings of parameters to fields.
	 * 
	 * @param parameterNames
	 *            The list of parameter
	 */
	public FieldVariableVisitor(List<SimpleName> parameterNames) {
		super();
		this.fields = new HashSet<IVariableBinding>();
		if(parameterNames != null) {
			this.parameterNames = new ArrayList<SimpleName>(parameterNames);
		}
		this.fieldsAssigningParameters = new HashSet<IVariableBinding>();
		this.parameterToFieldBindings = new HashMap<IVariableBinding, IVariableBinding>();
	}

	/**
	 * Visit all simple name nodes that access fields
	 */
	@Override
	public boolean visit(SimpleName node) {

		// Check if this is a variable binding
		IBinding binding = node.resolveBinding();
		if (binding instanceof IVariableBinding) {

			// Check if this is bound to a field
			IVariableBinding variableBinding = (IVariableBinding) binding;
			if (variableBinding.isField()) {
				fields.add(variableBinding);

				// Add this field to our list of fields assigning parameters if
				// necessary
				checkFieldAssignsParameter(node, variableBinding);

				return true;
			}
		}
		return true;
	}

	/**
	 * Visit all field access nodes
	 */
	@Override
	public boolean visit(FieldAccess node) {

		// Add this field assignment access
		IVariableBinding variableBinding = node.resolveFieldBinding();
		fields.add(variableBinding);

		// Add this field to our list of fields assigning parameters if
		// necessary
		checkFieldAssignsParameter(node, variableBinding);

		return false;
	}

	/**
	 * Checks if the variable binding assigns a parameter, and if it does, adds
	 * it to our list.
	 * 
	 * @param node
	 *            The AST node we're investigating
	 * @param fieldBinding
	 *            The binding for the variable assigned (a field)
	 */
	private void checkFieldAssignsParameter(ASTNode node, IVariableBinding fieldBinding) {

		// Check it's assigning a parameter
		if (parameterNames != null) {

			// Get the parent assignment node
			if (node.getParent() instanceof Assignment) {
				Assignment fieldAssignment = (Assignment) node.getParent();

				// Get the RHS of the assignment
				if (fieldAssignment.getRightHandSide() instanceof SimpleName) {
					SimpleName assignedVariableName = (SimpleName) fieldAssignment.getRightHandSide();

					// If this field is assigning a parameter name not yet
					// assigned
					for (int index = 0; index < parameterNames.size(); index++) {
						SimpleName parameterName = parameterNames.get(index);
						
						// Resolve the bindings of the right hand side and the parameter in question
						IBinding parameterBinding = parameterName.resolveBinding();
						IBinding assignedParameterBinding = assignedVariableName.resolveBinding();
						
						if (parameterBinding.equals(assignedParameterBinding)) {
							
							// Remove the corresponding parameter binding from the list
							parameterNames.remove(index);
							fieldsAssigningParameters.add(fieldBinding);
							index = parameterNames.size();
							
							// Store this binding to the map
							parameterToFieldBindings.put((IVariableBinding) parameterBinding, fieldBinding);
						}
					}
				}
			}
		}
	}

	public Set<IVariableBinding> getFieldsAssigningParameters() {
		return fieldsAssigningParameters;
	}

	public Set<IVariableBinding> getFields() {
		return fields;
	}
	
	public Map<IVariableBinding, IVariableBinding> getParametersToFieldBindings(){
		return parameterToFieldBindings;
	}
}