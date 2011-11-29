package edu.illinois.canistelCassabanana;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.compiler.IProblem;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.ConstructorInvocation;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.ExpressionStatement;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.Modifier.ModifierKeyword;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ImportRewrite;
import org.eclipse.jdt.core.dom.rewrite.ListRewrite;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.ChangeDescriptor;
import org.eclipse.ltk.core.refactoring.CompositeChange;
import org.eclipse.ltk.core.refactoring.Refactoring;
import org.eclipse.ltk.core.refactoring.RefactoringChangeDescriptor;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.TextFileChange;
import org.eclipse.text.edits.MalformedTreeException;
import org.eclipse.text.edits.MultiTextEdit;
import org.eclipse.text.edits.TextEdit;

import edu.illinois.canistelCassabanana.utility.Utility;
import edu.illinois.canistelCassabanana.visitors.AssignmentExpressionVisitor;
import edu.illinois.canistelCassabanana.visitors.ConstructorDeclarationVisitor;
import edu.illinois.canistelCassabanana.visitors.ConstructorInvocationVisitor;
import edu.illinois.canistelCassabanana.visitors.FieldVariableVisitor;
import edu.illinois.canistelCassabanana.visitors.TypeDeclarationVisitor;

/**
 * The refactoring itself.
 * 
 * @author jon
 */
public class RemoveDuplicateCodeInConstructorsRefactoring extends Refactoring {

	/**
	 * The constructors on which to perform this refactoring
	 */
	private List<IMethod> constructors;

	/**
	 * The constructors for this class
	 */
	private Set<MethodDeclaration> constructorDeclarations;

	/**
	 * A map of constructors to the fields in them that appear in assignments
	 */
	private Map<MethodDeclaration, Set<IVariableBinding>> fieldsInConstructorAssignments;

	/**
	 * The compilation unit, the class on which to perform this refactoring
	 */
	private ICompilationUnit parent;

	/**
	 * The heavy weight, parsed compilation unit corresponding to the class.
	 */
	private CompilationUnit compilationUnit;

	/**
	 * Stores a map of changes made to each compilation unit.
	 */
	private Map<ICompilationUnit, TextFileChange> changes;

	/**
	 * The AST to use to create new methods, and from which to create the
	 * rewriter.
	 */
	private AST ast;

	/**
	 * Whether or not this refactoring should use a helper function instead of a
	 * constructor.
	 */
	private boolean useHelperFunction;

	/**
	 * The name of the helper function, only set if the
	 * <code>useHelperFunction</code> is <code>true</code>.
	 */
	private String helperFunctionName;

	/**
	 * The access modifier to use for creating a new constructor or helper
	 * method.
	 */
	private String accessModifier;

	/**
	 * Constructs this refactoring given the compilation unit, the parent of all
	 * constructors to be refactored.
	 * 
	 * @param unit
	 *            The compilation unit, the class in which to perform the
	 *            refactoring.
	 */
	public RemoveDuplicateCodeInConstructorsRefactoring(ICompilationUnit unit) {
		this.constructors = Utility.getConstructors(unit);
		this.parent = unit;
	}
	
	public RemoveDuplicateCodeInConstructorsRefactoring(){
		
	}

	/**
	 * Constructs this refactoring given a specific set of constructors to
	 * refactor.
	 * 
	 * @param constructors
	 *            The constructors to refactor.
	 */
	public RemoveDuplicateCodeInConstructorsRefactoring(List<IMethod> constructors) {
		this.constructors = constructors;
		this.parent = Utility.getParent(constructors);
	}

	/**
	 * Gets the name of the refactoring.
	 * 
	 * @param The
	 *            unique name given to this refactoring
	 */
	@Override
	public String getName() {
		return RemoveDuplicateCodeInConstructorsConstants.REFACTORING_ID;
	}

	/**
	 * Check that the initial conditions for the refactoring are satisfied,
	 * specifically that:
	 * <ul>
	 * <li>There are at least two constructors in the class
	 * <li>Each constructor contains no compile errors
	 * </ul>
	 * 
	 * @param progressMonitor
	 *            UI component to monitor the progress of the refactoring
	 * @return The status of the refactoring, i.e. if there was an error and
	 *         what severity it has.
	 */
	@Override
	public RefactoringStatus checkInitialConditions(IProgressMonitor progressMonitor) throws CoreException,
			OperationCanceledException {

		// Create the new refactoring
		RefactoringStatus status = new RefactoringStatus();
		progressMonitor.beginTask(RemoveDuplicateCodeInConstructorsConstants.CHECKING_PRECONDITIONS_MESSAGE,
				1);

		// The error status that will be set
		RefactoringStatus errorStatus = null;

		// Check for errors (errors, not warnings) in the constructor bodies
		errorStatus = checkCompileErrors(status, errorStatus);

		// Check error conditions with the number of constructors
		errorStatus = checkNumberOfConstructors(errorStatus);

		// If there was some error, update the refactoring status
		if (errorStatus != null) {
			status.merge(errorStatus);
			return status;
		}

		// Check the intersection of all the fields assigned in all of the
		// constructors
		boolean constructorsOverlapAssignments = constructorsOverlapAssignments();
		MethodDeclaration constructorAssigningAllAssignedFields = getConstructorContainingAllAssignedFields();
		boolean allOtherConstructorsCallConstructors = otherConstructorsCallConstructors(constructorAssigningAllAssignedFields);

		// check the case that the refactoring has already been done, which is
		// where no field is assigned in every
		// constructor, and there is a constructor that assigns all of the
		// fields.
		if (!constructorsOverlapAssignments && constructorAssigningAllAssignedFields != null
				&& allOtherConstructorsCallConstructors) {
			errorStatus = RefactoringStatus
					.createFatalErrorStatus(RemoveDuplicateCodeInConstructorsConstants.NO_DUPLICATE_CODE_MESSAGE);
			status.merge(errorStatus);
			return status;
		}

		// Register the preconditions check as being done
		progressMonitor.done();

		return status;
	}

	/**
	 * Check for errors with the number of constructors in the class.
	 * 
	 * @param errorStatus
	 *            The error status object to update if errors are found.
	 * @return The updated error status
	 */
	private RefactoringStatus checkNumberOfConstructors(RefactoringStatus errorStatus) {

		// If there was an error finding the constructors
		if (constructorDeclarations == null) {
			errorStatus = RefactoringStatus
					.createFatalErrorStatus(RemoveDuplicateCodeInConstructorsConstants.NULL_CONSTRUCTORS_MESSAGE);

			// If there are fewer than two constructors
		} else if (constructorDeclarations.size() == 0) {
			errorStatus = RefactoringStatus
					.createFatalErrorStatus(RemoveDuplicateCodeInConstructorsConstants.NO_CONSTRUCTORS_MESSAGE);
		} else if (constructorDeclarations.size() == 1) {
			errorStatus = RefactoringStatus
					.createFatalErrorStatus(RemoveDuplicateCodeInConstructorsConstants.ONE_CONSTRUCTOR_MESSAGE);
		}
		return errorStatus;
	}

	/**
	 * Checks for compile errors in the constructors of the compilation unit,
	 * and mergers the proper error statuses into the given status objects if it
	 * finds any errors.
	 * 
	 * @param status
	 *            The overall error of the refactoring
	 * @param errorStatus
	 *            A local variable for storing the error status
	 * @return The updated refactoring status
	 */
	private RefactoringStatus checkCompileErrors(RefactoringStatus status, RefactoringStatus errorStatus) {

		// initialize the set of constructorDeclarations and obtain a list of
		// compiler problems.
		List<IProblem> compileErrors = initializeConstructors();

		// check if there are compiler problems. Does NOT check if the problem
		// is unrelated to fields or constructors.
		if (compileErrors.size() > 0) {
			List<String> compileErrorsMessage = new LinkedList<String>();
			for (IProblem problem : compileErrors) {

				// IProblem.PackageIsNotExpectedPackage assumes the default
				// package "invalid" while our tests with no package specified
				// have the value "". So don't consider this difference a
				// problem.
				if (problem.getID() != IProblem.PackageIsNotExpectedPackage) {
					String compileError = "On line " + problem.getSourceLineNumber() + " : "
							+ problem.getMessage();
					compileErrorsMessage.add(compileError);
				}
			}

			// Nicely format compile errors if they exist
			if (compileErrorsMessage.size() > 0) {
				String compileErrorString = "";
				for (String compileError : compileErrorsMessage) {
					compileErrorString += compileError + "\n";
				}
				errorStatus = RefactoringStatus.createFatalErrorStatus(compileErrorString);
				status.merge(errorStatus);
			}
		}
		return errorStatus;
	}

	/**
	 * Checks that the final output of the refactoring is valid
	 * 
	 * @param progressMonitor
	 *            UI component to monitor the progress of the refactoring
	 * @return The status of the refactoring
	 */
	@Override
	public RefactoringStatus checkFinalConditions(IProgressMonitor progressMonitor) throws CoreException,
			OperationCanceledException {

		// Get rewriting utilities for AST
		ASTRewrite astRewrite = ASTRewrite.create(ast);
		ImportRewrite importRewrite = ImportRewrite.create(compilationUnit, true);
		
		changes = new LinkedHashMap<ICompilationUnit, TextFileChange>();

		// Find the constructor that assigns all fields
		MethodDeclaration masterConstructor = getConstructorContainingAllAssignedFields();

		// The order list of parameters for the master constructor, and the
		// fields to be assigned, in the same order.
		List<SingleVariableDeclaration> parameters = null;
		List<IVariableBinding> fieldsAssignedInOrder;

		// If there is no constructor that assigns all fields, we need to create
		// it
		if (masterConstructor == null) {

			// Create the master constructor
			fieldsAssignedInOrder = new ArrayList<IVariableBinding>();
			masterConstructor = createMasterConstructor(fieldsAssignedInOrder, importRewrite);

		} else {
			
			// TODO: implement case where masterconstructor exists

			// Take the arbitrary field order from the master constructor's
			// parameter list
			parameters = masterConstructor.parameters();
			fieldsAssignedInOrder = masterConstructor.parameters();
		}
		

		// For each constructor, find the field assignments and delegate these
		// to the master constructor
		for (MethodDeclaration constructor : fieldsInConstructorAssignments.keySet()) {

			// Don't need to do this for the master constructor
			if (!constructor.equals(masterConstructor)) {

				// Get a handle on this constructor's statements
				List<Statement> statements = constructor.getBody().statements();

				// Get the mapping of assigned fields to their values
				Map<IVariableBinding, Expression> finalFieldAssignments = getFieldAssignments(statements);

				Block block = constructor.getBody();

				// Delegate all field assignments in this constructor to the
				// master constructor
				createMasterConstructorCall(astRewrite, fieldsAssignedInOrder, statements,
						finalFieldAssignments, block);

				removeFinalFieldAssignments(astRewrite, finalFieldAssignments, block, statements);
			}
			System.out.println(constructor.toString());
		}

		TypeDeclarationVisitor visitor = new TypeDeclarationVisitor();
		compilationUnit.accept(visitor);
		
		ListRewrite list = astRewrite.getListRewrite(visitor.getTypes().get(0), TypeDeclaration.BODY_DECLARATIONS_PROPERTY);
		list.insertLast(masterConstructor, null);
		
		rewriteAST(astRewrite, importRewrite);

		return new RefactoringStatus();
	}

	private void rewriteAST(ASTRewrite astRewrite, ImportRewrite importRewrite) {
		try {
			MultiTextEdit edit= new MultiTextEdit();
			TextEdit astEdit= astRewrite.rewriteAST();

			if (!isEmptyEdit(astEdit))
				edit.addChild(astEdit);
			TextEdit importEdit= importRewrite.rewriteImports(new NullProgressMonitor());
			if (!isEmptyEdit(importEdit))
				edit.addChild(importEdit);
			if (isEmptyEdit(edit))
				return;

			TextFileChange change= changes.get(parent);
			if (change == null) {
				change= new TextFileChange(parent.getElementName(), (IFile) parent.getResource());
				change.setTextType("java");
				change.setEdit(edit);
			} else
				change.getEdit().addChild(edit);

			changes.put(parent, change);
		} catch (MalformedTreeException exception) {
			System.err.println(exception.toString());
		} catch (IllegalArgumentException exception) {
			System.err.println(exception.toString());
		} catch (CoreException exception) {
			System.err.println(exception.toString());
		}
		
	}

	private boolean isEmptyEdit(TextEdit astEdit) {
		return astEdit.getClass() == MultiTextEdit.class && !astEdit.hasChildren();
	}

	private void removeFinalFieldAssignments(ASTRewrite astRewrite,
			Map<IVariableBinding, Expression> finalFieldAssignments, Block block, List<Statement> statements) {
		for (Statement statement : statements) {
			if (statement instanceof ExpressionStatement) {
				Expression expression = ((ExpressionStatement) statement).getExpression();
				if (expression instanceof Assignment) {
					FieldVariableVisitor visitor = new FieldVariableVisitor();
					statement.accept(visitor);
					Set<IVariableBinding> fields = visitor.getFields();
					for (IVariableBinding field : fields) {
						Expression rhs = finalFieldAssignments.get(field);
						// check that this 
						if(((Assignment) expression).getRightHandSide().equals(rhs)){
							ListRewrite listRewrite = astRewrite.getListRewrite(block, Block.STATEMENTS_PROPERTY);
							listRewrite.remove(statement, null);
						}
					}
				}
			}
		}
	}

	/**
	 * Create the master constructor call in the given constructor.
	 * 
	 * @param astRewrite
	 *            Utility to allow us to rewrite the AST
	 * @param fieldsAssignedInOrder
	 *            The list of fields assigned in any constructor, in order
	 *            (matches the order of the parameters of the master
	 *            constructor)
	 * @param statements
	 *            The statements of this constructor
	 * @param finalFieldAssignments
	 *            The map of values assigned to fields in this constructor
	 * @param block
	 *            TODO
	 */
	private void createMasterConstructorCall(ASTRewrite astRewrite,
			List<IVariableBinding> fieldsAssignedInOrder, List<Statement> statements,
			Map<IVariableBinding, Expression> finalFieldAssignments, Block block) {

		// Create a call to the master constructor
		ConstructorInvocation masterInvocation = ast.newConstructorInvocation();
		List<ASTNode> invocationArguments = masterInvocation.arguments();

		// Create the arguments to the master constructor
		// TODO: Assign default values
		for (IVariableBinding field : fieldsAssignedInOrder) {
			Expression rhs = finalFieldAssignments.get(field);
			ASTNode newArgument;
			if (rhs != null) {
				newArgument = ASTNode.copySubtree(ast, rhs);
			} else { // else assign default value
				newArgument = createDefaultValue(field, ast);
			}
			invocationArguments.add(newArgument);
		}

		ListRewrite list = astRewrite.getListRewrite(block, Block.STATEMENTS_PROPERTY);

		// insert master constructor invocation
		list.insertFirst(masterInvocation, null);

		// System.out.println(astRewrite.toString());
	}

	private ASTNode createDefaultValue(IVariableBinding field, AST ast) {
		ASTNode result = null;
		if (field.getType().getName().equals("boolean")) {
			result = ast.newBooleanLiteral(false);
		} else if (field.getType().isPrimitive()) {
			result = ast.newNumberLiteral();
		} else {
			result = ast.newNullLiteral();
		}
		return result;
	}

	/**
	 * Gets the map of fields and their assigned values from a list of
	 * statements.
	 * 
	 * @param statements
	 *            The list of statements from which to find assigned field
	 *            values
	 * @return The map of fields to their assigned expressions
	 */
	private Map<IVariableBinding, Expression> getFieldAssignments(List<Statement> statements) {

		// A map of variable bindings to their assigned values
		Map<IVariableBinding, Expression> finalFieldAssignments = new HashMap<IVariableBinding, Expression>();

		// For each statement in the body, get the right and left hand sides
		// of each field assignment.
		// If the lhs is a field, correlate it with the right hand side of
		// the expression.
		// Note: the inner for loops should execute only once.
		for (Statement statement : statements) {

			// Find all assignments in this statement
			AssignmentExpressionVisitor assignmentVisitor = new AssignmentExpressionVisitor();
			statement.accept(assignmentVisitor);

			// Find all field accesses in the assignment expressions
			FieldVariableVisitor fieldVisitor = new FieldVariableVisitor();
			Set<Expression> leftHandSideExpresions = assignmentVisitor.getLeftHandSideAssigmentExpressions();
			Set<Expression> rightHandSideExpressions = assignmentVisitor
					.getRightHandSideAssignmentExpressions();

			// For each expression that assigns a field, add it to the map
			for (Expression expression : leftHandSideExpresions) {

				// Get the field accessed in the left hand side (should only be
				// one)
				expression.accept(fieldVisitor);
				Set<IVariableBinding> fields = fieldVisitor.getFields();

				// Add its received value (expression) to the map
				for (IVariableBinding iVariableBinding : fields) {
					for (Expression rhsExpression : rightHandSideExpressions) {
						finalFieldAssignments.put(iVariableBinding, rhsExpression);
					}
				}
			}
		}
		return finalFieldAssignments;
	}

	/**
	 * Helper function to create the master constructor
	 * 
	 * @param fieldsAssignedInOrder
	 *            The list of fields assigned, in order
	 * @param importRewrite
	 *            Utility to allow us to effectively deal with types
	 * @return The new constructor, with the body also initialized
	 */
	private MethodDeclaration createMasterConstructor(List<IVariableBinding> fieldsAssignedInOrder,
			ImportRewrite importRewrite) {

		// Create the new constructor (just the name & modifiers)
		MethodDeclaration newMasterConstructor = ast.newMethodDeclaration();
		newMasterConstructor.setConstructor(true);
		String className = parent.findPrimaryType().getElementName();
		newMasterConstructor.setName(ast.newSimpleName(className));
		List<Modifier> modifierList = newMasterConstructor.modifiers();
		modifierList.add(ast.newModifier(ModifierKeyword.PUBLIC_KEYWORD));

		// Get handles on the list of assigned fields and the parameters
		Set<IVariableBinding> allFieldsAssigned = getFieldsAssignedInAnyConstructor();
		List<SingleVariableDeclaration> parameters = newMasterConstructor.parameters();

		// for each field assigned, add a corresponding parameter to the
		// masterConstructor. Also add the field to the fieldsAssignedInOrder
		// so at the end fieldsAssignedInOrder[i] corresponds with
		// parameters[i].
		for (IVariableBinding iVariableBinding : allFieldsAssigned) {

			// Add the new parameter to list of fields assigned
			SingleVariableDeclaration parameter = ast.newSingleVariableDeclaration();
			fieldsAssignedInOrder.add(iVariableBinding);

			// Add the new parameter to the constructor
			Type type = importRewrite.addImport(iVariableBinding.getType(), ast);
			parameter.setType(type);
			parameter.setName(ast.newSimpleName("_" + iVariableBinding.getName()));
			parameters.add(parameter);
		}

		// Create the body of the constructor
		Block constructorBody = createConstructorBody(parameters, fieldsAssignedInOrder);
		newMasterConstructor.setBody(constructorBody);

		return newMasterConstructor;

	}

	/**
	 * Create the block of statements for a new constructor or helper method.
	 * The lists given for parameters and fields are assumed to be provided in
	 * the corresponding order.
	 * 
	 * @param parameters
	 *            The parameters to the method
	 * @param fieldsAssigned
	 *            The fields that need to be assigned
	 * @return The block of statements assigning the parameters to the
	 */
	private Block createConstructorBody(List<SingleVariableDeclaration> parameters,
			List<IVariableBinding> fieldsAssigned) {

		// Create a new block of statements
		Block block = ast.newBlock();

		// The list of statements inside the block
		List<Statement> statements = block.statements();
		for (int i = 0; i < parameters.size(); i++) {

			// Create the new assignment statement
			Assignment newAssignment = ast.newAssignment();
			newAssignment.setLeftHandSide(ast.newSimpleName(fieldsAssigned.get(i).getName()));
			newAssignment.setRightHandSide(ast.newSimpleName(parameters.get(i).getName().getIdentifier()));

			// Add it to the list of statements we're creating
			statements.add(ast.newExpressionStatement(newAssignment));
		}

		return block;
	}

	/**
	 * Actually performs the refactoring.
	 * 
	 * @param progressMonitor
	 *            A handle on the progress monitor UI component
	 * @return The change object representing the completed refactoring
	 */
	@Override
	public Change createChange(IProgressMonitor progressMonitor) throws CoreException,
			OperationCanceledException {
		
		try {
			progressMonitor.beginTask("Creating change...", 1);
			final Collection<TextFileChange> changeCollection= changes.values();
			CompositeChange change= new CompositeChange(getName(), changeCollection.toArray(new Change[changeCollection.size()])) {

				@Override
				public ChangeDescriptor getDescriptor() {
					String project= parent.getJavaProject().getElementName();
//					String description= MessageFormat.format("Introduce indirection for ''{0}''", new Object[] { fMethod.getElementName()});
					String description = "description";
//					String methodLabel= JavaElementLabels.getTextLabel(fMethod, JavaElementLabels.ALL_FULLY_QUALIFIED);
//					String typeLabel= JavaElementLabels.getTextLabel(fType, JavaElementLabels.ALL_FULLY_QUALIFIED);
//					String comment= MessageFormat.format("Introduce indirection for ''{0}'' in ''{1}''", new Object[] { methodLabel, typeLabel});
					String comment = "comment";
					Map<String, String> arguments= new HashMap<String, String>();
					return new RefactoringChangeDescriptor(new RemoveDuplicateCodeInConstructorsDescriptor(project, description, comment, arguments));
				}
			};
			return change;
		} finally {
			progressMonitor.done();
		}
		
	}

	/**
	 * Initializes the set of constructors for this class, and returns any
	 * problems ('error' level) that it found in these constructors.
	 * 
	 * @return IProblem[] - array of problems found in compilation of the unit.
	 *         Compiler errors do not include anything within methods that are
	 *         not constructors.
	 */
	private List<IProblem> initializeConstructors() {

		// Create an AST parser for this class
		ASTParser parser = ASTParser.newParser(AST.JLS3);
		parser.setResolveBindings(true);
		parser.setKind(ASTParser.K_COMPILATION_UNIT);
		parser.setSource(parent);
		compilationUnit = (CompilationUnit) parser.createAST(null);
		ast = compilationUnit.getAST();

		// Get all the constructors
		constructorDeclarations = getConstructors(compilationUnit);

		// Remove everything that isn't an 'error' level problem
		IProblem[] problems = compilationUnit.getProblems();
		List<IProblem> errors = new ArrayList<IProblem>();
		for (IProblem problem : problems) {
			if (problem.isError()) {
				errors.add(problem);
			}
		}

		return errors;
	}

	/**
	 * Determines whether or not all other constructors, besides one assigning
	 * all used fields, delegate their work to another constructor.
	 * 
	 * @param constructorAssigningAllAssignedFields
	 *            A constructor that assigns all fields, or null if none exists
	 * @return Whether or not all other constructors (besides the parameter)
	 *         call another constructor.
	 */
	private boolean otherConstructorsCallConstructors(MethodDeclaration constructorAssigningAllAssignedFields) {

		boolean foundConstructorWithoutConstructorCall = false;

		for (MethodDeclaration constructor : fieldsInConstructorAssignments.keySet()) {

			// If we didn't find any constructor assigning all fields, or as
			// long as this constructor is not that one, it should delegate its
			// work
			if (constructorAssigningAllAssignedFields == null
					|| !constructor.equals(constructorAssigningAllAssignedFields)) {

				// Find all constructor invocations inside this constructor
				ConstructorInvocationVisitor constructorInvocationVisitor = new ConstructorInvocationVisitor();
				constructor.accept(constructorInvocationVisitor);

				// If we found no invocations, then we found some constructor
				// that should, but does not, delegate its work
				if (constructorInvocationVisitor.getConstructorInvocations().size() == 0) {
					foundConstructorWithoutConstructorCall = true;
				}
			}
		}

		return !foundConstructorWithoutConstructorCall;
	}

	/**
	 * Returns the constructor with every (explicitly) assigned field as
	 * arguments, or null if none exists.
	 */
	private MethodDeclaration getConstructorContainingAllAssignedFields() {
		Set<IVariableBinding> fieldsAssignedInAnyConstructors = getFieldsAssignedInAnyConstructor();
		for (MethodDeclaration constructor : constructorDeclarations) {
			Set<IVariableBinding> fieldAssignedInThisConstructor = fieldsInConstructorAssignments
					.get(constructor);
			if (fieldAssignedInThisConstructor.equals(fieldsAssignedInAnyConstructors)) {
				return constructor;
			}
		}
		return null;
	}

	/**
	 * Returns the set of all fields assigned in all of the constructors.
	 * 
	 * @return A set of variable bindings, corresponding the the set of all
	 *         fields assigned in any constructors.
	 */
	private Set<IVariableBinding> getFieldsAssignedInAnyConstructor() {

		// Set of all fields explicitly assigned
		Set<IVariableBinding> fieldsAssignedInAnyConstructor = new HashSet<IVariableBinding>();
		for (Set<IVariableBinding> fieldsAssignedInThisConstructors : fieldsInConstructorAssignments.values()) {
			fieldsAssignedInAnyConstructor.addAll(fieldsAssignedInThisConstructors);
		}
		return fieldsAssignedInAnyConstructor;
	}

	/**
	 * Check for any duplicate assignments between pairs of constructors, from
	 * the set of all fields assigned in any constructors. This implicitly
	 * handles constructors that give the default value to some field assigned
	 * by other constructors.
	 * 
	 * @return True if there are explicit overlapping assignments from the set
	 *         of all fields assigned in any constructor, false otherwise.
	 */
	private boolean constructorsOverlapAssignments() {

		// Get all of the assignment expressions in these constructors
		fieldsInConstructorAssignments = getFieldAssignmentsInConstructors(constructorDeclarations);

		Object[] constructorArray = constructorDeclarations.toArray();

		// Get the union of fields assigned in all constructors.
		Set<IVariableBinding> fieldsAssignedInAnyConstructor = getFieldsAssignedInAnyConstructor();

		// Go through the set of pairs of constructors. If there is a duplicate
		// assignment between any pair of constructors, then we know there is
		// duplicate code, so return true;
		Set<Set<MethodDeclaration>> pairsOfConstructors = Utility.getPairs(fieldsInConstructorAssignments
				.keySet());
		for (Set<MethodDeclaration> set : pairsOfConstructors) {
			Set<IVariableBinding> fieldsAssignedInBothConstructors = fieldsAssignedInAnyConstructor;
			for (MethodDeclaration constructor : set) {
				fieldsAssignedInBothConstructors.retainAll(fieldsInConstructorAssignments.get(constructor));
			}
			if (fieldsAssignedInBothConstructors.size() > 0)
				return true;
		}

		return false;

	}

	/**
	 * Helper function to extract the set of fields that appear in assignment
	 * expressions in each constructor
	 * 
	 * @param constructorDeclarations
	 *            The constructors in question
	 * @return A map of constructor declarations to sets of fields
	 */
	private Map<MethodDeclaration, Set<IVariableBinding>> getFieldAssignmentsInConstructors(
			Set<MethodDeclaration> constructorDeclarations) {

		Map<MethodDeclaration, Set<IVariableBinding>> constructorAssignmentsExpressions = new HashMap<MethodDeclaration, Set<IVariableBinding>>();
		for (MethodDeclaration constructorDeclaration : constructorDeclarations) {

			// Visit all of the assignment expressions in this constructor
			AssignmentExpressionVisitor assignmentExpressionVisitor = new AssignmentExpressionVisitor();
			constructorDeclaration.accept(assignmentExpressionVisitor);

			// Add all of the assignment expression in this constructor to this
			// constructor's table entry
			Set<Expression> constructorAssignments = new HashSet<Expression>();
			constructorAssignments.addAll(assignmentExpressionVisitor.getLeftHandSideAssigmentExpressions());

			// Get all of the variable bindings that appear in these assignments
			Set<IVariableBinding> fieldsAssigned = new HashSet<IVariableBinding>();
			for (Expression expression : constructorAssignments) {

				// Visit all of the fields that appear in the left hand sides of
				// these assignments
				FieldVariableVisitor fieldVisitor = new FieldVariableVisitor();
				expression.accept(fieldVisitor);
				fieldsAssigned.addAll(fieldVisitor.getFields());
			}
			constructorAssignmentsExpressions.put(constructorDeclaration, fieldsAssigned);
		}

		return constructorAssignmentsExpressions;
	}

	/**
	 * Helper function to recursively retrieve all the constructors in some
	 * compilation unit
	 * 
	 * @param unit
	 *            The compilation unit from which to retrieve the constructors
	 * @param fieldName
	 *            The field name
	 * @return The list of methods in this compilation unit
	 */
	private Set<MethodDeclaration> getConstructors(CompilationUnit ast) {
		ConstructorDeclarationVisitor constructorVisitor = new ConstructorDeclarationVisitor();
		ast.accept(constructorVisitor);
		Set<MethodDeclaration> constructorDeclarations = constructorVisitor.getConstructorDeclarations();
		return constructorDeclarations;
	}

	public void setUseHelperFunction(boolean useHelperFunction) {
		this.useHelperFunction = useHelperFunction;
	}

	public void setHelperFunctionName(String helperFunctionName) {
		this.helperFunctionName = helperFunctionName;
	}

	public void setAccessModifier(String accessModifier) {
		this.accessModifier = accessModifier;
	}
}
