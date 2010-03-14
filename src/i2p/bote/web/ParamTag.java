package i2p.bote.web;

import java.io.IOException;

import javax.servlet.jsp.tagext.JspTag;
import javax.servlet.jsp.tagext.SimpleTagSupport;

import net.i2p.util.Log;

public class ParamTag extends SimpleTagSupport {
    private Log log = new Log(ParamTag.class);
    private String value;

    @Override
    public void doTag() throws IOException {
        JspTag parent = findAncestorWithClass(this, MessageTag.class);
        if (parent instanceof MessageTag) {
            MessageTag messageTag = (MessageTag)parent;
            messageTag.addParameter(value);
        }
        else
            log.error("No MessageTag ancestor found. Ancestor: " + parent);
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}