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

import i2p.bote.I2PBote;
import i2p.bote.RecipientType;
import i2p.bote.packet.Email;

import java.io.IOException;

import javax.servlet.jsp.JspWriter;
import javax.servlet.jsp.PageContext;
import javax.servlet.jsp.tagext.SimpleTagSupport;

import net.i2p.util.Log;

public class SendEmailTag extends SimpleTagSupport {
    // TODO make all Log instances final
    private Log log = new Log(SendEmailTag.class);
	private String recipientAddress;
	private String message;

	public void doTag() {
		PageContext pageContext = (PageContext) getJspContext();
		JspWriter out = pageContext.getOut();
		
		Email email = new Email();
		String statusMessage;
		try {
			email.addRecipient(RecipientType.TO, recipientAddress);
			email.setContent(message);
			I2PBote.getInstance().sendEmail(email);
			statusMessage = "Email has been queued for sending.";
		}
		catch (Exception e) {
			statusMessage = "Error sending email: " + e.getLocalizedMessage();
			log.error("Error sending email", e);
		}

		try {
			out.println(statusMessage);
		} catch (IOException e) {
			log.error("Can't write output to HTML page", e);
		}
	}

	public void setRecipient(String recipient) {
		this.recipientAddress = recipient;
	}

	public String getRecipient() {
		return recipientAddress;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	public String getMessage() {
		return message;
	}
}