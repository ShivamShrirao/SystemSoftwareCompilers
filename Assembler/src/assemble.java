import java.io.*;
import java.util.*;

class Operator {
    String cls;
    Integer opCode;

    public Operator(String c, Integer op) {
        this.cls = c;
        this.opCode = op;
    }

    @Override
    public String toString() {
        return cls+","+opCode;
    }
}

class Globals {
    public static boolean undefined_sym = false;
}

class Symbol {
    Integer idx;
    Integer addr;
    Integer length;
    String name;
    String line;
    Integer line_num;
    boolean errored = false;

    public Symbol(Integer idx, Integer addr, Integer length, String name, String line, Integer line_num) {
        this.idx = idx;
        this.addr = addr;
        this.length = length;
        this.name = name;
        this.line = line;
        this.line_num = line_num;
    }

    public void checkDef(){
        if(addr==null) {
            if(!errored) {
                System.out.println(line_num+". "+line);
                System.out.println("** ERROR ** UNDEFINED SYM '" + name + "' IN OPERAND FIELD at line "+line_num);
                Globals.undefined_sym = true;
                errored = true;
            }
        }
    }

    @Override
    public String toString() {
        return String.format("%03d", addr);
    }
}

class Literal {
    Integer idx;
    Integer addr;

    public Literal(Integer idx, Integer addr) {
        this.idx = idx;
        this.addr = addr;
    }

    @Override
    public String toString() {
        return String.format("%03d", addr);
    }
}

class Mcode {
    Integer loc;
    Integer opCode;
    Integer arg1;
    Object arg2;

    public Mcode(Integer loc, Integer opCode, Integer arg1, Object arg2) {
        this.loc = loc;
        this.opCode = opCode;
        this.arg1 = arg1;
        this.arg2 = arg2;
    }

    @Override
    public String toString() {
        if(arg2 instanceof Symbol){
            ((Symbol) arg2).checkDef();
        }
        return Objects.toString(loc, "") + "\t\t" +
                String.format("%02d", opCode) + "\t" +
                String.format("%02d", arg1).replace("null","") + "\t" +
                String.format("%3s", arg2).replace(" ", "0").replace("null","");
    }
}

public class assemble {

    static ArrayList<String> to_tokens(String line) {
        String currentLine = line.replace(',', ' ');       // replace ',' with space to split
        String[] tokens_spl = currentLine.split("\\s+");              // split by one or more space
        ArrayList<String> tokens = new ArrayList<>();
        for (String tok : tokens_spl) {
            if (tok.length()>0)
                tokens.add(tok);
        }
        return tokens;
    }

    public static void main(String[] args) throws IOException {
        HashMap<String, Operator> OPTAB = new HashMap<>();
        OPTAB.put("STOP", new Operator("IS",0));
        OPTAB.put("ADD", new Operator("IS",1));
        OPTAB.put("SUB", new Operator("IS",2));
        OPTAB.put("MULT", new Operator("IS",3));
        OPTAB.put("MOVER", new Operator("IS",4));
        OPTAB.put("MOVEM", new Operator("IS",5));
        OPTAB.put("COMP", new Operator("IS",6));
        OPTAB.put("BC", new Operator("IS",7));
        OPTAB.put("DIV", new Operator("IS",8));
        OPTAB.put("READ", new Operator("IS",9));
        OPTAB.put("PRINT", new Operator("IS",10));
        OPTAB.put("DC", new Operator("DL",1));
        OPTAB.put("DS", new Operator("DL",2));
        OPTAB.put("START", new Operator("AD",1));
        OPTAB.put("END", new Operator("AD",2));
        OPTAB.put("ORIGIN", new Operator("AD",3));
        OPTAB.put("EQU", new Operator("AD",4));
        OPTAB.put("LTORG", new Operator("AD",5));

        HashMap<String, Integer> REGISTERS = new HashMap<>();
        REGISTERS.put("AREG", 1);
        REGISTERS.put("BREG", 2);
        REGISTERS.put("CREG", 3);
        REGISTERS.put("DREG", 4);

        HashMap<String, Integer> CONDITIONS = new HashMap<>();
        CONDITIONS.put("LT", 1);
        CONDITIONS.put("LE", 2);
        CONDITIONS.put("EQ", 3);
        CONDITIONS.put("GT", 4);
        CONDITIONS.put("GE", 5);
        CONDITIONS.put("ANY", 6);

        BufferedReader codeReader = new BufferedReader(new FileReader("assembly.txt"));
        BufferedWriter interWriter = new BufferedWriter(new FileWriter("intermediate.txt"));
        BufferedWriter mcodeWriter = new BufferedWriter(new FileWriter("machinecode.txt"));

        ArrayList<Mcode> mcodeList = new ArrayList<>();
        LinkedHashMap<String, Symbol> SYMTAB = new LinkedHashMap<>();
        LinkedHashMap<String, Literal> LITAB = new LinkedHashMap<>();
        ArrayList<LinkedHashMap<String, Literal>> LPs = new ArrayList<>();
        ArrayList<Integer> POOLTAB = new ArrayList<>();
        POOLTAB.add(1);
        int loc_cntr=0, litIdx=0, line_num=0;
        boolean errored=false, duplicate_def=false;
        String currentLine;
        // read till EOF
        while ((currentLine = codeReader.readLine()) != null) {
            line_num++;
            if (currentLine.startsWith("#"))       // '#' is a comment
                continue;
            loc_cntr++;                     // increase in loop
            ArrayList<String> tokens = to_tokens(currentLine);
            if (tokens.size() > 0) {
                int tokenIdx = 0;           // to keep track of index of operation name
                // if operation not found then first should be a symbol
                if(OPTAB.get(tokens.get(tokenIdx))==null) {
                    Symbol symb = SYMTAB.get(tokens.get(tokenIdx));              // check if symbol already exists
                    if (symb==null) {       // add new symbol with addr loc_cntr
                        SYMTAB.put(tokens.get(tokenIdx), new Symbol(SYMTAB.size()+1, loc_cntr, 1, tokens.get(tokenIdx), currentLine, line_num));
                    }
                    else {                  // if it exists
                        if (symb.addr == null) {          // if address is empty, update the address
                            symb.addr = loc_cntr;
                            duplicate_def = false;
                        }
                        else
                            duplicate_def = true;
                    }
                    tokenIdx++;             // point to next as operation name
                }
                Operator currentOperator = OPTAB.get(tokens.get(tokenIdx));
                if(currentOperator==null){
                    System.out.println(line_num+". "+currentLine);
                    System.out.println("**ERROR ** INVALID OPCODE at line "+line_num);
                    errored = true;
                    continue;
                }
                switch (currentOperator.cls) {       // process according to class
                    case "IS" -> {
                        // it can have 2 parameters
                        Integer arg1 = null;
                        String arg2 = null;
                        interWriter.write(loc_cntr+"\t\t"+currentOperator);
                        Mcode mcodeobj = new Mcode(loc_cntr, currentOperator.opCode, 0, 0);
                        if(!tokens.get(tokenIdx).equals("STOP")) {       // if not STOP, cause STOP has no parameters
                            arg1 = REGISTERS.get(tokens.get(tokenIdx + 1));
                            if(arg1==null)
                                arg1 = CONDITIONS.get(tokens.get(tokenIdx + 1));
                            try{
                                arg2 = tokens.get(tokenIdx + 2);
                            }
                            catch (ArrayIndexOutOfBoundsException e) {
                                arg2=null;
                            }
                            if(arg2!=null){
                                if(arg2.contains("=")) {            // it's a literal
                                    litIdx++;
                                    Literal litobj = new Literal(litIdx,null);
                                    LITAB.put(arg2, litobj);
                                    arg2 = "L,"+litIdx;
                                    mcodeobj.arg2 = litobj;
                                }
                                else {                              // else a symbol
                                    if(SYMTAB.get(arg2)==null){     // if it's not in symbol table
                                        SYMTAB.put(arg2, new Symbol(SYMTAB.size()+1, null, 1, arg2, currentLine, line_num));
                                    }
                                    Symbol symbobj = SYMTAB.get(arg2);
                                    mcodeobj.arg2 = symbobj;
                                    arg2 = "S,"+symbobj.idx;// for intermediate code.
                                }
                            }
                        }
                        if(arg1!=null){
                            interWriter.write("\t"+arg1);
                            mcodeobj.arg1 = arg1;
                            if(arg2!=null)
                                interWriter.write("\t" + arg2);
                        }
                        interWriter.write("\n");
                        mcodeList.add(mcodeobj);
                    }
                    case "AD" -> {
                        switch (tokens.get(tokenIdx)) {
                            case "START" -> {               // update loc counter for start.
                                loc_cntr = Integer.parseInt(tokens.get(tokenIdx + 1));
                                interWriter.write("\t\tAD,1\t\tC,"+loc_cntr+"\n");
                                loc_cntr--;
                            }
                            case "ORIGIN" -> {              // update loc counter
                                String operand = tokens.get(tokenIdx + 1);
                                String[] osp = operand.split("[\\W]");  // split by non alphanumeric
                                String arg1 = osp[0];
                                String wtxt = arg1;
                                Symbol val = SYMTAB.get(arg1);      // check if argument is a symbol
                                if (val==null)                      // if not symbol then set loc_cntr
                                    loc_cntr = Integer.parseInt(arg1);
                                else {                              // set address of symbol if it's symbol
                                    loc_cntr = val.addr;
                                    wtxt = "S," + val.idx;
                                }
                                if (osp.length>1) {                 // check if something is added or subtracted from address
                                    if(operand.contains("+")) {
                                        loc_cntr += Integer.parseInt(osp[1]);
                                        wtxt += " +"+osp[1];
                                    }
                                    else if(operand.contains("-")) {
                                        loc_cntr -= Integer.parseInt(osp[1]);
                                        wtxt += " -" + osp[1];
                                    }
                                }
                                interWriter.write("\t\tAD,3\t\t"+ wtxt +"\n");
                                mcodeList.add(new Mcode(null, 3, null, loc_cntr));
                                loc_cntr--;                         // decreased because it's increased later at start of loop
                            }
                            case "LTORG", "END" -> {
                                if (LITAB.size()>0){
                                    loc_cntr--;         // decrement because incremented later.
                                }
                                // iterate through literal table and assign address
                                for (Map.Entry<String, Literal> entry: LITAB.entrySet()) {
                                    if(entry.getValue().addr==null) {
                                        loc_cntr++;
                                        entry.getValue().addr = loc_cntr;
                                        String literal = entry.getKey().replaceAll("[^\\d]","");
                                        interWriter.write(loc_cntr+"\t\tAD,"+currentOperator.opCode+"\t\t"+literal+"\n");
                                        mcodeList.add(new Mcode(loc_cntr, 0, 0, literal));
                                    }
                                }
                                // new literal table for next instructions/pool
                                LPs.add(LITAB);
                                LITAB = new LinkedHashMap<>();
                                // update pool table
                                POOLTAB.add(litIdx+1);
                            }
                            case "EQU" -> {
                                Symbol dest = SYMTAB.get(tokens.get(tokenIdx - 1));
                                Symbol source = SYMTAB.get(tokens.get(tokenIdx + 1));
                                // set value of symbol from other symbol
                                dest.addr = source.addr;
                                interWriter.write("\t\tAD,"+currentOperator.opCode+"\t\tS,"+source.idx+"\n");
                                mcodeList.add(new Mcode(null, currentOperator.opCode, null, source.addr));
                                loc_cntr--;
                            }
                        }
                    }
                    case "DL" -> {
                        if(duplicate_def) {
                            System.out.println(line_num+". "+currentLine);
                            System.out.println("** ERROR ** DUPLICATE DEFINITION OF SYM '"+tokens.get(tokenIdx-1)+"' at line "+line_num);
                            errored = true;
                        }
                        interWriter.write(loc_cntr+"\t\tDL,"+currentOperator.opCode+"\t\tC,"+tokens.get(tokenIdx+1)+"\n");
                        mcodeList.add(new Mcode(loc_cntr, currentOperator.opCode, null, tokens.get(tokenIdx+1)));
                    }
                }
            }
        }
        POOLTAB.remove(POOLTAB.size()-1);
        codeReader.close();

        if(!errored){
            System.out.println("\nSYMBOL TABLE");
            for (Map.Entry<String, Symbol> entry: SYMTAB.entrySet())
                System.out.println(entry.getKey() + "\t" + entry.getValue().addr + "\t" + entry.getValue().length);

            System.out.println("\nLITERAL TABLE");
            for (LinkedHashMap<String, Literal> LIT : LPs)
                for (Map.Entry<String, Literal> entry: LIT.entrySet())
                    System.out.println(entry.getKey() + "\t" + entry.getValue());

            System.out.println("\nPOOL TABLE");
            System.out.println(POOLTAB);

            interWriter.close();
        }

        // generate final machine code
        for (Mcode mc : mcodeList){
            mcodeWriter.write(mc.toString()+"\n");
        }
        if(!Globals.undefined_sym)
            mcodeWriter.close();
    }
}

/*
assembly.txt ->
START 200
	MOVER AREG, ='5'
	MOVEM AREG, A
LOOP MOVER AREG, A
	MOVER CREG, B
	ADD CREG, ='1'
	BC ANY, NEXT
	LTORG
NEXT SUB AREG, ='1'
	BC LT, BACK
LAST STOP
	ORIGIN LOOP+2
	MULT CREG, B
	ORIGIN LAST+1
	A DS 1
BACK EQU LOOP
	B DC 1
END


CONSOLE OUTOUT ->
SYMBOL TABLE
A	211	1
LOOP	202	1
B	212	1
NEXT	208	1
BACK	202	1
LAST	210	1

LITERAL TABLE
='5'	206
='1'	207
='1'	213

POOL TABLE
[1, 3]


intermediate.txt ->
		AD,1		C,200
200		IS,4	1	L,1
201		IS,5	1	S,1
202		IS,4	1	S,1
203		IS,4	3	S,3
204		IS,1	3	L,2
205		IS,7	6	S,4
206		AD,5		5
207		AD,5		1
208		IS,2	1	L,3
209		IS,7	1	S,5
210		IS,0
		AD,3		S,2 +2
204		IS,3	3	S,3
		AD,3		S,6 +1
211		DL,2		C,1
		AD,4		S,2
212		DL,1		C,1
213		AD,2		1


machinecode.txt - >
200		04	01	206
201		05	01	211
202		04	01	211
203		04	03	212
204		01	03	207
205		07	06	208
206		00	00	005
207		00	00	001
208		02	01	213
209		07	01	202
210		00	00	000
		03		204
204		03	03	212
		03		211
211		02		001
		04		202
212		01		001
213		00	00	001

 */