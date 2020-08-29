package macroprocessor;

import java.io.*;
import java.util.*;

public class macroprocess {

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
        BufferedReader codeReader = new BufferedReader(new FileReader("macro_inp.txt"));
        BufferedWriter outWriter = new BufferedWriter(new FileWriter("output.txt"));

        ArrayList<String> MDT = new ArrayList<>();                                  // macro definition table
        LinkedHashMap<String, Integer> MNT = new LinkedHashMap<>();                 // macro name, index in definition
        LinkedHashMap<String, ArrayList<String>> arg_names = new LinkedHashMap<>(); // macro name, list of arguments
        LinkedHashMap<String, String> ALA = new LinkedHashMap<>();                  // arg name, value

        int line_num=0;
        boolean in_macro=false, name_in_next=false;
        String currentLine;
        // read till EOF
        while ((currentLine = codeReader.readLine()) != null) {
            line_num++;
            if (currentLine.startsWith("#"))       // '#' is a comment
                continue;
            ArrayList<String> tokens = to_tokens(currentLine);
            if (tokens.size() > 0) {
                boolean check_macro = true;
                if (in_macro) {
                    if (tokens.get(0).equals("MEND")) {
                        in_macro = false;
                    }
                    else if (name_in_next) {
                        MNT.put(tokens.get(0), MDT.size()-1);
                        check_macro = false;
                        ArrayList<String> al = new ArrayList<>();
                        for (int i = 1; i<tokens.size(); i++)
                            al.add(tokens.get(i));
                        arg_names.put(tokens.get(0), al);
                        name_in_next = false;
                        outWriter.write(currentLine+"\n");
                    }
                    MDT.add(currentLine);
                }
                else if (tokens.get(0).equals("MACRO")) {
                    if (tokens.size() > 1) {
                        MNT.put(tokens.get(1), MDT.size());
                        ArrayList<String> al = new ArrayList<>();
                        for (int i = 2; i<tokens.size(); i++)
                            al.add(tokens.get(i));
                        arg_names.put(tokens.get(1), al);
                    }
                    else
                        name_in_next = true;
                    MDT.add(currentLine);
                    in_macro = true;
                }
                if(check_macro){
                    if (MNT.get(tokens.get(0)) != null){
                        String mname = tokens.get(0);
                        int i = 1;
                        for(String argnm: arg_names.get(mname))
                            ALA.put(argnm, tokens.get(i++));
                        Integer idx = MNT.get(mname);
                        while (!MDT.get(idx).contains("MEND")) {
                            if(!MDT.get(idx).contains("MACRO") && !MDT.get(idx).contains(mname)) {
                                String line = MDT.get(idx) + "\n";
                                for(String argnm: arg_names.get(mname))
                                    line = line.replace(argnm, ALA.get(argnm));
                                outWriter.write(line);
                            }
                            idx++;
                        }
                    }
                    else
                        outWriter.write(currentLine+"\n");
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
