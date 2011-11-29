package edu.illinois.canistelCassabanana.utility;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.IPackageBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;

/**
 * A utility class that contains shared static methods for the plugin. 
 * 
 * @author jon
 */
public class Utility {

	/**
	 * Returns the fully qualified name for a ITypeBinding or null if the name
	 * can't be determined.
	 * 
	 * @param type The binding to determine the fully qualified name for.
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
		
		// Loop through and add all the declaring classes (only for nested classes)
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
	 * @param type	The ITypeBinding to convert to an IType.
	 * @param in 	The Java project the binding is contained within.
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
				System.err.println("(fullyQualifiedName) failed to find the type : "
								+ type.getName());
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
	 * 	@param originalSet	The original set
	 * 	@return				A set of every possible pair from the original set
	 * 
	 * 	@see http://stackoverflow.com/questions/1670862/obtaining-powerset-of-a-set-in-java
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
}
