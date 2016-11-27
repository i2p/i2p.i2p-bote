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
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib prefix="ib" uri="I2pBoteTags" %>

<%--
    Valid actions:
        <default> - show the "edit contact" form
        save      - add a contact or update an existing one
        cancel    - go to the URL in backUrl
        
    Other parameters:
        createNew - true for new contact, false for existing contact.
                    If createNew=true, the name field is populated with
                    the value of the "name" parameter.
                    If createNew=false, the name is read from the address
                    book, and the "name" parameter is ignored.
--%>

<ib:requirePassword>

<c:set var="action" value="${param.action}" scope="request"/>
<c:if test="${not empty action and pageContext.request.method ne 'POST'}">
    <c:set var="action" value="" scope="request"/>
    <ib:message key="Form must be submitted using POST." var="errorMessage" scope="request"/>
</c:if>

<c:if test="${action eq 'cancel'}">
    <c:set var="backUrl" value="${param.backUrl}"/>
    <c:if test="${empty backUrl}">
        <c:set var="backUrl" value="addressBook.jsp"/>
    </c:if>
    <jsp:forward page="${backUrl}">
        <jsp:param name="action" value="none"/>
    </jsp:forward>
</c:if>

<c:if test="${action eq 'save'}">
    <c:choose>
        <c:when test="${empty param.destination}">
            <ib:message key="Please fill in the Destination field." var="errorMessage"/>
        </c:when>
        <c:when test="${empty param.name}">
            <ib:message key="Please fill in the Name field." var="errorMessage"/>
        </c:when>
        <c:otherwise>
            <c:set var="errorMessage" value="${ib:saveContact(param.destination, param.name, param.picture, param.text)}"/>
        </c:otherwise>
    </c:choose>

    <c:if test="${empty errorMessage}">
        <ib:message key="The contact has been saved." var="infoMessage"/>
        <c:set var="forwardUrl" value="${param.forwardUrl}"/>
        <c:if test="${empty forwardUrl}">
            <c:set var="forwardUrl" value="addressBook.jsp"/>
        </c:if>
        <jsp:forward page="${forwardUrl}">
            <jsp:param name="action" value="none"/>
            <jsp:param name="infoMessage" value="${infoMessage}"/>
        </jsp:forward>
    </c:if>
    <c:if test="${!empty errorMessage}">
        <jsp:forward page="editContact.jsp">
            <jsp:param name="action" value="none"/>
            <jsp:param name="errorMessage" value="${errorMessage}"/>
        </jsp:forward>
    </c:if>
</c:if>

<c:choose>
    <c:when test="${param.createNew}">
        <ib:message key="New Contact" var="title"/>
        <c:set var="title" value="${title}" scope="request"/>
        <ib:message key="Add" var="submitButtonText"/>
        <c:set var="name" value="${param.name}"/>
    </c:when>
    <c:otherwise>
        <ib:message key="Edit Contact" var="title"/>
        <c:set var="title" value="${title}" scope="request"/>
        <ib:message key="Save" var="submitButtonText"/>
        <c:set var="contact" value="${ib:getContact(param.destination)}"/>
        <c:set var="name" value="${contact.name}"/>
    </c:otherwise>
</c:choose>
<jsp:include page="header.jsp"/>

    <csrf:form name="form" action="editContact.jsp" method="POST">
        <ib:copyParams paramsToCopy="${param.paramsToCopy}"/>
        
        <div class="contact-form-label">
            <div><ib:message key="Email Destination:"/></div>
            <c:if test="${empty param.destination}">
                <div class="addtl-text"><ib:message key="(required field)"/></div>
            </c:if>
        </div>
        <div class="contact-form-value">
            <input type="text" size="60" name="destination" value="${ib:escapeQuotes(param.destination)}"/>
        </div>
                    
        <div class="contact-form-label">
            <div class="field-label"><ib:message key="Name:"/></div>
            <c:if test="${empty param.destination}">
                <div class="addtl-text"><ib:message key="(required field)"/></div>
            </c:if>
        </div>
        <div class="contact-form-value">
            <input type="text" size="40" name="name" value="${ib:escapeQuotes(name)}"/>
        </div>
        
        <div class="contact-form-label">
            <div class="field-label"><ib:message key="Picture:"/></div>
        </div>
        <div class="contact-form-picture">
            <img src="data:${contact.pictureType};base64,${contact.pictureBase64}"/>
            <input type="hidden" name="picture" value="${contact.pictureBase64}"/>
        </div>
        
        <div class="contact-form-label">
            <div class="field-label"><ib:message key="Text:"/></div>
        </div>
        <div class="contact-form-value">
            <input type="text" size="40" name="text" value="${ib:escapeQuotes(contact.text)}"/>
        </div>
        
        <p>&nbsp;</p>
        <button name="action" value="save">${submitButtonText}</button>
        <button name="action" value="cancel"><ib:message key="Cancel"/></button>
    </csrf:form>

    <script type="text/javascript" language="JavaScript">
        if (document.forms['form'].elements['destination'].value == "")
            document.forms['form'].elements['destination'].focus();
        else
            document.forms['form'].elements['name'].focus();
    </script>
</ib:requirePassword>

<jsp:include page="footer.jsp"/>
