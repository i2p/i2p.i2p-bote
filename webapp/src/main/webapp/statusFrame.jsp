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
<%@ taglib prefix="csrf" uri="http://www.owasp.org/index.php/Category:OWASP_CSRFGuard_Project/Owasp.CsrfGuard.tld" %>
<%@ taglib prefix="ib" uri="I2pBoteTags" %>

<jsp:useBean id="jspHelperBean" class="i2p.bote.web.JSPHelper"/>
<c:set var="themeDir" value="themes/${jspHelperBean.configuration.theme}" scope="request"/>
<jsp:include page="getStatus.jsp"/>

<html>
<head>
    <meta charset="utf-8">
    <link rel="stylesheet" href="themes/${jspHelperBean.configuration.theme}/i2pbote.css?v=${jspHelperBean.appVersion}" />
    <c:if test="${connStatus ne CONNECTED and connStatus ne ERROR}">
        <meta http-equiv="refresh" content="20" />
    </c:if>
</head>

<body class="iframe-body">

<c:set var="statusUrl" value="network.jsp"/>
<c:choose>
    <c:when test="${connStatus == NOT_STARTED}">
        <c:set var="statusIcon" value="not_started"/>
        <ib:message key="Not Started" var="statusMessage"/>
    </c:when>
    <c:when test="${connStatus == DELAY}">
        <c:set var="statusIcon" value="delay"/>
        <ib:message key="Waiting 3 Minutes..." var="statusMessage"/>
    </c:when>
    <c:when test="${connStatus == CONNECTING}">
        <c:set var="statusIcon" value="connecting"/>
        <ib:message key="Connecting..." var="statusMessage"/>
    </c:when>
    <c:when test="${connStatus == CONNECTED}">
        <c:set var="statusIcon" value="connected"/>
        <ib:message key="Connected" var="statusMessage"/>
    </c:when>
    <c:when test="${connStatus == ERROR}">
        <c:set var="statusIcon" value="connect_error"/>
        <ib:message key="Error" var="statusMessage"/>
        <c:set var="statusUrl" value="connectError.jsp"/>
    </c:when>
    <c:otherwise>
        <c:set var="statusMessageKey" value="Unknown Status"/>
    </c:otherwise>
</c:choose>

<div class="statusbox">
<a class="menuitem${param.selected == 'network' ? ' selected' : '' }" href="${statusUrl}" target="_parent">
    <div class="menu-icon"><img src="${themeDir}/images/${statusIcon}.png"/></div>
    <div class="menu-text">${statusMessage}</div>
    
    <c:if test="${connStatus == DELAY or connStatus == ERROR}">
        <%-- Show the connect button --%>
        <div class="status-frame-connect">
            <%-- When the connect button is clicked, refresh the entire page so the buttons in buttonFrame.jsp are enabled --%>
            <csrf:form action="connect.jsp" target="_top" method="POST">
                <button type="submit">
                    <c:if test="${connStatus eq ERROR}">
                        <ib:message key="Retry Connecting"/>
                    </c:if>
                    <c:if test="${connStatus ne ERROR}">
                        <ib:message key="Connect Now"/>
                    </c:if>
                </button>
            </csrf:form>
        </div>
    </c:if>
</a>
</div>

</body>
</html>
