package edu.illinois.canistelCassabanana.visitors;

import java.util.HashSet;
import java.util.Set;

import org.eclipse.core.resources.IFile;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.FieldAccess;

/**
 * Collects the assignments found in the AST subtree from the starting root node,
 * 	and exposes the results.
 * 
 * @author jon
 */
public class AssignmentExpressionVisitor extends ASTVisitor {
	
	/**
	 * The expressions that appear in the left hand side of assignments.
	 */
	private Set<Expression> leftHandSideAssignmentExpressions;
	private Set<Expression> rightHandSideAssignmentExpressions;
	
	public AssignmentExpressionVisitor() {
		super();
		this.leftHandSideAssignmentExpressions = new HashSet<Expression>();
		this.rightHandSideAssignmentExpressions = new HashSet<Expression>();
	}
	
	/**
	 * Collects the expressions that appear in the left hand side of 
	 * 	assignments. 
	 */
	@Override
	public boolean visit(Assignment assignment) {
		Expression lhsExpression = assignment.getLeftHandSide();
		Expression rhsExpression = assignment.getRightHandSide();
		leftHandSideAssignmentExpressions.add(lhsExpression);
		rightHandSideAssignmentExpressions.add(rhsExpression);
		return false;
	}

	public Set<Expression> getLeftHandSideAssigmentExpressions() {
		return leftHandSideAssignmentExpressions;
	}
	
	public Set<Expression> getRightHandSideAssignmentExpressions(){
		return rightHandSideAssignmentExpressions;
	}
	
}
