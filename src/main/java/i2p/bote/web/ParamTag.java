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

import javax.servlet.jsp.tagext.BodyTagSupport;
import javax.servlet.jsp.tagext.JspTag;

import net.i2p.util.Log;

public class ParamTag extends BodyTagSupport {
    private static final long serialVersionUID = 4675618920560347711L;
    
    private Log log = new Log(ParamTag.class);
    private String value;

    @Override
    public int doEndTag() {
        JspTag parent = findAncestorWithClass(this, MessageTag.class);
        if (parent instanceof MessageTag) {
            MessageTag messageTag = (MessageTag)parent;
            if (value == null)
                messageTag.addParameter(getBodyContent().getString());
            else
                messageTag.addParameter(value);
        }
        else
            log.error("No MessageTag ancestor found. Ancestor: " + parent);
        
        return EVAL_PAGE;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}