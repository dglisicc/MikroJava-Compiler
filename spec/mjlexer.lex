
package rs.ac.bg.etf.pp1;

import java_cup.runtime.Symbol;


%%

%{
	
	// Ukljucivanje informacija o poziciji tokena (Pravi token na osnovu tipa)
	private Symbol new_symbol(int type){
		return new Symbol(type, yyline+1, yycolumn);
	}
	
	// Ukljucivanje informacije o poziciji tokena ( Pravi token na osnovu tipa i vrednosti)
	private Symbol new_symbol(int type, Object value){
		return new Symbol(type, yyline+1, yycolumn, value);
	}

%}

%cup
%line
%column

%xstate COMMENT

%eofval{
	return new_symbol(sym.EOF);
%eofval}

%%

" "		{}
"\b"	{}
"\t"	{}
"\r\n"	{}
"\f"	{}

"program"	{ return new_symbol(sym.PROGRAM, yytext()); }
"break"		{ return new_symbol(sym.BREAK, yytext()); }
"class"		{ return new_symbol(sym.CLASS, yytext()); }
"else"		{ return new_symbol(sym.ELSE, yytext()); }
"const"		{ return new_symbol(sym.CONST, yytext()); }
"if"		{ return new_symbol(sym.IF, yytext()); }
"new"		{ return new_symbol(sym.NEW, yytext()); }
"print"		{ return new_symbol(sym.PRINT, yytext()); }
"read"		{ return new_symbol(sym.READ, yytext()); }
"return"	{ return new_symbol(sym.RETURN, yytext()); }
"void"      { return new_symbol(sym.VOID, yytext()); }
"extends"   { return new_symbol(sym.EXTENDS, yytext()); }
"continue"  { return new_symbol(sym.CONTINUE, yytext()); }
"set"       { return new_symbol(sym.SET, yytext()); }
"union"     { return new_symbol(sym.UNION, yytext()); }
"do"        { return new_symbol(sym.DO, yytext()); }
"while"     { return new_symbol(sym.WHILE, yytext()); }
"map"       { return new_symbol(sym.MAP, yytext()); }
"interface" { return new_symbol(sym.INTERFACE, yytext()); }

"+"    { return new_symbol(sym.PLUS, yytext()); }
"-"    { return new_symbol(sym.MINUS, yytext()); }
"*"    { return new_symbol(sym.MUL, yytext()); }
"/"    { return new_symbol(sym.DIV, yytext()); }
"%"    { return new_symbol(sym.MOD, yytext()); }
"=="   { return new_symbol(sym.EQUAL, yytext()); }
"!="   { return new_symbol(sym.NOTEQUAL, yytext()); }
">"    { return new_symbol(sym.GREATER, yytext()); }
">="   { return new_symbol(sym.GREATEREQ, yytext()); }
"<"    { return new_symbol(sym.LESS, yytext()); }
"<="   { return new_symbol(sym.LESSEQ, yytext()); }
"&&"   { return new_symbol(sym.AND, yytext()); }
"||"   { return new_symbol(sym.OR, yytext()); }
"="    { return new_symbol(sym.ASSIGN, yytext()); }
"++"   { return new_symbol(sym.INC, yytext()); }
"--"   { return new_symbol(sym.DEC, yytext()); }

";"    { return new_symbol(sym.SEMICOLON, yytext()); }
":"    { return new_symbol(sym.COLON, yytext()); }
","    { return new_symbol(sym.COMMA, yytext()); }
"."    { return new_symbol(sym.DOT, yytext()); }
"("    { return new_symbol(sym.LPAREN, yytext()); }
")"    { return new_symbol(sym.RPAREN, yytext()); }
"["    { return new_symbol(sym.LBRACK, yytext()); }
"]"    { return new_symbol(sym.RBRACK, yytext()); }
"{"    { return new_symbol(sym.LBRACE, yytext()); }
"}"    { return new_symbol(sym.RBRACE, yytext()); }


"//" {yybegin(COMMENT);}
<COMMENT> . { yybegin(COMMENT); }
<COMMENT> "\r\n" { yybegin(YYINITIAL); }

("true"|"false")	{ return new_symbol(sym.BOOLCONST, yytext()); }
[0-9]+	{ return new_symbol(sym.NUMCONST, Integer.parseInt(yytext()));}
([a-z]|[A-Z])[a-z|A-Z|0-9|_]*	{ return new_symbol(sym.IDENT, yytext()); }
"'"[a-z|A-Z|0-9|_]"'"	{return new_symbol(sym.CHARCONST, yytext().charAt(1));}

.	{ System.err.println("Leksicka greska ("+yytext()+") u liniji "+(yyline+1)); }



