package i2p.bote.web;

import javax.servlet.jsp.tagext.BodyTagSupport;
import javax.servlet.jsp.tagext.JspTag;

import net.i2p.util.Log;

public class ParamTag extends BodyTagSupport {
    private static final long serialVersionUID = 4675618920560347711L;
    
    private Log log = new Log(ParamTag.class);
    private String value;

    @Override
    public int doEndTag() {
        JspTag parent = findAncestorWithClass(this, MessageTag.class);
        if (parent instanceof MessageTag) {
            MessageTag messageTag = (MessageTag)parent;
            if (value == null)
                messageTag.addParameter(getBodyContent().getString());
            else
                messageTag.addParameter(value);
        }
        else
            log.error("No MessageTag ancestor found. Ancestor: " + parent);
        
        return EVAL_PAGE;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}