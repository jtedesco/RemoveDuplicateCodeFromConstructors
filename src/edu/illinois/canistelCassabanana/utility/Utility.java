package edu.illinois.canistelCassabanana.utility;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.CastExpression;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.IPackageBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.PrimitiveType;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;

import edu.illinois.canistelCassabanana.visitors.AssignmentExpressionVisitor;
import edu.illinois.canistelCassabanana.visitors.ConstructorDeclarationVisitor;
import edu.illinois.canistelCassabanana.visitors.FieldVariableVisitor;
import edu.illinois.canistelCassabanana.visitors.MethodDeclarationVisitor;

public class Utility {

	/**
	 * Returns the fully qualified name for a ITypeBinding or null if the name
	 * can't be determined.
	 * 
	 * @param type
	 *            The binding to determine the fully qualified name for.
	 * @return The fully qualified name of the binding.
	 */
	public static String fullyQualifiedName(ITypeBinding type) {

		// Check that we really got an ITypeBinding
		if (type == null) {
			return null;
		}

		// If type is anonymous then it doesn't have a name
		if (type.isAnonymous()) {
			return null;
		}

		// If type is an array get the element type
		ITypeBinding baseType = (type.isArray() ? type.getElementType() : type);

		// Add the package name (if one is used)
		IPackageBinding typePackage = baseType.getPackage();
		String result = "";
		if (typePackage != null) {
			if (!typePackage.isUnnamed()) {
				String pkgName = typePackage.getName();
				result = pkgName + ".";
			}
		}

		// Loop through and add all the declaring classes (only for nested
		// classes)
		ITypeBinding declaringClass = type;
		while (declaringClass.isNested()) {
			declaringClass = declaringClass.getDeclaringClass();
			result += declaringClass.getName() + ".";
		}

		// Finally, add the type name itself and return our result
		result += type.getName().intern();
		return result;
	}

	/**
	 * Converts an AST org.eclipse.jdt.core.dom.ITypeBinding into an
	 * org.eclipse.jtd.core.IType, or null if the conversion is not possible.
	 * This conversion is useful for resolving types on a project basis a
	 * project basis (rather than within a single Java file). If the given
	 * ITypeBinding is an array then the IType of the element type is returned.
	 * 
	 * @param type
	 *            The ITypeBinding to convert to an IType.
	 * @param in
	 *            The Java project the binding is contained within.
	 * @return The IType corresponding to binding, or null.
	 */
	public static IType lookupIType(ITypeBinding type, IJavaProject in) {

		// Assume we don't find an IType
		IType result = null;
		if (type != null) {
			String fullyQualifiedName = fullyQualifiedName(type);

			// Did we get a name?
			if (fullyQualifiedName != null) {
				try {
					result = in.findType(fullyQualifiedName);
				} catch (JavaModelException e) {
					// @ ignore since we return null
				}
				if (result == null) {
					System.err.println("(lookup) failed to find the type : " + fullyQualifiedName);
				}
			} else {
				System.err.println("(fullyQualifiedName) failed to find the type : " + type.getName());
			}
		} else {
			System.err.println("(null binding) failed to find the type");
		}
		return result;
	}

	/**
	 * Finds the parent compilation unit of the given constructors.
	 * 
	 * @param constructors
	 *            The list of constructors for which to find the parent
	 * @return The compilation unit containing the given constructors. If the
	 *         constructors do not share
	 *         the same compilation unit, this method will return null.
	 */
	public static ICompilationUnit getParent(List<IMethod> constructors) {
		ICompilationUnit unit = null;
		for (IMethod constructor : constructors) {
			if (unit == null) {
				unit = constructor.getCompilationUnit();
			} else {
				if (constructor.getCompilationUnit() != unit) {
					return null;
				}
			}
		}
		return unit;
	}

	/**
	 * Helper function to recursively retrieve all the constructors in some
	 * compilation unit.
	 * 
	 * @param unit
	 *            The compilation unit from which to retrieve the constructors
	 * @param fieldName
	 *            The field name
	 * @return The list of methods in this compilation unit
	 */
	public static List<IMethod> getConstructors(ICompilationUnit unit) {

		ArrayList<IMethod> result = new ArrayList<IMethod>();
		IType[] types = null;
		try {
			types = unit.getAllTypes();
		} catch (JavaModelException exception) {
			System.err.println("Error getting constructors: " + exception.getLocalizedMessage());
		}

		for (IType iType : types) {
			try {
				IMethod[] methods = iType.getMethods();
				for (IMethod method : methods) {
					if (method.isConstructor()) {
						result.add(method);
					}
				}
			} catch (JavaModelException exception) {
				System.err.println("Error getting constructors: " + exception.getLocalizedMessage());
			}
		}

		return result;
	}

	/**
	 * Find every pair of items from a set
	 * 
	 * @param originalSet
	 *            The original set
	 * @return A set of every possible pair from the original set
	 * 
	 * @see http
	 *      ://stackoverflow.com/questions/1670862/obtaining-powerset-of-a-set
	 *      -in-java
	 */
	public static <T> Set<Set<T>> getPairs(Set<T> originalSet) {
		Set<Set<T>> sets = new HashSet<Set<T>>();
		List<T> list = new ArrayList<T>(originalSet);
		if (list.size() > 0) {
			T head = list.get(0);
			Set<T> rest = new HashSet<T>(list.subList(1, list.size()));
			for (Set<T> set : getPairs(rest)) {
				Set<T> newSet = new HashSet<T>();
				newSet.add(head);
				newSet.addAll(set);

				// Only add this subset if it's of size 2, i.e. if it's a pair
				if (newSet.size() == 2) {
					sets.add(newSet);
					sets.add(set);
				}
			}
		}
		return sets;
	}

	/**
	 * Creates the default value ASTNode for a particular field, given the AST.
	 * 
	 * @param field
	 *            The field for which to get the default value
	 * @param ast
	 *            The AST to use to create this default value
	 * @return The ASTNode representing the literal default value for this field
	 */
	public static ASTNode createDefaultValue(IVariableBinding field, AST ast) {
		ASTNode defaultValue = null;
		if (field.getType().getName().equals("boolean")) {
			defaultValue = ast.newBooleanLiteral(false);
		} else if (field.getType().isPrimitive()) {
			String primitiveName = field.getType().getName();
			// if we are passing 0 to a char parameter, we need to cast it to a
			// char in order to avoid type errors.
			if (primitiveName.equalsIgnoreCase("char")) {
				CastExpression charExpression = ast.newCastExpression();
				charExpression.setExpression(ast.newNumberLiteral());
				charExpression.setType(ast.newPrimitiveType(PrimitiveType.CHAR));
				defaultValue = charExpression;
			} else {
				defaultValue = ast.newNumberLiteral();
			}
		} else {
			defaultValue = ast.newNullLiteral();
		}
		return defaultValue;
	}

	/**
	 * Helper function to extract the set of fields that appear in assignment
	 * (from parameters and from anything) in each constructor
	 */
	public static void getFieldAssignmentsInConstructors(Set<MethodDeclaration> constructorDeclarations,
			Map<MethodDeclaration, Set<IVariableBinding>> constructorParameterAssignmentsExpressions,
			Map<MethodDeclaration, Set<IVariableBinding>> constructorAssignmentsExpressions) {

		for (MethodDeclaration constructorDeclaration : constructorDeclarations) {

			// Visit all of the assignment expressions in this constructor
			AssignmentExpressionVisitor assignmentExpressionVisitor = new AssignmentExpressionVisitor();
			constructorDeclaration.accept(assignmentExpressionVisitor);

			// Add all of the assignment expression in this constructor to this
			// constructor's table entry
			Set<Expression> constructorAssignments = new HashSet<Expression>();
			constructorAssignments.addAll(assignmentExpressionVisitor.getLeftHandSideAssigmentExpressions());

			// Get all of the variable bindings that appear in these assignments
			Set<IVariableBinding> fieldsAssignedFromParameters = new HashSet<IVariableBinding>();
			Set<IVariableBinding> fieldsAssigned = new HashSet<IVariableBinding>();
			for (Expression expression : constructorAssignments) {

				// Get the simple names for the parameters
				List<SimpleName> parameterNames = new LinkedList<SimpleName>();
				List<SingleVariableDeclaration> parameters = constructorDeclaration.parameters();
				for (SingleVariableDeclaration parameter : parameters) {
					parameterNames.add(parameter.getName());
				}

				// Visit all of the fields that appear in the left hand sides of
				// these assignments
				FieldVariableVisitor fieldVisitor = new FieldVariableVisitor(parameterNames);
				expression.accept(fieldVisitor);
				fieldsAssignedFromParameters.addAll(fieldVisitor.getFieldsAssigningParameters());
				fieldsAssigned.addAll(fieldVisitor.getFields());
			}
			constructorParameterAssignmentsExpressions.put(constructorDeclaration, fieldsAssignedFromParameters);
			constructorAssignmentsExpressions.put(constructorDeclaration, fieldsAssigned);
		}
	}

	/**
	 * Helper function to recursively retrieve all the constructors in some
	 * compilation unit
	 * 
	 * @param unit
	 *            The compilation unit from which to retrieve the constructors
	 * @return The list of methods in this compilation unit
	 */
	public static Set<MethodDeclaration> getConstructors(CompilationUnit ast) {
		ConstructorDeclarationVisitor constructorVisitor = new ConstructorDeclarationVisitor();
		ast.accept(constructorVisitor);
		Set<MethodDeclaration> constructorDeclarations = constructorVisitor.getConstructorDeclarations();
		return constructorDeclarations;
	}

	/**
	 * Get the set of top-level method names declared
	 * 
	 * @param unit
	 *            The compilation unit from which to retrieve the methods
	 * @return The list of methods in this compilation unit
	 */
	public static Set<MethodDeclaration> getMethods(CompilationUnit ast) {
		MethodDeclarationVisitor methodVisitor = new MethodDeclarationVisitor();
		ast.accept(methodVisitor);
		Set<MethodDeclaration> methodDeclarations = methodVisitor.getMethodDeclarations();
		return methodDeclarations;
	}
}
