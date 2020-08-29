package macroprocessor;

import java.io.*;
import java.util.*;

public class macroprocess {
    public static void main(String[] args) throws IOException {
        BufferedReader codeReader = new BufferedReader(new FileReader("macro_inp.txt"));
        BufferedWriter outWriter = new BufferedWriter(new FileWriter("output.txt"));

        ArrayList<String> MDT = new ArrayList<>();                       // macro definition
        LinkedHashMap<String, Integer> MNT = new LinkedHashMap<>();                 // macro name, index in definition
        LinkedHashMap<String, ArrayList<String>> arg_names = new LinkedHashMap<>(); // macro name, list of arguments
        LinkedHashMap<String, String> ALA = new LinkedHashMap<>();                  // arg name, value

        int line_num=0;
        boolean in_macro=false, name_in_next=false;
        String currentLineO;
        // read till EOF
        while ((currentLineO = codeReader.readLine()) != null) {
            line_num++;
            if (currentLineO.startsWith("#"))       // '#' is a comment
                continue;
            String currentLine = currentLineO.replace(',', ' ');  // replace ',' with space to split
            String[] tokens_spl = currentLine.split("\\s+");              // split by one or more space
            ArrayList<String> tokens = new ArrayList<>();
            for (String tok : tokens_spl) {
                if (tok.length()>0)
                    tokens.add(tok);
            }
            if (tokens.size() > 0) {
                if (in_macro) {
                    if (tokens.get(0).equals("MEND")) {
                        in_macro = false;
                    }
                    else if (name_in_next) {
                        MNT.put(tokens.get(0), MDT.size()-1);
                        ArrayList<String> al = new ArrayList<>();
                        for (int i =1; i<tokens.size(); i++)
                            al.add(tokens.get(i));
                        arg_names.put(tokens.get(0), al);
                        name_in_next = false;
                    }
                    MDT.add(currentLineO);
                }
                else if (tokens.get(0).equals("MACRO")) {
                    if (tokens.size() > 1) {
                        MNT.put(tokens.get(1), MDT.size());
                    }
                    else
                        name_in_next = true;
                    MDT.add(currentLineO);
                    in_macro = true;
                }
                else if (MNT.get(tokens.get(0)) != null){
                    String mname = tokens.get(0);
                    int i = 1;
                    for(String argnm: arg_names.get(mname))
                        ALA.put(argnm, tokens.get(i++));
                }
            }
        }
        codeReader.close();
        for (int i=0; i < MDT.size(); i++) {
            System.out.println(i+" "+MDT.get(i));
        }

        System.out.println("\n");
        for (Map.Entry<String, Integer> entry: MNT.entrySet())
            System.out.println(entry.getKey() + "\t" + entry.getValue());

        System.out.println("\n");
        for (Map.Entry<String, ArrayList<String>> entry: arg_names.entrySet())
            System.out.println(entry.getKey() + ":\t" + entry.getValue());

        System.out.println("\n");
        for (Map.Entry<String, String> entry: ALA.entrySet())
            System.out.println(entry.getKey() + ":\t" + entry.getValue());
    }
}
