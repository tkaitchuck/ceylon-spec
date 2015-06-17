package com.redhat.ceylon.compiler.typechecker.analyzer;

import static com.redhat.ceylon.compiler.typechecker.analyzer.AnalyzerUtil.checkAssignable;
import static com.redhat.ceylon.compiler.typechecker.tree.TreeUtil.name;

import com.redhat.ceylon.compiler.typechecker.context.TypecheckerUnit;
import com.redhat.ceylon.compiler.typechecker.tree.Tree;
import com.redhat.ceylon.compiler.typechecker.tree.Tree.AnyAttribute;
import com.redhat.ceylon.compiler.typechecker.tree.Tree.Variable;
import com.redhat.ceylon.compiler.typechecker.tree.Visitor;
import com.redhat.ceylon.model.typechecker.model.Class;
import com.redhat.ceylon.model.typechecker.model.Type;

/**
 * Validates that classes annotated with Immutable do not have variable members and their members are also
 * annotated Immutable.
 * 
 * @author Tom Kaitchuck 
 * TODO: Identify super classes that are not immutable
 * TODO: Identify immutable primitives to exclude them from the check. 
 * TODO: Handle satisfying an immutable interface.
 * TODO: Validate nested types.
 * TODO: Figure out how to automatically add Immutable interface to annotated types.
 */
public class ImmutabilityVisitor extends Visitor {

	private boolean annotatedImmutableScope = false;
	private String className;
	private final Type immutableType;

	public ImmutabilityVisitor(TypecheckerUnit unit) {
		immutableType = unit.getImmutableMaskDeclaration().getType();
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
    					+ "' satisfies Immutable. Members cannot be variable.'");
    		} else if (!that.getDeclarationModel().isNative()) {
    			Type type = that.getDeclarationModel().getType();
    			checkAssignable(type, immutableType, that, "'" + className
    					+ "' satisfies Immutable. Members must be immutable. Found: "+ type +" which is: "+type.getSatisfiedTypes());
    		}
    	}
	}
    
    @Override
    public void visit(Variable that) {
    	super.visit(that);
    	if (annotatedImmutableScope) {
    		if (that.getDeclarationModel().isVariable()) {
    			that.addError("'"
    					+ className
    					+ "' satisfies Immutable. Members cannot be variable.'");
    		} else if (!that.getDeclarationModel().isNative()) {
    			Type type = that.getDeclarationModel().getType();
    			checkAssignable(type, immutableType, that, "'" + className
    					+ "' satisfies Immutable. Members must be immutable. Found: "+ type);
    		}
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
		boolean ai = beginAnnotatedImmutableScope(isDeclaredImmutable(that.getDeclarationModel()));
		className = name(that.getIdentifier());
		super.visit(that);
		endAnnotatedImmutableScope(ai);
	}

	private boolean isDeclaredImmutable(Class declarationModel) {
		return !declarationModel.isNative() && declarationModel.getType().isSubtypeOf(immutableType);
	}

	@Override
	public void visit(Tree.AnyMethod that) {
		boolean ai = beginAnnotatedImmutableScope(false);
		super.visit(that);
		endAnnotatedImmutableScope(ai);
	}
	
	
}
