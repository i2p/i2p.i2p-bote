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

import i2p.bote.fileencryption.PasswordException;

import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.jsp.tagext.BodyTagSupport;
import javax.servlet.jsp.tagext.TryCatchFinally;

public class RequirePasswordTag extends BodyTagSupport implements TryCatchFinally {
    private static final long serialVersionUID = -7546294707936895413L;
    
    private String forwardUrl;

    @Override
    public int doStartTag() {
        return EVAL_BODY_INCLUDE;
    }
    
    @Override
    @SuppressWarnings("deprecation")   // for javax.servlet.jsp.el.ELException
    public void doCatch(Throwable t) throws Throwable {
        boolean isPasswordException = t instanceof PasswordException || t.getCause() instanceof PasswordException;
        // Special handling of javax.servlet.jsp.el.ELException thrown by Jetty (version 5.1.15, at least):
        // This exception has a separate method named getRootCause() which returns the PasswordException
        // while the regular getCause() method returns null.
        isPasswordException |= t instanceof javax.servlet.jsp.el.ELException && ((javax.servlet.jsp.el.ELException)t).getRootCause() instanceof PasswordException;
        
        if (isPasswordException) {
            String url;
            if (forwardUrl != null)
                url = forwardUrl;
            else {
                // if no forwardUrl is given, use the original URL
                ServletRequest request = pageContext.getRequest();
                if (!(request instanceof HttpServletRequest))
                    throw new IllegalStateException("Servlet request ist not an HttpServletRequest: " + request.getClass());
                HttpServletRequest httpRequest = (HttpServletRequest)request;
                String params = httpRequest.getQueryString();
                url = httpRequest.getRequestURI();
                if (params != null)
                    url += "?" + params;
                // strip the context path (usually /i2pbote)
                int contextPathLength = httpRequest.getContextPath().length();
                url = url.substring(contextPathLength + 1);   // add one because the context path has no slash at the end
            }
            url = "password.jsp?passwordJspForwardUrl=" + url;
            pageContext.forward(url);
        }
        else
            throw t;
    }

    @Override
    public void doFinally() {
    }

    public String getForwardUrl() {
        return forwardUrl;
    }

    public void setForwardUrl(String forwardUrl) {
        this.forwardUrl = forwardUrl;
    }
}