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
    Valid actions:
        start - Check if the name is taken
        step2 - Add a picture and text
        store - Store in DHT
    
    Other parameters:
        key         - The email destination
        description - New value for the private description field (publish does save first)
        publicName  - New value for the public name field
        text        - Text to include in the DHT record
--%>

<c:set var="action" value="${param.action}" scope="request"/>
<c:if test="${not empty action and pageContext.request.method ne 'POST'}">
    <c:set var="action" value="" scope="request"/>
    <ib:message key="Form must be submitted using POST." var="errorMessage" scope="request"/>
</c:if>

<ib:message key="Add Email Destination to Directory" var="title" scope="request"/>
<jsp:include page="header.jsp"/>

    <c:if test="${action eq 'start'}">
        <%-- If the user changed the Public Name to try a new name that isn't taken, update it so they don't have to click save first --%>
        <ib:requirePassword>
            ${ib:modifyIdentity(param.key, param.publicName, param.description, null, param.emailAddress, null, false)}
        </ib:requirePassword>
        <c:set var="result" value="${ib:lookupInDirectory(param.publicName)}"/>
        <c:if test="${not empty result}">
            <ib:message key="The name exists already. Please choose a different Public Name." var="errorMessage" scope="request"/>
            <jsp:forward page="editIdentity.jsp?rnd=${jspHelperBean.randomNumber}&amp;new=false&amp;key=${identity.destination}"/>
        </c:if>
        <c:if test="${empty result}">
            <jsp:forward page="publishDestination.jsp">
                <jsp:param name="action" value="step2"/>
                <jsp:param name="text" value="${param.text}"/>
                <jsp:param name="key" value="${param.key}"/>
            </jsp:forward>
        </c:if>
    </c:if>
    <c:if test="${action eq 'step2'}">
        <h1><ib:message key="Publish to the Address Directory"/></h1>
        <c:set var="csrf_tokenname"><csrf:tokenname/></c:set>
        <c:set var="csrf_tokenvalue"><csrf:tokenvalue uri="publishDestination.jsp"/></c:set>
        <form action="publishDestination.jsp?${csrf_tokenname}=${csrf_tokenvalue}" method="post" enctype="multipart/form-data" accept-charset="UTF-8">
            <input type="hidden" name="action" value="store"/>
            <input type="hidden" name="name" value="${param.publicName}"/>
            <input type="hidden" name="destination" value="${param.key}"/>
            <div class="publish-form-label">
                <ib:message key="Picture to upload:"/>
                <div class="addtl-text"><ib:message key="(optional, 7.5 kBytes max)"/></div>
            </div>
            <div class="publish-form-value">
                <input type="file" name="picture"/>
            </div>
            <div class="publish-form-label">
                <ib:message key="Text to include:"/>
                <div class="addtl-text"><ib:message key="(optional, 2000 chars max)"/></div>
            </div>
            <div class="publish-form-value">
                <textarea rows="15" cols="50" name="text"></textarea>
            </div>
            <button type="submit"><ib:message key="Publish"/></button>
        </form>
    </c:if>
    <c:if test="${action eq 'store'}">
        <c:set var="picFilename" value="${requestScope['picture'].tempFilename}"/>
        <ib:publishDestination destination="${param.destination}" pictureFilename="${picFilename}" text="${param.text}"/>
        <ib:message var="infoMessage" scope="request" key="The identity has been added to the address directory."/>
        <jsp:useBean id="jspHelperBean" class="i2p.bote.web.JSPHelper"/>
        <jsp:forward page="editIdentity.jsp?rnd=${jspHelperBean.randomNumber}&new=false&key=${param.destination}"/>
    </c:if>

<jsp:include page="footer.jsp"/>
