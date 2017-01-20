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

<ib:message key="Identities" var="title" scope="request"/>
<c:set var="navSelected" value="identities" scope="request"/>
<jsp:include page="header.jsp"/>

<jsp:include page="getStatus.jsp"/>

<jsp:useBean id="jspHelperBean" class="i2p.bote.web.JSPHelper"/>
    <h1><ib:message key="Email Identities"/></h1>

    <ib:requirePassword>
        <c:set var="identities" value="${jspHelperBean.identities.all}"/>
    </ib:requirePassword>
    <c:if test="${empty identities}">
        <ib:message key="No email identities are defined."/>
    </c:if>
    
    <table>
    <c:if test="${!empty identities}">
        <tr>
            <th class="header-column-default"><ib:message key="Def."/></th>
            <th class="header-column-public-name"><ib:message key="Public Name"/></th>
            <th class="header-column-description"><ib:message key="Description"/></th>
            <th class="header-column-id-email-dest"><ib:message key="Email Destination"/></th>
            <th class="header-column-check-email"></th>
        </tr>
    </c:if>
    <c:forEach items="${identities}" var="identity" varStatus="status">
        <c:set var="backgroundClass" value="even-table-cell"/>
        <c:if test="${status.index%2 != 0}">
            <c:set var="backgroundClass" value="odd-table-cell"/>
        </c:if>
        
        <tr class="${backgroundClass}">
        <td class="data-column-default">
            <c:if test="${identity.defaultIdentity}">
                <img src="${themeDir}/images/default.png"/>
            </c:if>
        </td>
        <%-- Insert a random number into the request string so others can't see contacts or identities using the CSS history hack --%>
        <c:set var="editIdentityURL" value="editIdentity.jsp?rnd=${jspHelperBean.randomNumber}&amp;createNew=false&amp;key=${identity.key}&amp;cryptoImpl=${identity.cryptoImpl}&amp;publicName=${ib:escapeQuotes(identity.publicName)}&amp;description=${ib:escapeQuotes(identity.description)}&amp;emailAddress=${ib:escapeQuotes(identity.emailAddress)}&amp;defaultIdentity=${identity.defaultIdentity}"/>
        <td>
            <a href="${editIdentityURL}" title="Click to see identity details">
                ${fn:escapeXml(identity.publicName)}
            </a>
        </td>
        <td>${identity.description}</td>
        <td class="ellipsis"><a href="${editIdentityURL}" title="Click to see identity details">${identity.key}</a></td>
        <td><c:choose>
        <c:when test="${ib:isCheckingForMail(identity)}">
            <img src="${themeDir}/images/wait.gif" alt="<ib:message key='Checking for mail...'/>" title='<ib:message key='Checking for mail...'/>'/>
        </c:when>
        <c:when test="${connStatus eq CONNECTED}">
            <a href="checkMail.jsp?identity=${identity.key}"><img src="${themeDir}/images/refresh.png" alt="<ib:message key='Check Mail'/>" title='<ib:message key='Check mail for this identity'/>'/></a>
        </c:when>
        <c:otherwise>
        </c:otherwise>
        </c:choose></td>
        </tr>
    </c:forEach>
    </table>
    
    <p/>
    <csrf:form class="onebutton" action="editIdentity.jsp" method="POST">
        <input type="hidden" name="createNew" value="true"/>
        <button type="submit" value="New"><ib:message key="New Identity"/></button>
    </csrf:form>
    <csrf:form class="onebutton" action="importIdentities.jsp" method="POST">
        <button type="submit"><ib:message key="Import Identities"/></button>
    </csrf:form>
    <csrf:form class="onebutton" action="exportIdentities.jsp" method="POST">
        <button type="submit"><ib:message key="Export Identities"/></button>
    </csrf:form>

<jsp:include page="footer.jsp"/>
