package com.redhat.ceylon.compiler.typechecker.analyzer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.redhat.ceylon.compiler.typechecker.tree.Tree;
import com.redhat.ceylon.compiler.typechecker.tree.Tree.Identifier;
import com.redhat.ceylon.compiler.typechecker.tree.Tree.SimpleType;
import com.redhat.ceylon.compiler.typechecker.tree.Tree.TypeArgumentList;
import com.redhat.ceylon.compiler.typechecker.tree.Tree.TypeParameterDeclaration;
import com.redhat.ceylon.compiler.typechecker.tree.Tree.TypeParameterList;
import com.redhat.ceylon.compiler.typechecker.tree.Visitor;
import com.redhat.ceylon.model.typechecker.model.TypeDeclaration;
import com.redhat.ceylon.model.typechecker.model.TypeParameter;

/**
 * This checks to make sure that objects that are supposed to be opaque 
 * are never looked at. This is done by:
 * 1. Making sure that an opaque generic does not satisfy any other types
 * 2. Making sure that no arguments that are generically typed to the type are passed.
 * 3. Disabling is checks or switching on the type of a opaque generic.
 * 
 * @author Tom Kaitchuck
 */
public class OpaqueVisitor extends Visitor {
	
	List<String> opaqueTypes = Collections.emptyList();
	
	List<String> beginNewScope(List<String> types) {
		List<String> origional = opaqueTypes;
		opaqueTypes = types;
		return origional;
	}

	void endNewScope(List<String> types) {
		opaqueTypes = types;
	}
	
	public void visit(Tree.TypeParameterDeclaration that) {
		TypeParameter declarationModel = that.getDeclarationModel();
		if (declarationModel.isOpaque() && declarationModel.isConstrained()) {
			that.addError("Opaque type " + declarationModel.getName()
					+ " may not be constrained.");
		}
		super.visit(that);
	}

//	@Override
//	public void visit(Tree.ParameterDeclaration that) {
//		TypedDeclaration typedDeclaration = that.getTypedDeclaration();
//		Type type = typedDeclaration.getType().getTypeModel();
//		List<TypeParameter> typeParameters = type.getDeclaration().getTypeParameters();
//		for (TypeParameter typeParam : typeParameters) {
//			if (typeParam.isOpaque()) {
//				that.addError("Parameters may not have themselves constrained types. "+that);
//			}
//		}
//		super.visit(that);
//	}
	
	@Override
	public void visit(Tree.BaseType that) {
		if (opaqueTypes.isEmpty()) {
			return;
		}
		TypeArgumentList typeArgumentList = that.getTypeArgumentList();
		if (typeArgumentList == null) {
			return;
		}
		List<Tree.Type> typeArguments = typeArgumentList.getTypes();
		List<TypeParameter> typeParameters = that.getDeclarationModel()
				.getTypeParameters();
		for (int i = 0; i < typeParameters.size() && i < typeArguments.size(); i++) {
			Tree.Type arg = typeArguments.get(i);
			if (arg instanceof Tree.SimpleType) {
				TypeDeclaration declarationModel = ((Tree.SimpleType) arg)
						.getDeclarationModel();
				if (declarationModel instanceof TypeParameter) {
					if (((TypeParameter) declarationModel).isOpaque()) {
						if (!typeParameters.get(i).isOpaque()) {
							that.addError("Opaque type: "
									+ declarationModel.getName()
									+ " may not be used in a non-opaque way.");
						}
					}
				}
			}
		}
	}
	
	@Override
	public void visit(Tree.AnyClass that) {
//		if (that.getIdentifier().getText().equals("BadGenericParameterClass")) {			
//			System.out.println(that);
//		}
		List<String> opaqueParamaters = new ArrayList<>();
		TypeParameterList typeParameterList = that.getTypeParameterList();
		if (typeParameterList != null) {
			for (TypeParameterDeclaration param : typeParameterList.getTypeParameterDeclarations()) {
				if (param.getDeclarationModel().isOpaque()) {
					opaqueParamaters.add(param.getIdentifier().getText());
				}
			}
		}
		List<String> origional = beginNewScope(opaqueParamaters);
		super.visit(that);
		endNewScope(origional);
//		TypeConstraintList typeConstraints = that.getTypeConstraintList();
//		if (typeConstraints != null) {
//			for (TypeConstraint tc : typeConstraints.getTypeConstraints()) {
//				if (opaqueParamaters.contains(tc.getIdentifier())) {
//					that.addError("Opaque types may not be constrained. "+ tc.getIdentifier().getText());
//				}
//			}
//		}
//		ExtendedType et = that.getExtendedType();
//		if (et != null) {
//			SimpleType type = et.getType();
//			List<Tree.Type> typeArguments = type.getTypeArgumentList().getTypes();
//			List<Type> typeModels = type.getTypeArgumentList().getTypeModels();
//			for (Type t : typeModels) {
//				t.gett
//			}
//			TypeDeclaration td = type.getDeclarationModel();
//			if (td != null) {
//				for (TypeParameter parentParam : td.getTypeParameters()) {
//					if (parentParam.isOpaque()) {
//						System.out.println(type);
//						that.addError("Types that have opaque parameters may not be extended");
//						break;
//					}
//				}
//			}
//		}
//		SatisfiedTypes satisfiedTypes = that.getSatisfiedTypes();
//		for (StaticType st : satisfiedTypes.getTypes()) {
//			if (st instanceof Tree.SimpleType) {
//				List<com.redhat.ceylon.compiler.typechecker.tree.Tree.Type> types = ((Tree.SimpleType)st).getTypeArgumentList().getTypes();
//				types.get(0).get;
//			}
//		}
	}
	
}
