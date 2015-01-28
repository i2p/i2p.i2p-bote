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
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib prefix="ib" uri="I2pBoteTags" %>

<ib:message key="Identities" var="title" scope="request"/>
<jsp:include page="header.jsp"/>

<jsp:useBean id="jspHelperBean" class="i2p.bote.web.JSPHelper"/>
    <h2>
        <ib:message key="Email Identities"/>
    </h2>

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
        <td>
            <%-- Insert a random number into the request string so others can't see contacts or identities using the CSS history hack --%>
            <a href="editIdentity.jsp?rnd=${jspHelperBean.randomNumber}&amp;createNew=false&amp;key=${identity.key}&amp;cryptoImpl=${identity.cryptoImpl}&amp;publicName=${ib:escapeQuotes(identity.publicName)}&amp;description=${ib:escapeQuotes(identity.description)}&amp;emailAddress=${ib:escapeQuotes(identity.emailAddress)}&amp;defaultIdentity=${identity.defaultIdentity}">
                ${fn:escapeXml(identity.publicName)}
            </a>
        </td>
        <td>${identity.description}</td>
        <td class="ellipsis">${identity.key}</td>
        </tr>
    </c:forEach>
    </table>
    
    <p/>
    <form action="editIdentity.jsp?createNew=true" method="POST">
        <button type="submit" value="New"><ib:message key="New Identity"/></button>
    </form>
    <form action="exportIdentities.jsp" method="POST">
        <button type="submit"><ib:message key="Export Identities"/></button>
    </form>

<jsp:include page="footer.jsp"/>
