<%--
 Copyright (C) 2015  str4d@mail.i2p
 
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
<%@ taglib prefix="ib" uri="I2pBoteTags" %>

<%--
    Valid actions:
        <default> - Show the "select identities file" form
        import    - Import the identities
    
    Other parameters:
        identitiesFile    - The identities file to import
        nofilter_password - Password if file is encrypted
        overwrite         - True if old identities should be dropped
        replace           - True if duplicate identities should overwrite the existing ones
--%>

<%--
    The identitiesFile request attribute contains a UploadedFile object, see MultipartFilter.java.
    When action='attach', originalIdentitiesFilename contains the name of the file selected by the user.
--%>
<c:set var="originalIdentitiesFilename" value="${requestScope['identitiesFile'].originalFilename}"/>

<ib:message key="Import Identities" var="title" scope="request"/>
<c:if test="${param.action eq 'import' and empty originalIdentitiesFilename}">
    <ib:message key="Please select an identities file and try again." var="noIdentitiesMsg"/>
    <c:set var="errorMessage" value="${noIdentitiesMsg}" scope="request"/>
</c:if>
<jsp:include page="header.jsp"/>

<ib:requirePassword>
    <c:choose>
        <c:when test="${param.action eq 'import' and not empty originalIdentitiesFilename}">
            <c:set var="identitiesFilename" value="${requestScope['identitiesFile'].tempFilename}"/>
            <ib:importIdentities identitiesFilename="${identitiesFilename}" password="${param.nofilter_password}" overwrite="${param.overwrite}" replace="${param.replace}"/>
            <ib:message var="infoMessage" scope="request" key="The identities have been imported."/>
            <jsp:forward page="identities.jsp"/>
        </c:when>
        <c:otherwise>
            <form action="importIdentities.jsp?action=import" method="POST" enctype="multipart/form-data" accept-charset="UTF-8">
                <div class="import-form-label">
                    <ib:message key="Identities file:"/>
                </div>
                <div class="import-form-value">
                    <input type="file" name="identitiesFile"/>
                </div>
                <div class="import-form-label">
                    <ib:message key="Password:"/>
                    <div class="addtl-text"><ib:message key="(leave blank if identities not encrypted)"/></div>
                </div>
                <div class="import-form-value">
                    <input type="password" name="nofilter_password"/>
                </div>
                <div class="import-form-label">
                    <ib:message key="Overwrite all existing identities:"/>
                </div>
                <div class="import-form-value">
                    <input type="checkbox" name="overwrite"/>
                </div>
                <div class="import-form-label">
                    <ib:message key="Duplicates: replace existing identity:"/>
                </div>
                <div class="import-form-value">
                    <input type="checkbox" name="replace"/>
                </div>
                <button type="submit"><ib:message key="Import"/></button>
            </form>
        </c:otherwise>
    </c:choose>
</ib:requirePassword>

<jsp:include page="footer.jsp"/>