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
<%@ taglib prefix="ib" uri="I2pBoteTags" %>

<ib:message key="Password Required" var="title" scope="request"/>
<jsp:include page="header.jsp"/>

<c:if test="${param.passwordJspAction eq 'check'}">
    <c:choose>
        <c:when test="${ib:tryPassword(param.password)}">
            <jsp:forward page="${param.passwordJspForwardUrl}"/>
        </c:when>
        <c:otherwise>
            <div class="errorMessage"><ib:message key="Wrong password. Try again."/></div>
        </c:otherwise>
    </c:choose>
</c:if>

<div class="main">
    <h2><ib:message key="Password required"/></h2>
    
    <form name="form" action="password.jsp?passwordJspAction=check" method="POST">
        <ib:copyParams paramsToCopy="*" paramsToExclude="password"/>
        <ib:message key="Password:"/> <input type="password" name="password"/>
        <ib:message key="OK" var="ok"/>
        <input type="submit" value="${ok}"/>
    </form>

    <script type="text/javascript" language="JavaScript">
        document.forms['form'].elements['password'].focus();
    </script>
</div>

<jsp:include page="footer.jsp"/>