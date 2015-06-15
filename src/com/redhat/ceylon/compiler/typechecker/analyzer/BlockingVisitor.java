package com.redhat.ceylon.compiler.typechecker.analyzer;

import static com.redhat.ceylon.compiler.typechecker.tree.TreeUtil.name;
import static com.redhat.ceylon.compiler.typechecker.tree.TreeUtil.unwrapExpressionUntilTerm;

import com.redhat.ceylon.compiler.typechecker.tree.Tree;
import com.redhat.ceylon.compiler.typechecker.tree.Visitor;
import com.redhat.ceylon.model.typechecker.model.Constructor;
import com.redhat.ceylon.model.typechecker.model.Declaration;
import com.redhat.ceylon.model.typechecker.model.Function;

/**
 * Validates that methods not annotated with blocking do not call methods
 * annotated with blocking.
 * 
 * @author Tom Kaitchuck 
 * TODO: Check formal methods are annotated if refined method is annotated. 
 */
public class BlockingVisitor extends Visitor {

	private boolean annotatedBlocking = false;
	private String methodName;

	boolean beginAnnotatedBlockingScope(boolean ab) {
		boolean oab = annotatedBlocking;
		annotatedBlocking = ab;
		return oab;
	}

	void endAnnotatedBlockingScope(boolean ab) {
		annotatedBlocking = ab;
	}

	@Override
	public void visit(Tree.AttributeGetterDefinition that) {
		boolean ab = beginAnnotatedBlockingScope(false);
		methodName = name(that.getIdentifier());
		super.visit(that);
		endAnnotatedBlockingScope(ab);
	}

	@Override
	public void visit(Tree.AttributeArgument that) {
		boolean ab = beginAnnotatedBlockingScope(false);
		methodName = name(that.getIdentifier());
		super.visit(that);
		endAnnotatedBlockingScope(ab);
	}

	@Override
	public void visit(Tree.MethodArgument that) {
		boolean ab = beginAnnotatedBlockingScope(that.getDeclarationModel()
				.isBlocking());
		methodName = name(that.getIdentifier());
		super.visit(that);
		endAnnotatedBlockingScope(ab);
	}

	@Override
	public void visit(Tree.FunctionArgument that) {
		boolean ab = beginAnnotatedBlockingScope(that.getDeclarationModel()
				.isBlocking());
		methodName = "anonymous function";
		super.visit(that);
		endAnnotatedBlockingScope(ab);
	}

	@Override
	public void visit(Tree.ObjectDefinition that) {
		boolean ab = beginAnnotatedBlockingScope(false);
		methodName = name(that.getIdentifier());
		super.visit(that);
		endAnnotatedBlockingScope(ab);
	}

	@Override
	public void visit(Tree.ObjectArgument that) {
		boolean ab = beginAnnotatedBlockingScope(false);
		methodName = name(that.getIdentifier());
		super.visit(that);
		endAnnotatedBlockingScope(ab);
	}

	@Override
	public void visit(Tree.ObjectExpression that) {
		boolean ab = beginAnnotatedBlockingScope(false);
		methodName = "anonymous function";
		super.visit(that);
		endAnnotatedBlockingScope(ab);
	}

	@Override
	public void visit(Tree.AnyClass that) {
		boolean ab = beginAnnotatedBlockingScope(false);
		methodName = name(that.getIdentifier());
		super.visit(that);
		endAnnotatedBlockingScope(ab);
	}

	@Override
	public void visit(Tree.AnyMethod that) {
		// System.out.println("In file " + fileName + " method " +
		// that.getDeclarationModel());
		boolean ab = beginAnnotatedBlockingScope(that.getDeclarationModel()
				.isBlocking());
		methodName = name(that.getIdentifier());
		super.visit(that);
		endAnnotatedBlockingScope(ab);
	}

	@Override
	public void visit(Tree.Constructor that) {
		boolean ab = beginAnnotatedBlockingScope(that.getDeclarationModel()
				.isBlocking());
		methodName = name(that.getIdentifier());
		super.visit(that);
		endAnnotatedBlockingScope(ab);
	}

	@Override
	public void visit(Tree.ExpressionStatement that) {
		super.visit(that);
		Tree.Expression expr = that.getExpression();
		if (expr != null) {
			Tree.Term t = expr.getTerm();
			if (t instanceof Tree.InvocationExpression) {
				Tree.InvocationExpression ie = ((Tree.InvocationExpression) t);
				Tree.Term primary = unwrapExpressionUntilTerm(ie.getPrimary());
				if (primary == null) {
					return;
				}
				Tree.MemberOrTypeExpression mte = (Tree.MemberOrTypeExpression) primary;
				Declaration dec = mte.getDeclaration();
				if (dec instanceof Function) {
					if (((Function) dec).isBlocking()) {
						that.addError("'"
								+ methodName
								+ "' is not annotated 'blocking' but is calling a blocking method: '"
								+ dec.getName() + "'");
					}
				}
				if (dec instanceof Constructor) {
					if (((Constructor) dec).isBlocking()) {
						that.addError("'"
								+ methodName
								+ "' is not annotated 'blocking' but is calling a blocking method: '"
								+ dec.getName() + "'");
					}
				}
			}
		}
	}
}
