package edu.illinois.canistelCassabanana.visitors;

import java.util.HashSet;
import java.util.Set;

import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.ConstructorInvocation;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.internal.compiler.ast.ConstructorDeclaration;
import org.eclipse.jdt.internal.compiler.lookup.ClassScope;


/**
 * Collects the constructor invocations found in the AST subtree from the starting root node,
 * 	and exposes the results.
 * 
 * @author jon
 */
public class ConstructorInvocationVisitor extends ASTVisitor {
	
	/**
	 * The set of constructor calls found
	 */
	private Set<ConstructorInvocation> constructorInvocations;
	
	public ConstructorInvocationVisitor() {
		super();
		constructorInvocations = new HashSet<ConstructorInvocation>();
	}

	/**
	 * Collects this method invocation if it is a constructor, and 
	 * 	prevents further recursion.
	 */
	@Override
	public boolean visit(ConstructorInvocation node) {
		constructorInvocations.add(node);
		return false;
	}

	public Set<ConstructorInvocation> getConstructorInvocations() {
		return constructorInvocations;
	}
}
