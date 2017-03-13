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
import i2p.bote.Util;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.PageContext;
import javax.servlet.jsp.tagext.BodyTagSupport;

import net.i2p.util.Log;

public class MessageTag extends BodyTagSupport {
    private static final long serialVersionUID = 2446806168091763863L;
    private static final String REQUEST_SCOPE = "request";
    private static final String PAGE_SCOPE = "page";
    private static final String SESSION_SCOPE = "session";
    private static final String APPLICATION_SCOPE = "application";
    
    private Log log = new Log(MessageTag.class);
    private String key;
    private String bundle;
    private String var;
    private int scope = PageContext.PAGE_SCOPE;
    private boolean hide;
    private List<String> parameters = new ArrayList<String>();   // holds values specified in <ib:param> tags
    private PageContext pageContext;

    @Override
    public void setPageContext(PageContext pageContext) {
        this.pageContext = pageContext;
    }
    
    /**
     * Overridden to remove parameters from the previous MessageTag object so the old
     * parameters aren't used for the new MessageTag (at least Tomcat re-uses MessageTag objects)
     */
    @Override
    public int doStartTag() throws JspException {
        parameters.clear();
        return super.doStartTag();
    }
    
    @Override
    public int doEndTag() throws JspException {
        if (key==null && getBodyContent()!=null)
            key = getBodyContent().getString();
        
        // JspStrings.java does this, so do it here too, in order for the strings to match
        key = Util.removeExtraWhitespace(key);
        
        String translation;
        if (hide && I2PBote.getInstance().getConfiguration().getHideLocale())
            translation = key;
        else
            translation = WebappUtil._t(key);
        
        // replace {0}, {1},... with param values
        do {
            int curlyStart = translation.indexOf('{');
            int curlyEnd = translation.indexOf('}', curlyStart);
            if (curlyStart<0 || curlyEnd<0)
                break;
            String indexStr = translation.substring(curlyStart+1, curlyEnd);
            try {
                int index = Integer.valueOf(indexStr);
                if (parameters.size() <= index)
                    log.error("Parameter #" + index + " doesn't exist for message key <" + key + ">.");
                else
                    translation = translation.substring(0, curlyStart) + parameters.get(index) + translation.substring(curlyEnd + 1);
            }
            catch (NumberFormatException e) {
                log.error("Expected an int, got <" + indexStr + "> for a parameter index; message key: <" + key + ">.");
            }
        } while (true);
        
        // write the translated string to the page or into a variable
        if (var != null)
            pageContext.setAttribute(var, translation, scope);
        else
            try {
                pageContext.getOut().println(translation);
            } catch (IOException e) {
                throw new JspException(e);
            }
        
        // Prevent old keys from overriding the body when the object is re-used (which at least Tomcat does)
        key = null;
        
        return EVAL_PAGE;
    }
    
    public void setKey(String key) {
        this.key = key;
    }

    public String getKey() {
        return key;
    }

    public void setBundle(String bundle) {
        this.bundle = bundle;
    }

    public String getBundle() {
        return bundle;
    }

    public void setVar(String var) {
        this.var = var;
    }

    public String getVar() {
        return var;
    }

    public void setScope(String scope) {
        if (REQUEST_SCOPE.equalsIgnoreCase(scope))
            this.scope = PageContext.REQUEST_SCOPE;
        else if (PAGE_SCOPE.equalsIgnoreCase(scope))
            this.scope = PageContext.PAGE_SCOPE;
        else if (SESSION_SCOPE.equalsIgnoreCase(scope))
            this.scope = PageContext.SESSION_SCOPE;
        else if (APPLICATION_SCOPE.equalsIgnoreCase(scope))
            this.scope = PageContext.APPLICATION_SCOPE;
        else
            this.scope = PageContext.PAGE_SCOPE;
    }

    public String getScope() {
        return String.valueOf(scope);
    }

    /**
     * If <code>hide</code> is <code>true</code>, and the user has chosen to
     * hide the UI language from email recipients, the message is not translated.
     * @param hide
     */
    public void setHide(boolean hide) {
        this.hide = hide;
    }

    public boolean isHide() {
        return hide;
    }

    /**
     * Does nothing. Only there because the attribute is defined in the .tld
     * and the JSP compiler expects a setter and getter.
     */
    public void setNoextract(boolean noextract) {
    }

    /** See {@link #setNoextract}. */
    public boolean isNoextract() {
        return false;
    }

    void addParameter(String param) {
        parameters.add(param);
    }
}
