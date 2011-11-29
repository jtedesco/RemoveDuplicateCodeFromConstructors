package edu.illinois.canistelCassabanana.visitors;

import java.util.HashSet;
import java.util.Set;

import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.internal.compiler.ast.ConstructorDeclaration;
import org.eclipse.jdt.internal.compiler.lookup.ClassScope;

/**
 * Collects the constructor declarations found in the AST subtree from the starting root node,
 * 	and exposes the results.
 * 
 * @author jon
 */
public class ConstructorDeclarationVisitor extends ASTVisitor {
	
	/**
	 * The set of constructors (declarations) found
	 */
	private Set<MethodDeclaration> constructorDeclarations;
	
	public ConstructorDeclarationVisitor() {
		super();
		constructorDeclarations = new HashSet<MethodDeclaration>();
	}

	/**
	 * Collects this method declaration if it is a constructor, and 
	 * 	prevents further recursion.
	 */
	@Override
	public boolean visit(MethodDeclaration node) {
		if(node.isConstructor()) {
			constructorDeclarations.add(node);
		}
		return false;
	}

	public Set<MethodDeclaration> getConstructorDeclarations() {
		return constructorDeclarations;
	}
}
