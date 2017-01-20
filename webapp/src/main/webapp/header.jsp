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

<c:if test="${empty pagetitle and !empty title}"><c:set var="pagetitle" value="${title}"/></c:if>

<html>
<head>
    <meta charset="utf-8">
    
    <%-- Refresh --%>
    <c:if test="${not empty refreshInterval}">
        <meta http-equiv="refresh" content="${refreshInterval};url=${refreshUrl}" />
    </c:if>
    
    <link rel="stylesheet" href="themes/${jspHelperBean.configuration.theme}/i2pbote.css?v=${jspHelperBean.appVersion}" />
    <link rel="icon" type="image/png" href="${themeDir}/images/favicon.png" />
    <c:if test="${!empty title}">
        <title>${fn:escapeXml(title)} <ib:message key="- I2P-Bote"/></title>
    </c:if>
</head>

<body>

<header class="titlebar">
    <c:if test="${jspHelperBean.passwordInCache}">
        <div class="password">
            <ib:message key="Password is cached. Click to clear the password cache." var="linkTitle"/>
            <a href="clearPasswordCache.jsp" title="${linkTitle}"><img src="${themeDir}/images/clear_password.png"/></a>
        </div>
    </c:if>
    <div class="title"><ib:message key="I2P-Bote"/></div>
    <div class="subtitle"><ib:message key="Secure Distributed Email"/></div>
    <c:if test="${!empty pagetitle}"><div class="pagetitle">${fn:escapeXml(pagetitle)}</div></c:if>
</header>

<aside>
<div class="menubox">
    <iframe src="buttonFrame.jsp" class="button-frame"></iframe>
</div>

<div class="menubox-divider"></div>

<div class="menubox">
    <iframe src="folders.jsp?selected=${navSelected}" class="folders-frame"></iframe>
</div>

<div class="menubox-divider"></div>

<div class="menubox">
    <h2><ib:message key="Addresses"/></h2>
    <a class="menuitem${navSelected == 'identities' ? ' selected' : '' } identities" href="identities.jsp">
        <div class="menu-icon"></div>
        <div class="menu-text"><ib:message key="Identities"/></div>
    </a>
    <a class="menuitem${navSelected == 'address-book' ? ' selected' : '' } address-book" href="addressBook.jsp">
        <div class="menu-icon"></div>
        <div class="menu-text"><ib:message key="Address Book"/></div>
    </a>
    <%--
    <div class="menuitem public-address-directory">
        <div class="menu-icon"></div>
        <div class="menu-text">Public Address Directory</div>
    </div>
    --%>
</div>

<div class="menubox-divider"></div>

<div class="menubox">
    <h2><ib:message key="Configuration"/></h2>
    <a class="menuitem${navSelected == 'settings' ? ' selected' : '' } settings" href="settings.jsp">
        <div class="menu-icon"></div>
        <div class="menu-text"><ib:message key="Settings"/></div>
    </a>
</div>

<div class="menubox-divider"></div>

<div class="menubox">
    <h2><ib:message key="Network Status"/></h2>
    <iframe src="statusFrame.jsp?selected=${navSelected}" class="status-frame"></iframe>
</div>

<div class="menubox-divider"></div>

<div class="menubox">
    <h2><ib:message key="Help"/></h2>
    <a class="menuitem${navSelected == 'user-guide' ? ' selected' : '' }" href="userGuide.jsp"><ib:message key="User Guide"/></a>
    <a class="menuitem${navSelected == 'faq' ? ' selected' : '' }" href="faq.jsp"><ib:message key="FAQ"/></a>
    <a class="menuitem${navSelected == 'about' ? ' selected' : '' }" href="about.jsp"><ib:message key="About"/></a>
</div>
</aside>

<c:if test="${not empty contentClass}">
<section class="${contentClass}">
</c:if>
<c:if test="${empty contentClass}">
<section class="main">
</c:if>

<c:if test="${not jspHelperBean.requiredCryptoStrengthSatisfied}">
  <ib:message key="For I2P-Bote to function properly, you must download the JCE Unlimited Strength Jurisdiction Policy files and move them into this folder:" var="infoMessage"/>
  <c:set var="errorMessage" value="${jspHelperBean.JREHome}/lib/security"/>
  <c:set var="snackbarAction" value="userGuide.jsp#Unlimited-Strength-Crypto"/>
  <ib:message key="Guide" var="snackbarActionText"/>
</c:if>

<c:if test="${empty infoMessage}"><c:set var="infoMessage" value="${param.infoMessage}"/></c:if>
<c:if test="${empty errorMessage}"><c:set var="errorMessage" value="${param.errorMessage}"/></c:if>

<c:if test="${not empty infoMessage or not empty errorMessage}">
<div class="snackbar"><div>
<div class="message">
<c:if test="${not empty infoMessage}"><div class="infoMessage">${fn:escapeXml(infoMessage)}</div></c:if>
<c:if test="${not empty errorMessage}"><div class="errorMessage">${fn:escapeXml(errorMessage)}</div></c:if>
</div>

<c:if test="${not empty snackbarAction}">
  <c:if test="${empty snackbarActionTarget}">
  <a href="${snackbarAction}" class="action">${fn:escapeXml(snackbarActionText)}</a>
  </c:if>
  <c:if test="${not empty snackbarActionTarget}">
  <a href="${snackbarAction}" class="action" target="${snackbarActionTarget}">${fn:escapeXml(snackbarActionText)}</a>
  </c:if>
</c:if>
</div></div>
</c:if>
