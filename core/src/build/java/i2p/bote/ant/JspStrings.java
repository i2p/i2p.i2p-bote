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
 * A command line program that extracts strings from <ib:message> tags and prints them
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

        String[] tags = fileContents.split("<ib:message");
        
        List<String> msgKeys = new ArrayList<String>();
        for (int i=1; i<tags.length; i++) {
            String tag = tags[i];
            if (!shouldSkip(tag)) {
                String key = extract(tag);
                if (key != null)
                    msgKeys.add(key);
            }
        }
        
        List<PoEntry> entries = new ArrayList<PoEntry>();
        entries.addAll(PoEntry.create(msgKeys, file));
        
        return entries;
    }
    
    /**
     * Extracts a message key from a ib:message tag
     * @param tag a ib:message tag minus the &lt;ib:message part at the beginning
     * @return
     */
    static String extract(String tag) {
        if (tag.contains(" key="))   // format is <ib:message key="foobar" .../> ...
            return extractFromKeyAttribute(tag);
        else   // format is <ib:message ...>foobar</ib:message> ...
            return extractFromTagBody(tag);
    }
    
    /**
     * Extracts the text between <ib:message> and </ib:message>
     * @param tag a ib:message tag minus the &lt;ib:message part at the beginning
     */
    static String extractFromTagBody(String tag) {
        int gtIndex = tag.indexOf(">");
        tag = tag.substring(gtIndex + 1);
        int endIdx = tag.indexOf("</ib:message");
        if (endIdx < 0) {
            System.err.println("No \"</ib:message\" end tag in string: <" + tag + ">");
            return null;
        }
        String str = tag.substring(0, endIdx);
        // convert multiple whitespaces in a row to a single space, remove leading and trailing whitespace
        str = Util.removeExtraWhitespace(str);
        return str;
    }
    
    /** Extracts the values of the "key" attribute */
    static String extractFromKeyAttribute(String string) {
        String keyAttrSQ = "key='";
        String keyAttrDQ = "key=\"";
        int keyAttrLen = keyAttrDQ.length();
        
        int startIdxSQ = string.indexOf(keyAttrSQ);
        int endIdxSQ = string.indexOf("'", startIdxSQ+keyAttrLen);
        int startIdxDQ = string.indexOf(keyAttrDQ);
        int endIdxDQ = string.indexOf("\"", startIdxDQ+keyAttrLen);
        
        if (startIdxSQ>=0 && (startIdxSQ<startIdxDQ || startIdxDQ<0) && endIdxSQ>=0)
            return string.substring(startIdxSQ+keyAttrLen, endIdxSQ);
        else if (startIdxDQ>=0 && (startIdxDQ<startIdxSQ || startIdxSQ<0) && endIdxDQ>=0)
            return string.substring(startIdxDQ+keyAttrLen, endIdxDQ);
        else {
            System.err.println("Expected a string containing key=\"...\" or key='...', got " + string);
            return null;
        }
    }
    
    /** Returns <code>true</code> if a ib:message tag has the noextract attribute set to <code>true</code>. */
    static boolean shouldSkip(String tag) {
        tag = tag.replaceAll("\\s+", "");
        return tag.contains("noextract=\"true\"") || tag.contains("noextract='true'");
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