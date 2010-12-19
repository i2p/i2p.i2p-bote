package i2p.bote.web;

import i2p.bote.io.PasswordException;

import javax.servlet.jsp.el.ELException;
import javax.servlet.jsp.tagext.BodyTagSupport;
import javax.servlet.jsp.tagext.TryCatchFinally;

@SuppressWarnings("deprecation")   // for javax.servlet.jsp.el.ELException, see below
public class RequirePasswordTag extends BodyTagSupport implements TryCatchFinally {
    private static final long serialVersionUID = -7546294707936895413L;
    
    private String forwardUrl;

    @Override
    public int doStartTag() {
        return EVAL_BODY_INCLUDE;
    }
    
    @Override
    public void doCatch(Throwable t) throws Throwable {
        boolean isPasswordException = t instanceof PasswordException || t.getCause() instanceof PasswordException;
        // Special handling of javax.servlet.jsp.el.ELException thrown by Jetty (version 5.1.15, at least):
        // This exception has a a separate method named getRootCause() which returns the PasswordException
        // while the regular getCause() method returns null.
        isPasswordException |= t instanceof ELException && ((ELException)t).getRootCause() instanceof PasswordException;
        
        if (isPasswordException) {
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