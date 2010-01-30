package i2p.bote.web;

import java.io.IOException;

import javax.servlet.jsp.JspWriter;
import javax.servlet.jsp.PageContext;
import javax.servlet.jsp.tagext.SimpleTagSupport;

import net.i2p.util.Log;

public class LocalDestinationTag extends SimpleTagSupport {
    private Log log = new Log(LocalDestinationTag.class);

    public void doTag() {
        PageContext pageContext = (PageContext) getJspContext();
        JspWriter out = pageContext.getOut();
        
        try {
            out.println(JSPHelper.getLocalDestination());
        } catch (IOException e) {
            log.error("Can't write output to HTML page", e);
        }
    }
}