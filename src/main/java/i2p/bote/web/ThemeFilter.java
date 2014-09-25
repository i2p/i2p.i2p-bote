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

import i2p.bote.Configuration.Theme;
import i2p.bote.I2PBote;

import java.io.IOException;
import java.util.List;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.i2p.util.Log;

/**
 * Redirects requests for files in an external UI theme to {@link ThemeServlet}.
 * Requests for built-in themes pass through unaltered.
 */
public class ThemeFilter implements Filter {
    private String themeServletPath;
    private Log log = new Log(ThemeFilter.class);
    
    @Override
    public void init(FilterConfig config) {
        themeServletPath = config.getInitParameter("externalThemes");
        if (themeServletPath == null)
            themeServletPath = "externalThemes";
    }
    
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        if (request instanceof HttpServletRequest && response instanceof HttpServletResponse) {
            HttpServletRequest httpRequest = (HttpServletRequest)request;
            HttpServletResponse httpResponse = (HttpServletResponse)response;
            
            String path = httpRequest.getRequestURI();
            
            // strip off the "/i2pbote/themes/" part
            if (path.startsWith("/"))
                path = path.substring(1);
            path = path.substring(path.indexOf('/') + 1);
            int index = path.indexOf('/');
            if (index < 1)
                throw new ServletException("No themes directory specified! Resource path: <" + path + ">");
            String themesDir = path.substring(0, index);
            path = path.substring(index + 1);
            log.debug("Theme resource requested: <" + path + ">");
            
            if (path.indexOf('/') < 1)
                throw new ServletException("No theme specified! Resource path: <" + path + ">");
            String theme = path.substring(0, path.indexOf('/'));
            List<Theme> builtInThemes = I2PBote.getInstance().getConfiguration().getBuiltInThemes();
            // if theme is external, redirect to ThemeServlet
            if (!containsThemeId(builtInThemes, theme)) {
                String newUrl = httpRequest.getRequestURI().replace(themesDir, themeServletPath);
                httpResponse.sendRedirect(newUrl);
            }
            else
                chain.doFilter(request, response);
        }
        else
            chain.doFilter(request, response);
    }

    /** Returns <code>true</code> if a list of Themes contains a theme with given ID */
    private boolean containsThemeId(List<Theme> themes, String themeId) {
        for (Theme theme: themes)
            if (theme.getId().equals(themeId))
                return true;
        return false;
    }
    
    @Override
    public void destroy() { }
}