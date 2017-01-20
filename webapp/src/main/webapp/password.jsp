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

<c:set var="action" value="${param.passwordJspAction}" scope="request"/>
<c:if test="${not empty action and pageContext.request.method ne 'POST'}">
    <c:set var="action" value="" scope="request"/>
    <ib:message key="Form must be submitted using POST." var="errorMessage" scope="request"/>
</c:if>

<ib:message key="Password Required" var="title" scope="request"/>
<jsp:include page="header.jsp"/>

<c:if test="${action eq 'check'}">
    <c:choose>
        <c:when test="${ib:tryPassword(param.nofilter_password)}">
            <jsp:forward page="${param.passwordJspForwardUrl}"/>
        </c:when>
        <c:otherwise>
            <div class="errorMessage"><ib:message key="Wrong password. Try again."/></div>
        </c:otherwise>
    </c:choose>
</c:if>

    <h1><ib:message key="Password required"/></h1>
    
    <csrf:form name="form" action="password.jsp" method="POST">
        <input type="hidden" name="passwordJspAction" value="check"/>
        <ib:copyParams paramsToCopy="*" paramsToExclude="nofilter_password"/>
        <ib:message key="Password:"/> <input type="password" name="nofilter_password"/>
        <button type="submit"><ib:message key="OK"/></button>
    </csrf:form>

    <script type="text/javascript" language="JavaScript">
        document.forms['form'].elements['nofilter_password'].focus();
    </script>

<jsp:include page="footer.jsp"/>
