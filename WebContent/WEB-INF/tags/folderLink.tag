<%@ attribute name="dirName" required="true" description="The directory used by the folder" %>
<%@ attribute name="displayName" required="true" description="The display name for the folder" %>

<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib prefix="ib" uri="I2pBoteTags" %>

<c:set var="numEmails" value="${ib:getMailFolder(dirName).numElements}"/>
<c:set var="numNew" value="${ib:getMailFolder(dirName).numNewEmails}"/>
<a href="folder.jsp?path=${dirName}" title="${numEmails} emails total, ${numNew} new">
    <img src="images/folder.png"/>${displayName}
</a>
<c:if test="${numNew>0}">(${numNew})</c:if>