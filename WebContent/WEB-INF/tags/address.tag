<%@ attribute name="address" required="true" description="The email address to display" %>

<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>

<% jspContext.setAttribute("newline", "\n"); %>

<%-- If the address contains a name that is followed by an email destination in angle brackets, put a line break after the name --%>
<c:set var="gtHtml" value="${fn:escapeXml('<')}"/>
<c:set var="newlinePlusGt" value="${newline}${gtHtml}"/>
<c:set var="formattedAddress" value="${fn:escapeXml(address)}"/>
<c:set var="formattedAddress" value="${fn:replace(formattedAddress, gtHtml, newlinePlusGt)}"/>

<%-- if the address contains an email destination, use a textarea; otherwise just print it --%>
<c:if test="${fn:length(formattedAddress) ge 512}">
    <textarea cols="64" rows="9" readonly="yes" wrap="soft" class="nobordertextarea">${formattedAddress}</textarea>
</c:if>
<c:if test="${fn:length(formattedAddress) lt 512}">
    ${formattedAddress}
</c:if>