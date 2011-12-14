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

<jsp:useBean id="jspHelperBean" class="i2p.bote.web.JSPHelper"/>
<c:set var="themeDir" value="themes/${jspHelperBean.configuration.theme}" scope="request"/>
<jsp:include page="getStatus.jsp"/>

<html>
<head>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
    <link rel="stylesheet" href="themes/${jspHelperBean.configuration.theme}/i2pbote.css" />
    <c:if test="${connStatus ne CONNECTED and connStatus ne ERROR}">
        <meta http-equiv="refresh" content="20" />
    </c:if>
</head>

<body class="iframe-body">

<div class="statusbox">
    <c:choose>
        <c:when test="${connStatus == NOT_STARTED}"><img src="${themeDir}/images/connect_error.png"/> <ib:message key="Not Started"/></c:when>
        <c:when test="${connStatus == DELAY}"><img src="${themeDir}/images/connecting.png"/> <ib:message key="Waiting 3 Minutes..."/><br/>
            <div class="status-frame-connect">
                <%-- When the connect button is clicked, refresh the entire page so the buttons in buttonFrame.jsp are enabled --%>
                <form action="connect.jsp" target="_top" method="GET">
                    <button type="submit"><ib:message key="Connect Now"/></button>
                </form>
            </div>
        </c:when>
        <c:when test="${connStatus == CONNECTING}"><img src="${themeDir}/images/connecting.png"/> <ib:message key="Connecting..."/></c:when>
        <c:when test="${connStatus == CONNECTED}"><img src="${themeDir}/images/connected.png"/> <ib:message key="Connected"/></c:when>
        <c:when test="${connStatus == ERROR}"><img src="${themeDir}/images/connect_error.png"/> <ib:message key="Error"/></c:when>
        <c:otherwise> <ib:message key="Unknown Status"/></c:otherwise>
    </c:choose>
</div>

</body>
</html>