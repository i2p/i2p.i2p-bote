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
<!DOCTYPE html>

<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="ib" uri="I2pBoteTags" %>

<jsp:useBean id="jspHelperBean" class="i2p.bote.web.JSPHelper"/>
<c:set var="themeDir" value="themes/${jspHelperBean.configuration.theme}" scope="request"/>
<c:set var="selected" value="${param.selected}" scope="request"/>

<html>
<head>
    <meta charset="utf-8">
    <link rel="stylesheet" href="${themeDir}/i2pbote.css?v=${jspHelperBean.appVersion}" />
    <c:set var="refreshInterval" value="120"/>
    <c:if test="${ib:getMailFolder('Outbox').numElements gt 0}">
        <c:set var="refreshInterval" value="20"/>
    </c:if>
    <meta http-equiv="refresh" content="${refreshInterval}" />
</head>

<body class="iframe-body">

<div class="folderbox">
    <h2><ib:message key="Folders"/></h2>
    <ib:message key="Inbox" var="displayName"/><ib:folderLink dirName="Inbox" displayName="${displayName}"/>
    <ib:message key="Outbox" var="displayName"/><ib:folderLink dirName="Outbox" displayName="${displayName}"/>
    <ib:message key="Sent" var="displayName"/><ib:folderLink dirName="Sent" displayName="${displayName}"/>
    <ib:message key="Trash" var="displayName"/><ib:folderLink dirName="Trash" displayName="${displayName}"/>
</div>

</body>
</html>
