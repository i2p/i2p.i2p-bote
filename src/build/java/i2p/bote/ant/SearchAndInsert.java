/**
 * Copyright (C) 2009  HungryHobo@mail.i2p
 * 
 * The GPG fingerprint for HungryHobo@mail.i2p is:
 * 6DD3 EAA2 9990 29BC 4AD2 7486 1E2C 7B61 76DC DC12
 * 
 * This file is part of I2P-Bote.
 * I2P-Bote is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * I2P-Bote is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with I2P-Bote.  If not, see <http://www.gnu.org/licenses/>.
 */

package i2p.bote.ant;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

/**
 * Searches a text file for a string and adds text before
 * or after the first occurrence of the search string.
 */
public class SearchAndInsert {
    enum Position {BEFORE, AFTER};
    
    public static void main(String[] args) throws IOException {
        if (args.length < 4) {
            System.out.println("Syntax: SearchAndInsert [-after|-before] <inputFile> <outputFile> <key> <newTextFile>");
            System.out.println();
            System.out.println("Searches a text file for a string and adds text before or after");
            System.out.println("the first occurrence of the search string. The new text is read");
            System.out.println("from a file.");
            System.out.println("The default is to insert after the search string.");
            System.exit(1);
        }
        
        int argIndex = 0;
        Position position = Position.AFTER;
        if ("-before".equals(args[0]) || "-after".equals(args[0])) {
            argIndex++;
            if ("-before".equals(args[0]))
                position = Position.BEFORE;
        }
        
        BufferedReader reader = null;
        BufferedWriter writer = null;
        try {
            reader = new BufferedReader(new FileReader(args[argIndex++]));
            writer = new BufferedWriter(new FileWriter(args[argIndex++]));
            
            String searchString = args[argIndex++];
            String newTextFile = args[argIndex];
            
            while (true) {
                String line = reader.readLine();
                if (line == null)
                    break;
                boolean match = line.contains(searchString);
                if (match && position==Position.BEFORE) {
                    write(writer, newTextFile);
                    writer.newLine();
                }
                writer.write(line);
                writer.newLine();
                if (match && position==Position.AFTER) {
                    write(writer, newTextFile);
                    writer.newLine();
                }
            }
        }
        finally {
            if (reader != null)
                reader.close();
            if (writer != null)
                writer.close();
        }
    }
    
    private static void write(BufferedWriter writer, String filename) throws IOException {
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new FileReader(filename));
            while (true) {
                String line = reader.readLine();
                if (line == null)
                    break;
                writer.write(line);
                writer.newLine();
            }
        }
        finally {
            if (reader != null)
                reader.close();
        }
    }
}