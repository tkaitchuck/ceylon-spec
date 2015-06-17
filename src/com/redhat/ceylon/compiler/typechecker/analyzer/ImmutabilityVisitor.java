package com.redhat.ceylon.compiler.typechecker.analyzer;

import static com.redhat.ceylon.compiler.typechecker.analyzer.AnalyzerUtil.checkAssignable;
import static com.redhat.ceylon.compiler.typechecker.tree.TreeUtil.name;

import com.redhat.ceylon.compiler.typechecker.context.TypecheckerUnit;
import com.redhat.ceylon.compiler.typechecker.tree.Tree;
import com.redhat.ceylon.compiler.typechecker.tree.Tree.AnyAttribute;
import com.redhat.ceylon.compiler.typechecker.tree.Tree.Variable;
import com.redhat.ceylon.compiler.typechecker.tree.Visitor;

/**
 * Validates that classes annotated with Immutable do not have variable members and their members are also
 * annotated Immutable.
 * 
 * @author Tom Kaitchuck 
 * TODO: Handle satisfying an immutable interface.
 * TODO: Validate nested types.
 * TODO: Figure out how to automatically add Immutable interface to annotated types.
 */
public class ImmutabilityVisitor extends Visitor {

	private boolean annotatedImmutableScope = false;
	private String className;
	private TypecheckerUnit unit;

	public ImmutabilityVisitor(TypecheckerUnit unit) {
		this.unit = unit;
	}
	
	boolean beginAnnotatedImmutableScope(boolean ais) {
		boolean oai = annotatedImmutableScope;
		annotatedImmutableScope = ais;
		return oai;
	}

	void endAnnotatedImmutableScope(boolean ais) {
		annotatedImmutableScope = ais;
	}
    
    @Override
    public void visit(AnyAttribute that) {    	
		super.visit(that);
		if (annotatedImmutableScope) {
			if (that.getDeclarationModel().isVariable()) {
				that.addError("'"
						+ className
						+ "' is annotated 'immutable'. Members cannot be variable.'");
			}
			checkAssignable(that.getDeclarationModel().getType(),
					unit.getImmutableDeclaration().getType(), that, "'" + className
					+ "' is annotated 'immutable'. Members must be immutable.'");
		}
	}
    
    @Override
    public void visit(Variable that) {
    	super.visit(that);
    	if (annotatedImmutableScope) {
    		if (that.getDeclarationModel().isVariable()) {
    			that.addError("'"
    					+ className
    					+ "' is annotated 'immutable'. Members cannot be variable.'");
    		}
			checkAssignable(that.getDeclarationModel().getType(), 
					unit.getImmutableDeclaration().getType(), that, "'" + className
					+ "' is annotated 'immutable'. Members must be immutable.'");
		}
    }

//	@Override
//	public void visit(Tree.ObjectDefinition that) {
//		boolean ai = beginAnnotatedImmutableScope(that.getDeclarationModel().isImmutable());
//		className = name(that.getIdentifier());
//		super.visit(that);
//		endAnnotatedImmutableScope(ai);
//	}
//
//	@Override
//	public void visit(Tree.ObjectArgument that) {
//		boolean ai = beginAnnotatedImmutableScope(that.getDeclarationModel().isi);
//		className = name(that.getIdentifier());
//		super.visit(that);
//		endAnnotatedImmutableScope(ai);
//	}
    

	@Override
	public void visit(Tree.AnyClass that) {
		boolean ai = beginAnnotatedImmutableScope(that.getDeclarationModel().isImmutable());
		className = name(that.getIdentifier());
		super.visit(that);
		endAnnotatedImmutableScope(ai);
	}
	
	@Override
	public void visit(Tree.AnyMethod that) {
		boolean ai = beginAnnotatedImmutableScope(false);
		super.visit(that);
		endAnnotatedImmutableScope(ai);
	}
	
	
}
