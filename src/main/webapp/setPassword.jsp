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

<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>

<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="csrf" uri="http://www.owasp.org/index.php/Category:OWASP_CSRFGuard_Project/Owasp.CsrfGuard.tld" %>
<%@ taglib prefix="ib" uri="I2pBoteTags" %>

<c:set var="action" value="${param.action}" scope="request"/>
<c:if test="${not empty action}">
    <ib:csrfCheck>
        <c:set var="action" value="" scope="request"/>
        <ib:message key="Form must be submitted using POST." var="errorMessage" scope="request"/>
    </ib:csrfCheck>
</c:if>

<c:set var="csrf_tokenname"><csrf:tokenname/></c:set>
<c:set var="csrf_tokenvalue"><csrf:tokenvalue uri="setPassword.jsp"/></c:set>
<c:set var="csrfParam" value="${csrf_tokenname}=${csrf_tokenvalue}&amp;"/>

<ib:message key="Set Password" var="title" scope="request"/>
<c:if test="${action eq 'wait'}">
    <c:catch var="exception">
        <ib:waitForPasswordChange/>
    </c:catch>
    <c:if test="${empty exception}">
        <ib:message key="The password has been changed." var="infoMessage" scope="request"/>
        <jsp:forward page="index.jsp"/>
    </c:if>
    <c:if test="${not empty exception}">
        <c:set var="errorMessage" value="${exception.cause.localizedMessage}" scope="request"/>
    </c:if>
</c:if>
<c:if test="${action eq 'set'}">
    <c:set var="refreshUrl" value="setPassword.jsp?${csrfParam}action=wait" scope="request"/>
    <c:set var="refreshInterval" value="0" scope="request"/>
    <ib:setPassword oldPassword="${param.nofilter_oldPassword}" newPassword="${param.nofilter_newPassword}" confirmNewPassword="${param.nofilter_confirm}"/>
</c:if>

<jsp:include page="header.jsp"/>

    <c:if test="${action eq 'set'}">
        <h2><ib:message key="Please wait"/></h2>
        <p>
        <img src="${themeDir}/images/wait.gif"/> <ib:message key="Please wait while the password is being changed..."/>
        </p>
    </c:if>
    <c:if test="${action ne 'set'}">
        <h1><ib:message key="Set a new Password"/></h1>
        
        <p>
        <ib:message key="If you have not set a password, leave the old password blank."/>
        </p><p>
        <ib:message>
            Please note that if a password is set, emails cannot be checked automatically
            but only when the Check Mail button is clicked.
        </ib:message>
        </p><br/>
        
        <csrf:form name="form" action="setPassword.jsp" method="POST">
            <input type="hidden" name="action" value="set"/>
            
            <div class="password-label"><ib:message key="Old password:"/></div>
            <div class="password-field"><input type="password" name="nofilter_oldPassword"/></div>
            
            <div class="password-label"><ib:message key="New password:"/></div>
            <div class="password-field"><input type="password" name="nofilter_newPassword"/></div>
            
            <div class="password-label"><ib:message key="Confirm:"/></div>
            <div class="password-field"><input type="password" name="nofilter_confirm"/></div>
            
            <p/>
            <button type="submit"><ib:message key="OK"/></button>
        </csrf:form>
    
        <script type="text/javascript" language="JavaScript">
            document.forms['form'].elements['nofilter_oldPassword'].focus();
        </script>
    </c:if>

<jsp:include page="footer.jsp"/>
