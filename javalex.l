%{
FILE* yyin;
#include<stdio.h>
#include<string.h>
struct SymbolTable
{
	char symbol[100];
	char type[100];	
}SymbolTable[10];
void insert();
int search();
void display();
char data[100];
char type[100];
//For counter
int count = 0;
int i=0;

%}

DIGIT [0-9]
TEXT [A-Za-z]
INT {DIGIT}+
FLOAT ({DIGIT}*[.]{INT})
OPERATOR "+"|"-"|"/"|"%"|"*"
LOGICAL_OP "&&"|"||"|"!"
RELATIONS "<"|">"|"<="|">="|"=="|"!="
UNI_OP "++"|"--"
KEYWORDS "class"|"static"|"return"|"main"
ACCESS "public"|"private"|"protected"|""
DATATYPE "int"|"float"|"double"|"long"|"void"|"String"
CONDITIONAL "if"|"else"|"else if"|"switch"|"case"
LOOP "for"|"while"|"do"
IDENTIFIER {TEXT}({DIGIT}|{TEXT}|"_")*
FUNCTION ({ACCESS}" "{DATATYPE}" "{IDENTIFIER}"("({DATATYPE}" "{IDENTIFIER})*")")
SEMICOLON ";"
IMPORT "import"[^\n]*";"

%%
[ \n\t]+ ;
{IMPORT} {printf("%s\t==> IMPORT STATEMENT\n",yytext);}
{INT} {printf("%s\t==> INTEGER\n",yytext);}
{FLOAT} {printf("%s\t==> REAL CONSTANT\n",yytext);}
{OPERATOR} {printf("%s\t==> OPERATOR\n",yytext);}
{FUNCTION} {	printf("%s\t==> FUNCTION\n",yytext);
		strcpy(data,yytext);
		strcpy(type,"FUNCTION");
		return 5;
	   }
{LOGICAL_OP} {printf("%s\t==> LOGICAL OP\n",yytext);}
{CONDITIONAL} { printf("%s\t==> CONDITIONAL\n",yytext);}
{LOOP} {printf("%s\t==> LOOP\n",yytext);}
{DATATYPE}  {printf("%s\t==> DATATYPE\n",yytext);}
{ACCESS} {printf("%s\t==> ACCESS SPECIFIER\n",yytext);}
{KEYWORDS} {printf("%s\t==> KEYWORDS\n",yytext);}
{IDENTIFIER} {	printf("%s\t==> IDENTIFIER\n",yytext);
		strcpy(data,yytext);
		strcpy(type,"VARAIBLE");
		return 5;
	     }
{UNI_OP} {printf("%s\t==> UNARY OPERATOR\n",yytext);}
{RELATIONS} {printf("%s\t==> RELATIONAL OP\n",yytext);}
{SEMICOLON} {printf("%s\t==> SEMICOLON\n",yytext);}
"=" {printf("%s\t==> ASSIGNMENT OP\n",yytext);}
"{"  {printf("%s\t==> BLOCK BEGIN\n",yytext);}
"}"  {printf("%s\t==> BLOCK END\n",yytext);}
"("  {printf("%s\t==> PARANTHESIS BEGIN\n",yytext);}
")"  {printf("%s\t==> PARENTHESIS END\n",yytext);}
. ;
%%


int yywrap()
{
	return 1;
}
void insert()
{
	if(search(data)==0)
	{
		strcpy(SymbolTable[count].symbol, data);
		strcpy(SymbolTable[count].type, type);
		++count;
	}
}
int search()
{
	for(int i=0; i<count;i++)
	{
		if(strcmp(SymbolTable[i].symbol, data)==0)
			return 1;
	}
	return 0;
}
void display()
{
	for(int i=0; i<count;i++)
	{
		printf("\n%s\t%s\n", SymbolTable[i].symbol, SymbolTable[i].type);
	}
}

int main(int argc,char* argv[])
{
	yyin=fopen(argv[1],"r");

	while(i=yylex())
	{
		if(i==5)
		{
			insert();
		}
	}
	printf("\n\nSymbol Table \n\n");
	display();
	fclose(yyin);
	yywrap();
}


/*
Output:

import java.io.*;	==> IMPORT STATEMENT
public int f1(int a)	==> FUNCTION
{	==> BLOCK BEGIN
return	==> KEYWORDS
1	==> INTEGER
;	==> SEMICOLON
}	==> BLOCK END
class	==> KEYWORDS
Test	==> IDENTIFIER
{	==> BLOCK BEGIN
public	==> ACCESS SPECIFIER
static	==> KEYWORDS
void	==> DATATYPE
main	==> KEYWORDS
(	==> PARANTHESIS BEGIN
String	==> DATATYPE
args	==> IDENTIFIER
)	==> PARENTHESIS END
{	==> BLOCK BEGIN
int	==> DATATYPE
b	==> IDENTIFIER
;	==> SEMICOLON
int	==> DATATYPE
a	==> IDENTIFIER
=	==> ASSIGNMENT OP
12.7	==> REAL CONSTANT
;	==> SEMICOLON
if	==> CONDITIONAL
(	==> PARANTHESIS BEGIN
a	==> IDENTIFIER
==	==> RELATIONAL OP
12	==> INTEGER
)	==> PARENTHESIS END
{	==> BLOCK BEGIN
;	==> SEMICOLON
}	==> BLOCK END
}	==> BLOCK END
}	==> BLOCK END


Symbol Table 


public int f1(int a)	FUNCTION

Test	VARAIBLE

args	VARAIBLE

b	VARAIBLE

a	VARAIBLE

*/
