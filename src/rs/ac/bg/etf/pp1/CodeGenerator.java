package rs.ac.bg.etf.pp1;

import java.util.HashMap;

import javax.crypto.Cipher;
import rs.ac.bg.etf.pp1.CounterVisitor.FormParamCounter;
import rs.ac.bg.etf.pp1.CounterVisitor.VarCounter;
import rs.ac.bg.etf.pp1.ast.*;
import rs.etf.pp1.mj.runtime.*;
import rs.etf.pp1.symboltable.*;
import rs.etf.pp1.symboltable.concepts.Obj;
import rs.etf.pp1.symboltable.concepts.Struct;

public class CodeGenerator extends VisitorAdaptor {
	private int mainPc;

	public int getMainPc() {
		return mainPc;
	}

	// Printovanje
	boolean print = false;

	public void visit(StmtPRINT printStmt) {
		if (printStmt.getExpr().struct == Tab.intType) {
			Code.loadConst(5);
			Code.put(Code.print);
		} else if (printStmt.getExpr().struct == Tab.charType) {
			Code.loadConst(1);
			Code.put(Code.bprint);
		} else if (printStmt.getExpr().struct.getKind() == Struct.Bool) {
			Code.loadConst(5);
			Code.put(Code.print);
		} else {
			// Print set
			System.out.println("Printuju se elementi seta");
			print = true;
			SyntaxNode expr = printStmt.getExpr();
			expr.traverseTopDown(this);

			Code.loadConst(1);
			// Namesten je stek sad trebam da printam vrednosti od 0 do cnt
			int loopTop = Code.pc;
			Code.put(Code.dup);
			Code.load(set);
			Code.loadConst(0);
			Code.put(Code.aload);
			Code.putFalseJump(1, 0);
			int exitJump = Code.pc - 2;

			// Printovanje vrednosti
			Code.put(Code.dup_x1);
			Code.put(Code.aload);
			Code.loadConst(5);
			Code.put(Code.print);

			// Namestanje steka
			Code.put(Code.dup_x1);
			Code.put(Code.pop);
			Code.put(Code.dup_x1);
			Code.put(Code.dup_x2);
			Code.put(Code.pop);
			Code.loadConst(1);
			Code.put(Code.add);
			Code.putJump(loopTop);

			Code.fixup(exitJump);
			Code.put(Code.pop);
			Code.put(Code.pop);
			Code.put(Code.pop);
			return;
		}
	}

	public void visit(StmtREAD readStmt) {
		int desigKind = readStmt.getDesignator().obj.getType().getKind();
		if (desigKind == Struct.Int) {
			Code.put(Code.read);
		} else {
			Code.put(Code.bread);
		}

		Code.store(readStmt.getDesignator().obj);
		Code.put(Code.pop);

	}
	public void visit(StmtRETURN return1) {
		System.out.println("Return naredba");
		Code.put(Code.exit);
		Code.put(Code.return_);
	}

	// ===================Metode======================
	// Ima povratnu vrednost
	public void visit(SignatureMethodNoVoid declName) {
		if (declName.getMethName().equalsIgnoreCase("main")) {
			mainPc = Code.pc;
		}

		declName.obj.setAdr(Code.pc);

		// Prebrojavanje argumenata i lokalnih varijabli metoda
		SyntaxNode methodNode = declName.getParent();

		VarCounter varCnt = new VarCounter();
		methodNode.traverseTopDown(varCnt);

		FormParamCounter fpCnt = new FormParamCounter();
		methodNode.traverseTopDown(fpCnt);

		// Generisanje ulaza u metodu
		Code.put(Code.enter);
		Code.put(fpCnt.getCount());
		Code.put(varCnt.getCount() + fpCnt.getCount());
	}

	// Nema povratnu vrednost
	public void visit(SignatureMethodVoid declName) {
		if (declName.getMethName().equalsIgnoreCase("main")) {
			mainPc = Code.pc;
		}

		declName.obj.setAdr(Code.pc);

		// Prebrojavanje argumenata i lokalnih varijabli metoda
		SyntaxNode methodNode = declName.getParent();

		VarCounter varCnt = new VarCounter();
		methodNode.traverseTopDown(varCnt);

		FormParamCounter fpCnt = new FormParamCounter();
		methodNode.traverseTopDown(fpCnt);

		// Generisanje ulaza u metodu
		Code.put(Code.enter);
		Code.put(fpCnt.getCount());
		Code.put(varCnt.getCount() + fpCnt.getCount());
	}

	public void visit(MethodDeclaration declaration) {
		Code.put(Code.exit);
		Code.put(Code.return_);
	}

	// =====================DESIGNATORSTMT=====================
	public void visit(DesignatorStmtAssign assign) {
		// ovo vrv moze da bude samo jedan flag posto je kod isti
		if (newArrayFlag) {
			Code.store(assign.getDesignator().obj);
			newArrayFlag = false;
			Code.put(Code.pop);
		} else if (newSetFlag) {
			Code.store(assign.getDesignator().obj);
			Code.put(Code.pop);

			// Init counter
			Code.load(assign.getDesignator().obj);
			Code.loadConst(0);
			Code.loadConst(1);
			Code.put(Code.astore);
			newSetFlag = false;
		} else {
			// Dodela vrednosti
			if (assign.getDesignator().obj.getType().getKind() == Struct.Array) {
				// Dodeljujem nizu
				System.out.println("Dodela nizu");
				if (assign.getDesignator().obj.getType().getElemType() == Tab.intType) {
					Code.put(Code.astore);
				} else {
					Code.put(Code.bastore);
				}
			} else {
				// Dodela promenljivoj
				Code.store(assign.getDesignator().obj);
				Code.put(Code.pop);
			}
		}
	}

	public void visit(DesignatorStmtINC inc) {
		Code.loadConst(1);
		Code.put(Code.add);
		Code.store(inc.getDesignator().obj);
	}

	public void visit(DesignatorStmtDEC dec) {
		Code.loadConst(1);
		Code.put(Code.sub);
		Code.store(dec.getDesignator().obj);
	}

	Obj set = null;
	Obj niz = null;

	public void visit(DesignatorStmtActPars function) {
		String fName = function.getDesignator().getDesignatorName().getDesignatorName();
		if (fName.equalsIgnoreCase("add")) {
			System.out.println("Poziv funkcije add unutar main");
			addFunc = false;

			// Funkcija add u bajtkodu
			int cnt = 0;
			// Treba mi loop od 0 do coutner i da proverava da li vec postoji

			// Proverava uslov cnt != cap
			Code.put(Code.dup);
			Code.loadConst(1);
			int countCheckTop = Code.pc;
			Code.put(Code.dup);
			Code.load(set);
			Code.loadConst(0);
			Code.put(Code.aload);
			Code.putFalseJump(1, 0);
			int adr = Code.pc - 2;

			// Proverava da li je elem isti
			Code.put(Code.dup_x1);
			Code.load(set);
			Code.put(Code.dup_x1);
			Code.put(Code.pop);
			Code.put(Code.aload);
			Code.putFalseJump(1, 0);
			int incCnt = Code.pc - 2;

			// Inc cnt
			Code.loadConst(1);
			Code.put(Code.add);
			Code.put(Code.dup_x1);
			Code.put(Code.pop);
			Code.put(Code.dup);
			Code.put(Code.dup_x2);
			Code.put(Code.pop);
			Code.put(Code.dup_x2);
			Code.put(Code.pop);
			Code.putJump(countCheckTop);

			// Dodavanje elementa u set nakon provera
			Code.fixup(adr);
			Code.put(Code.pop);
			Code.put(Code.pop);
			Code.load(set);
			Code.loadConst(0);
			Code.put(Code.aload);
			Code.put(Code.dup_x1);
			Code.put(Code.pop);
			Code.put(Code.astore);

			Code.load(set);
			Code.loadConst(0);
			Code.load(set);
			Code.loadConst(0);
			Code.put(Code.aload);
			Code.loadConst(1);
			Code.put(Code.add);
			Code.put(Code.astore);

			Code.loadConst(1);
			Code.loadConst(1);
			Code.loadConst(1);
			// Elem je isti exit
			Code.fixup(incCnt);
			Code.put(Code.pop);
			Code.put(Code.pop);
			Code.put(Code.pop);

		} else if (fName.equalsIgnoreCase("addAll")) {
			System.out.println("Poziv funckije addAll unutar main");
			// Trebam da dohvatam jednu po jednu cifru iz niza i da preko add funkcije
			// dodajem u set
			// Duplira niz za petlju i postavlja indeks
			Code.loadConst(0);
			int startOuter = Code.pc;

			// if (i >= niz.len) -> exit
			Code.put(Code.dup);
			Code.load(niz);
			Code.put(Code.arraylength);
			Code.putFalseJump(Code.lt, 0);
			int exitOuter = Code.pc - 2;

			// j = 1 za drugu petlju // if (j > set[0]) → izlaz iz unutrašnje petlje
			Code.loadConst(1);
			int startInner = Code.pc;
			Code.put(Code.dup);
			Code.load(set);
			Code.loadConst(0);
			Code.put(Code.aload);
			Code.putFalseJump(Code.le, 0); // izlaz iz unutrašnje petlje
			int exitInner = Code.pc - 2;

			// Glavni deo funkcije
			// if (niz[i] == set[j]) break; Ovde bi trebao da uradm ono sto sam u UNION samo
			// zapamtim vrednost
			// Duplira i i j
			Code.put(Code.dup_x1);
			Code.put(Code.pop);
			Code.put(Code.dup_x1);
			Code.put(Code.dup_x1);
			Code.put(Code.pop);
			Code.put(Code.dup_x1);

			// Namestam niz[i] i set[j] i radi proveru
			Code.load(set);
			Code.put(Code.dup_x1);
			Code.put(Code.pop);
			Code.put(Code.aload);
			Code.put(Code.dup_x1);
			Code.put(Code.pop);
			Code.load(niz);
			Code.put(Code.dup_x1);
			Code.put(Code.pop);
			Code.put(Code.aload);
			Code.putFalseJump(Code.ne, 0);
			int breakInner1 = Code.pc - 2;

			// if (j == set[0]) ovde bi trebalo ako je dobro da se upise broj
			Code.put(Code.dup);
			Code.load(set);
			Code.loadConst(0);
			Code.put(Code.aload);
			Code.putFalseJump(Code.eq, 0);
			int breakInner2 = Code.pc - 2;

			// Telo if bloka dodela vrednosti setu
			Code.put(Code.dup_x1);
			Code.put(Code.pop);
			Code.put(Code.dup_x1);
			// set[0]
			Code.load(set);
			Code.loadConst(0);
			Code.put(Code.aload);
			Code.put(Code.dup_x1);
			Code.put(Code.pop);
			// set[set[0]]
			Code.load(set);
			Code.put(Code.dup_x2);
			Code.put(Code.pop);

			// set[j] = niz[i]
			Code.load(niz);
			Code.put(Code.dup_x1);
			Code.put(Code.pop);
			Code.put(Code.aload);
			Code.put(Code.astore);

			// Inc set[0]
			Code.load(set);
			Code.loadConst(0);
			Code.load(set);
			Code.loadConst(0);
			Code.put(Code.aload);
			Code.loadConst(1);
			Code.put(Code.add);
			Code.put(Code.astore);

			Code.loadConst(1);
			Code.loadConst(1);
			Code.putFalseJump(Code.ne, 0);
			int breakInner3 = Code.pc - 2;

			Code.fixup(breakInner2);
			// Na kraju inner trebam da inc j
			Code.loadConst(1);
			Code.put(Code.add);
			Code.putJump(startInner);

			// Inner end // Na kraju outer trebam da inc i
			Code.fixup(breakInner1);
			Code.fixup(breakInner3);
			Code.fixup(exitInner);
			Code.put(Code.pop);
			Code.loadConst(1);
			Code.put(Code.add);
			Code.putJump(startOuter);

			// Kraj funckije
			Code.fixup(exitOuter);
			Code.put(Code.pop);
			Code.put(Code.pop);
			Code.put(Code.pop);

			addAllFunc = false;
		} else if (fName.equalsIgnoreCase("len")) {
			System.out.println("Poziv funckije len unutar main");
			desigFunc = false;
			Code.put(Code.arraylength);
		}
	}

	public void visit(DesignatorStmtSET set) {
		

		Obj dst = set.getDesignator().obj;
		Obj set1 = set.getDesignator1().obj;
		Obj set2 = set.getDesignator2().obj;
		
		if (set.getSetop().getClass().equals(SetUnion.class)) {
			System.out.println("Odradjuje se UNION operacija!");
			
			// Namestam stek
			Code.put(Code.pop);
			Code.put(Code.pop);

			// Resetujem counter u dst
			Code.loadConst(0);
			Code.loadConst(1);
			Code.put(Code.astore);

			// Koprima sve elemente iz set1 u dst
			Code.load(dst);
			Code.load(set1);
			Code.loadConst(1);
			// for (od 1 do cnt kopiraj u dst)
			int S1Copy = Code.pc;

			// Kopiranje prom
			Code.put(Code.dup_x1);
			Code.put(Code.pop);
			Code.put(Code.dup_x1);
			Code.put(Code.dup_x1);
			Code.put(Code.pop);

			// Provera da li je kraj
			Code.put(Code.dup); // Ovde se duplira indeks
			Code.load(set1);
			Code.loadConst(0);
			Code.put(Code.aload);
			Code.putFalseJump(1, 0);
			int exitCopy = Code.pc - 2;

			// Stavlja se element na poziciju
			Code.put(Code.dup_x1);
			Code.load(dst);
			Code.put(Code.dup_x2);
			Code.put(Code.pop);
			Code.put(Code.dup_x1);
			Code.put(Code.aload);
			Code.put(Code.astore);

			// Inc dst counter
			Code.load(dst);
			Code.loadConst(0);
			Code.load(dst);
			Code.loadConst(0);
			Code.put(Code.aload);
			Code.loadConst(1);
			Code.put(Code.add);
			Code.put(Code.astore);

			// Inc for petlju
			Code.loadConst(1);
			Code.put(Code.add);
			Code.putJump(S1Copy);

			// Izlazak iz for petlje
			Code.fixup(exitCopy);
			Code.put(Code.pop);
			Code.put(Code.pop);
			Code.put(Code.pop);
			Code.put(Code.pop);

			// =========================================================
			// Petlja gde se dodaju elementi iz set2 koji ne postoje u dst
			Code.load(dst);
			Code.load(set2);
			Code.put(Code.dup);

			// for petlja
			Code.loadConst(1);
			int S2Copy = Code.pc;

			// Provera da li je kraj
			Code.put(Code.dup);
			Code.load(set2);
			Code.loadConst(0);
			Code.put(Code.aload);
			Code.putFalseJump(1, 0);
			int exitCopy2 = Code.pc - 2;

			// FOR UNUTRA Ubacivanje sa proveravanjem (u sustini add metoda)
			Code.put(Code.dup_x1);
			Code.put(Code.aload);
			Code.load(dst);
			Code.put(Code.dup_x1);
			Code.put(Code.pop);
			Code.put(Code.dup);

			Code.loadConst(1);
			// For petlja add metode
			int addS2 = Code.pc;
			Code.put(Code.dup);

			// Provera kraja petlje
			Code.load(dst);
			Code.loadConst(0);
			Code.put(Code.aload);
			Code.putFalseJump(1, 0);
			int writeElem = Code.pc - 2;

			// Provera jednakosti elem
			Code.put(Code.dup_x1);
			Code.load(dst);
			Code.put(Code.dup_x1);
			Code.put(Code.pop);
			Code.put(Code.aload);
			Code.putFalseJump(1, 0);
			int equal = Code.pc - 2;

			// Ako nije jednako ide dalje sa porverama
			Code.put(Code.dup_x1);
			Code.put(Code.pop);
			Code.put(Code.dup_x1);
			Code.put(Code.dup_x1);
			Code.put(Code.pop);
			Code.loadConst(1);
			Code.put(Code.add);
			Code.putJump(addS2);

			// Ako je jednako ne upisuje se elem i ide na veci loop
			Code.fixup(equal);
			Code.put(Code.pop);
			Code.put(Code.pop);
			Code.put(Code.pop);
			// ===================
			Code.put(Code.dup_x1);
			Code.put(Code.pop);
			Code.put(Code.dup_x1);
			Code.put(Code.dup_x1);
			Code.put(Code.pop);
			// ===================
			Code.loadConst(1);
			Code.put(Code.add);
			Code.putJump(S2Copy);

			// Upis elementa
			Code.fixup(writeElem);
			Code.put(Code.dup_x2);
			Code.put(Code.pop);
			Code.put(Code.pop);
			Code.put(Code.astore);

			// Inc dst
			Code.load(dst);
			Code.loadConst(0);
			Code.load(dst);
			Code.loadConst(0);
			Code.put(Code.aload);
			Code.loadConst(1);
			Code.put(Code.add);
			Code.put(Code.astore);

			// Inc glavnu for petlju
			// ===================
			Code.put(Code.dup_x1);
			Code.put(Code.pop);
			Code.put(Code.dup_x1);
			Code.put(Code.dup_x1);
			Code.put(Code.pop);
			// ===================
			Code.loadConst(1);
			Code.put(Code.add);
			Code.putJump(S2Copy);

			// Izlazak iz glavne for petlje
			Code.fixup(exitCopy2);
			Code.put(Code.pop);
			Code.put(Code.pop);
			Code.put(Code.pop);
			Code.put(Code.pop);
		}
		else {
			// Ovde idu sledece union funckije koje mozda dobijemo na modif
		}
	}

	// =====================FACOR=====================
	public void visit(FacNumConst numConst) {
		Obj con = Tab.insert(Obj.Con, "numconst", numConst.struct);
		con.setLevel(0);
		con.setAdr(numConst.getN1());
		Code.load(con);
	}

	public void visit(FacCharConst charConst) {
		Obj con = Tab.insert(Obj.Con, "charconst", charConst.struct);
		con.setLevel(0);
		con.setAdr(charConst.getC1());
		Code.load(con);
	}

	public void visit(FacBoolConst boolConst) {
		Obj con = Tab.insert(Obj.Con, "boolconst", boolConst.struct);
		con.setLevel(0);
		if (boolConst.getB1().equalsIgnoreCase("true")) {
			con.setAdr(1);
		} else {
			con.setAdr(0);
		}
		Code.load(con);
	}

	// Poziv funkicja
	public void visit(FacDesignatorFunction function) {
		String fName = function.getDesignator().getDesignatorName().getDesignatorName();
		if (fName.equalsIgnoreCase("add")) {
			System.out.println("Poziv funkcije add unutar main");
			addFunc = false;
		} else if (fName.equalsIgnoreCase("addAll")) {
			System.out.println("Poziv funckije addAll unutar main");
			addAllFunc = false;
		} else if (fName.equalsIgnoreCase("len")) {
			System.out.println("Poziv funckije len unutar main");
			desigFunc = false;
			Code.put(Code.arraylength);
		}
	}

	// New stmt
	boolean newArrayFlag = false;
	boolean newSetFlag = false;

	public void visit(FacNew facNew) {
		newArrayFlag = true;
		// Trebam da vidim koje velicine se pravi niz
		if (facNew.getType().struct.equals(Tab.intType)) {
			System.out.println("Pravi se niz tipa int");
			Code.put(Code.newarray);
			Code.loadConst(1);
		} else {
			System.out.println("Pravi se niz tipa char");
			Code.put(Code.newarray);
			Code.loadConst(0);
		}
	}

	public void visit(FacNewSet newSet) {
		newSetFlag = true;
		System.out.println("Pravi se niz tipa set");
		Code.put(Code.const_1);
		Code.put(Code.add);
		Code.put(Code.newarray);
		Code.loadConst(0);
	}

	// =====================DESIGNATOR=====================
	// Trebam da odvojim niz od set
	boolean desigFunc = false;
	boolean addFunc = false;
	boolean addAllFunc = false;

	public void visit(Designator designator) {
		if (designator.getParent().getClass() == FacDesignator.class
				&& designator.obj.getType().getKind() == Struct.Array && (addAllFunc == false)) {
			System.out.println("Skrati se");
			if (designator.obj.getType().getElemType() == Tab.intType) {
				Code.put(Code.aload);
			} else {
				Code.put(Code.baload);
			}
		} else {
			System.out.println("Ne skrati");
		}
	}

	public void visit(DesignatorName designatorName) {
		int objKind = designatorName.obj.getKind();

		// Kupim set iz argumenata za set fje

		if (objKind != Obj.Meth) {
			Code.load(designatorName.obj); // Stavljam set na exp stakc
			if ((addFunc == true || addAllFunc == true || print == true)
					&& designatorName.obj.getType().getKind() == 8) {
				System.out.println("Dohvatio set argument");
				set = designatorName.obj;
			} else if ((addAllFunc == true) && designatorName.obj.getType().getKind() == Struct.Array) {
				System.out.println("Dohvatio niz argument");
				niz = designatorName.obj;
			}
		} else {
			// Ako je funkcija proveri da li je jedna od ove dve
			if (designatorName.getDesignatorName().equalsIgnoreCase("add")) {
				System.out.println("Prepoznata add funckija");
				addFunc = true;
			} else if (designatorName.getDesignatorName().equalsIgnoreCase("addAll")) {
				System.out.println("Prepoznata addAll funkcija");
				addAllFunc = true;
			} else {
				desigFunc = true;
			}
		}
	}

	// =====================OPERATIONS=====================
	public void visit(AddOperationTermList list) {
		Addop op = list.getAddop();
		if (op.getClass() == AddopP.class) {
			Code.put(Code.add);
		} else {
			Code.put(Code.sub);
		}
	}

	public void visit(MulOperationFactorList list) {
		Mulop op = list.getMulop();
		if (op.getClass() == MullopMUL.class) {
			Code.put(Code.mul);
		} else if (op.getClass() == MullopDIV.class) {
			Code.put(Code.div);
		} else {
			Code.put(Code.rem);
		}
	}

	public void visit(TermMain main) {
		if (main.getParent().getClass() == ExpressionYesMinus.class) {
			Code.put(Code.neg);
		}
	}
}