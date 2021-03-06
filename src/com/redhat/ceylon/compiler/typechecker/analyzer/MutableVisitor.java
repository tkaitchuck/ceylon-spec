package com.redhat.ceylon.compiler.typechecker.analyzer;

import java.util.List;

import com.redhat.ceylon.compiler.typechecker.context.TypecheckerUnit;
import com.redhat.ceylon.compiler.typechecker.tree.Tree;
import com.redhat.ceylon.compiler.typechecker.tree.Tree.AnyAttribute;
import com.redhat.ceylon.compiler.typechecker.tree.Tree.AttributeDeclaration;
import com.redhat.ceylon.compiler.typechecker.tree.Tree.ObjectDefinition;
import com.redhat.ceylon.compiler.typechecker.tree.Tree.Parameter;
import com.redhat.ceylon.compiler.typechecker.tree.Tree.ParameterList;
import com.redhat.ceylon.compiler.typechecker.tree.Tree.TypeArgumentList;
import com.redhat.ceylon.compiler.typechecker.tree.Tree.Variable;
import com.redhat.ceylon.compiler.typechecker.tree.Visitor;
import com.redhat.ceylon.model.typechecker.model.Class;
import com.redhat.ceylon.model.typechecker.model.Type;
import com.redhat.ceylon.model.typechecker.model.TypeDeclaration;
import com.redhat.ceylon.model.typechecker.model.TypeParameter;
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
public class MutableVisitor extends Visitor {

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
	private final OpaqueValidationVisitor opaqueVisitor = new OpaqueValidationVisitor();
	
	public MutableVisitor(TypecheckerUnit unit) {
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
	public void visit(Tree.TypedDeclaration that) {
		if (that instanceof AnyAttribute || that instanceof Variable || that instanceof ObjectDefinition) {
			handelVariable((Value)that.getDeclarationModel(), that);
		}
		CurrentClass orig;
		if (that instanceof ObjectDefinition) {
			orig = beginObjectScope(new CurrentClass(((ObjectDefinition)that).getAnonymousClass()));
		} else {
			orig = beginObjectScope(new CurrentClass(null, false));			
		} 
		super.visit(that);
		endObjectScope(orig);	
	}

	void handelVariable(Value declarationModel, Tree.TypedDeclaration that) {
		if (state.isInClass()) {
			if (state.isMutable()) {
				if (declarationModel.isVariable() && !declarationModel.isTransient()) {
					Type type = declarationModel.getType();
					TypeDeclaration declaration = type.eliminateNull().getDeclaration();
					boolean opaque = false;
					if (declaration instanceof TypeParameter) {
						opaque = ((TypeParameter)declaration).isOpaque();
					}
					if (type != null && !type.isSubtypeOf(immutableType) && !opaque) {
						if(declarationModel.isShared()) {
							that.addError("Shared variable values on mutable annotated class '"
								+ state.className
								+ "' must be immutable or opaque. Found: " + type);
						}
						if (state.isInParameterList()) {
							that.addError("Parameters to a mutable annotated class '"
									+ state.className
									+ "' must be immutable or opaque. Found: " + type);
						}
					}
				}
				if (declarationModel.isShared() || state.isInParameterList()) {
					that.visit(opaqueVisitor);
				}
			} else {
				if (declarationModel.isVariable()
						&& !declarationModel.isNative()
						&& !declarationModel.isTransient()) {
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
		CurrentClass previous = beginObjectScope(new CurrentClass(
				declarationModel));
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
					if (type.isSubtypeOf(immutableType)) {
						//Check to make sure it is not 'aware' of any opaque types.
						p.visitChildren(opaqueVisitor);
					} else {
						TypeDeclaration declaration = type.eliminateNull().getDeclaration();
						boolean opaque = false;
						if (declaration instanceof TypeParameter) {
							opaque = ((TypeParameter)declaration).isOpaque();
						}
						if (!opaque) {
							that.addError("Method parameters to mutable annotated class '"
									+ orig.className
									+ "' must be immutable or opaque. Found: " + type);
						}
					}
				}
			}
		}
		endObjectScope(orig);
	}
	

	private class OpaqueValidationVisitor extends Visitor {
		@Override
		public void visit(Tree.SimpleType that) {
			TypeArgumentList tal = that.getTypeArgumentList();
			TypeDeclaration dm = that.getDeclarationModel();
			if (tal == null || dm == null) {
				return;
			}
			List<Tree.Type> typeArguments = tal.getTypes();
			List<TypeParameter> typeParameters = dm.getTypeParameters();

			for (int i = 0; i < typeParameters.size() && i < typeArguments.size(); i++) {
				Tree.Type arg = typeArguments.get(i);

				if (arg instanceof Tree.SimpleType) {
					TypeDeclaration declaration = ((Tree.SimpleType) arg).getDeclarationModel();
					if (declaration instanceof TypeParameter) {
						if (((TypeParameter) declaration).isOpaque()) {
							if (!typeParameters.get(i).isOpaque()) {
								that.addError("Opaque type: "
										+ declaration.getName()
										+ " may not be used in a non-opaque way.");
							}
						}
					}
				}
			}
		}
	}
}
