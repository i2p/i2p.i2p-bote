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

package i2p.bote.crypto.wordlist;

import i2p.bote.Util;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import net.i2p.util.Log;

/**
 * Provides access to word lists.
 */
public class WordListAnchor {
    private Log log = new Log(WordListAnchor.class);
    private Map<String, String[]> lists;
    
    public WordListAnchor() {
        lists = new ConcurrentHashMap<String, String[]>();
    }
    
    public String[] getWordList(String localeCode) {
        return getList(localeCode);
    }

    /**
     * Based on <a href="http://www.uofr.net/~greg/java/get-resource-listing.html">
     * http://www.uofr.net/~greg/java/get-resource-listing.html</a>
     * @throws UnsupportedEncodingException 
     * @throws IOException 
     * @throws URISyntaxException 
     */
    public List<String> getLocaleCodes() throws UnsupportedEncodingException, IOException, URISyntaxException {
        String dir = WordListAnchor.class.getName();
        dir = dir.substring(0, dir.lastIndexOf('.')).replaceAll("\\.", "/");

        URL dirUrl = WordListAnchor.class.getClassLoader().getResource(dir);
        if (dirUrl == null) {
            log.error("Can't list locales in resource directory " + dir);
            return null;
        }
        String protocol = dirUrl.getProtocol();
        if ("file".equalsIgnoreCase(protocol)) {
            String[] files = new File(dirUrl.toURI()).list();
            List<String> locales = new ArrayList<String>();
            for (String filename: files)
                if (filename.matches("words_.*\\.txt"))
                    locales.add(filename.substring(6, 8));
            return locales;
        }
        else if ("jar".equalsIgnoreCase(protocol)) {
            String jarPath = dirUrl.getPath().substring(5, dirUrl.getPath().indexOf("!"));   // strip out only the JAR file
            JarFile jarFile = null;
            List<String> locales = new ArrayList<String>();
            try {
                jarFile = new JarFile(URLDecoder.decode(jarPath, "UTF-8"));
                Enumeration<JarEntry> entries = jarFile.entries();
                while (entries.hasMoreElements()) {
                    JarEntry entry = entries.nextElement();
                    if (!entry.isDirectory()) {
                        String path = entry.getName();
                        if (path.startsWith(dir)) {
                            String filename = path.substring(dir.length());
                            if (filename.matches("words_.*\\.txt"))
                                locales.add(filename.substring(6, 8));
                        }
                    }
                }
                return locales;
            } finally {
                jarFile.close();
            }
        }
        else {
            log.error("Unknown resource protocol: " + protocol);
            return null;
        }
    }
    
    private synchronized String[] getList(String localeCode) {
        if (!lists.containsKey(localeCode)) {
            URL wordListUrl = getWordListUrl(localeCode);
            if (wordListUrl == null)
                wordListUrl = getWordListUrl("en");
            List<String> wordList = Util.readLines(wordListUrl);
            String[] wordArr = wordList.toArray(new String[0]);
            lists.put(localeCode, wordArr);
            return wordArr;
        }
        return lists.get(localeCode);
    }
    
    private URL getWordListUrl(String localeCode) {
        URL wordListUrl = WordListAnchor.class.getResource("words_" + localeCode + ".txt");
        return wordListUrl;
    }
}