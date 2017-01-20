/**
 * Copyright (C) 2017 str4d@mail.i2p
 *
 * Based on a code sample from the OWASP website:
 * https://www.owasp.org/index.php/Content_Security_Policy
 *
 * License: CC BY-SA 3.0
 * https://creativecommons.org/licenses/by-sa/3.0/
 */

package i2p.bote.web;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class CSPApplier implements Filter {
    /** Configuration member to specify if web app use web fonts */
    public static final boolean APP_USE_WEBFONTS = false;

    /** Configuration member to specify if web app use videos or audios */
    public static final boolean APP_USE_AUDIOS_OR_VIDEOS = false;

    /** Configuration member to specify if filter must add CSP directive used by Mozilla (Firefox) */
    public static final boolean INCLUDE_MOZILLA_CSP_DIRECTIVES = true;

    /** Collection of CSP policies that will be applied **/
    private String policies = null;

    /**
     * Used to prepare (one time for all) set of CSP policies that will be applied on each HTTP response.
     *
     * @see javax.servlet.Filter#init(javax.servlet.FilterConfig)
     */
    @Override
    public void init(FilterConfig config) throws ServletException {
        // Define CSP policies
        List<String> cspPolicies = new ArrayList<String>();
        String originLocationRef = "'self'";
        // --Disable default source in order to avoid browser fallback loading using 'default-src' locations
        cspPolicies.add("default-src 'none'");
        // --Define loading policies for Frames
        cspPolicies.add("child-src " + originLocationRef);
        cspPolicies.add("frame-ancestors " + originLocationRef);
        // --Define loading policies for Scripts
        cspPolicies.add("script-src " + originLocationRef + " 'unsafe-inline' 'unsafe-eval'");
        if (INCLUDE_MOZILLA_CSP_DIRECTIVES) {
            cspPolicies.add("options inline-script eval-script");
            cspPolicies.add("xhr-src 'self'");
        }
        // --Define loading policies for Plugins
        cspPolicies.add("object-src " + originLocationRef);
        // --Define loading policies for Styles (CSS)
        cspPolicies.add("style-src " + originLocationRef);
        // --Define loading policies for Images
        cspPolicies.add("img-src " + originLocationRef);
        // --Define loading policies for Form
        cspPolicies.add("form-action " + originLocationRef);
        // --Define loading policies for Audios/Videos
        if (APP_USE_AUDIOS_OR_VIDEOS) {
            cspPolicies.add("media-src " + originLocationRef);
        }
        // --Define loading policies for Fonts
        if (APP_USE_WEBFONTS) {
            cspPolicies.add("font-src " + originLocationRef);
        }
        // --Define loading policies for Connection
        cspPolicies.add("connect-src " + originLocationRef);
        // --Define browser XSS filtering feature running mode
        cspPolicies.add("reflected-xss block");

        // Target formating
        this.policies = cspPolicies.toString().replaceAll("(\\[|\\])", "").replaceAll(",", ";").trim();
    }

    /**
     * Add CSP policies on each HTTP response.
     *
     * @see javax.servlet.Filter#doFilter(javax.servlet.ServletRequest, javax.servlet.ServletResponse, javax.servlet.FilterChain)
     */
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain next)
            throws IOException, ServletException {
        HttpServletRequest httpRequest = ((HttpServletRequest) request);
        HttpServletResponse httpResponse = ((HttpServletResponse) response);

        httpResponse.setHeader("Content-Security-Policy", this.policies);

        next.doFilter(request, response);
    }

    /**
     * {@inheritDoc}
     *
     * @see javax.servlet.Filter#destroy()
     */
    @Override
    public void destroy() {
    }
}
