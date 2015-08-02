package com.redhat.ceylon.compiler.typechecker.analyzer;

import static com.redhat.ceylon.compiler.typechecker.analyzer.AnalyzerUtil.checkAssignable;
import static com.redhat.ceylon.compiler.typechecker.tree.TreeUtil.name;

import com.redhat.ceylon.compiler.typechecker.context.TypecheckerUnit;
import com.redhat.ceylon.compiler.typechecker.tree.Tree;
import com.redhat.ceylon.compiler.typechecker.tree.Tree.AnyAttribute;
import com.redhat.ceylon.compiler.typechecker.tree.Tree.AttributeDeclaration;
import com.redhat.ceylon.compiler.typechecker.tree.Tree.Variable;
import com.redhat.ceylon.compiler.typechecker.tree.Visitor;
import com.redhat.ceylon.model.typechecker.model.Class;
import com.redhat.ceylon.model.typechecker.model.Type;

/**
 * Validates that classes annotated with Immutable do not have variable members and their members are also
 * annotated Immutable.
 * 
 * @author Tom Kaitchuck 
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
    public void visit(AttributeDeclaration that) {    	
		super.visit(that);
    	if (annotatedImmutableScope) {
    		if (that.getDeclarationModel().isVariable()) {
    			that.addError("'"
    					+ className
    					+ "' satisfies Immutable. Members cannot be variable.'");
    		} else if (!that.getDeclarationModel().isNative()) {
    			Type type = that.getDeclarationModel().getType();
				if (!type.isSubtypeOf(immutableType)) {
					that.addError("'"
							+ className
							+ "' satisfies Immutable. Members must be immutable. Found: "
							+ type);
				}
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

	@Override
	public void visit(Tree.AnyClass that) {
		boolean ai = beginAnnotatedImmutableScope(isDeclaredImmutable(that.getDeclarationModel()));
		className = name(that.getIdentifier());
		checkIsExtendingNonImmutable(that);
		super.visit(that);
		endAnnotatedImmutableScope(ai);
	}

	private void checkIsExtendingNonImmutable(Tree.AnyClass that) {
		if (annotatedImmutableScope) {
			Tree.ExtendedType et = that.getExtendedType();
			if (et != null) {
				Type type = et.getType().getDeclarationModel().getType();
				if (!type.isSubtypeOf(immutableType) && !isSimpleType(type)) {
					that.addError(className
							+ " cannot be Immutable if it is extending a non-immutable type: "
							+ type.asString());
				}
			}
		}
	}
	
	private boolean isSimpleType(Type t) {
		return t.isAnything() || t.isObject() || t.isBasic() || t.isNothing()
				|| t.isNull();
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
