package edu.illinois.canistelCassabanana.visitors;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.TypeDeclaration;

/**
 * Collects the type declarations found in the AST subtree from the starting root node.
 * 
 * @author jon
 */
public class TypeDeclarationVisitor extends ASTVisitor {

	/**
	 * The list of type declarations visited.
	 */
	List<TypeDeclaration> types;
	
	public List<TypeDeclaration> getTypes() {
		return types;
	}

	public TypeDeclarationVisitor(){
		types = new ArrayList<TypeDeclaration>();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean visit(TypeDeclaration node) {
		types.add(node);
		return false;
	}

}
