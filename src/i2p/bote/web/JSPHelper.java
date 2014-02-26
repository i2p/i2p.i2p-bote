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

package i2p.bote.web;

import i2p.bote.util.GeneralHelper;

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import javax.servlet.ServletContext;
import net.i2p.I2PAppContext;
import net.i2p.util.Log;
import net.i2p.util.Translate;

/**
 * Implements the JSP functions defined in the <code>i2pbote.tld</code> file,
 * and serves as a bean for JSPs.
 */
public class JSPHelper extends GeneralHelper {
    public static String escapeQuotes(String s) {
        if (s == null)
            return null;

        return s.replaceAll("\"", "&quot;").replaceAll("'", "&apos;");
    }

    public static String urlEncode(String input) {
        try {
            return URLEncoder.encode(input, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            new Log(JSPHelper.class).error("UTF-8 not supported!", e);
            return input;
        }
    }

    public static String urlDecode(String input) {
        try {
            return URLDecoder.decode(input, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            new Log(JSPHelper.class).error("UTF-8 not supported!", e);
            return input;
        }
    }

    /**
     * Inserts the current locale (<code>de</code>, <code>fr</code>, etc.) into a filename
     * if the locale is not <code>en</code>. For example, <code>FAQ.html</code> becomes
     * <code>FAQ_fr.html</code> in the French locale.
     * @param baseName The filename for the <code>en</code> locale
     * @param context To find out if a localized file exists
     * @return The name of the localized file, or <code>baseName</code> if no localized version exists
     */
    public static String getLocalizedFilename(String baseName, ServletContext context) {
        String language = Translate.getLanguage(I2PAppContext.getGlobalContext());

        if (language.equals("en"))
            return baseName;
        else {
            String localizedName;
            if (baseName.contains(".")) {
                int dotIndex = baseName.lastIndexOf('.');
                localizedName = baseName.substring(0, dotIndex) + "_" + language + baseName.substring(dotIndex);
            }
            else
                localizedName = baseName + "_" + language;

            try {
                if (context.getResource("/" + localizedName) != null)
                    return localizedName;
                else
                    return baseName;
            } catch (MalformedURLException e) {
                new Log(JSPHelper.class).error("Invalid URL: </" + localizedName + ">", e);
                return baseName;
            }
        }
    }
}