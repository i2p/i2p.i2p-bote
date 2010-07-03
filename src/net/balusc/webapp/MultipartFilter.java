/*
 * net/balusc/webapp/MultipartFilter.java
 * 
 * Copyright (C) 2007 BalusC
 * 
 * This program is free software: you can redistribute it and/or modify it under the terms of the
 * GNU Lesser General Public License as published by the Free Software Foundation, either version 3
 * of the License, or (at your option) any later version.
 * 
 * This library is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License along with this library.
 * If not, see <http://www.gnu.org/licenses/>.
 */

package net.balusc.webapp;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.disk.DiskFileItem;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;

/**
 * Check for multipart HttpServletRequests and parse the multipart form data so that all regular
 * form fields are available in the parameterMap of the HttpServletRequest and that all form file
 * fields are available as attribute of the HttpServletRequest. The attribute value of a form file
 * field can be an instance of FileItem or FileUploadException.
 * <p>
 * This filter requires at least the following JAR's (newer versions are allowed) in the classpath,
 * e.g. in /WEB-INF/lib.
 * <ul>
 * <li>commons-fileupload-1.2.jar</li>
 * <li>commons-io-1.3.2.jar</li>
 * </ul>
 * <p>
 * This filter should be definied as follows in the web.xml:
 * <pre>
 * &lt;filter&gt;
 *     &lt;description&gt;
 *         Check for multipart HttpServletRequests and parse the multipart form data so that all
 *         regular form fields are available in the parameterMap of the HttpServletRequest and that
 *         all form file fields are available as attribute of the HttpServletRequest. The attribute
 *         value of a form file field can be an instance of FileItem or FileUploadException.
 *     &lt;/description&gt;
 *     &lt;filter-name&gt;multipartFilter&lt;/filter-name&gt;
 *     &lt;filter-class&gt;net.balusc.webapp.MultipartFilter&lt;/filter-class&gt;
 *     &lt;init-param&gt;
 *         &lt;description&gt;
 *             Sets the maximum file size of the uploaded file in bytes. Set to 0 to indicate an
 *             unlimited file size. The example value of 1048576 indicates a maximum file size of
 *             1MB. This parameter is not required and can be removed safely.
 *         &lt;/description&gt;
 *         &lt;param-name&gt;maxFileSize&lt;/param-name&gt;
 *         &lt;param-value&gt;1048576&lt;/param-value&gt;
 *     &lt;/init-param&gt;
 * &lt;/filter&gt;
 * &lt;filter-mapping&gt;
 *     &lt;filter-name&gt;multipartFilter&lt;/filter-name&gt;
 *     &lt;url-pattern&gt;/*&lt;/url-pattern&gt;
 * &lt;/filter-mapping&gt;
 * </pre>
 * 
 * I2P-Bote customizations:<br/>
 * <ul>
 * <li>Set size threshold to zero so all files are written to disk</li>
 * <li>Don't auto-delete files</li>
 * <li>Delete non-attachment files created by ServletFileUpload.parseRequest().
 * Not creating them in the first place would be more elegant, but much more
 * difficult to do.</li>
 * </ul>
 *
 * @author BalusC
 * @link http://balusc.blogspot.com/2007/11/multipartfilter.html
 */
public class MultipartFilter implements Filter {

    // Init ---------------------------------------------------------------------------------------

    private long maxFileSize;

    // Actions ------------------------------------------------------------------------------------

    /**
     * Configure the 'maxFileSize' parameter.
     * @throws ServletException If 'maxFileSize' parameter value is not numeric.
     * @see javax.servlet.Filter#init(javax.servlet.FilterConfig)
     */
    public void init(FilterConfig filterConfig) throws ServletException {
        // Configure maxFileSize.
        String maxFileSize = filterConfig.getInitParameter("maxFileSize");
        if (maxFileSize != null) {
            if (!maxFileSize.matches("^\\d+$")) {
                throw new ServletException("MultipartFilter 'maxFileSize' is not numeric.");
            }
            this.maxFileSize = Long.parseLong(maxFileSize);
        }
    }

    /**
     * Check the type request and if it is a HttpServletRequest, then parse the request.
     * @throws ServletException If parsing of the given HttpServletRequest fails.
     * @see javax.servlet.Filter#doFilter(
     *      javax.servlet.ServletRequest, javax.servlet.ServletResponse, javax.servlet.FilterChain)
     */
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
        throws ServletException, IOException
    {
        // Check type request.
        if (request instanceof HttpServletRequest) {
            // Cast back to HttpServletRequest.
            HttpServletRequest httpRequest = (HttpServletRequest) request;

            // Parse HttpServletRequest.
            HttpServletRequest parsedRequest = parseRequest(httpRequest);

            // Continue with filter chain.
            chain.doFilter(parsedRequest, response);
        } else {
            // Not a HttpServletRequest.
            chain.doFilter(request, response);
        }
    }

    /**
     * @see javax.servlet.Filter#destroy()
     */
    public void destroy() {
        // I am a boring method.
    }

    // Helpers ------------------------------------------------------------------------------------

    /**
     * Parse the given HttpServletRequest. If the request is a multipart request, then all multipart
     * request items will be processed, else the request will be returned unchanged. During the
     * processing of all multipart request items, the name and value of each regular form field will
     * be added to the parameterMap of the HttpServletRequest. The name and File object of each form
     * file field will be added as attribute of the given HttpServletRequest. If a
     * FileUploadException has occurred when the file size has exceeded the maximum file size, then
     * the FileUploadException will be added as attribute value instead of the FileItem object.
     * @param request The HttpServletRequest to be checked and parsed as multipart request.
     * @return The parsed HttpServletRequest.
     * @throws ServletException If parsing of the given HttpServletRequest fails.
     */
    @SuppressWarnings("unchecked") // ServletFileUpload#parseRequest() does not return generic type.
    private HttpServletRequest parseRequest(HttpServletRequest request) throws ServletException {

        // Check if the request is actually a multipart/form-data request.
        if (!ServletFileUpload.isMultipartContent(request)) {
            // If not, then return the request unchanged.
            return request;
        }

        // Prepare the multipart request items.
        // I'd rather call the "FileItem" class "MultipartItem" instead or so. What a stupid name ;)
        List<FileItem> multipartItems = null;

        try {
            // Parse the multipart request items.
            DiskFileItemFactory fileItemFactory = new DiskFileItemFactory() {
                private int sizeThreshold = 0;
                private File repository = new File(System.getProperty("java.io.tmpdir"));
                
                public FileItem createItem(String fieldName, String contentType, 
                        boolean isFormField, String fileName) {
                    DiskFileItem result = new DiskFileItem(fieldName, contentType,
                            isFormField, fileName, sizeThreshold, repository) {
                        private static final long serialVersionUID = 915956459070041695L;

                        /** Overridden so files are not deleted when the File object is finalized */
                        @Override
                        protected void finalize() {
                        }
                    };
                    return result;
                }
            };
            fileItemFactory.setSizeThreshold(0);
            fileItemFactory.setFileCleaningTracker(null);
            multipartItems = new ServletFileUpload(fileItemFactory).parseRequest(request);
            // Note: we could use ServletFileUpload#setFileSizeMax() here, but that would throw a
            // FileUploadException immediately without processing the other fields. So we're
            // checking the file size only if the items are already parsed. See processFileField().
        } catch (FileUploadException e) {
            throw new ServletException("Cannot parse multipart request: " + e.getMessage());
        }

        // Prepare the request parameter map.
        Map<String, String[]> parameterMap = new HashMap<String, String[]>();

        // Loop through multipart request items.
        for (FileItem multipartItem : multipartItems) {
            if (multipartItem.isFormField()) {
                // Process regular form field (input type="text|radio|checkbox|etc", select, etc).
                processFormField(multipartItem, parameterMap);
                ((DiskFileItem)multipartItem).delete();
            } else {
                // Process form file field (input type="file").
                processFileField(multipartItem, request);
            }
        }

        // Wrap the request with the parameter map which we just created and return it.
        return wrapRequest(request, parameterMap);
    }

    /**
     * Process multipart request item as regular form field. The name and value of each regular
     * form field will be added to the given parameterMap.
     * @param formField The form field to be processed.
     * @param parameterMap The parameterMap to be used for the HttpServletRequest.
     */
    private void processFormField(FileItem formField, Map<String, String[]> parameterMap) {
        String name = formField.getFieldName();
        String value = formField.getString();
        String[] values = parameterMap.get(name);

        if (values == null) {
            // Not in parameter map yet, so add as new value.
            parameterMap.put(name, new String[] { value });
        } else {
            // Multiple field values, so add new value to existing array.
            int length = values.length;
            String[] newValues = new String[length + 1];
            System.arraycopy(values, 0, newValues, 0, length);
            newValues[length] = value;
            parameterMap.put(name, newValues);
        }
    }

    /**
     * Process multipart request item as file field. The name and FileItem object of each file field
     * will be added as attribute of the given HttpServletRequest. If a FileUploadException has
     * occurred when the file size has exceeded the maximum file size, then the FileUploadException
     * will be added as attribute value instead of the FileItem object.
     * @param fileField The file field to be processed.
     * @param request The involved HttpServletRequest.
     */
    private void processFileField(FileItem fileField, HttpServletRequest request) {
        if (fileField.getName().length() <= 0) {
            // No file uploaded.
            request.setAttribute(fileField.getFieldName(), null);
        } else if (maxFileSize > 0 && fileField.getSize() > maxFileSize) {
            // File size exceeds maximum file size.
            request.setAttribute(fileField.getFieldName(), new FileUploadException(
                "File size exceeds maximum file size of " + maxFileSize + " bytes."));
            // Immediately delete temporary file to free up memory and/or disk space.
            fileField.delete();
        } else {
            // File uploaded with good size.
            request.setAttribute(fileField.getFieldName(), fileField);
        }
    }

    // Utility (may be refactored to public utility class) ----------------------------------------

    /**
     * Wrap the given HttpServletRequest with the given parameterMap.
     * @param request The HttpServletRequest of which the given parameterMap have to be wrapped in.
     * @param parameterMap The parameterMap to be wrapped in the given HttpServletRequest.
     * @return The HttpServletRequest with the parameterMap wrapped in.
     */
    private static HttpServletRequest wrapRequest(
        HttpServletRequest request, final Map<String, String[]> parameterMap)
    {
        return new HttpServletRequestWrapper(request) {
            public Map<String, String[]> getParameterMap() {
                return parameterMap;
            }
            public String[] getParameterValues(String name) {
                return parameterMap.get(name);
            }
            public String getParameter(String name) {
                String[] params = getParameterValues(name);
                return params != null && params.length > 0 ? params[0] : null;
            }
            public Enumeration<String> getParameterNames() {
                return Collections.enumeration(parameterMap.keySet());
            }
        };
    }

}
