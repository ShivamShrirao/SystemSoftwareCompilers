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

        BufferedReader codeReader = new BufferedReader(new FileReader("assembly.txt"));
        BufferedWriter interWriter = new BufferedWriter(new FileWriter("intermediate.txt"));

        HashMap<String, Symbol> SYMTAB = new HashMap<String, Symbol>();
        HashMap<String, Integer> LITAB = new HashMap<String, Integer>();
        ArrayList<HashMap<String, Integer>> LPs= new ArrayList<>();
        int loc_cntr=0, symIdx=0, litIdx=0, conIdx=0;
        String currentLine;
        while ((currentLine = codeReader.readLine()) != null) {
            loc_cntr++;
            currentLine = currentLine.replace(',', ' ');
            String[] split = currentLine.split("\\s+");
            if (split.length > 0) {
                int tokenIdx = 0;
                if(OPTAB.get(split[tokenIdx])==null) {
                    Symbol symb = SYMTAB.get(split[tokenIdx]);
                    if (symb==null) {
                        symIdx++;
                        SYMTAB.put(split[tokenIdx], new Symbol(symIdx, loc_cntr, 1));
                    }
                    else {
                        if (symb.addr == null)
                            symb.addr = loc_cntr;
                    }
                    tokenIdx++;
                }
                switch (OPTAB.get(split[tokenIdx]).cls) {
                    case "IS" -> {
                        Integer arg1 = null;
                        String arg2 = null;
                        interWriter.write(loc_cntr+"\t"+OPTAB.get(split[tokenIdx]));
                        if(!split[tokenIdx].equals("STOP")) {
                            arg1 = REGISTERS.get(split[tokenIdx+1]);
                            if(arg1==null)
                                arg1 = CONDITIONS.get(split[tokenIdx+1]);

                            arg2 = split[tokenIdx+2];
                            if(arg2!=null){
                                if(arg2.contains("=")) {
                                    litIdx++;
                                    LITAB.put(arg2, null);
                                    arg2 = "L,"+litIdx;
                                }
                                else {
                                    if(SYMTAB.get(arg2)==null){
                                        symIdx++;
                                        SYMTAB.put(arg2, new Symbol(symIdx, null, 1));
                                    }
                                    arg2 = "S,"+SYMTAB.get(arg2).idx;
                                }
                            }
                        }
                        if(arg1!=null){
                            interWriter.write("\t"+arg1);
                            if(arg2!=null)
                                interWriter.write("\t"+arg2);
                        }
                        interWriter.write("\n");
                    }
                    case "AD" -> {
                        switch (split[tokenIdx]) {
                            case "START" -> {
                                loc_cntr = Integer.parseInt(split[tokenIdx + 1]);
                                interWriter.write("\tAD,1\t\tC,"+loc_cntr+"\n");
                                loc_cntr--;
                            }
                            case "ORIGIN" -> {
                                String operand = split[tokenIdx + 1];
                                String[] osp = operand.split("[\\W]");
                                String arg1 = osp[0];
                                String wtxt = arg1;
                                Symbol val = SYMTAB.get(arg1);
                                if (val==null)
                                    loc_cntr = Integer.parseInt(arg1);
                                else {
                                    loc_cntr = val.addr;
                                    wtxt = "S," + val.idx;
                                }
                                if (osp.length>1) {
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
                                loc_cntr--;
                            }
                            case "LTORG", "END" -> {
                                if (LITAB.size()>0){
                                    loc_cntr--;
                                }
                                for (Map.Entry<String, Integer> entry: LITAB.entrySet()) {
                                    if(entry.getValue()==null) {
                                        loc_cntr++;
                                        entry.setValue(loc_cntr);
                                        interWriter.write(loc_cntr+"\tAD,"+OPTAB.get(split[tokenIdx]).opCode+"\t\t"+entry.getKey().replaceAll("[^\\d]","")+"\n");
                                    }
                                }
                                LPs.add(LITAB);
                                LITAB = new HashMap<String, Integer>();
                            }
                            case "EQU" -> {
                                SYMTAB.put(split[tokenIdx-1], SYMTAB.get(split[tokenIdx+1]));
                                symIdx++;
                                interWriter.write("\tAD,"+OPTAB.get(split[tokenIdx]).opCode+"\t\tS,"+SYMTAB.get(split[tokenIdx+1]).idx+"\n");
                                loc_cntr--;
                            }
                        }
                    }
                    case "DL" -> {
                        interWriter.write(loc_cntr+"\tDL,"+OPTAB.get(split[tokenIdx]).opCode+"\t\tC,1\n");
                    }
                }
            }
        }
        System.out.println("\nSYMBOL TABLE");
        for (Map.Entry<String, Symbol> entry: SYMTAB.entrySet())
            System.out.println(entry.getKey() + "\t" + entry.getValue());
        System.out.println("\nLITERAL TABLE");
        for (HashMap<String, Integer> LIT : LPs)
            for (Map.Entry<String, Integer> entry: LIT.entrySet())
                System.out.println(entry.getKey() + "\t" + entry.getValue());
        codeReader.close();
        interWriter.close();
    }
}
