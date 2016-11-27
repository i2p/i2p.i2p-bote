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

<ib:message key="No Identity" var="title" scope="request"/>
<ib:message key="No Email Identity Defined" var="pagetitle" scope="request"/>
<jsp:include page="header.jsp"/>

    <h1><ib:message key="No Email Identity Defined"/></h1>
    <p>
    <jsp:include page="identitiesHelp.jsp"/>
    <csrf:form action="editIdentity.jsp" method="POST">
        <input type="hidden" name="createNew" value="true"/>
        <button type="submit" value="New"><ib:message key="Create a New Email Identity"/></button>
    </csrf:form>

<jsp:include page="footer.jsp"/>
