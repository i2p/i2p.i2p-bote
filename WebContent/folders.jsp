<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">

<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="ib" uri="I2pBoteTags" %>

<html>
<head>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
    <link rel="stylesheet" href="i2pbote.css" />
    <c:set var="refreshInterval" value="120"/>
    <c:if test="${ib:getMailFolder('Outbox').numElements gt 0}">
        <c:set var="refreshInterval" value="20"/>
    </c:if>
    <meta http-equiv="refresh" content="${refreshInterval}" />
</head>

<body style="background-color: transparent; margin: 0px;">

<div class="folderbox">
    <h2><ib:message key="Folders"/></h2>
    <ib:message key="Inbox" var="displayName"/><ib:folderLink dirName="Inbox" displayName="${displayName}"/><br/>
    <ib:message key="Outbox" var="displayName"/><ib:folderLink dirName="Outbox" displayName="${displayName}"/><br/>
    <ib:message key="Sent Emails" var="displayName"/><ib:folderLink dirName="Sent" displayName="${displayName}"/><br/>
    <img src="images/folder.png"/><ib:message key="Drafts"/><br/>
    <ib:message key="Trash" var="displayName"/><ib:folderLink dirName="Trash" displayName="${displayName}"/><br/>
    <hr/>
    <img src="images/folder.png"/>User_created_Folder_1<br/>
    <img src="images/folder.png"/>User_created_Folder_2<br/>
    <img src="images/folder.png"/>User_created_Folder_3<br/>
    <hr/>
    <ib:message key="Manage Folders"/>
</div>

</body>
</html>