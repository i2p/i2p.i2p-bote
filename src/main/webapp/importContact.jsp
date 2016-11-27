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

<jsp:useBean id="jspHelperBean" class="i2p.bote.web.JSPHelper"/>

<ib:message key="Address Directory Lookup" var="title" scope="request"/>

<c:if test="${pageContext.request.method ne 'POST'}">
    <ib:message key="Form must be submitted using POST." var="errorMessage" scope="request"/>
    <jsp:forward page="addressBook.jsp"/>
</c:if>

    <c:if test="${param.confirm eq true}">
        <ib:requirePassword>
            <c:set var="errorMessage" value="${ib:saveContact(param.destination, param.name, param.picture, param.text)}"/>
            <c:if test="${empty errorMessage}">
                <ib:message key="The name has been imported to the address book." var="infoMessage" scope="request"/>
            </c:if>
            <jsp:forward page="addressBook.jsp"/>
        </ib:requirePassword>
    </c:if>
        
    <c:if test="${param.confirm ne true}">
        <c:set var="result" value="${ib:lookupInDirectory(param.name)}"/>
        
        <c:if test="${empty result}">
            <ib:message key="The name &quot;{0}&quot; was not found in the directory." var="errorMessage" scope="request">
                <ib:param value="${param.name}"/>
            </ib:message>
            <jsp:forward page="addressBook.jsp"/>
        </c:if>
        
        <c:if test="${not empty result}">
            <ib:message key="Import Contact" var="pagetitle" scope="request"/>
            <jsp:include page="header.jsp"/>
            <h1><ib:message key="Import Contact"/></h1>
            <p>
            <ib:message>
                A matching record was found in the address directory. Note that the address directory is
                not secure against manipulation, so do not click &quot;import&quot; unless you trust
                that it is the right email destination.
            </ib:message>
            </p>
            
            <%-- fingerprint --%>
            <c:set var="uiLocaleCode" value="${jspHelperBean.language}"/>
            <b><ib:message key="Fingerprint:"/></b> ${ib:getContactFingerprint(result, uiLocaleCode)}
            <ib:expandable>
                <c:forEach items="${jspHelperBean.wordListLocales}" var="localeCode">
                    <c:if test="${localeCode ne uiLocaleCode}">
                        &nbsp;&nbsp;<b>${localeCode}</b>: ${ib:getContactFingerprint(result, localeCode)}<br/>
                    </c:if>
                </c:forEach>
            </ib:expandable>
            
            <%-- image, name, text --%>
            <p/>
            <div class="contact-detail-container">
                <div class="contact-detail-left">
                    <img src="data:${result.pictureType};base64,${result.pictureBase64}"/><br/>
                    ${fn:escapeXml(param.name)}
                </div>
                <div class="contact-detail-text">${fn:escapeXml(result.text)}</div>
            </div>
            <div class="contact-detail-dest">
                <b><ib:message key="Email Destination: "/></b>
                ${result.destination}
            </div>
            
            <csrf:form action="importContact.jsp" method="post">
                <input type="hidden" name="confirm" value="true"/>
                <input type="hidden" name="name" value="${param.name}"/>
                <input type="hidden" name="destination" value="${result.destination}"/>
                <input type="hidden" name="picture" value="${result.pictureBase64}"/>
                <input type="hidden" name="text" value="${fn:escapeXml(result.text)}"/>
                <ib:message key="Import" var="import" scope="request"/>
                <input type="submit" value="${import}"/>
            </csrf:form>
        </c:if>
    </c:if>

<jsp:include page="footer.jsp"/>
