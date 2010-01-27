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
 * Prints up to <code>MAX_LENGTH</code> characters of a "sender" header field.
 */
public class PrintShortSenderNameTag extends SimpleTagSupport {
    private static final int MAX_LENGTH = 50;
    
    private Log log = new Log(PrintShortSenderNameTag.class);
    private String sender;

    public void doTag() {
        PageContext pageContext = (PageContext)getJspContext();
        JspWriter out = pageContext.getOut();
        
        if (sender != null)
            try {
                int angBracketIndex = sender.indexOf('<');
                if (angBracketIndex > 0)
                    sender = sender.substring(0, angBracketIndex-1);
                
                if (sender.length() > MAX_LENGTH)
                    out.print(sender.substring(0, MAX_LENGTH-3) + "...");
                else
                    out.print(sender);
            } catch (IOException e) {
                log.error("Can't write output to HTML page", e);
            }
    }

    public void setSender(String sender) {
        this.sender = sender;
    }

    public String getSender() {
        return sender;
    }
}