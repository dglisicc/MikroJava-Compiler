package rs.ac.bg.etf.pp1;

import org.apache.log4j.*;
import rs.ac.bg.etf.pp1.ast.*;
import rs.etf.pp1.symboltable.*;
import rs.etf.pp1.symboltable.concepts.*;

public class SemanticAnalyzer extends VisitorAdaptor {

	final static int Set = 8;

	int printCallCount = 0;
	int varDeclCount = 0;
	int nVars = 0;

	Struct voidType = new Struct(Struct.None);
	Struct boolType = new Struct(Struct.Bool);
	Struct setType = new Struct(Set, Tab.intType);
	boolean returnFound = false;
	boolean errorDetected = false;

	// =====================ERROR=====================

	Logger log = Logger.getLogger(getClass());

	public void report_error(String message, SyntaxNode info) {
		errorDetected = true;
		StringBuilder msg = new StringBuilder(message);
		int line = (info == null) ? 0 : info.getLine();
		if (line != 0)
			msg.append(" na liniji ").append(line);
		log.error(msg.toString());
	}

	public void report_info(String message, SyntaxNode info) {
		StringBuilder msg = new StringBuilder(message);
		int line = (info == null) ? 0 : info.getLine();
		if (line != 0)
			msg.append(" na liniji ").append(line);
		log.error(msg.toString());
	}

	public boolean passed() {
		return !errorDetected;
	}

	// =====================PROGRAM=====================

	public void visit(ProgName progName) {
		progName.obj = Tab.insert(Obj.Prog, progName.getProgName(), Tab.noType); // Stavljamo program u tabelu simbola i
																					// prosledjujemo gore
		Tab.openScope(); // Otvaramo scope za novoprepoznati program
	}

	public void visit(Program program) {
		nVars = Tab.currentScope.getnVars();
		// Posto smo stigli do kraja programa moramo da ulancamo simbole i da zatvorimo
		// scope
		Tab.chainLocalSymbols(program.getProgName().obj);
		Tab.closeScope();
	}

	// =====================DEKLARACIJA PROM I CONST=====================

	// TODO Odraditi i za nizove, konstante, setove
	boolean hasBrack = false;
	Struct globalVarType = null;

	public void visit(VarDeclarationType declarationType) {
		// Salje tip promenljive uz stablo (Struktura)
		declarationType.struct = declarationType.getType().struct;
		globalVarType = declarationType.getType().struct;
	}

	public void visit(VarDeclarationIdent declarationIdent) {
		// Deklarise novu promenljivu
		varDeclCount++;
		Obj varNode = null;
		String varName = declarationIdent.getVarName();
		Struct varType = globalVarType;

		// Provera da li promenljiva postoji vec
		Obj varFind = Tab.find(varName);
		if (!varFind.equals(Tab.noObj)) {
			report_error("Promenljiva " + varName + " je vec deklarisana!", declarationIdent);
		}

		if (hasBrack) {
			// Ako je lista
			Struct varList = new Struct(Struct.Array, varType);
			varNode = Tab.insert(Obj.Var, varName, varList);
			report_info("Deklarisana lista " + varName, declarationIdent);
		} else {
			// Ako nije lista
			varNode = Tab.insert(Obj.Var, varName, varType);
			report_info("Deklarisana promenljiva " + varName, declarationIdent);
		}

		// Salje ime promenljive uz stablo
		declarationIdent.obj = varNode;
	}

	// Konstante
	public void visit(ConstDeclarationType declarationType) {
		// Salje tip promenljive uz stablo (Struktura)
		declarationType.struct = declarationType.getType().struct;
		globalVarType = declarationType.getType().struct;
	}

	public void visit(ConstDeclarationIdent declarationIdent) {
		varDeclCount++;
		Obj conNode = null;
		String conName = declarationIdent.getVarName();
		Struct conType = globalVarType;

		// Provera da li promenljiva postoji vec
		Obj varFind = Tab.find(conName);
		if (!varFind.equals(Tab.noObj)) {
			report_error("Konstanta " + conName + " je vec deklarisana!", declarationIdent);
		}

		// Provere podudaranja tipova sa leve i desne strane jednakosti
		if (!globalVarType.equals(declarationIdent.getConstChoice().obj.getType())) {
			report_error("Greska: Tipovi se ne podudaraju", declarationIdent);
		}

		conNode = Tab.insert(Obj.Con, conName, conType);
		report_info("Deklarisana konstanta " + conName, declarationIdent);
		conNode.setAdr(declarationIdent.getConstChoice().obj.getAdr());
		declarationIdent.obj = conNode;

	}

	public void visit(NumConstantChoice choice) {
		Obj numConst = new Obj(Obj.Con, "NumConst", Tab.intType, choice.getValue(), 0);
		choice.obj = numConst;
	}

	public void visit(CharConstantChoice choice) {
		Obj charConst = new Obj(Obj.Con, "CharConst", Tab.charType, choice.getValue(), 0);
		choice.obj = charConst;
	}

	public void visit(BoolConstantChoice choice) {
		Obj boolConst = null;
		if (choice.getValue().equalsIgnoreCase("true")) {
			boolConst = new Obj(Obj.Con, "BoolConst", boolType, 1, 0);
		} else if (choice.getValue().equalsIgnoreCase("false")) {
			boolConst = new Obj(Obj.Con, "BoolConst", boolType, 0, 0);
		}
		choice.obj = boolConst;
	}

	public void visit(BracketsListOne bracketsListOne) {
		hasBrack = true;
	}

	public void visit(NoBracketsList bracketsList) {
		hasBrack = false;
	}

	// =====================SET=DEKLARACIJA=====================
	public void visit(SetDeclarationType declarationType) {
		// Prosledjujem tip set
		declarationType.struct = setType;
		globalVarType = setType;
	}

	public void visit(SetDeclarationIdent declarationIdent) {
		varDeclCount++;
		Obj varNode = null;
		String varName = declarationIdent.getSetName();
		Struct varType = globalVarType;

		// Provera da li promenljiva postoji vec
		Obj varFind = Tab.find(varName);
		if (!varFind.equals(Tab.noObj)) {
			report_error("Promenljiva " + varName + " je vec deklarisana!", declarationIdent);
		}

		// Ako nije lista
		varNode = Tab.insert(Obj.Var, varName, varType);
//		varNode.set(1);
		report_info("Deklarisana promenljiva " + varName, declarationIdent);

		// Salje ime promenljive uz stablo
		declarationIdent.obj = varNode;
	}

	// =====================DEKLARACIJA METODA=====================
	Obj currentMethod = null;

	public void visit(MethodDeclaration mathodDeclaration) {
		// Ulancava simbole metode i zatvara scope
		Tab.chainLocalSymbols(currentMethod);
		Tab.closeScope();

		// Resetujemo currentMethod
		currentMethod = null;
	}

	public void visit(SignatureMethodVoid methodVoid) {
		currentMethod = Tab.insert(Obj.Meth, methodVoid.getMethName(), voidType);
		methodVoid.obj = currentMethod;
		Tab.openScope();
		report_info("Obradjuje se funkcija " + currentMethod.getName(), methodVoid);
	}

	public void visit(SignatureMethodNoVoid methodNoVoid) {
		currentMethod = Tab.insert(Obj.Meth, methodNoVoid.getMethName(), methodNoVoid.getType().struct);
		methodNoVoid.obj = currentMethod;
		Tab.openScope();
		report_info("Obradjuje se funkcija " + currentMethod.getName(), methodNoVoid);
	}

	// =====================TYPE=====================
	public void visit(VarType type) {
		Obj typeNode = Tab.find(type.getTypeName());
		if (typeNode == Tab.noObj) {
			report_error("Nije pronadjen tip " + type.getTypeName() + " u tabeli simbola!", null);
			type.struct = Tab.noType;
		} else {
			// Provera ako je tabela simbola vratila objekat tipa TYPE
			if (Obj.Type == typeNode.getKind()) {
				type.struct = typeNode.getType();
			} else {
				report_error("Greska: Ime " + type.getTypeName() + " ne predstavlja tip!", type);
				type.struct = Tab.noType;
			}
		}
	}

	// =====================STATEMENT=====================
	public void visit(StmtPRINT print) {
		// Printovanje elementa niza
		if (print.getExpr().struct.getKind() == Struct.Array || print.getExpr().struct.getKind() == Set) {
			report_info("Printovanje elemnta niza", print);
			if (print.getExpr().struct.getElemType().getKind() != Struct.Int
					&& print.getExpr().struct.getElemType().getKind() != Struct.Char
					&& print.getExpr().struct.getElemType().getKind() != Struct.Bool) {
				report_error("Greska: Los tip operanda u funkiciji PRINT! Operand MORA biti ili int ili char tipa!",
						print);
			}
		}
		// Printovanje promenljive
		if (print.getExpr().struct.getKind() != Struct.Array && print.getExpr().struct.getKind() != Set) {
			report_info("Printovanje promenljive", print);
			if (print.getExpr().struct != Tab.intType && print.getExpr().struct != Tab.charType && print.getExpr().struct.getKind() != Struct.Bool) {
				report_error("Greska: Los tip operanda u funkiciji PRINT! Operand MORA biti ili int ili char tipa!",
						print);
			}
		}
		printCallCount++;
	}

	public void visit(StmtREAD read) {
		Obj des = read.getDesignator().obj;

		if (des.getKind() != Obj.Var && des.getKind() != Obj.Elem && des.getKind() != Obj.Fld) {
			report_error("Greska : Designator " + des.getName()
					+ " mora biti ili varijabla ili element niza ili polje unutar objekta!", read);
			return;
		}

		if (des.getType().getKind() != Struct.Int && des.getType().getKind() != Struct.Char
				&& des.getType().getKind() != Struct.Bool) {
			report_error("Greska: Designator " + des.getName() + " mora biti ili int ili karakter ili boolean!", read);
			return;
		}
	}

	// =====================DESIGNATORSTMT=====================
	public void visit(DesignatorStmtAssign assign) {
		Struct dest = assign.getDesignator().obj.getType();
		Struct expr = assign.getExpr().struct;

		// Ako je destinacija niz
		if (dest.getKind() == Struct.Array) {
			report_info("DESTINACIJA JE NIZ", assign);
			// Expr je niz
			if (expr.getKind() == Struct.Array) {
				report_info("Dodela vrednosti niza nizu", assign);
				if (!expr.assignableTo(dest)) {
					report_error("Nije moguce uraditi dodelu vrednosti promenljivoj "
							+ assign.getDesignator().obj.getName() + " jer tipovi nisu kompatibilni", assign);
				}
			}
			// Expr je promenljiva
			if (expr.getKind() != Struct.Array) {
				report_info("Dodela vrednosti promenljive nizu", assign);
				if (dest.getElemType().getKind() != expr.getKind()) {
					report_error("Nije moguce uraditi dodelu vrednosti promenljivoj "
							+ assign.getDesignator().obj.getName() + " jer tipovi nisu kompatibilni", assign);
				}
			}
		}
		// Ako je destinacija promenljiva
		if (dest.getKind() != Struct.Array) {
			report_info("DESTINACIJA JE PROMENLJIVA", assign);
			// Expr je niz
			if (expr.getKind() == Struct.Array) {
				report_info("Dodela vrednosti niza promenljivi", assign);
				if (!dest.assignableTo(expr.getElemType())) {
					report_error("Nije moguce uraditi dodelu vrednosti promenljivoj "
							+ assign.getDesignator().obj.getName() + " jer tipovi nisu kompatibilni", assign);
				}
			}
			// Expr je promenljiva
			if (expr.getKind() != Struct.Array) {
				report_info("Dodela vrednosti promenljive promenljivi", assign);
				if (!dest.assignableTo(expr)) {
					report_error("Nije moguce uraditi dodelu vrednosti promenljivoj "
							+ assign.getDesignator().obj.getName() + " jer tipovi nisu kompatibilni", assign);
				}
			}
		}
	}

	public void visit(DesignatorStmtActPars functionCall) {
		// Proverava da li je designator tipa funkcije
		Obj func = functionCall.getDesignator().obj;
		if (Obj.Meth == func.getKind()) {
			// Jeste tipa funkcije
			// TODO functionCall.struct = func.getType();
		} else {
			// Nije tipa funkcije
			report_error("Greska na liniji " + functionCall.getLine() + " : ime nije funkcija!", null);
			// TODO Treba da se posalje Tab.noType uz stablo
		}
	}

	public void visit(DesignatorStmtINC stmtINC) {
		// Treba da proveri da li je designator tipa int
		log.info("INC instrukcija");
		if (stmtINC.getDesignator().obj.getKind() != Obj.Var && stmtINC.getDesignator().obj.getKind() != Obj.Elem
				&& stmtINC.getDesignator().obj.getKind() != Obj.Fld) {
			report_error("Greska : Promenljiva mora biti ili varijabla ili element niza ili polje unutar objekta!",
					stmtINC);
		}
		if (!stmtINC.getDesignator().obj.getType().equals(Tab.intType)) {
			report_error("Greska: Promenljiva nije tipa int", stmtINC);
		}
	}

	public void visit(DesignatorStmtDEC stmtDEC) {
		// Treba da proveri da li je designator tipa int
		log.info("DEC instrukcija");
		if (stmtDEC.getDesignator().obj.getKind() != Obj.Var && stmtDEC.getDesignator().obj.getKind() != Obj.Elem
				&& stmtDEC.getDesignator().obj.getKind() != Obj.Fld) {
			report_error("Greska : Promenljiva mora biti ili varijabla ili element niza ili polje unutar objekta!",
					stmtDEC);
		}
		if (!stmtDEC.getDesignator().obj.getType().equals(Tab.intType)) {
			report_error("Greska: Promenljiva nije tipa int", stmtDEC);
		}
	}

	public void visit(DesignatorStmtSET stmtSET) {
		// TODO Ovde jos nz sta treba da se uradi
		if (!stmtSET.getDesignator().obj.getType().equals(setType)
				|| !stmtSET.getDesignator1().obj.getType().equals(setType)
				|| !stmtSET.getDesignator2().obj.getType().equals(setType)) {
			report_error(
					"Greska desgiSet: Designator " + stmtSET.getDesignator().getDesignatorName() + " nije tipa SET!",
					stmtSET);
		}
	}
	// =====================ACTPARS=====================
	// Ovo je za metode tako da ne bi trebalo da bude potrebno!!!!!

	// =====================CONDITION=====================
	public void visit(MainCondition condition) {
		condition.struct = condition.getCondTerm().struct;
	}

	// TODO CondTermORList
	// =====================CONDTERM=====================
	public void visit(ConditionalTerm conditionalTerm) {
		conditionalTerm.struct = conditionalTerm.getCondFact().struct;
	}

	// TODO CondFactANDList
	// =====================CONDFACT=====================
	public void visit(ConditionFact conditionFact) {
		conditionFact.struct = conditionFact.getExpr().struct;
	}

	// TODO RELOP EXPR
	// =====================EXPR=====================
	public void visit(ExpressionYesMinus exp) {
		// Treba da se proveri da li je Term tipa int
		if (exp.getAddopTermList().struct.equals(Tab.noType)) {
			exp.struct = exp.getTerm().struct;
		} else {
			if (!exp.getAddopTermList().struct.equals(exp.getTerm().struct)) {
				report_error("Greska Expr: Strukture se ne podudaraju!", exp);
				exp.struct = Tab.noType;
			} else {
				exp.struct = exp.getTerm().struct;
			}
		}
	}

	public void visit(ExpressionNoMinus exp) {
		if (exp.getAddopTermList().struct.equals(Tab.noType)) {
			exp.struct = exp.getTerm().struct;
		} else {
			if (!exp.getAddopTermList().struct.equals(exp.getTerm().struct)) {
				report_error("Greska Expr: Strukture se ne podudaraju!", exp);
				exp.struct = Tab.noType;
			} else {
				exp.struct = exp.getTerm().struct;
			}
		}
	}

	public void visit(AddOperationTermList list) {
		if (list.getAddopTermList().struct.equals(Tab.noType)) {
			list.struct = list.getTerm().struct;
		} else {
			if (!list.getAddopTermList().struct.equals(list.getTerm().struct)) {
				report_error("Greska Expr: Strukture se ne podudaraju!", list);
				list.struct = Tab.noType;
			} else {
				list.struct = list.getTerm().struct;
			}
		}
	}

	public void visit(NoAddOperationTermList list) {
		list.struct = Tab.noType;
	}

	// =====================TERM=====================
	public void visit(TermMain termMain) {
		// Trebam da proverim da li se tipovi podudaraju
		// Prosljedjujem tip
		report_info("Factor je tipa: " + termMain.getFactor().struct.getKind(), termMain);
		report_info("Lista je tipa: " + termMain.getMulopFactorList().struct.getKind(), termMain);
		if (termMain.getMulopFactorList().struct.equals(Tab.noType)) {
			termMain.struct = termMain.getFactor().struct;
		} else {
			if (!termMain.getMulopFactorList().struct.equals(termMain.getFactor().struct)) {
				report_error("Greska Term: Strukture se ne podudaraju!", termMain);
				termMain.struct = Tab.noType;
			} else {
				termMain.struct = termMain.getFactor().struct;
			}
		}
	}

	public void visit(MulOperationFactorList mulOpFactor) {
		// Trebam da proverim da li se tipovi podudaraju
		// Prosljedjujem tip
		report_info("Factor je tipa: " + mulOpFactor.getFactor().struct.getKind(), mulOpFactor);
		report_info("Lista je tipa: " + mulOpFactor.getMulopFactorList().struct.getKind(), mulOpFactor);
		if (mulOpFactor.getMulopFactorList().struct.equals(Tab.noType)) {
			mulOpFactor.struct = mulOpFactor.getFactor().struct;
		} else {
			if (!mulOpFactor.getMulopFactorList().struct.equals(mulOpFactor.getFactor().struct)) {
				report_error("Greska Term: Strukture se ne podudaraju!", mulOpFactor);
				mulOpFactor.struct = Tab.noType;
			} else {
				mulOpFactor.struct = mulOpFactor.getFactor().struct;
			}
		}
	}

	public void visit(NoMulOperationFactorList noMulOpFactor) {
		// Prosledjujem tip int
		noMulOpFactor.struct = Tab.noType;
	}

	// =====================FACTOR=====================
	public void visit(FacDesignator factor) {
		// Prosledjuje tip promenljive
		if (factor.getDesignator().obj.getType().getKind() == Struct.Array) {
			factor.struct = factor.getDesignator().obj.getType().getElemType();
		} else {
			factor.struct = factor.getDesignator().obj.getType();
		}
	}

	public void visit(FacDesignatorFunction designatorFunction) {
		// Proveravam da li je globalna funckija i prosledjujem povratnu vrednost
		// funkcije
		Obj function = Tab.find(designatorFunction.getDesignator().obj.getName());
		if (function == Tab.noObj) {
			report_error("Greska: Funckija nije deklarisana!", designatorFunction);
		} else {
			designatorFunction.struct = designatorFunction.getDesignator().obj.getType();
		}
	}

	public void visit(FacNumConst numConst) {
		report_info("Definisan int faktor", numConst);
		numConst.struct = Tab.intType;

		if (addCounter == 1) {
			addCounter = 0;
			addFlag = false;
		}
	}

	public void visit(FacCharConst charConst) {
		report_info("Definisan char faktor", charConst);
		charConst.struct = Tab.charType;
	}

	public void visit(FacBoolConst boolConst) {
		report_info("Definisan bool faktor", boolConst);
		log.info(boolConst.getB1());
		boolConst.struct = boolType;
	}

	public void visit(FacNew facNew) {
		// Pravi se niz
		log.info("Pravi se novi NIZ");
		Struct array = new Struct(Struct.Array, facNew.getType().struct);
		facNew.struct = array;
	}

	public void visit(FacNewSet facNewSet) {
		// Expr mora biti tipa int
		// Pravi se set velicine expr
		if (!facNewSet.getExpr().struct.equals(Tab.intType)) {
			report_error("Greska FACTOR newSET: Expr nije tipa int!", facNewSet);
		} else {
			log.info("Pravi se novi SET");
			// TODO TREBAM DA NAPRAVIM STRUKTURU ZA SETOVE
			facNewSet.struct = setType;
			facNewSet.struct.setElementType(Tab.intType);
		}
	}

	public void visit(FacExpr expr) {
		// Prosledjuje tip strukture izraza u zagradi
		expr.struct = expr.getExpr().struct;
	}

	public void visit(NewOptionArray optionArray) {
		// Provera da li je Expr tipa int
		if (!optionArray.getExpr().struct.equals(Tab.intType)) {
			report_error("Greska FACTOR newNIZ:: Expr nije tipa int!", optionArray);
		} else {
			optionArray.struct = optionArray.getExpr().struct;
		}
	}

	public void visit(NewOptionClass optionClass) {
		// Ne treba mi vrv
	}

	// =====================DESIGNATOR=====================
	public void visit(Designator designator) {
		// Ako ima zagrade mora da proveri da li je desiganator niz
		// Treba da prosledi designator gore
		designator.obj = designator.getDesignatorName().obj;
	}

	boolean addFlag = false;
	int addCounter = 0;
	boolean addAllFlag = false;
	int addAllCounter = 0;

	public void visit(DesignatorName designatorName) {
		Obj obj = Tab.find(designatorName.getDesignatorName());
		if (obj == Tab.noObj) {
			// Nije pronasao objekat u tabeli simbola
			report_error("Greska DESIGNATORname: ime " + designatorName.getDesignatorName() + " nije deklarisano!",
					null);
			designatorName.obj = obj;
			return;
		} else {
			if (obj.getKind() == Obj.Con) {
				report_info("Upotreba simbolicke konstante " + obj.getName(), designatorName);
			} else if (obj.getKind() == Obj.Meth) {
				report_info("Poziv globalne funkcije " + obj.getName(), designatorName);
				if (obj.getName().equalsIgnoreCase("add")) {
					report_info("Prepoznata add funckija!", designatorName);
					addFlag = true;
				} else if (obj.getName().equalsIgnoreCase("addAll")) {
					report_info("Prepoznata addAll funckija!", designatorName);
					addAllFlag = true;
				}
			} else if (obj.getKind() == Obj.Var) {
				// Provera da li je lokalna ili globalna promenljiva
				if (obj.getLevel() == 1) {
					report_info("Upotreba formalnog parametra " + obj.getName(), designatorName);
				} else {
					report_info("Upotreba lokalne pormenljive " + obj.getName(), designatorName);
				}
				if (addFlag) {
					if (addCounter == 0) {
						if (obj.getType().getKind() != 8)
							report_error("Prvi parametar funckije add nije tipa SET!", designatorName);
						else
							addCounter++;
					} else if (addCounter == 1) {
						if (obj.getType().getKind() != Tab.intType.getKind())
							report_error(
									"Drugi parametar funckije add nije tipa INT!" + designatorName.getDesignatorName(),
									designatorName);
						addCounter = 0;
						addFlag = false;
					}
				} else if (addAllFlag) {
					if (addAllCounter == 0) {
						if (obj.getType().getKind() != 8)
							report_error("Prvi parametar funckije addAll nije tipa SET!", designatorName);
						else
							addAllCounter++;
					} else {
						if (obj.getType().getKind() != Struct.Array || obj.getType().getElemType() != Tab.intType) {
							report_error("Drugi parametar funkcije addAll mora biti niz sa elemenitma tipa INT!",
									designatorName);
						} else {
							addAllFlag = false;
							addAllCounter = 0;
						}
					}
				}

			}
		}
		designatorName.obj = obj;
	}

	public void visit(DesignatorMoreExpr moreExpr) {
		// TODO Proveri da li ovo dobro radi
		// Tip Expr mora da bude int
		if (moreExpr.getExpr().struct.getKind() != Struct.Int) {
			log.info("Expr greska 2");
			report_error("Greska DESIGNATORexpr: Expr nije tipa int!", moreExpr);
			return;
		}
	}
}
