/**
 * Copyright (C) 2015  str4d@mail.i2p
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
import i2p.bote.fileencryption.PasswordException;

import java.io.IOException;
import java.io.OutputStream;
import java.security.GeneralSecurityException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Displays identities to a file given the following HTTP parameters:<br/>
 * <ul>
 * <li><code>nofilter_password</code>: The password to encrypt with</li>
 * <li><code>nofilter_confirm</code>: A confirmation of the password</li>
 * </ul>
 * The parameters may be blank, in which case the identities are not encrypted.
 */
public class ExportIdentities extends HttpServlet {
    private static final long serialVersionUID = 3176228001938336428L;

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String password = request.getParameter("nofilter_password");
        String confirm = request.getParameter("nofilter_confirm");
        if (password != null && !password.equals(confirm))
            throw new ServletException("Passwords do not match.");
        if ("".equals(password))
            password = null;

        if (password == null) {
            response.setContentType("text/plain");
            response.setHeader("Content-Disposition", "attachment; filename=identities.txt");
        } else {
            response.setContentType("application/octet-stream");
            response.setHeader("Content-Disposition", "attachment; filename=identities.bote");
        }

        // write the identities to the servlet response stream
        OutputStream output = response.getOutputStream();
        try {
            I2PBote.getInstance().getIdentities().export(output, password);
        } catch (GeneralSecurityException e) {
            throw new ServletException("Failed to export identities", e);
        } catch (PasswordException e) {
            throw new ServletException("You are not logged in to I2P-Bote");
        }
    }
}