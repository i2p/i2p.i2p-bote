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
<c:if test="${not empty action and pageContext.request.method ne 'POST'}">
    <c:set var="action" value="" scope="request"/>
    <ib:message key="Form must be submitted using POST." var="errorMessage" scope="request"/>
</c:if>

<ib:message key="Debug" var="title" scope="request"/>
<jsp:include page="header.jsp"/>

    <h1><ib:message key="Debug Page"/></h1>
    
    <c:if test="${empty action}">
        <csrf:form action="debug.jsp" method="POST">
            <input type="hidden" name="action" value="checkFiles"/>
            <ib:message key="Test encrypted files" var="submitButtonText"/>
            <input type="submit" value="${submitButtonText}"/>
        </csrf:form>
    </c:if>
    
    <c:if test="${action eq 'checkFiles'}">
        <jsp:useBean id="jspHelperBean" class="i2p.bote.web.JSPHelper"/>
        <ib:requirePassword>
            <c:set var="undecryptableFiles" value="${jspHelperBean.undecryptableFiles}"/>
            <c:if test="${empty undecryptableFiles}">
                <b><ib:message key="No file encryption problems found."/></b>
            </c:if>
            <c:if test="${not empty undecryptableFiles}">
                <b><ib:message key="Undecryptable files:"/></b><br/>
                <ul>
                <c:forEach items="${undecryptableFiles}" var="file">
                    <li>${file}</li>
                </c:forEach>
                </ul>
            </c:if>
        </ib:requirePassword>
    </c:if>

<jsp:include page="footer.jsp"/>
