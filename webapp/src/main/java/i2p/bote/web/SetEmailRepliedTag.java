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

import i2p.bote.folder.EmailFolder;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.tagext.SimpleTagSupport;

public class SetEmailRepliedTag extends SimpleTagSupport {
    private EmailFolder folder;
    private String messageId;
    private boolean replied;

    @Override
    public void doTag() throws JspException {
        try {
            folder.setReplied(messageId, replied);
        } catch (Exception e) {
            throw new JspException(e);
        }
    }

    public void setFolder(EmailFolder folder) {
        this.folder = folder;
    }
    
    public EmailFolder getFolder() {
        return folder;
    }
    
    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }

    public String getMessageId() {
        return messageId;
    }
    
    public void setReplied(boolean replied) {
        this.replied = replied;
    }
    
    public boolean getReplied() {
        return replied;
    }
}