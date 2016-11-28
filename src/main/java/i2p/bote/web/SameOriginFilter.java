package i2p.bote.web;

import net.i2p.util.Log;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class SameOriginFilter implements Filter {
    private static final Log log = new Log(SameOriginFilter.class);

    @Override
    public void init(FilterConfig config) throws ServletException {
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain next) throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest)request;
        String name = req.getServerName();
        String origin = getHost(req, "Origin");
        String referer = getHost(req, "Referer");
        if ((!"POST".equals(req.getMethod())) ||
                rewriteLocalhost(name).equals(rewriteLocalhost(origin)) ||
                rewriteLocalhost(name).equals(rewriteLocalhost(referer))) {
            next.doFilter(request, response);
        } else {
            log.warn(
                "Potential cross-site attack thwarted (server name: " + name + ", origin: " + origin + ", referer: " + referer + ")"
            );
        }
        ((HttpServletResponse)response).addHeader("X-Frame-Options", "SAMEORIGIN");
    }

    private String getHost(HttpServletRequest request, String header) {
        String val = request.getHeader(header);
        if (val == null) {
            return null;
        }
        try {
            URL url = new URL(val);
            return url.getHost();
        } catch (MalformedURLException e) {
            return null;
        }
    }

    private String rewriteLocalhost(String host) {
        return ("127.0.0.1".equals(host) || "[::1]".equals(host)) ? "localhost" : host;
    }

    @Override
    public void destroy() {
    }
}
