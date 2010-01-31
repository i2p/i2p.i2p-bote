<%@ attribute name="folderName" required="true" description="This is the folder directory and also the folder's display name" %>

<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib prefix="ib" uri="I2pBoteTags" %>

<c:set var="numEmails" value="${ib:getMailFolder(folderName).numElements}"/>
<c:set var="numNew" value="${ib:getMailFolder(folderName).numNewEmails}"/>
<a href="folder.jsp?path=${folderName}" title="${numEmails} emails total, ${numNew} new">
    <img src="images/folder.png"/>${folderName}
</a>
<c:if test="${numNew>0}">(${numNew})</c:if>