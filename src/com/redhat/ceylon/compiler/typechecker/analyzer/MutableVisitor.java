package com.redhat.ceylon.compiler.typechecker.analyzer;

import static com.redhat.ceylon.compiler.typechecker.analyzer.AnalyzerUtil.checkAssignable;
import static com.redhat.ceylon.compiler.typechecker.tree.TreeUtil.name;

import java.util.List;

import com.redhat.ceylon.compiler.typechecker.context.TypecheckerUnit;
import com.redhat.ceylon.compiler.typechecker.tree.Node;
import com.redhat.ceylon.compiler.typechecker.tree.Tree;
import com.redhat.ceylon.compiler.typechecker.tree.Tree.AnyAttribute;
import com.redhat.ceylon.compiler.typechecker.tree.Tree.Parameter;
import com.redhat.ceylon.compiler.typechecker.tree.Tree.ParameterList;
import com.redhat.ceylon.compiler.typechecker.tree.Tree.Variable;
import com.redhat.ceylon.compiler.typechecker.tree.Visitor;
import com.redhat.ceylon.model.typechecker.model.Class;
import com.redhat.ceylon.model.typechecker.model.Constructor;
import com.redhat.ceylon.model.typechecker.model.Type;
import com.redhat.ceylon.model.typechecker.model.TypedDeclaration;
import com.redhat.ceylon.model.typechecker.model.Value;

/**
 * This checks to make sure that objects that are locally mutable are properly annotated and their methods are safe.
 * Safe in this context mean that all of their methods only receive immutable objects and they have no blocking methods.
 * 
 * @author Tom Kaitchuck 
 */
public class MutableVisitor extends Visitor {

	private Class currentClass;
	private final Type immutableType;

	public MutableVisitor(TypecheckerUnit unit) {
		immutableType = unit.getImmutableMaskDeclaration().getType();
	}
	
	Class beginObjectScope(Class newClass) {
		Class origional = currentClass;
		currentClass = newClass;
		return origional;
	}

	void endObjectScope(Class origional) {
		currentClass = origional;
	}
    
    @Override
    public void visit(AnyAttribute that) {
    	handelVariable(that.getDeclarationModel(),that);
		Class orig = beginObjectScope(null); 
		super.visit(that);
		endObjectScope(orig);
	}
    
    @Override
    public void visit(Variable that) {
    	handelVariable(that.getDeclarationModel(),that);
		Class orig = beginObjectScope(null); 
		super.visit(that);
		endObjectScope(orig);
    }

    void handelVariable(TypedDeclaration declarationModel, Node that) {
    	if (currentClass != null) {
    		if (currentClass.isMutable()) {
    			if (declarationModel.isVariable() && declarationModel.isShared()) {
    				Type type = declarationModel.getType();
    				checkAssignable(type, immutableType, that, "'" + currentClass.getName()
    						+ "Shared variable values on mutable annotated classes must be immutable. Found: "+type);
    			}
    		} else {
    			if (declarationModel.isVariable() && !declarationModel.isNative()) {
    				that.addError("Member is variable but class is not annotated 'mutable' " + currentClass);
    			}
    		}
    	}
    }

	@Override
	public void visit(Tree.AnyClass that) {
		Class declarationModel = that.getDeclarationModel();
		Class previous = beginObjectScope(declarationModel); 
		super.visit(that);
		endObjectScope(previous);
	}
	
	@Override
	public void visit(Tree.ObjectDefinition that) {
		Value declarationModel = that.getDeclarationModel();
		Class previous = beginObjectScope(null); //TODO: Add support for these. 
		super.visit(that);
		endObjectScope(previous);
	}
	
	@Override
	public void visit(Tree.ObjectExpression that) {
		Class declarationModel = that.getAnonymousClass();
		Class previous = beginObjectScope(null); //TODO: Should be passing declarationModel here. However we cannot annotate these classes.
		super.visit(that);
		endObjectScope(previous);
	}

	@Override
	public void visit(Tree.Constructor that) {
		Class previous = beginObjectScope(null); 
		super.visit(that);
		endObjectScope(previous);
	}
	
	@Override
	public void visit(Tree.MemberOrTypeExpression that) {
		Class previous = beginObjectScope(null); 
		super.visit(that);
		endObjectScope(previous);
	}

	@Override
	public void visit(Tree.AnyMethod that) {
		Class orig = beginObjectScope(null);  
		List<ParameterList> parameterLists = that.getParameterLists();
		if (orig != null && orig.isMutable() && that.getDeclarationModel().isBlocking()) {
			that.addError("Blocking methods are not allowed on mutable annotated classes.");
		}
		super.visit(that);
		if (orig != null && orig.isMutable()) {
			for (ParameterList list : parameterLists) {
				for (Parameter p : list.getParameters()) {
					Type type = p.getParameterModel().getModel().getType();
					checkAssignable(type, immutableType, that, "'" + orig.getName()
							+ "Method parameters to mutable annotated classes must be immutable. Found: "+type);
				}
			}
		}
		endObjectScope(orig);
	}
}
