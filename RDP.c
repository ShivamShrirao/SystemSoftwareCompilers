#include<stdio.h>
#include<string.h>
#include<ctype.h>
 

//Input string
char input[10];

//Non-terminal definitions
void E();
void EDash();
void T();
void TDash();
void F();

//For counting characters
int i;
//For flagging error
int error;

void E()
{
	//E->TE'
	T();
	EDash();
}

void EDash()
{
	//E'->+TE'
	if(input[i]=='+')
	{
		i++;
		T();
		EDash();
	}
	//No error since T'->eps
}

void T()
{
	//T->FT'
	F();
	TDash();
}
void TDash()
{
	//T'->*FT'
	if(input[i]=='*')
	{
		i++;
		F();
		TDash();
	}
	//No error since T'->eps
}
void F()
{
	//F->id
	if(isalnum(input[i]))
		i++;

	//F->(E)
	else if(input[i]=='(')
	{
		i++;
		E();
		if(input[i]==')')
			i++;
		else 
			//Missing closing bracket
			error=1;
	}
	//Wrong production | No eps
	else error=1;
}
      

int main()
{
	//Initializing count
	i=0;
	error=0;
	//Getting the expresssions
	printf("Enter the expression:  ");
	scanf("%s",input);
	//Starting symbol
	E();
	if(strlen(input)==i && error==0)
		printf("\nExpression is correct\n");
	else 
		printf("\nExpressions is incorrect\n");

	return 0;
}
     

/*
Output:

Enter the expression:  a++b*c

Expressions is incorrect

Enter the expression:  a+b*X

Expression is correct
*/

