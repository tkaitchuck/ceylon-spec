package com.redhat.ceylon.compiler.typechecker.analyzer;

import static com.redhat.ceylon.compiler.typechecker.analyzer.AnalyzerUtil.checkAssignable;
import static com.redhat.ceylon.compiler.typechecker.tree.TreeUtil.name;

import java.util.List;

import com.redhat.ceylon.compiler.typechecker.context.TypecheckerUnit;
import com.redhat.ceylon.compiler.typechecker.tree.Node;
import com.redhat.ceylon.compiler.typechecker.tree.Tree;
import com.redhat.ceylon.compiler.typechecker.tree.CustomTree.IsCase;
import com.redhat.ceylon.compiler.typechecker.tree.Tree.AnyAttribute;
import com.redhat.ceylon.compiler.typechecker.tree.Tree.AttributeDeclaration;
import com.redhat.ceylon.compiler.typechecker.tree.Tree.ClassBody;
import com.redhat.ceylon.compiler.typechecker.tree.Tree.Parameter;
import com.redhat.ceylon.compiler.typechecker.tree.Tree.ParameterList;
import com.redhat.ceylon.compiler.typechecker.tree.Tree.ValueParameterDeclaration;
import com.redhat.ceylon.compiler.typechecker.tree.Tree.Variable;
import com.redhat.ceylon.compiler.typechecker.tree.Visitor;
import com.redhat.ceylon.model.typechecker.model.Class;
import com.redhat.ceylon.model.typechecker.model.Constructor;
import com.redhat.ceylon.model.typechecker.model.Interface;
import com.redhat.ceylon.model.typechecker.model.Type;
import com.redhat.ceylon.model.typechecker.model.TypedDeclaration;
import com.redhat.ceylon.model.typechecker.model.Value;

/**
 * This checks to make sure that objects that are locally mutable are properly
 * annotated and their methods are safe. Safe in this context mean that all of
 * their methods only receive immutable objects and they have no blocking
 * methods.
 * 
 * @author Tom Kaitchuck
 */
public class OpaqueVisitor extends Visitor {

	private class CurrentClass {
		private final String className;
		private final boolean isMutable;
		private boolean isInParameterList;

		CurrentClass(Class c) {
			className = c.getName();
			isMutable = c.isMutable();
			isInParameterList = false;
		}

		CurrentClass(String className, boolean isMutable) {
			this.className = className;
			this.isMutable = isMutable;
			this.isInParameterList = false;
		}
		
		boolean isInClass() {
			return className != null;
		}

		boolean isMutable() {
			return isMutable;
		}

		public boolean isInParameterList() {
			return isInParameterList;
		}

		public void setInParameterList(boolean isInParameterList) {
			this.isInParameterList = isInParameterList;
		}
	}

	private CurrentClass state = new CurrentClass(null, false);
	private final Type immutableType;

	public OpaqueVisitor(TypecheckerUnit unit) {
		immutableType = unit.getImmutableMaskDeclaration().getType();
	}

	CurrentClass beginObjectScope(CurrentClass newClass) {
		CurrentClass origional = state;
		state = newClass;
		return origional;
	}

	void endObjectScope(CurrentClass origional) {
		state = origional;
	}
	
	@Override
	public void visit(ParameterList that) {
		boolean inParameterList = state.isInParameterList;
		if (state.isInClass()) {
			state.setInParameterList(true);
		}
		super.visit(that);
		state.setInParameterList(inParameterList);
	}
	
	@Override
	public void visit(AnyAttribute that) {
//		if ("BadMutableParClass".equals(state.className)) {
//			System.out.println();
//			System.out.println(that.toString());
//			System.out.println(state.isMutable);
//			System.out.println(that.getDeclarationModel());
//			System.out.println(that.getDeclarationModel().isVariable());
//		}
		handelVariable(that.getDeclarationModel(), that);
		CurrentClass orig = beginObjectScope(new CurrentClass(null, false));
		super.visit(that);
		endObjectScope(orig);
	}

	@Override
	public void visit(Variable that) {
		handelVariable(that.getDeclarationModel(), that);
		CurrentClass orig = beginObjectScope(new CurrentClass(null, false));
		super.visit(that);
		endObjectScope(orig);
	}

	void handelVariable(TypedDeclaration declarationModel, Node that) {
		if (state.isInClass()) {
			if (state.isMutable()) {
				if (declarationModel.isVariable()) {
					Type type = declarationModel.getType();
					if (type != null && !type.isSubtypeOf(immutableType)) {
						if(declarationModel.isShared()) {
							that.addError("Shared variable values on mutable annotated class '"
								+ state.className
								+ "' must be immutable. Found: " + type);
						}
						if (state.isInParameterList()) {
							that.addError("Parameters to a mutable annotated class '"
									+ state.className
									+ "' must be immutable. Found: " + type);
						}
					}
				}
			} else {
				if (declarationModel.isVariable()
						&& !declarationModel.isNative()) {
					that.addError("Member is variable but class is not annotated 'mutable' "
							+ state.className);
				}
			}
		}
	}

	@Override
	public void visit(Tree.AnyInterface that) {
		CurrentClass previous = beginObjectScope(new CurrentClass(null, false));
		super.visit(that);
		endObjectScope(previous);
	}

	@Override
	public void visit(Tree.AnyClass that) {
		Class declarationModel = that.getDeclarationModel();
		if ("BadMutableParClass".equals(that.getDeclarationModel().getName())) {
			System.out.println();
			System.out.println(that.toString());
			System.out.println(state.isMutable);
			System.out.println(that.getDeclarationModel());
		}
		CurrentClass previous = beginObjectScope(new CurrentClass(
				declarationModel));
		super.visit(that);
		endObjectScope(previous);
	}

	@Override
	public void visit(Tree.ObjectDefinition that) {
		CurrentClass previous = beginObjectScope(new CurrentClass(
				that.getAnonymousClass()));
		super.visit(that);
		endObjectScope(previous);
	}

	@Override
	public void visit(Tree.ObjectExpression that) {
		Class declarationModel = that.getAnonymousClass();
		CurrentClass previous = beginObjectScope(new CurrentClass(
				declarationModel));
		super.visit(that);
		endObjectScope(previous);
	}

	@Override
	public void visit(Tree.Constructor that) {
		CurrentClass previous = beginObjectScope(new CurrentClass(null, false));
		super.visit(that);
		endObjectScope(previous);
	}

	@Override
	public void visit(Tree.MemberOrTypeExpression that) {
		CurrentClass previous = beginObjectScope(new CurrentClass(null, false));
		super.visit(that);
		endObjectScope(previous);
	}

	@Override
	public void visit(Tree.AnyMethod that) {
		CurrentClass orig = beginObjectScope(new CurrentClass(null, false));
		List<ParameterList> parameterLists = that.getParameterLists();
		if (orig != null && orig.isMutable()
				&& that.getDeclarationModel().isBlocking()) {
			that.addError("Blocking methods are not allowed on mutable annotated classes.");
		}
		super.visit(that);
		if (orig != null && orig.isMutable()) {
			for (ParameterList list : parameterLists) {
				for (Parameter p : list.getParameters()) {
					Type type = p.getParameterModel().getModel().getType();
					if (!type.isSubtypeOf(immutableType)) {
						that.addError("Method parameters to mutable annotated class '"
								+ orig.className
								+ "' must be immutable. Found: " + type);
					}
				}
			}
		}
		endObjectScope(orig);
	}
}
