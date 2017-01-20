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

<ib:message key="Full Identity" var="title" scope="request"/>
<jsp:include page="header.jsp"/>

<ib:requirePassword>
    <c:set var="key" value="${param.key}"/>
    <c:set var="identity" value="${ib:getIdentity(key)}"/>
    <c:set var="publicName" value="${ib:escapeQuotes(identity.publicName)}"/>
    
    <div class="full-identity">
        <p>
        <ib:message key="Full Email Identity for {0}:">
            <ib:param value="${publicName}"/>
        </ib:message>
        </p>
        <textarea cols="64" rows="9" readonly="readonly">${identity.fullKey}</textarea>
    </div>
    <p/>
    <div class="warning"><b><ib:message key="Do not show the above information to anyone! It contains your private keys."/></b></div>
    <p/>
    <csrf:form action="editIdentity.jsp" method="POST">
        <input type="hidden" name="key" value="${key}"/>
        <button type="submit"><ib:message key="Return"/></button>
    </csrf:form>
</ib:requirePassword>

<jsp:include page="footer.jsp"/>
