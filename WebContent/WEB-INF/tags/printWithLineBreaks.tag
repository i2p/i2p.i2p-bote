<%@ attribute name="text" required="true" description="The text to display" %>
<%@ attribute name="width" required="false" description="The maximum number of characters in each line" %>

<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>

<c:if test="${empty width}">
    <c:set var="width" value="64"/>
</c:if>

<c:set var="length" value="${fn:length(text)}"/>
<c:forEach var="i" begin="0" end="${length}" step="${width}">
    <c:if test="${i > 0}">
        <br/>
    </c:if>
    ${fn:substring(text, i, i+64)}
</c:forEach>