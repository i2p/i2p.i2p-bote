<%--
 Copyright (C) 2009  HungryHobo@mail.i2p
 
 The GPG fingerprint for HungryHobo@mail.i2p is:
 6DD3 EAA2 9990 29BC 4AD2 7486 1E2C 7B61 76DC DC12
 
 This file is part of I2P-Bote.
 I2P-Bote is free software: you can redistribute it and/or modify
 it under the terms of the GNU General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.
 
 I2P-Bote is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.
 
 You should have received a copy of the GNU General Public License
 along with I2P-Bote.  If not, see <http://www.gnu.org/licenses/>.
 --%>

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

<body class="iframe-body">

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