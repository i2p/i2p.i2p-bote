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

import javax.mail.Address;
import javax.mail.Message.RecipientType;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.servlet.jsp.tagext.BodyTagSupport;
import javax.servlet.jsp.tagext.JspTag;

import net.i2p.util.Log;

public class RecipientTag extends BodyTagSupport {
    private static final long serialVersionUID = 4675618920560347711L;
    
    private Log log = new Log(RecipientTag.class);
    private RecipientType type;
    private Address address;

    @Override
    public int doEndTag() {
        JspTag parent = findAncestorWithClass(this, SendEmailTag.class);
        if (parent instanceof SendEmailTag) {
            SendEmailTag sendEmailTag = (SendEmailTag)parent;
            sendEmailTag.addRecipient(type, address);
        }
        else
            log.error("No SendEmailTag ancestor found. Ancestor: " + parent);
        
        return EVAL_PAGE;
    }

    /**
     * Sets the type of recipient.
     * @param type Can be any of the {@link javax.mail.Message.RecipientType} values (TO, CC, or BCC).
     */
    public void setType(String type) {
        if (RecipientType.TO.toString().equalsIgnoreCase(type))
            this.type = RecipientType.TO;
        else if (RecipientType.CC.toString().equalsIgnoreCase(type))
            this.type = RecipientType.CC;
        else if (RecipientType.BCC.toString().equalsIgnoreCase(type))
            this.type = RecipientType.BCC;
        else {
            log.error("Unknown recipient type: <" + type + ">");
            this.type = RecipientType.TO;
        }
    }

    public String getType() {
        return type.toString();
    }

    public void setAddress(String address) throws AddressException {
        this.address = new InternetAddress(address);
    }

    public String getAddress() {
        return address.toString();
    }
}