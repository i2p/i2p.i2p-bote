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

<jsp:include page="getStatus.jsp"/>

<c:if test="${param.checkMail eq 1}">
    <ib:requirePassword forwardUrl="checkMail.jsp">
        <ib:checkForMail/>
    </ib:requirePassword>
</c:if>

<jsp:useBean id="jspHelperBean" class="i2p.bote.web.JSPHelper"/>
<c:set var="themeDir" value="themes/${jspHelperBean.configuration.theme}" scope="request"/>
<c:if test="${jspHelperBean.checkingForMail}">
    <c:set var="checkingForMail" value="true"/>
</c:if>

<html>
<head>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
    <link rel="stylesheet" href="themes/${jspHelperBean.configuration.theme}/i2pbote.css" />
    
    <%-- Refresh until the Check Mail button becomes ungreyed --%>
    <c:if test="${checkingForMail or connStatus eq NOT_STARTED or connStatus eq DELAY}">
        <meta http-equiv="refresh" content="20;url=buttonFrame.jsp" />
    </c:if>
</head>

<body class="iframe-body">

<c:if test="${checkingForMail}">
    <div class="checkmail">
        <img src="${themeDir}/images/wait.gif"/><ib:message key="Checking for mail..."/>
    </div>
</c:if>
<c:if test="${!checkingForMail}">
    <div class="checkmail">
        <c:set var="frame" value=""/>
        <c:choose>
            <c:when test="${jspHelperBean.identities.none}">
                <c:set var="url" value="noIdentities.jsp"/>
                <c:set var="frame" value='target="_parent"'/>
            </c:when>
            <c:otherwise>
                <%--
                    If the user needs to enter a password to check mails, take them
                    to checkMail.jsp and use the entire browser window
                --%>
                <c:if test="${jspHelperBean.passwordRequired}">
                    <c:set var="frame" value='target="_parent"'/>
                    <c:set var="url" value="checkMail.jsp"/>
                </c:if>
                <c:if test="${not jspHelperBean.passwordRequired}">
                    <c:set var="url" value="buttonFrame.jsp"/>
                </c:if>
            </c:otherwise>
        </c:choose>
        
        <form action="${url}" ${frame} method="GET">
            <input type="hidden" name="checkMail" value="1"/>
            <c:set var="disable" value=""/>
            <c:if test="${connStatus == DELAY}">
                <c:set var="disable" value="disabled=&quot;disabled&quot;"/>
            </c:if>
            <button type="submit" value="Check Mail" ${disable}><ib:message key="Check Mail"/></button>
        </form>
    </div>
    <c:if test="${jspHelperBean.newMailReceived}">
        <script language="Javascript">
            // refresh folder list to update the new message count
            parent.frames[1].location.href = 'folders.jsp';
            // If inbox is being displayed, reload so the new email(s) show
            if (parent.document.getElementById('inboxFlag'))
                parent.location.href = 'folder.jsp?path=Inbox';
        </script>
    </c:if>
</c:if>

<div class="compose">
    <form action="newEmail.jsp" target="_top" method="GET">
        <button type="submit" value="New"><ib:message key="Compose"/></button>
    </form>
</div>

<div class="lastcheck">
    <ib:message key="Last checked:"/>
    <c:set var="lastCheck" value="${jspHelperBean.lastMailCheckTime}" scope="request"/>
    <ib:printDate date="${lastCheck}" type="time" timeStyle="short" printUnknown="true"/>
</div>

</body>
</html>
