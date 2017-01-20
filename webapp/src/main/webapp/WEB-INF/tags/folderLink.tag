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

<%@ attribute name="dirName" required="true" description="The directory used by the folder" %>
<%@ attribute name="displayName" required="true" description="The display name for the folder; must match the translation for dirName or the page title won't be translated." %>

<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib prefix="ib" uri="I2pBoteTags" %>

<jsp:useBean id="jspHelperBean" class="i2p.bote.web.JSPHelper"/>

<c:set var="numEmails" value="${ib:getMailFolder(dirName).numElements}"/>
<c:if test="${jspHelperBean.passwordRequired}">
    <ib:message key="{0} emails total" var="linkTitle">
        <ib:param value="${numEmails}"/>
    </ib:message>
</c:if>
<c:if test="${not jspHelperBean.passwordRequired}">
    <c:set var="numNew" value="${ib:getMailFolder(dirName).numNewEmails}"/>
    <ib:message key="{0} emails total, {1} new" var="linkTitle">
        <ib:param value="${numEmails}"/>
        <ib:param value="${numNew}"/>
    </ib:message>
</c:if>

<a class="menuitem${selected == dirName ? ' selected' : '' } folder ${dirName}" href="folder.jsp?path=${dirName}" target="_parent" title="${linkTitle}">
    <div class="menu-icon"><img src="themes/${jspHelperBean.configuration.theme}/images/folder.png"/></div>
    <div class="menu-text">${displayName}</div>

<c:if test="${numNew>0}"><div class="folder-new">(${numNew})</div></c:if>

<c:if test="${dirName == 'Inbox'}">
    <c:set var="numIncomplete" value="${jspHelperBean.numIncompleteEmails}"/>
    <c:if test="${numIncomplete>0}">
        <ib:message key="{0} incomplete" var="numIncompleteMsg">
            <ib:param value="${numIncomplete}"/>
        </ib:message>
        <br /><div class="folder-incomplete">(${numIncompleteMsg})</div>
    </c:if>
</c:if>
</a>