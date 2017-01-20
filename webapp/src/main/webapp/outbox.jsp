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

<%
    pageContext.setAttribute("FROM", i2p.bote.email.EmailAttribute.FROM, PageContext.PAGE_SCOPE);
    pageContext.setAttribute("TO", i2p.bote.email.EmailAttribute.TO, PageContext.PAGE_SCOPE);
    pageContext.setAttribute("SUBJECT", i2p.bote.email.EmailAttribute.SUBJECT, PageContext.PAGE_SCOPE);
    pageContext.setAttribute("CREATE_TIME", i2p.bote.email.EmailAttribute.CREATE_TIME, PageContext.PAGE_SCOPE);
    pageContext.setAttribute("STATUS", i2p.bote.email.EmailAttribute.STATUS, PageContext.PAGE_SCOPE);
%> 

<c:set var="refreshUrl" value="outbox.jsp?sortcolumn=${param.sortcolumn}&amp;descending=${param.descending}" scope="request"/>
<ib:requirePassword forwardUrl="${refreshUrl}">
<%-- Refresh page if there are mails in the outbox --%>
<c:if test="${ib:getMailFolder('Outbox').numElements gt 0}">
    <c:set var="refreshInterval" value="20" scope="request"/>
</c:if>
<ib:message key="Outbox" var="title" scope="request"/>
<c:set var="contentClass" value="main foldermain" scope="request"/>
<c:set var="navSelected" value="Outbox" scope="request"/>
<jsp:include page="header.jsp"/>

<div class="compose float">
    <csrf:form action="newEmail.jsp" method="POST">
        <button type="submit" value="New"><img src="${themeDir}/images/compose.png"/></button>
    </csrf:form>
</div>

<c:set var="sortcolumn" value="${CREATE_TIME}"/>
<c:if test="${!empty param.sortcolumn}">
    <c:set var="sortcolumn" value="${param.sortcolumn}"/>
</c:if>

<c:choose>
    <c:when test="${empty param.descending}">
        <c:set var="descending" value="false"/>
    </c:when>
    <c:otherwise>
        <%-- Set the sort direction depending on param.descending --%>
        <c:set var="descending" value="false"/>
        <c:if test="${param.descending}">
            <c:set var="descending" value="true"/>
        </c:if>
    </c:otherwise>
</c:choose>

<c:if test="${!descending}">
    <c:set var="sortIndicator" value="&#x25b4;"/>
    <c:set var="reverseSortOrder" value="&amp;descending=true"/>
</c:if>
<c:if test="${descending}">
    <c:set var="sortIndicator" value="&#x25be;"/>
    <c:set var="reverseSortOrder" value="&amp;descending=false"/>
</c:if>

    <table>
        <c:set var="folder" value="${ib:getMailFolder('Outbox')}"/>
        <tr>
            <th class="header-column-from">
                <c:set var="sortLink" value="outbox.jsp?sortcolumn=${FROM}"/>
                <c:if test="${sortcolumn eq FROM}">
                    <c:set var="sortLink" value="${sortLink}${reverseSortOrder}"/>
                    <c:set var="fromColumnIndicator" value=" ${sortIndicator}"/>
                </c:if>
                <a href="${sortLink}"><ib:message key="From"/>${fromColumnIndicator}</a>
            </th>
            <th class="header-column-to">
                <c:set var="sortLink" value="outbox.jsp?sortcolumn=${TO}"/>
                <c:if test="${sortcolumn eq TO}">
                    <c:set var="sortLink" value="${sortLink}${reverseSortOrder}"/>
                    <c:set var="toColumnIndicator" value=" ${sortIndicator}"/>
                </c:if>
                <a href="${sortLink}"><ib:message key="To"/>${toColumnIndicator}</a>
            </th>
            <th class="header-column-subject">
                <c:set var="sortLink" value="outbox.jsp?sortcolumn=${SUBJECT}"/>
                <c:if test="${sortcolumn eq SUBJECT}">
                    <c:set var="sortLink" value="${sortLink}${reverseSortOrder}"/>
                    <c:set var="subjectColumnIndicator" value=" ${sortIndicator}"/>
                </c:if>
                <a href="${sortLink}"><ib:message key="Subject"/>${subjectColumnIndicator}</a>
            </th>
            <th class="header-column-date">
                <c:set var="sortLink" value="outbox.jsp?sortcolumn=${CREATE_TIME}"/>
                <c:if test="${sortcolumn eq CREATE_TIME}">
                    <c:set var="sortLink" value="${sortLink}${reverseSortOrder}"/>
                    <c:set var="createTimeColumnIndicator" value=" ${sortIndicator}"/>
                </c:if>
                <a href="${sortLink}"><ib:message key="Create Time"/>${createTimeColumnIndicator}</a>
            </th>
            <th class="header-column-status">
                <c:set var="sortLink" value="outbox.jsp?sortcolumn=${STATUS}"/>
                <c:if test="${sortcolumn eq STATUS}">
                    <c:set var="sortLink" value="${sortLink}${reverseSortOrder}"/>
                    <c:set var="statusColumnIndicator" value=" ${sortIndicator}"/>
                </c:if>
                <a href="${sortLink}"><ib:message key="Status"/>${statusColumnIndicator}</a>
            </th>
            <th class="header-column-trash"></th>
        </tr>
        
        <c:forEach items="${ib:getEmails(folder, sortcolumn, descending)}" var="email" varStatus="status">
            <c:set var="sender" value="${ib:getNameAndShortDestination(email.oneFromAddress)}"/>
            <c:if test="${empty sender}">
                <ib:message key="Anonymous" var="sender"/>
            </c:if>
            
            <c:set var="recipient" value="${ib:getNameAndShortDestination(email.oneRecipient)}"/>
            
            <c:set var="createTime" value="${email.createTime}"/>
            
            <c:set var="subject" value="${email.subject}"/>
            <c:if test="${empty subject}">
                <ib:message key="(No subject)" var="subject"/>
            </c:if>
            
            <c:set var="mailUrl" value="showEmail.jsp?folder=Outbox&amp;messageID=${email.messageID}"/>
            
            <c:choose>
                <c:when test="${email.unread}"><c:set var="textClass" value="folder-item-new"/></c:when>
                <c:otherwise><c:set var="textClass" value="folder-item-old"/></c:otherwise>
            </c:choose>
            
            <c:set var="backgroundClass" value="even-table-cell"/>
            <c:if test="${status.index%2 != 0}">
                <c:set var="backgroundClass" value="odd-table-cell"/>
            </c:if>
            
            <tr class="${textClass} ${backgroundClass}">
            <td class="ellipsis"><a href="${mailUrl}">${fn:escapeXml(sender)}</a></td>
            <td class="ellipsis"><a href="${mailUrl}">${fn:escapeXml(recipient)}</a></td>
            <td class="ellipsis"><a href="${mailUrl}">${fn:escapeXml(subject)}</a></td>
            <td>
                <a href="${mailUrl}"><ib:printDate date="${email.sentDate}" timeStyle="short" printUnknown="true"/></a>
            </td>
            <td><div><a href="${mailUrl}">${ib:getEmailStatusText(email)}</a></div></td>
            <td>
                <a href="deleteEmail.jsp?folder=Outbox&amp;messageID=${email.messageID}">
                <img src="${themeDir}/images/delete.png" alt="<ib:message key='Delete'/>" title="<ib:message key='Delete this email'/>"/></a>
            </td>
            </tr>
        </c:forEach>
    </table>
</ib:requirePassword>

<jsp:include page="footer.jsp"/>
