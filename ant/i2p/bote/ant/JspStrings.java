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

import i2p.bote.Util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

/**
 * Extracts strings from <ib:message> tags and prints them
 * to stdout in .po format (the .po header is not printed).
 */
public class JspStrings {

    public static void main(String[] args) throws IOException {
        if (args.length < 1) {
            System.out.println("Syntax: JspStrings <Directory>");
            System.out.println();
            System.out.println("Prints .po entries for the <ib:message> keys in all .jsp");
            System.out.println("and .tag files in <Directory> and its subdirectories.");
            System.exit(1);
        }
        
        List<PoEntry> poEntries = processDirectory(new File(args[0]));
        removeDuplicates(poEntries);
        
    for (PoEntry poEntry: poEntries)
        print(poEntry);
    }
    
    static void removeDuplicates(List<PoEntry> poEntries) {
        for (int i=poEntries.size()-1; i>=0; i--)
            for (int j=poEntries.size()-1; j>i; j--) {
                PoEntry entry1 = poEntries.get(i);
                PoEntry entry2 = poEntries.get(j);
                if (entry1!=entry2 && entry1.msgKey.equals(entry2.msgKey))
                    poEntries.remove(entry2);
            }
    }
    
    static void print(PoEntry poEntry) {
        System.out.println("#: " + poEntry.comment);
        System.out.println("msgid \"" + poEntry.msgKey + "\"");
        System.out.println("msgstr \"\"");
        System.out.println();
    }
    
    static List<PoEntry> processDirectory(File dir) throws IOException {
        List<PoEntry> results = new ArrayList<PoEntry>();
        
        // extract strings from all files in the directory
        File[] files = dir.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.toLowerCase().endsWith(".jsp") || name.toLowerCase().endsWith(".tag");
            }
        });
        for (File file: files)
            results.addAll(processFile(file));
        
        // recurse into subdirectories
        File[] subdirs = dir.listFiles(new FileFilter() {
            @Override
            public boolean accept(File pathname) {
                return pathname.isDirectory();
            }
        });
        for (File subdir: subdirs)
            results.addAll(processDirectory(subdir));
        return results;
    }
    
    static List<PoEntry> processFile(File file) throws IOException {
        FileInputStream inputStream = new FileInputStream(file);
        BufferedReader inputReader = new BufferedReader(new InputStreamReader(inputStream));
        
        // Read the file into a string
        StringBuilder strBuilder = new StringBuilder();
        while (true) {
            String line = inputReader.readLine();
            if (line == null)
                break;
            if (strBuilder.length() > 0)
                strBuilder = strBuilder.append(System.getProperty("line.separator"));
            strBuilder = strBuilder.append(line);
        }
        inputStream.close();
        String fileContents = strBuilder.toString();

        List<PoEntry> entries = new ArrayList<PoEntry>();
        // Process all tags of the format <ib:message>foobar</ib:message>
        String[] noAttributeTags = fileContents.split("<ib:message>");
        List<String> msgKeys = (extractFromTagBody(noAttributeTags));
        entries.addAll(PoEntry.create(msgKeys, file));
        // Process all tags of the format <ib:message key="foobar".../>, as well as
        // <ib:message key="foo {0}"><ib:param value="bar"/></ib:message>
        String[] attributeTags = fileContents.split("<ib:message ");
        msgKeys = extractFromKeyAttribute(attributeTags);
        entries.addAll(PoEntry.create(msgKeys, file));
        
        return entries;
    }
    
    /** Extracts the text between each pair of <ib:message> and </ib:message> tags */
    static List<String> extractFromTagBody(String[] strings) {
        List<String> keys = new ArrayList<String>();
        for (int i=1; i<strings.length; i++) {
            String element = strings[i];
            int endIdx = element.indexOf("</ib:message");
            if (endIdx < 0) {
                System.err.println("No \"</ib:message\" end tag in string: <" + element + ">");
                continue;
            }
            String str = element.substring(0, endIdx);
            // convert multiple whitespaces in a row to a single space, remove leading and trailing whitespace
            str = Util.removeExtraWhitespace(str);
            keys.add(str);
        }
        return keys;
    }
    
    /** Extracts the values of all "key" attributes */
    static List<String> extractFromKeyAttribute(String[] strings) {
        String keyAttrSQ = "key='";
        String keyAttrDQ = "key=\"";
        int keyAttrLen = keyAttrDQ.length();
        
        List<String> keys = new ArrayList<String>();
        for (int i=1; i<strings.length; i++) {
            String element = strings[i];
            int startIdxSQ = element.indexOf(keyAttrSQ);
            int endIdxSQ = element.indexOf("'", startIdxSQ+keyAttrLen);
            int startIdxDQ = element.indexOf(keyAttrDQ);
            int endIdxDQ = element.indexOf("\"", startIdxDQ+keyAttrLen);
            
            String message;
            if (startIdxSQ>=0 && (startIdxSQ<startIdxDQ || startIdxDQ<0) && endIdxSQ>=0)
                message = element.substring(startIdxSQ+keyAttrLen, endIdxSQ);
            else if (startIdxDQ>=0 && (startIdxDQ<startIdxSQ || startIdxSQ<0) && endIdxDQ>=0)
                message = element.substring(startIdxDQ+keyAttrLen, endIdxDQ);
            else {
                System.err.println("Expected a string containing key=\"...\" or key='...', got " + element);
                continue;
            }
            keys.add(message);
        }
        return keys;
    }
    
    static class PoEntry {
        String msgKey;
        String comment;
        
        static List<PoEntry> create(List<String> msgKeys, File file) {
            List<PoEntry> poEntries = new ArrayList<PoEntry>();
            for (String msgKey: msgKeys) {
                PoEntry entry = new PoEntry();
                entry.msgKey = msgKey;
                entry.comment = file.toString();
                poEntries.add(entry);
            }
            return poEntries;
        }
    }
}