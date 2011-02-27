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

<%--
    This JSP creates a new email identity if new=true, or saves
    an existing one if new=false. If the chosen cryptoImpl is slow
    at generating keys, a "wait" page is displayed.
--%>

<c:if test="${param.action == 'cancel'}">
    <jsp:forward page="identities.jsp"/>
</c:if>

<c:if test="${empty keygenCounter}">
    <c:set var="keygenCounter" value="0"/>
</c:if>
<c:choose>
    <c:when test="${empty param.publicName}">
        <ib:message key="Please fill in the Public Name field." var="errorMessage"/>
    </c:when>
    <%-- If cryptoImpl=4 and a new identity is to be generated, show the wait page --%>
    <c:when test="${param.new eq 'true' and param.cryptoImpl eq 4 and param.action ne 'wait' and empty param.counter}">
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
        <c:set var="refreshUrl" value="submitIdentity.jsp?counter=${counterParam}&new=${param.new}&cryptoImpl=${param.cryptoImpl}&publicName=${ib:urlEncode(ib:urlEncode(param.publicName))}&description=${param.description}&emailAddress=${param.emailAddress}&isDefault=${param.isDefault}" scope="request"/>
        <c:set var="refreshInterval" value="0" scope="request"/>
        <jsp:include page="header.jsp"/>
        <div class="main">
            <h2><ib:message key="Please wait..."/></h2>
            <img src="images/wait.gif"/>
            <ib:message key="The Email Identity is being generated."/>
        </div>
    </c:when>
    <%-- This is where the actual identity generation takes place --%>
    <c:when test="${param.counter gt keygenCounter or param.new ne 'true' or param.cryptoImpl ne 4}">
        <c:set var="publicName" value="${param.publicName}"/>
        <c:if test="${not empty param.counter}">
            <c:set var="publicName" value="${ib:urlDecode(publicName)}"/>
        </c:if>
        
        <%-- after password entry, go to the wait page if a new "slow" identity is being generated --%>
        <c:if test="${param.new eq 'true' and param.cryptoImpl eq 4}">
            <c:set var="actionParam" value="action=wait&"/>
        </c:if>
        <c:set var="forwardUrl" value="submitIdentity.jsp?${actionParam}counter=${param.counter}&new=${param.new}&cryptoImpl=${param.cryptoImpl}&publicName=${param.publicName}&description=${param.description}&emailAddress=${param.emailAddress}&isDefault=${param.isDefault}"/>
        <ib:requirePassword forwardUrl="${forwardUrl}">
            <c:set var="errorMessage" value="${ib:createOrModifyIdentity(param.new, param.cryptoImpl, param.key, publicName, param.description, param.emailAddress, param.isDefault=='on')}"/>
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