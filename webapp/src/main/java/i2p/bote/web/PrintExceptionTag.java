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
import java.io.PrintWriter;

import javax.servlet.jsp.JspWriter;
import javax.servlet.jsp.PageContext;
import javax.servlet.jsp.tagext.SimpleTagSupport;

import net.i2p.util.Log;

public class PrintExceptionTag extends SimpleTagSupport {
    private Log log = new Log(PrintExceptionTag.class);
    private Exception exception;

    @Override
    public void doTag() {
        PageContext pageContext = (PageContext) getJspContext();
        JspWriter out = pageContext.getOut();
        
        if (exception != null) {
            try {
                out.println("<div class=\"stackTrace\">");
                exception.printStackTrace(new PrintWriter(out));
                out.println("</div>");
            } catch (IOException e) {
                log.error("Can't write output to HTML page", e);
            }
        }
    }

    public Exception getException() {
        return exception;
    }

    public void setException(Exception exception) {
        this.exception = exception;
    }
}