package org.siphon.visualbasic;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import org.siphon.visualbasic.compile.CompileException;
import org.siphon.visualbasic.compile.Compiler;
import org.siphon.visualbasic.compile.ImplementorClassModuleDecl;
import org.siphon.visualbasic.compile.JavaClassModuleDecl;
import org.siphon.visualbasic.runtime.ArrayDef;
import org.siphon.visualbasic.runtime.CallFrame;
import org.siphon.visualbasic.runtime.ModuleInstance;
import org.siphon.visualbasic.runtime.RuntimeLibrary;
import org.siphon.visualbasic.runtime.VbRuntimeException;
import org.siphon.visualbasic.runtime.VbValue;
import org.siphon.visualbasic.runtime.VbVarType;
import org.siphon.visualbasic.runtime.VbVariable;
import org.siphon.visualbasic.runtime.framework.vb.Form;
import org.siphon.visualbasic.runtime.statements.AssignStatement;
import org.siphon.visualbasic.runtime.statements.EvalAssignableStatement;
import org.siphon.visualbasic.runtime.statements.NewStatement;

import vba.VbaParser.EventStmtContext;
import vba.VbaParser.ImplementsStmtContext;

public class FormModuleDecl extends ClassModuleDecl {

	private final ControlDef controlDef;
	
	private VarDecl varDecl;
	
	private List<VarDecl> controlDecls = new ArrayList<>();

	public FormModuleDecl(Library library, Compiler compiler, ControlDef controlDef) {
		super(library, compiler);
		this.controlDef = controlDef;
		this.moduleType = ModuleType.Form;
		this.name = controlDef.getName();
	}
	
	public void initVarDecls() {
		// 初始化变量声明，如 Form1，既是一个类，又自动声明一个同名对象
		VarDecl varDecl = new VarDecl(this.library, null);
		varDecl.name = this.name;
		varDecl.visibility = Visibility.PUBLIC;
		varDecl.withNew = false;
		varDecl.varType = new VbVarType(VbVarType.vbObject, 
				this.library.types.get(this.upperCaseName()), null, null);
		compiler.addDecl(this, null, varDecl);
		this.varDecl = varDecl;

		// 初始化成员
		for(ControlDef child : controlDef.getChildren()) {			
			VbVarType type = child.getType();			
			if(child.getAttributes().containsKey("Index")) {
				if(this.members.containsKey(child.getName().toUpperCase()) == false) {
					int maxIndex = findMaxControlArrayIndex(child.getName());
					type = type.toArrayType(new ArrayDef.Rank[] {new ArrayDef.Rank(0, maxIndex)});
				} else {
					continue;
				}
			} 
			VarDecl controlDecl = new VarDecl(this.library, this);
			controlDecl.visibility = Visibility.PRIVATE;
			controlDecl.varType = type;
			controlDecl.name = child.getName();
			controlDecl.withEvents = true;
			if(!type.isArray()) controlDecl.withNew = true;
			this.addMember(controlDecl);
			this.controlDecls.add(controlDecl);
		}
	}
	

	private int findMaxControlArrayIndex(String name) {
		int result = 0;
		Stack<ControlDef> stk = new Stack<>();
		for(ControlDef child : this.controlDef.getChildren()) {
			stk.push(child);
		}
		while(stk.isEmpty() == false) {
			ControlDef def = stk.pop();
			if(def.getName().equalsIgnoreCase(name)) {
				VbValue index = def.getAttributes().get("Index");
				if(index != null) {
					Integer i = (Integer) index.toJava();
					if(i > result) {
						result = i;
					}
				}
			}
			for(ControlDef child : def.getChildren()) {
				stk.push(child);
			}
		}
		return result;
	}

	protected void addTheBaseObjDecl() {
		VarDecl c = new VarDecl(this.library, this);
		c.name = "Form";
		c.withEvents = true;
		c.visibility = Visibility.PRIVATE;	// TODO HIDDEN
		c.withNew = true;
		
		JavaClassModuleDecl formClass = (JavaClassModuleDecl) compiler.getVbLib().modules.get("FORM");
		ClassTypeDecl classTypeDecl = (ClassTypeDecl) compiler.getVbLib().types.get("FORM");
		c.varType = new VbVarType(VbVarType.vbObject, classTypeDecl, null, null);

		this.addMember(c);

		this.baseObject = c;
		
		this.implementClasses.add(classTypeDecl);
		for(VbDecl m : formClass.members.values()) {			
			this.addMember(m);
		}
	}

	public ControlDef getControlDef() {
		return controlDef;
	}

	// 为窗体实例初始化变量, 由于窗体没有单独的 ModuleInstance 类，改由 Decl 负责初始化窗体
	public void initVariables(Interpreter interpreter, CallFrame callFrame, RuntimeLibrary runtimeLib) throws VbRuntimeException {
		// Form1 
		ModuleInstance formInstance = new ModuleInstance(this);
		VbValue value = formInstance.asVbValue();
		VbVariable var = this.varDecl.createVar();
		var.setReadonly(true);
		var.value = value;
		formInstance.initializeClass(interpreter, callFrame);
		runtimeLib.variables.put(this.varDecl, var);
		
		// 成员变量
		for(VarDecl decl : controlDecls) {
			VbVariable control = decl.createVar();
			control.setReadonly(true);
			formInstance.variables.put(decl, control);
		}
	}

	public VarDecl getVarDecl() {
		return varDecl;
	}



}
