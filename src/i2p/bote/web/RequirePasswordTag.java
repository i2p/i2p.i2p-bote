package i2p.bote.web;

import i2p.bote.io.PasswordException;

import javax.servlet.jsp.tagext.BodyTagSupport;
import javax.servlet.jsp.tagext.TryCatchFinally;

public class RequirePasswordTag extends BodyTagSupport implements TryCatchFinally {
    private static final long serialVersionUID = -7546294707936895413L;
    
    private String forwardUrl;

    @Override
    public int doStartTag() {
        return EVAL_BODY_INCLUDE;
    }
    
    @Override
    public void doCatch(Throwable t) throws Throwable {
        if (t instanceof PasswordException || t.getCause() instanceof PasswordException) {
            String url = "password.jsp";
            if (forwardUrl != null)
                url += "?passwordJspForwardUrl=" + forwardUrl;
            pageContext.forward(url);
        }
        else
            throw t;
    }

    @Override
    public void doFinally() {
    }

    public String getForwardUrl() {
        return forwardUrl;
    }

    public void setForwardUrl(String forwardUrl) {
        this.forwardUrl = forwardUrl;
    }
}