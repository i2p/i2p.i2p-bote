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
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib prefix="ib" uri="I2pBoteTags" %>

<%--
    JSP variables that can be passed into this JSP:
    
        title           - The page title to set
        refreshInterval - If this parameter is set, do an HTTP refresh every refreshInterval seconds
        refreshUrl      - If refreshInterval is set, load this URL when refreshing
        infoMessage     - Display an informational message
        errorMessage    - Display an error message
        
    infoMessage and errorMessage can also be passed in as an HTTP parameter.
--%>

<fmt:requestEncoding value="UTF-8"/>

<jsp:useBean id="jspHelperBean" class="i2p.bote.web.JSPHelper"/>
<c:set var="themeDir" value="themes/${jspHelperBean.configuration.theme}" scope="request"/>
<fmt:setLocale value="${jspHelperBean.language}" scope="request"/>

<html>
<head>
    <meta charset="utf-8">
    
    <%-- Refresh --%>
    <c:if test="${not empty refreshInterval}">
        <meta http-equiv="refresh" content="${refreshInterval};url=${refreshUrl}" />
    </c:if>
    
    <link rel="stylesheet" href="themes/${jspHelperBean.configuration.theme}/i2pbote.css" />
    <link rel="icon" type="image/png" href="${themeDir}/images/favicon.png" />
    <c:if test="${!empty title}">
        <title>${title} <ib:message key="- I2P-Bote"/></title>
    </c:if>
</head>

<body>

<header class="titlebar" onclick="document.location='.'">
    <c:if test="${jspHelperBean.passwordInCache}">
        <div class="password">
            <ib:message key="Password is cached. Click to clear the password cache." var="linkTitle"/>
            <a href="clearPasswordCache.jsp" title="${linkTitle}"><img src="${themeDir}/images/clear_password.png"/></a>
        </div>
    </c:if>
    <div class="title"><ib:message key="I2P-Bote"/></div>
    <div class="subtitle"><ib:message key="Secure Distributed Email"/></div>
    <c:if test="${!empty title}">
    <div class="pagetitle">${title}</div>
    </c:if>
</header>

<aside>
<div class="menubox">
    <iframe src="buttonFrame.jsp" class="button-frame"></iframe>
</div>

<div class="menubox">
    <iframe src="folders.jsp" class="folders-frame"></iframe>
</div>

<div class="menubox">
    <h2><ib:message key="Addresses"/></h2>
    <a class="menuitem" href="identities.jsp"><ib:message key="Identities"/></a>
    <a class="menuitem" href="addressBook.jsp"><ib:message key="Address Book"/></a>
    <%--<div class="menuitem">Public Address Directory</div> --%>
</div>

<div class="menubox">
    <h2><ib:message key="Configuration"/></h2>
    <a class="menuitem" href="settings.jsp"><ib:message key="Settings"/></a>
</div>

<div class="menubox">
    <h2><ib:message key="Network Status"/></h2>
    <iframe src="statusFrame.jsp" class="status-frame"></iframe>
</div>

<div class="menubox">
    <h2><ib:message key="Help"/></h2>
    <a class="menuitem" href="${ib:getLocalizedFilename('User\'s Guide.html', pageContext.servletContext)}"><ib:message key="User Guide"/></a>
    <a class="menuitem" href="${ib:getLocalizedFilename('FAQ.html', pageContext.servletContext)}"><ib:message key="FAQ"/></a>
    <a class="menuitem" href="about.jsp"><ib:message key="About"/></a>
</div>
</aside>

<c:if test="${not empty contentClass}">
<section class="${contentClass}">
</c:if>
<c:if test="${empty contentClass}">
<section class="main">
</c:if>

<div class="infoMessage">
    <c:if test="${not empty infoMessage}">
        ${fn:escapeXml(infoMessage)}
    </c:if>
    <c:if test="${empty infoMessage}">
        ${fn:escapeXml(param.infoMessage)}
    </c:if>
</div>

<div class="errorMessage">
    <c:if test="${not empty errorMessage}">
        ${fn:escapeXml(errorMessage)}
    </c:if>
    <c:if test="${empty errorMessage}">
        ${fn:escapeXml(param.errorMessage)}
    </c:if>
</div>