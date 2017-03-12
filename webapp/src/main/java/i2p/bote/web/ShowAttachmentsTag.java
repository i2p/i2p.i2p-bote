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

import org.apache.taglibs.standard.functions.Functions;

import java.io.IOException;
import java.util.List;

import javax.mail.MessagingException;
import javax.mail.Part;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.JspWriter;
import javax.servlet.jsp.PageContext;
import javax.servlet.jsp.tagext.SimpleTagSupport;

import i2p.bote.email.Email;

/** See <code>i2pbote.tld</code> for a description */
public class ShowAttachmentsTag extends SimpleTagSupport {
    private Email email;
    private String folder;
    
    @Override
    public void doTag() throws IOException, JspException {
        PageContext pageContext = (PageContext)getJspContext();
        JspWriter out = pageContext.getOut();
        
        try {
            // make an HTML link for each attachment
            List<Part> parts = email.getParts();
            for (int partIndex=0; partIndex<parts.size(); partIndex++) {
                Part part = parts.get(partIndex);
                if (Part.ATTACHMENT.equalsIgnoreCase(part.getDisposition())) {
                    String filename = Functions.escapeXml(part.getFileName());
                    out.println("<a href=\"showAttachment?messageID=" + email.getMessageID() + "&folder=" + folder + "&part=" + partIndex + "\">" +
                            filename + "</a> (" + JSPHelper.getHumanReadableSize(part) + ") <br/>");
                }
            }
        } catch (MessagingException e) {
            throw new JspException("Can't parse email.", e);
        }
    }
    
    public void setEmail(Email email) {
        this.email = email;
    }
    
    public Email getEmail() {
        return email;
    }

    public void setFolder(String folder) {
        this.folder = folder;
    }

    public String getFolder() {
        return folder;
    }
}
