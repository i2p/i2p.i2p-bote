package i2p.bote.web;

import java.io.IOException;
import java.util.Locale;

import javax.servlet.jsp.JspWriter;
import javax.servlet.jsp.PageContext;
import javax.servlet.jsp.tagext.SimpleTagSupport;

public class LocalizedLanguageNameTag extends SimpleTagSupport {
    private Locale locale;

    public void doTag() throws IOException {
        PageContext pageContext = (PageContext) getJspContext();
        JspWriter out = pageContext.getOut();
        
        out.println(locale.getDisplayLanguage(locale));
    }

    public void setLocale(Locale locale) {
        this.locale = locale;
    }

    public Locale getLocale() {
        return locale;
    }
}