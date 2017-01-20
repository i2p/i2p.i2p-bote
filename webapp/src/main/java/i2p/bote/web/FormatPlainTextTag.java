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

import java.io.IOException;

import javax.servlet.jsp.JspWriter;
import javax.servlet.jsp.PageContext;
import javax.servlet.jsp.tagext.SimpleTagSupport;

import net.i2p.util.Log;

/**
 * Formats a plain-text string for HTML by replacing newline chars with HTML tags.
 * The <code>text</code> property contains the input text.
 */
public class FormatPlainTextTag extends SimpleTagSupport {
    private static final String CRLF = "\r\n";
    private static final String LF = "\n";
    
    private Log log = new Log(FormatPlainTextTag.class);
    private String text;

    @Override
    public void doTag() {
        PageContext pageContext = (PageContext)getJspContext();
        JspWriter out = pageContext.getOut();
        
        try {
            // Handle both CRLF and LF
            text = text.replaceAll(CRLF + CRLF, "<p/>");
            text = text.replaceAll(CRLF, "<br/>");
            text = text.replaceAll(LF + LF, "<p/>");
            text = text.replaceAll(LF, "<br/>");
            // Insert a br tag between two p tags. Do it twice to handle >2 p tags in a row.
            text = text.replaceAll("<p/><p/>", "<p/><br/><p/>");
            text = text.replaceAll("<p/><p/>", "<p/><br/><p/>");
            out.println(text);
        } catch (IOException e) {
            log.error("Can't write output to HTML page", e);
        }
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getText() {
        return text;
    }
}