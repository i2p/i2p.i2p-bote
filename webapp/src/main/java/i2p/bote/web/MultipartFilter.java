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

import i2p.bote.Util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.mail.BodyPart;
import javax.mail.MessagingException;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMultipart;
import javax.mail.internet.MimePartDataSource;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;

/**
 * Saves all files in a <code>multipart/form-data</code> request to the
 * temp directory.<br/>
 * Sets a request attribute of type {@link UploadedFile} containing the
 * original filename and the path to the temporary file (the uploaded file).
 * The name of the request attribute is the parameter name of the uploaded
 * file (e.g. <code>"newAttachment"</code> for <code>newEmail.jsp</code>).
 * <p/>
 * Non-multipart requests are just passed through the filter.
 */
public class MultipartFilter implements Filter {
    
    @Override
    public void init(FilterConfig filterConfig) { }
    
    /**
     * Entry point into the filter
     * @throws IOException
     * @throws ServletException
     */
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        if (request instanceof HttpServletRequest) {
            HttpServletRequest httpRequest = (HttpServletRequest)request;
            request = processRequest(httpRequest);
        }
        chain.doFilter(request, response);
    }

    @Override
    public void destroy() { }

    /**
     * Saves all uploaded files in a request to the temporary directory so
     * they can be attached to an email.<br/>
     * All other request parameters are passed down the filter chain as a
     * new request.
     * @param request
     * @return a new <code>HttpServletRequest</code>
     * @throws IOException
     * @throws ServletException
     */
    private HttpServletRequest processRequest(final HttpServletRequest request) throws IOException, ServletException {
        // only process multipart requests
        if (!isMultipart(request))
            return request;

        DataSource dataSource = new DataSource() {
            @Override
            public OutputStream getOutputStream() throws IOException {
                return null;
            }
            
            @Override
            public String getName() {
                return "HTTP request data source";
            }
            
            @Override
            public InputStream getInputStream() throws IOException {
                return request.getInputStream();
            }
            
            @Override
            public String getContentType() {
                return request.getContentType();
            }
        };
        
        Map<String, String[]> nonFileParameters = new HashMap<String, String[]>();
        
        try {
            // <nasty hack>
            // JavaMail assumes ISO-8859-1 for text parts when parsing multipart data.
            // There seems to be no easy way to override this behavior, or to change the
            // content type of a BodyPart. Since the ISO-8859-1 encoding will mess up
            // non-ascii UTF-8 characters, we change all "text/plain" parts to
            // "text/plain; charset=UTF-8" to keep JavaMail from using the ISO-8859-1
            // default.
            // See also com.sun.mail.handlers.text_plain.getCharset(String)
            MimeMultipart multipart = new MimeMultipart(dataSource) {
                @Override
                public BodyPart getBodyPart(int index) throws MessagingException {
                    BodyPart part = super.getBodyPart(index);
                    part.setDataHandler(new DataHandler(new MimePartDataSource((MimeBodyPart)part) {
                        @Override
                        public String getContentType() {
                            String contentType = super.getContentType();
                            if ("text/plain".equals(contentType))
                                return "text/plain; charset=UTF-8";
                            else
                                return contentType;
                        }
                    }));
                    return part;
                }
            };
            // </nasty hack>
            
            for (int i=0; i<multipart.getCount(); i++) {
                BodyPart bodyPart = multipart.getBodyPart(i);
                String origFilename = bodyPart.getFileName();
                if (origFilename!=null && origFilename.length()>0) {
                    // write the temporary file
                    InputStream input = bodyPart.getInputStream();
                    File tempFile = File.createTempFile("i2pbote_attachment_", ".tmp");
                    tempFile.deleteOnExit();   // under normal circumstances, the temp files are deleted after the email is added to the outbox
                    FileOutputStream output = new FileOutputStream(tempFile);
                    try {
                        Util.copy(input, output);
                    }
                    finally {
                        output.close();
                    }
                    
                    UploadedFile uploadedFile = new UploadedFile(origFilename, tempFile.getAbsolutePath());
                    
                    String paramName = getParamName(bodyPart);
                    request.setAttribute(paramName, uploadedFile);
                }
                else {
                    // not a file, add the parameter to the map
                    String paramName = getParamName(bodyPart);
                    Object content = bodyPart.getContent();
                    String paramValue = content==null?"":content.toString();
                    add(nonFileParameters, paramName, paramValue);
                }
            }
        } catch (MessagingException e) {
            throw new ServletException("Can't write uploaded data to a temp file.", e);
        }
    
        // Wrap the request with the parameter map which we just created and return it.
        return wrapRequest(request, nonFileParameters);
    }
    
    private boolean isMultipart(HttpServletRequest request) {
        String contentType = request.getContentType();
        return contentType!=null && contentType.toLowerCase().startsWith("multipart/");
    }
    
    /**
     * Returns the name of the HTTP request parameter from which a given <code>BodyPart</code>
     * was constructed. This method relies on the <code>name=...</code> field being
     * present in the <code>Content-Disposition</code> header.<br/>
     * That field is optional according to RFC2388, so it is generally not safe to
     * assume it is there. But JavaMail includes the name field, so it shouldn't be an
     * issue.
     * @param bodyPart
     * @throws MessagingException
     */
    private String getParamName(BodyPart bodyPart) throws MessagingException {
        String[] headerValues = bodyPart.getHeader("Content-Disposition");
        if (headerValues==null || headerValues.length==0)
            return null;
        
        String disposition = headerValues[0];
        String[] split1 = disposition.split("name=\"", 2);
        if (split1.length < 2)
            return null;
        
        String[] split2 = split1[1].split("\"", 2);
        if (split2.length < 2)
            return null;
        
        return split2[0];
    }
    
    /**
     * Adds a name/value pair to the <code>parameters</code> map. If a map entry
     * exists for the name, the new mapping is added to the end of the array.
     * Otherwise, the name is mapped to a new string array of length <code>1</code>
     * containing the <code>value</code>.
     * @param parameters
     * @param name
     * @param value
     */
    private void add(Map<String, String[]> parameters, String name, String value) {
        if (parameters.containsKey(name)) {
            String[] values = parameters.get(name);
            String[] newValues = Arrays.copyOf(values, values.length+1);
            parameters.put(name, newValues);
        }
        else
            parameters.put(name, new String[] {value});
    }

    /**
     * Wrap the given HttpServletRequest with the given parameterMap.
     * @param request The HttpServletRequest of which the given parameterMap have to be wrapped in.
     * @param parameterMap The parameterMap to be wrapped in the given HttpServletRequest.
     * @return The HttpServletRequest with the parameterMap wrapped in.
     */
    private static HttpServletRequest wrapRequest(HttpServletRequest request, final Map<String, String[]> parameterMap) {
        return new HttpServletRequestWrapper(request) {
            // merge with the super parameters so parameters added in <jsp:forward> don't get lost
            @SuppressWarnings("unchecked")
            public Map<String, String[]> getParameterMap() {
                return merge(parameterMap, super.getParameterMap());
            }
            
            public String[] getParameterValues(String name) {
                return getParameterMap().get(name);
            }
            
            public String getParameter(String name) {
                String[] params = getParameterValues(name);
                return params != null && params.length > 0 ? params[0] : null;
            }
            
            public Enumeration<String> getParameterNames() {
                return Collections.enumeration(getParameterMap().keySet());
            }
            
            private Map<String, String[]> merge(Map<String, String[]> map1, Map<String, String[]> map2) {
                Map<String, Set<String>> mergedMap = new HashMap<String, Set<String>>();
                
                for (String key: map1.keySet())
                    addAll(mergedMap, key, map1.get(key));
                for (String key: map2.keySet())
                    addAll(mergedMap, key, map2.get(key));
                
                // convert Set<String> to String[]
                Map<String, String[]> arrayMap = new HashMap<String, String[]>();
                for (String key: mergedMap.keySet()) {
                    String[] value = mergedMap.get(key).toArray(new String[0]);
                    arrayMap.put(key, value);
                }
                
                return arrayMap;
            }
            
            private void addAll(Map<String, Set<String>> map, String key, String[] valuesToAdd) {
                Set<String> values = map.get(key);
                if (values == null)
                    values = new HashSet<String>();
                values.addAll(Arrays.asList(valuesToAdd));
                map.put(key, values);
            }
            
            // Do not pass the call through to the underlying request because it
            // causes an error in Jetty:
            // "java.lang.IllegalStateException: getReader() or getInputStream() called",
            // which indicates that setCharacterEncoding() was called after getReader()
            // or getInputStream().
            // Putting a <c:catch> around the <fmt:requestEncoding> in header.jsp makes
            // the problem go away, but fixing it here avoids the exception being thrown
            // in the first place.
            // I haven't figured out what the root problem is, but I think it might have
            // something to do with the <jsp:forward> from newEmail.jsp to sendEmail.jsp,
            // and <fmt:requestEncoding> being called on the original HTTP request after
            // the MultipartFilter has read the input stream already.
            @Override
            public void setCharacterEncoding(String enc) {
            }
            
            @Override
            public String getCharacterEncoding() {
                return "UTF-8";
            }
        };
    }
    
    public static class UploadedFile {
        private String originalFilename;
        private String tempFilename;
        
        UploadedFile(String originalFilename, String tempFilename) {
            this.setOriginalFilename(originalFilename);
            this.setTempFilename(tempFilename);
        }

        public void setOriginalFilename(String originalFilename) {
            this.originalFilename = originalFilename;
        }

        public String getOriginalFilename() {
            return originalFilename;
        }

        public void setTempFilename(String tempFilename) {
            this.tempFilename = tempFilename;
        }

        public String getTempFilename() {
            return tempFilename;
        }
    }
}