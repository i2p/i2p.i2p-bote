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

import i2p.bote.email.Email;
import i2p.bote.fileencryption.PasswordException;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

import javax.mail.MessagingException;
import javax.mail.Part;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.i2p.util.Log;

/**
 * Displays an email attachment given the following HTTP parameters:<br/>
 * <ul>
 * <li><code>messageID</code>: The message ID of the email</li>
 * <li><code>folder</code>: The name of the folder that contains the email</li>
 * <li><code>part</code>: An index into a <code>List</code> of <code>Part</code>s
 *   as returned by {@link Email#getParts()}</li>
 * </ul>
 */
public class ShowAttachment extends HttpServlet {
    private static final long serialVersionUID = -8141961290006672451L;
    private Log log = new Log(ShowAttachment.class);
       
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String folderName = request.getParameter("folder");
        String messageID = request.getParameter("messageID");
        Email email;
        try {
            email = JSPHelper.getEmail(folderName, messageID);
        } catch (PasswordException e) {
            throw new ServletException(e);
        }
        if (email == null)
            throw new ServletException("Message ID <" + messageID + "> not found in folder <" + folderName + ">.");
        
        List<Part> parts = null;
        try {
            parts = email.getParts();
        }
        catch (MessagingException e) {
            throw new ServletException("Can't parse mail. Message ID=" + messageID, e);
        }
        
        String partIndexStr = request.getParameter("part");
        int partIndex = -1;
        try {
            partIndex = Integer.valueOf(partIndexStr);
        }
        catch (NumberFormatException e) {
        }
        if (partIndex<0 || partIndex>=parts.size())
            throw new ServletException("Invalid part index: <" + partIndexStr + ">, must be a number between 0 and " + (parts.size()-1));
        
        Part part = parts.get(partIndex);
        try {
            response.setContentType(part.getContentType());
            String[] dispositionHeaders = part.getHeader("Content-Disposition");
            if (dispositionHeaders==null || dispositionHeaders.length==0)
                response.setHeader("Content-Disposition", "attachment; filename=attachment");
            else
                response.setHeader("Content-Disposition", dispositionHeaders[0]);
        }
        catch (MessagingException e) {
            log.error("Can't get MIME type of part " + partIndex + ". Message ID=" + messageID, e);
        }
        
        // write the attachments' content to the servlet response stream
        try {
            InputStream input = part.getInputStream();
            OutputStream output = response.getOutputStream();
            
            byte[] buffer = new byte[1024];
            while (true) {
                int bytesRead = input.read(buffer);
                if (bytesRead <= 0)
                    return;
                output.write(buffer, 0, bytesRead);
            }
        }
        catch (MessagingException e) {
            throw new ServletException("Can't get MIME type of part " + partIndex + ". Message ID=" + messageID, e);
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        doGet(request, response);
    }
}