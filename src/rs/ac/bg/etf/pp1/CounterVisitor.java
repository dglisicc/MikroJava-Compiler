package rs.ac.bg.etf.pp1;

import rs.ac.bg.etf.pp1.ast.*;

public class CounterVisitor extends VisitorAdaptor {
	
	protected int count;
	
	public int getCount() {
		return count;
	}
	
	// Metode koje sluze za prebrojavanje formalnih param i lokalnih param metode
	
	public static class FormParamCounter extends CounterVisitor {
		public void visit (FormalParameters fpars) {
			count++;
		}
		
		public void visit(FormalParametersMore fpars) {
			count++;
		}
	}
	
	public static class VarCounter extends CounterVisitor {
		
		public void visit(VarDeclarationIdent var) {
			count++;
		}
	}
}
