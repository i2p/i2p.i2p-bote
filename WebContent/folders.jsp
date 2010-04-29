<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">

<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="ib" uri="I2pBoteTags" %>

<html>
<head>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
    <link rel="stylesheet" href="i2pbote.css" />
    <c:if test="${ib:getMailFolder('Outbox').numElements gt 0}">
        <meta http-equiv="refresh" content="20" />
    </c:if>
</head>

<body style="background-color: transparent; margin: 0px;">

<div class="folderbox">
    <h2><ib:message key="Folders"/></h2>
    <ib:message key="Inbox" var="displayName"/><ib:folderLink dirName="Inbox" displayName="${displayName}"/><br/>
    <ib:message key="Outbox" var="displayName"/><ib:folderLink dirName="Outbox" displayName="${displayName}"/><br/>
    <img src="images/folder.png"/><ib:message key="Sent"/><br/>
    <img src="images/folder.png"/><ib:message key="Drafts"/><br/>
    <img src="images/folder.png"/><ib:message key="Trash"/><br/>
    <hr/>
    <img src="images/folder.png"/>User_created_Folder_1<br/>
    <img src="images/folder.png"/>User_created_Folder_2<br/>
    <img src="images/folder.png"/>User_created_Folder_3<br/>
    <hr/>
    <ib:message key="Manage Folders"/>
</div>

</body>
</html>