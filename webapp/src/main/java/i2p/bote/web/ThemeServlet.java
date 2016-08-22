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

import i2p.bote.Configuration;
import i2p.bote.I2PBote;
import i2p.bote.Util;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.i2p.util.Log;

/**
 * Loads resources for UI themes located outside the .war (i.e. user-provided themes).
 * @see Configuration#getExternalThemeDir()
 */
public class ThemeServlet extends HttpServlet {
    private static final long serialVersionUID = -6485689993514480128L;
    private Log log = new Log(ThemeServlet.class);
    
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String path = request.getRequestURI();
        
        // strip off the "/i2pbote/themes" part
        if (path.startsWith("/"))
            path = path.substring(1);
        path = path.substring(path.indexOf('/') + 1);
        path = path.substring(path.indexOf('/') + 1);
        log.debug("External theme resource requested: <" + path + ">");
        
        if (path.indexOf('/') < 1)
            throw new ServletException("No theme specified! Resource path: <" + path + ">");
        File themeDir = I2PBote.getInstance().getConfiguration().getExternalThemeDir();
        File resource = new File(themeDir, path);
        if (!resource.exists())
            return;
        FileInputStream inputStream = new FileInputStream(resource);
        try {
            Util.copy(inputStream, response.getOutputStream());
        }
        finally {
            inputStream.close();
        }
    }
}