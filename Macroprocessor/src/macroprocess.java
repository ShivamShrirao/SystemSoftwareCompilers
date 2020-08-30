import java.io.*;
import java.util.*;

public class macroprocess {

    static ArrayList<String> MDT = new ArrayList<>();                                  // macro definition table
    static LinkedHashMap<String, Integer> MNT = new LinkedHashMap<>();                 // macro name, index in definition
    static LinkedHashMap<String, ArrayList<String>> arg_names = new LinkedHashMap<>(); // macro name, list of arguments
    static LinkedHashMap<String, String> ALA = new LinkedHashMap<>();                  // arg name, value

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

    static void expand_macro(ArrayList<String> tokens, BufferedWriter outWriter, int depth) throws IOException {
        String mname = tokens.get(0);
        int i = 1;
        for(String argnm: arg_names.get(mname))
            ALA.put(argnm, tokens.get(i++));            // get values of arguments
        Integer idx = MNT.get(mname);                   // get index of macro definition
        while (!MDT.get(idx).contains("MEND")) {        // till end of macro
            if(!MDT.get(idx).contains("MACRO") && !MDT.get(idx).contains(mname)) {      // if not MACRO start and not declaration of name.
                String line = MDT.get(idx);             // get macro line
                for(String argnm: arg_names.get(mname)) {
                    line = line.replace(argnm, ALA.get(argnm)); // replace the argument with value
                }
                ArrayList<String> ltoks = to_tokens(line);
                if (MNT.get(ltoks.get(0)) != null){     // macro found inside a macro
                    if (depth<5){                       // check depth of recursion
                        expand_macro(ltoks, outWriter, ++depth);
                    }
                    else {
                        System.out.println("[!] Maximum recursion depth reached. Check macro definition !");
                        return;
                    }
                }
                else
                    outWriter.write(line + "\n");
            }
            idx++;
        }
    }

    public static void main(String[] args) throws IOException {
        BufferedReader codeReader = new BufferedReader(new FileReader("macro_inp.txt"));
        BufferedWriter outWriter = new BufferedWriter(new FileWriter("output.txt"));

        boolean in_macro=false, name_in_next=false;
        String currentLine;
        // read till EOF, Pass 1
        while ((currentLine = codeReader.readLine()) != null) {
            if (currentLine.startsWith("#"))       // '#' is a comment
                continue;
            ArrayList<String> tokens = to_tokens(currentLine);
            if (tokens.size() > 0) {                    // if line not empty
                if (in_macro) {                         // process inside macro
                    if (tokens.get(0).equals("MEND")) {
                        in_macro = false;               // mark end of macro
                    }
                    else if (name_in_next) {            // get name of macro from next line
                        MNT.put(tokens.get(0), MDT.size()-1);       // save name of macro
                        ArrayList<String> al = new ArrayList<>();
                        for (int i = 1; i<tokens.size(); i++)       // iterate through arguments of macro
                            al.add(tokens.get(i));
                        arg_names.put(tokens.get(0), al);
                        name_in_next = false;
                    }
                    MDT.add(currentLine);               // save macro line to MDT
                }
                else if (tokens.get(0).equals("MACRO")) {           // check start of macro
                    if (tokens.size() > 1) {                        // if it contains the name of macro too
                        MNT.put(tokens.get(1), MDT.size());         // save name of macro
                        ArrayList<String> al = new ArrayList<>();
                        for (int i = 2; i<tokens.size(); i++)       // iterate through arguments of macro
                            al.add(tokens.get(i));
                        arg_names.put(tokens.get(1), al);
                    }
                    else
                        name_in_next = true;            // set to look for macro name in next line
                    MDT.add(currentLine);
                    in_macro = true;
                }
            }
        }
        // Pass 2
        codeReader = new BufferedReader(new FileReader("macro_inp.txt"));
        in_macro = false;
        while ((currentLine = codeReader.readLine()) != null) {
            if (currentLine.startsWith("#"))       // '#' is a comment
                continue;
            ArrayList<String> tokens = to_tokens(currentLine);
            if (tokens.size() > 0) {
                if (currentLine.contains("MACRO")) {
                    outWriter.write(currentLine + "\n");        // if macro, just write to file
                    in_macro = true;                                // mark start of macro
                }
                else if (currentLine.contains("MEND")) {
                    outWriter.write(currentLine + "\n");
                    in_macro = false;                               // mark end of macro
                }
                else if (MNT.get(tokens.get(0)) != null && !in_macro){  // if macro name found and not in a macro definition
                    expand_macro(tokens, outWriter, 0);
                }
                else {
                    outWriter.write(currentLine + "\n");
                }
            }
        }

        System.out.println("MDT-");
        for (int i=0; i < MDT.size(); i++) {
            System.out.println(i+" "+MDT.get(i));
        }

        System.out.println("\nMNT-");
        for (Map.Entry<String, Integer> entry: MNT.entrySet())
            System.out.println(entry.getKey() + "\t" + entry.getValue());

        System.out.println("\nArg names-");
        for (Map.Entry<String, ArrayList<String>> entry: arg_names.entrySet())
            System.out.println(entry.getKey() + ":\t" + entry.getValue());

        System.out.println("\nALA-");
        for (Map.Entry<String, String> entry: ALA.entrySet())
            System.out.println(entry.getKey() + ":\t" + entry.getValue());

        codeReader.close();
        outWriter.close();
    }
}

/*
macro_inp.txt ->
MACRO
	M1 &ARG1,&ARG2
	M2 &ARG1,&ARG2
	ADD AREG &ARG1
	ADD BREG &ARG2
MEND
MACRO M2 &ARG3,&ARG4
	SUB AREG &ARG3
	SUB BREG &ARG4
MEND
START 300
MOVER AREG S1
MOVEM BREG S2
M1 D1 D2
MOVER AREG S1
M2 D3 D4
PRINT S1
PRINT S2
S1 DC 5
S2 DC 6
END


CONSOLE OUTPUT ->
MDT-
0 MACRO
1 	M1 &ARG1,&ARG2
2 	M2 &ARG1,&ARG2
3 	ADD AREG &ARG1
4 	ADD BREG &ARG2
5 MEND
6 MACRO M2 &ARG3,&ARG4
7 	SUB AREG &ARG3
8 	SUB BREG &ARG4
9 MEND

MNT-
M1	0
M2	6

Arg names-
M1:	[&ARG1, &ARG2]
M2:	[&ARG3, &ARG4]

ALA-
&ARG1:	D1
&ARG2:	D2
&ARG3:	D3
&ARG4:	D4


output.txt ->
MACRO
	M1 &ARG1,&ARG2
	M2 &ARG1,&ARG2
	ADD AREG &ARG1
	ADD BREG &ARG2
MEND
MACRO M2 &ARG3,&ARG4
	SUB AREG &ARG3
	SUB BREG &ARG4
MEND
START 300
MOVER AREG S1
MOVEM BREG S2
	SUB AREG D1
	SUB BREG D2
	ADD AREG D1
	ADD BREG D2
MOVER AREG S1
	SUB AREG D3
	SUB BREG D4
PRINT S1
PRINT S2
S1 DC 5
S2 DC 6
END

 */