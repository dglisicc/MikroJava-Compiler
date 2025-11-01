package rs.ac.bg.etf.pp1;

import rs.ac.bg.etf.pp1.ast.*;
import org.apache.log4j.*;

public class RuleVisitor extends VisitorAdaptor{
	
	int printCallCount = 0;
	int varDeclCount = 0;
	
	
	public void visit(StmtPRINT print) {
		printCallCount++;
	}
}
