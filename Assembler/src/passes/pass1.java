package passes;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

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

class Symbol {
    Integer idx;
    Integer addr;
    Integer length;

    public Symbol(Integer idx, Integer addr, Integer length) {
        this.idx = idx;
        this.addr = addr;
        this.length = length;
    }

    @Override
    public String toString() {
        return addr+"\t"+length;
    }
}

public class pass1 {
    public static void main(String[] args) throws IOException {
        HashMap<String, Operator> OPTAB = new HashMap<String, Operator>();
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

        HashMap<String, Integer> REGISTERS = new HashMap<String, Integer>();
        REGISTERS.put("AREG", 1);
        REGISTERS.put("BREG", 2);
        REGISTERS.put("CREG", 3);
        REGISTERS.put("DREG", 4);

        HashMap<String, Integer> CONDITIONS = new HashMap<String, Integer>();
        CONDITIONS.put("LT", 1);
        CONDITIONS.put("LE", 2);
        CONDITIONS.put("EQ", 3);
        CONDITIONS.put("GT", 4);
        CONDITIONS.put("GE", 5);
        CONDITIONS.put("ANY", 6);

        BufferedReader codeReader = new BufferedReader(new FileReader("assembly3.txt"));
        BufferedWriter interWriter = new BufferedWriter(new FileWriter("intermediate.txt"));
        BufferedWriter mcodeWriter = new BufferedWriter(new FileWriter("machinecode.txt"));

        HashMap<String, Symbol> SYMTAB = new HashMap<>();
        HashMap<String, Integer> LITAB = new HashMap<>();
        ArrayList<HashMap<String, Integer>> LPs= new ArrayList<>();
        ArrayList<Integer> POOLTAB = new ArrayList<>();
        POOLTAB.add(1);
        int loc_cntr=0, litIdx=0;
        String currentLine;
        // read till EOF
        while ((currentLine = codeReader.readLine()) != null) {
            loc_cntr++;                     // increase in loop
            currentLine = currentLine.replace(',', ' ');  // replace ',' with space to split
            String[] tokens_spl = currentLine.split("\\s+");              // split by one or more space
            ArrayList<String> tokens = new ArrayList<>();
            for (String tok : tokens_spl) {
                if (tok.length()>0)
                    tokens.add(tok);
            }
            if (tokens.size() > 0) {
                int tokenIdx = 0;           // to keep track of index of operation name
                // if operation not found then first should be a symbol
                if(OPTAB.get(tokens.get(tokenIdx))==null) {
                    Symbol symb = SYMTAB.get(tokens.get(tokenIdx));              // check if symbol already exists
                    if (symb==null) {       // add new symbol with addr loc_cntr
                        SYMTAB.put(tokens.get(tokenIdx), new Symbol(SYMTAB.size()+1, loc_cntr, 1));
                    }
                    else {                  // if it exists
                        if (symb.addr == null)          // if address is empty, update the address
                            symb.addr = loc_cntr;
                    }
                    tokenIdx++;             // point to next as operation name
                }
                switch (OPTAB.get(tokens.get(tokenIdx)).cls) {       // process according to class
                    case "IS" -> {
                        // it can have 2 parameters
                        Integer arg1 = null;
                        String arg2 = null;
                        interWriter.write(loc_cntr+"\t"+OPTAB.get(tokens.get(tokenIdx)));
                        mcodeWriter.write(loc_cntr+"\t"+OPTAB.get(tokens.get(tokenIdx)).opCode);
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
                                    LITAB.put(arg2, null);
                                    arg2 = "L,"+litIdx;
                                }
                                else {                              // else a symbol
                                    if(SYMTAB.get(arg2)==null){     // if it's not in symbol table
                                        SYMTAB.put(arg2, new Symbol(SYMTAB.size()+1, null, 1));
                                    }
                                    arg2 = "S,"+SYMTAB.get(arg2).idx;// for intermediate code.
                                }
                            }
                        }
                        if(arg1!=null){
                            interWriter.write("\t"+arg1);
                            mcodeWriter.write("\t"+arg1);
                            if(arg2!=null) {
                                interWriter.write("\t" + arg2);
                                mcodeWriter.write("\t" + arg2);
                            }
                        }
                        interWriter.write("\n");
                        mcodeWriter.write("\n");
                    }
                    case "AD" -> {
                        switch (tokens.get(tokenIdx)) {
                            case "START" -> {               // update loc counter for start.
                                loc_cntr = Integer.parseInt(tokens.get(tokenIdx + 1));
                                interWriter.write("\tAD,1\t\tC,"+loc_cntr+"\n");
                                mcodeWriter.write("\n");
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
                                interWriter.write("\tAD,3\t\t"+ wtxt +"\n");
                                mcodeWriter.write("\n");
                                loc_cntr--;
                            }
                            case "LTORG", "END" -> {
                                if (LITAB.size()>0){
                                    loc_cntr--;
                                }
                                // iterate through literal table and assign address
                                for (Map.Entry<String, Integer> entry: LITAB.entrySet()) {
                                    if(entry.getValue()==null) {
                                        loc_cntr++;
                                        entry.setValue(loc_cntr);
                                        String literal = entry.getKey().replaceAll("[^\\d]","");
                                        interWriter.write(loc_cntr+"\tAD,"+OPTAB.get(tokens.get(tokenIdx)).opCode+"\t\t"+literal+"\n");
                                        mcodeWriter.write(loc_cntr+"\t00\t0\t"+literal+"\n");
                                    }
                                }
                                // new literal table for next instructions/pool
                                LPs.add(LITAB);
                                LITAB = new HashMap<String, Integer>();
                                // update pool table
                                POOLTAB.add(litIdx+1);
                            }
                            case "EQU" -> {
                                // set value of symbol from other symbol
                                SYMTAB.put(tokens.get(tokenIdx - 1), SYMTAB.get(tokens.get(tokenIdx + 1)));
                                interWriter.write("\tAD,"+OPTAB.get(tokens.get(tokenIdx)).opCode+"\t\tS,"+SYMTAB.get(tokens.get(tokenIdx + 1)).idx+"\n");
                                mcodeWriter.write("\n");
                                loc_cntr--;
                            }
                        }
                    }
                    case "DL" -> {
                        interWriter.write(loc_cntr+"\tDL,"+OPTAB.get(tokens.get(tokenIdx)).opCode+"\t\tC,1\n");
                        mcodeWriter.write(loc_cntr+"\n");
                    }
                }
            }
        }
        POOLTAB.remove(POOLTAB.size()-1);
        System.out.println("\nSYMBOL TABLE");
        for (Map.Entry<String, Symbol> entry: SYMTAB.entrySet())
            System.out.println(entry.getKey() + "\t" + entry.getValue());
        System.out.println("\nLITERAL TABLE");
        for (HashMap<String, Integer> LIT : LPs)
            for (Map.Entry<String, Integer> entry: LIT.entrySet())
                System.out.println(entry.getKey() + "\t" + entry.getValue());
        System.out.println("\nPOOL TABLE");
        System.out.println(POOLTAB);
        codeReader.close();
        interWriter.close();
        mcodeWriter.close();
    }
}
