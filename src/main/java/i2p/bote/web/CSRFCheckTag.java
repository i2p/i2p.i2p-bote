/**
 * Copyright (C) 2017 str4d@mail.i2p
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

import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.jsp.tagext.BodyTagSupport;

import org.owasp.csrfguard.CsrfGuard;

public class CSRFCheckTag extends BodyTagSupport {
    @Override
    public int doStartTag() {
        ServletRequest request = pageContext.getRequest();
        ServletResponse response = pageContext.getResponse();
        if (!(request instanceof HttpServletRequest && response instanceof HttpServletResponse))
            throw new IllegalStateException("Servlet request is not an HttpServletRequest: " + request.getClass());
        HttpServletRequest httpRequest = (HttpServletRequest)request;
        HttpServletResponse httpResponse = (HttpServletResponse)response;

        String method = httpRequest.getMethod();
        if (method != "POST") {
            CsrfGuard csrfGuard = CsrfGuard.getInstance();
            csrfGuard.getLogger().log(String.format("CsrfGuard analyzing request %s with method %s", httpRequest.getRequestURI(), method));
            boolean valid = csrfGuard.isValidRequest(new POSTWrapper(httpRequest), httpResponse);

            if (!valid) {
                return EVAL_BODY_INCLUDE;
            }
        }
        return SKIP_BODY;
    }

    private class POSTWrapper extends HttpServletRequestWrapper {
        public POSTWrapper(HttpServletRequest request) {
            super(request);
        }

        @Override
        public String getMethod() {
            return "POST";
        }
    }
}
