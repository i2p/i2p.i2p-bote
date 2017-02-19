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

<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="csrf" uri="http://www.owasp.org/index.php/Category:OWASP_CSRFGuard_Project/Owasp.CsrfGuard.tld" %>
<%@ taglib prefix="ib" uri="I2pBoteTags" %>

<%--
    This JSP creates a new email identity if createNew=true, or saves
    an existing one if createNew=false. If the chosen cryptoImpl is slow
    at generating keys, a "wait" page is displayed.
--%>

<ib:csrfCheck>
    <ib:message key="Form must be submitted using POST." var="errorMessage" scope="request"/>
    <c:if test="${empty param.key}">
        <jsp:forward page="editIdentity.jsp?createNew=true"/>
    </c:if>
    <c:if test="${not empty param.key}">
        <jsp:forward page="editIdentity.jsp"/>
    </c:if>
</ib:csrfCheck>

<c:if test="${param.action == 'cancel'}">
    <jsp:forward page="identities.jsp"/>
</c:if>
<c:if test="${param.action == 'publish'}">
    <jsp:forward page="publishDestination.jsp">
        <jsp:param name="action" value="start"/>
    </jsp:forward>
</c:if>
<c:if test="${param.action == 'delete'}">
    <jsp:forward page="deleteIdentity.jsp"/>
</c:if>

<c:if test="${empty keygenCounter}">
    <c:set var="keygenCounter" value="0"/>
</c:if>

<c:set var="slow" value="false"/>
<%-- If cryptoImpl=4, show the wait page --%>
<c:if test="${param.cryptoImpl eq 4}">
    <c:set var="slow" value="true"/>
</c:if>
<%-- When generating a vanity address, show the wait page --%>
<c:if test="${not empty param.vanityPrefix}">
    <c:set var="slow" value="true"/>
</c:if>

<c:set var="csrf_tokenname"><csrf:tokenname/></c:set>
<c:set var="csrf_tokenvalue"><csrf:tokenvalue uri="submitIdentity.jsp"/></c:set>
<c:set var="csrfParam" value="${csrf_tokenname}=${csrf_tokenvalue}&amp;"/>
<c:choose>
    <c:when test="${empty param.publicName}">
        <ib:message key="Please fill in the Public Name field." var="errorMessage" scope="request"/>
        <c:if test="${empty param.key}">
            <jsp:forward page="editIdentity.jsp?createNew=true"/>
        </c:if>
        <c:if test="${not empty param.key}">
            <jsp:forward page="editIdentity.jsp"/>
        </c:if>
    </c:when>
    
    <%-- Show the wait page when creating an identity will take a while --%>
    <c:when test="${param.createNew eq 'true' and param.action ne 'wait' and empty param.counter and slow}">
        <jsp:forward page="submitIdentity.jsp">
            <jsp:param name="action" value="wait"/>
        </jsp:forward>
    </c:when>
    <%--
        From the wait page, do an HTTP refresh to start the actual identity generation.
        The counter is only used if there is a wait page. It prevents another identity
        from being generated if the user reloads the page in the browser.
    --%>
    <c:when test="${param.action eq 'wait'}">
        <c:set var="counterParam" value="${keygenCounter+1}"/>
        <%-- The double URL encoding prevents GET from breaking special chars --%>
        <c:set var="refreshUrl" value="submitIdentity.jsp?${csrfParam}counter=${counterParam}&amp;createNew=${param.createNew}&amp;cryptoImpl=${param.cryptoImpl}&amp;vanityPrefix=${param.vanityPrefix}&amp;publicName=${ib:urlEncode(ib:urlEncode(param.publicName))}&amp;description=${param.description}&amp;emailAddress=${param.emailAddress}&amp;defaultIdentity=${param.defaultIdentity}" scope="request"/>
        <c:set var="refreshInterval" value="0" scope="request"/>
        <jsp:include page="header.jsp"/>
        <div class="main">
            <h2><ib:message key="Please wait..."/></h2>
            <img src="${themeDir}/images/wait.gif"/>
            <ib:message key="The Email Identity is being generated. This may take a while."/>
        </div>
    </c:when>
    <%-- This is where the actual identity generation takes place --%>
    <c:when test="${param.counter gt keygenCounter or param.createNew ne 'true' or not slow}">
        <c:set var="publicName" value="${param.publicName}"/>
        <c:if test="${not empty param.counter}">
            <c:set var="publicName" value="${ib:urlDecode(publicName)}"/>
        </c:if>
        
        <%-- after password entry, go to the wait page if a new "slow" identity is being generated --%>
        <c:if test="${param.createNew eq 'true' and slow}">
            <c:set var="actionParam" value="action=wait&amp;"/>
        </c:if>
        <c:set var="forwardUrl" value="submitIdentity.jsp?${csrfParam}${actionParam}counter=${param.counter}&amp;createNew=${param.createNew}&amp;cryptoImpl=${param.cryptoImpl}&amp;vanityPrefix=${param.vanityPrefix}&amp;publicName=${param.publicName}&amp;description=${param.description}&amp;emailAddress=${param.emailAddress}&amp;defaultIdentity=${param.defaultIdentity}&amp;includeInGlobalCheck=${param.includeInGlobalCheck}"/>
        <ib:requirePassword forwardUrl="${forwardUrl}">
            <c:catch var="exception">
                <%
                java.util.Properties identityConfig = new java.util.Properties();
                identityConfig.setProperty("includeInGlobalCheck", request.getParameter("includeInGlobalCheck") != null ? "true" : "false");
                pageContext.setAttribute("identityConfig", identityConfig);
                %>
                <c:set var="errorMessage" value="${ib:createOrModifyIdentity(param.createNew, param.cryptoImpl, param.vanityPrefix, param.key, publicName, param.description, null, param.emailAddress, identityConfig, param.defaultIdentity=='on')}"/>
            </c:catch>
            <c:if test="${exception.cause['class'].name eq 'i2p.bote.email.IllegalDestinationParametersException'}">
                <ib:message key="This encryption type does not support destinations that start with {0}. Valid initial characters are {1}." var="errorMessage">
                    <ib:param value="${exception.cause.badChar}"/>
                    <ib:param value="${exception.cause.validChars}"/>
                </ib:message>
                <jsp:forward page="editIdentity.jsp">
                    <jsp:param name="errorMessage" value="${errorMessage}"/>
                </jsp:forward>
            </c:if>

            <c:if test="${not empty param.counter}">
                <c:set var="keygenCounter" value="${param.counter}" scope="session"/>
            </c:if>
            <c:if test="${empty errorMessage}">
                <ib:message key="The email identity has been saved." var="infoMessage"/>
                <jsp:forward page="saveIdentities.jsp">
                    <jsp:param name="infoMessage" value="${infoMessage}"/>
                </jsp:forward>
            </c:if>
            <c:if test="${!empty errorMessage}">
                <jsp:forward page="editIdentity.jsp">
                    <jsp:param name="errorMessage" value="${errorMessage}"/>
                </jsp:forward>
            </c:if>
        </ib:requirePassword>
    </c:when>
    <%-- If the user reloads after an identity has been generated and the wait mechanism was used, just show the identities page --%>
    <c:when test="${empty param.counter or param.counter le keygenCounter}">
        <jsp:forward page="identities.jsp"/>
    </c:when>
</c:choose>
