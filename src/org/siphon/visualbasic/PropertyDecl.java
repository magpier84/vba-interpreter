package org.siphon.visualbasic;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.siphon.visualbasic.compile.CompileException;
import org.siphon.visualbasic.runtime.ArgumentDecl;
import org.siphon.visualbasic.runtime.VbVarType;

public class PropertyDecl extends ModuleMemberDecl {

	public MethodDecl get;
	public MethodDecl let;
	public MethodDecl set;
	private VbVarType returnType;
	private List<ArgumentDecl> arguments;

	public List<ArgumentDecl> getArguments() {
		return arguments;
	}

	public PropertyDecl(Library library, ModuleDecl module) {
		super(library, module);
	}

	/**
	 * 获取成员，按get let set 返回，可能有空。
	 * 
	 * @return
	 */
	public MethodDecl[] getMembers() {
		return new MethodDecl[] { get, let, set };
	}

	public void addMember(MethodDecl member) throws CompileException {
		MethodDecl[] members = new MethodDecl[] { get, let, set };
		for (MethodDecl exist : members) {
			if (exist != null) {
				if (exist.methodType == member.methodType) {
					throw module.newCompileException(member.ast, CompileException.AMBIGUOUS_IDENTIFIER, member.ast);
				} else if (isArgsEq(packArgs(exist), packArgs(exist)) == false) {
					throw module.newCompileException(member.ast, CompileException.PROPERTY_NOT_MATCH, member.ast);
				}
			}

			switch (member.methodType) {
			case PropertyGet:
				this.get = member;
				if (this.returnType == null) {
					this.name = member.name;
					this.returnType = member.returnType;
					this.arguments = member.arguments;
				}
				if (this.visibility.compareTo(member.visibility) < 0)
					this.visibility = member.visibility;
				break;
			case PropertyLet:
				this.let = member;
				if (this.returnType == null) {
					this.name = member.name;
					this.returnType = member.arguments.get(member.arguments.size() - 1).varType;
					this.arguments = member.arguments.subList(0, member.arguments.size() - 1);
				}
				if (this.visibility.compareTo(member.visibility) < 0)
					this.visibility = member.visibility;
				break;
			case PropertySet:
				this.set = member;
				if (this.returnType == null) {
					this.name = member.name;
					this.returnType = member.arguments.get(member.arguments.size() - 1).varType;
					this.arguments = member.arguments.subList(0, member.arguments.size() - 1);
				}
				if (this.visibility.compareTo(member.visibility) < 0)
					this.visibility = member.visibility;
				break;
			}
		}
	}

	private boolean isArgsEq(List<VbVarType> packedArgs, List<VbVarType> packedArgs2) {
		if (packedArgs.size() != packedArgs2.size())
			return false;

		for (int i = 0; i < packedArgs.size(); i++) {
			if (packedArgs.get(i).equals(packedArgs2.get(i)) == false) {
				return false;
			}
		}
		return true;
	}

	private List<VbVarType> packArgs(MethodDecl method) {
		List<VbVarType> ls = new ArrayList<>();
		for (ArgumentDecl arg : method.arguments) {
			ls.add(arg.varType);
		}
		if (method.returnType != null) {
			ls.add(method.returnType);
		}
		return ls;
	}

	public VbVarType getReturnType() {
		return returnType;
	}

	@Override
	public String toString() {
		return "Property " + this.name + " TypeCode:" + this.returnType.vbType;
	}

	public boolean isReadonly() {
		return this.set == null && this.let == null;
	}

}
