<%@ attribute name="email" type="i2p.bote.email.Email" required="true" description="The directory used by the folder" %>
<%@ attribute name="timeStyle" required="false" description="See the timeStyle parameter of fmt:formatDate" %>

<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib prefix="ib" uri="I2pBoteTags" %>

<c:set var="date" value="${email.sentDate}"/>
<c:if test="${!empty date}">
    <fmt:formatDate value="${email.sentDate}" var="date" type="both" timeStyle="${timeStyle}"/>
</c:if>
<c:if test="${empty date}">
    <ib:message key="Unknown" var="date"/>
</c:if>
${fn:escapeXml(date)}