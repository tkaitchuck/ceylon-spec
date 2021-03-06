package com.redhat.ceylon.compiler.typechecker.analyzer;

import static com.redhat.ceylon.compiler.typechecker.tree.TreeUtil.name;
import static com.redhat.ceylon.compiler.typechecker.tree.TreeUtil.unwrapExpressionUntilTerm;

import com.redhat.ceylon.compiler.typechecker.tree.Node;
import com.redhat.ceylon.compiler.typechecker.tree.Tree;
import com.redhat.ceylon.compiler.typechecker.tree.Tree.InvocationExpression;
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
 * 
 * TODO: How to deal with imported methods and/or methods on parameters that we don't have the definition available for.
 * - Solution is to avoid passing them to stateful objects. If we REALLY lock down what goes in this is possible.
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
		methodName = null;
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
		boolean ab = beginAnnotatedBlockingScope(that.getDeclarationModel()
				.isBlocking());
		checkRefinesNonBlocking(that, that.getDeclarationModel());
		methodName = name(that.getIdentifier());
		super.visit(that);
		endAnnotatedBlockingScope(ab);
	}

	private void checkRefinesNonBlocking(Node that, Function declarationModel) {
		Declaration declaration = declarationModel.getRefinedDeclaration();
		if (declarationModel.isBlocking()) {
			if ((declaration instanceof Function && !((Function)declaration).isBlocking())  ||
					(declaration instanceof Constructor && !((Constructor)declaration).isBlocking())) {
				that.addError("A blocking method cannot refine a non-blocking method.");
			}
		}
	}

	@Override
	public void visit(Tree.Constructor that) {
		boolean ab = beginAnnotatedBlockingScope(that.getDeclarationModel()
				.isBlocking());
		checkRefinesNonBlocking(that, that.getDeclarationModel());
		methodName = name(that.getIdentifier());
		super.visit(that);
		endAnnotatedBlockingScope(ab);
	}

	@Override
	public void visit(Tree.MemberOrTypeExpression that) {
		Declaration dec = that.getDeclaration();
		handelInvocation(that, dec);
	}
	
	@Override
	public void visit(Tree.QualifiedMemberExpression that) {
		Declaration dec = that.getDeclaration();
		handelInvocation(that, dec);
	}
	
//	@Override
//	public void visit(Tree.InvocationExpression that) {
//		super.visit(that);
//		Tree.Term primary = unwrapExpressionUntilTerm(that.getPrimary());
//		if (primary == null) {
//			return;
//		}
//		System.out.println("Primary term: "+primary.getClass().getSimpleName());
//		Declaration dec = null;
//		if (primary instanceof Tree.MemberOrTypeExpression) {
//			Tree.MemberOrTypeExpression mte = (Tree.MemberOrTypeExpression) primary;
//			dec = mte.getDeclaration();
//		} else if (primary instanceof Tree.QualifiedMemberExpression) {
//			Tree.QualifiedMemberExpression qme = (Tree.QualifiedMemberExpression) primary;
//			dec = qme.getDeclaration();
//		}
//		if (dec != null) {
//			handelInvocation(that, dec);
//		}
//	}

	private void handelInvocation(Node that, Declaration dec) {
		if (annotatedBlocking) {
			return;
		}
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
