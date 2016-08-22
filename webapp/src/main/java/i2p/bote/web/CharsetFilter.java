/**
 * Presumably under the Apache License, v2.0 which is compatible with GPL v3.
 */

package i2p.bote.web;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

/**
 * Taken from <code>http://wiki.apache.org/tomcat/Tomcat/UTF-8.</code></br>
 * The link is dead now, but similar code ships with Tomcat and can be found at
 * <code>$TOMCAT/webapps/examples/WEB-INF/classes/filters/SetCharacterEncodingFilter.java</code>.
 */
public class CharsetFilter implements Filter {
    private String encoding;

    @Override
    public void init(FilterConfig config) throws ServletException {
        encoding = config.getInitParameter("requestEncoding");
        if (encoding == null)
            encoding = "UTF-8";
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain next) throws IOException, ServletException {
        // Respect the client-specified character encoding
        // (see HTTP specification section 3.4.1)
        if (request.getCharacterEncoding() == null)
            request.setCharacterEncoding(encoding);

        next.doFilter(request, response);
    }

    @Override
    public void destroy() {
    }
}