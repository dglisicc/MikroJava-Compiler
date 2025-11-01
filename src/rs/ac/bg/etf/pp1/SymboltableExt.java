package rs.ac.bg.etf.pp1;

import rs.etf.pp1.symboltable.*;
import rs.etf.pp1.symboltable.concepts.*;

public class SymboltableExt extends Tab{
	
	public static Obj addObj, addAllObj;
	public static final Struct boolType = new Struct(Struct.Bool);
	
	public static void initExt() {
		Scope universe = currentScope;
		
		universe.addToLocals(new Obj(Obj.Type, "bool", boolType));
		
		universe.addToLocals(addObj = new Obj(Obj.Meth, "add", noType, 0, 1));
		{
			openScope();
			currentScope.addToLocals(new Obj(Obj.Var, "set", new Struct(SemanticAnalyzer.Set, intType), 0, 1));
			currentScope.addToLocals(new Obj(Obj.Var, "i", intType, 0, 1));
			addObj.setLocals(currentScope.getLocals());
			closeScope();
		}
		
		universe.addToLocals(addAllObj = new Obj(Obj.Meth, "addAll", noType, 0, 1));
		{
			openScope();
			currentScope.addToLocals(new Obj(Obj.Var, "set", new Struct(SemanticAnalyzer.Set, intType), 0, 1));
			currentScope.addToLocals(new Obj(Obj.Var, "niz", new Struct(Struct.Array, intType), 0, 1));
			addAllObj.setLocals(currentScope.getLocals());
			closeScope();
		}
	}
}
