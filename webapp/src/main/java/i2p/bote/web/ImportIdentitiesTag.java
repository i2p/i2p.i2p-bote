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
import java.io.FileInputStream;
import java.io.IOException;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.tagext.SimpleTagSupport;

/** Imports identities from a file. */
public class ImportIdentitiesTag extends SimpleTagSupport {
    private String identitiesFilename;
    private String password;
    private boolean overwrite;
    private boolean replace;
    
    @Override
    public void doTag() throws JspException {
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(identitiesFilename);
            I2PBote.getInstance().getIdentities().importFromFileDescriptor(
                    fis.getFD(), "".equals(password) ? null : password, !overwrite, replace);
        } catch (Exception e) {
            throw new JspException(e);
        } finally {
            if (fis != null)
                try {
                    fis.close();
                } catch (IOException e) {
                    throw new JspException(e);
                }
        }
    }

    public String getIdentitiesFilename() {
        return identitiesFilename;
    }

    public void setIdentitiesFilename(String identitiesFilename) {
        this.identitiesFilename = identitiesFilename;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public boolean getOverwrite() {
        return overwrite;
    }

    public void setOverwrite(boolean overwrite) {
        this.overwrite = overwrite;
    }

    public boolean getReplace() {
        return replace;
    }

    public void setReplace(boolean replace) {
        this.replace = replace;
    }
}