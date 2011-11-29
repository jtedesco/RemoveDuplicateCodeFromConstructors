package edu.illinois.canistelCassabanana.visitors;

import java.util.HashSet;
import java.util.Set;

import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.Statement;

/**
 * Collects the field variables found in the AST subtree from the starting root node.
 * 
 * @author jon
 */
public class FieldVariableVisitor extends ASTVisitor {

	/**
	 * The fields that appear in any simple name nodes we visit.
	 */
	private Set<IVariableBinding> fields;

	/**
	 * The statements corresponding to assignments to fields
	 */
	private Set<Statement> fieldAssignmentStatements;

	public FieldVariableVisitor() {
		super();
		this.fields = new HashSet<IVariableBinding>();
		this.fieldAssignmentStatements = new HashSet<Statement>();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean visit(SimpleName node) {

		// Find any variable bindings that are are not declarations, and map to
		// fields
		if (!node.isDeclaration()) {
			IBinding nodeBinding = node.resolveBinding();
			if (nodeBinding instanceof IVariableBinding) {
				IVariableBinding variableBinding = (IVariableBinding) nodeBinding;
				if (variableBinding.isField()) {
					fields.add(variableBinding);

					// TODO: Fix this!

					fieldAssignmentStatements.add((Statement) node.getParent().getParent().getParent());
				}
			}
		}
		return false;
	}

	public Set<IVariableBinding> getFields() {
		return fields;
	}

	public Set<Statement> getFieldAssignmentStatements() {
		return fieldAssignmentStatements;
	}

	public void setFieldAssignmentStatements(Set<Statement> fieldAssignmentStatements) {
		this.fieldAssignmentStatements = fieldAssignmentStatements;
	}
}
