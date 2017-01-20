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
    pageContext.setAttribute("DATE", i2p.bote.email.EmailAttribute.DATE, PageContext.PAGE_SCOPE);
    pageContext.setAttribute("DELIVERED", i2p.bote.email.EmailAttribute.DELIVERED, PageContext.PAGE_SCOPE);
%> 

<%-- The outbox is implemented in a separate JSP --%>
<c:if test="${param.path eq 'Outbox'}">
    <jsp:forward page="outbox.jsp"/>
</c:if>

<c:set var="refreshUrl" value="folder.jsp?path=${param.path}&amp;sortcolumn=${param.sortcolumn}&amp;descending=${param.descending}" scope="request"/>
<ib:requirePassword forwardUrl="${refreshUrl}">
<%-- Autorefresh inbox and sent folders --%>
<c:if test="${param.path eq 'Inbox' or param.path eq 'Sent'}">
    <c:set var="refreshInterval" value="60" scope="request"/>
</c:if>
<ib:message key="${param.path}" var="title" scope="request" noextract="true"/>   <%-- Translation strings are extracted from folders.jsp --%>
<c:set var="contentClass" value="main foldermain" scope="request"/>
<c:set var="navSelected" value="${param.path}" scope="request"/>
<jsp:include page="header.jsp"/>

<div class="compose float">
    <csrf:form action="newEmail.jsp" target="_top" method="POST">
        <button type="submit" value="New"><img src="${themeDir}/images/compose.png"/></button>
    </csrf:form>
</div>

<c:set var="folderName" value="${param.path}"/>
<c:if test="${empty folderName}">
    <c:set var="folderName" value="Inbox"/>
</c:if>
<c:if test="${folderName == 'Inbox'}">
    <div id="inboxFlag"></div>
</c:if>

<c:set var="isSentFolder" value="false"/>
<c:if test="${folderName == 'Sent'}">
    <c:set var="isSentFolder" value="true"/>
</c:if>

<c:set var="sortcolumn" value="${DATE}"/>
<c:if test="${!empty param.sortcolumn}">
    <c:set var="sortcolumn" value="${param.sortcolumn}"/>
</c:if>

<c:choose>
    <c:when test="${empty param.descending}">
        <%-- Use the default sort direction: descending for date, ascending for everything else --%>
        <c:if test="${sortcolumn eq DATE}">
            <c:set var="descending" value="true"/>
        </c:if>
        <c:if test="${sortcolumn ne DATE}">
            <c:set var="descending" value="false"/>
        </c:if>
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
        <c:set var="folder" value="${ib:getMailFolder(folderName)}"/>
        <tr>
            <th class="header-column-replied"/>
            <th class="header-column-from">
                <c:set var="sortLink" value="folder.jsp?path=${param.path}&amp;sortcolumn=${FROM}"/>
                <c:if test="${sortcolumn eq FROM}">
                    <c:set var="sortLink" value="${sortLink}${reverseSortOrder}"/>
                    <c:set var="fromColumnIndicator" value=" ${sortIndicator}"/>
                </c:if>
                <a href="${sortLink}"><ib:message key="From"/>${fromColumnIndicator}</a>
            </th>
            <%-- Don't show the "known" column in the sent folder --%>
            <c:if test="${not isSentFolder}">
                <th class="header-column-known"><ib:message key="Know"/></th>
            </c:if>
            <th class="header-column-to">
                <c:set var="sortLink" value="folder.jsp?path=${param.path}&amp;sortcolumn=${TO}"/>
                <c:if test="${sortcolumn eq TO}">
                    <c:set var="sortLink" value="${sortLink}${reverseSortOrder}"/>
                    <c:set var="toColumnIndicator" value=" ${sortIndicator}"/>
                </c:if>
                <a href="${sortLink}"><ib:message key="To"/>${toColumnIndicator}</a>
            </th>
            <th class="header-column-subject">
                <c:set var="sortLink" value="folder.jsp?path=${param.path}&amp;sortcolumn=${SUBJECT}"/>
                <c:if test="${sortcolumn eq SUBJECT}">
                    <c:set var="sortLink" value="${sortLink}${reverseSortOrder}"/>
                    <c:set var="subjectColumnIndicator" value=" ${sortIndicator}"/>
                </c:if>
                <a href="${sortLink}"><ib:message key="Subject"/>${subjectColumnIndicator}</a>
            </th>
            <th class="header-column-attachment"/>
            <th class="header-column-date">
                <c:set var="sortLink" value="folder.jsp?path=${param.path}&amp;sortcolumn=${DATE}"/>
                <c:if test="${sortcolumn eq DATE}">
                    <c:set var="sortLink" value="${sortLink}${reverseSortOrder}"/>
                    <c:set var="dateColumnIndicator" value=" ${sortIndicator}"/>
                </c:if>
                <a href="${sortLink}"><ib:message key="Sent Time"/>${dateColumnIndicator}</a>
            </th>
            <%-- Show the "delivered" column only in the sent folder --%>
            <c:if test="${isSentFolder}">
                <th class="header-column-delivery">
                    <c:set var="sortLink" value="folder.jsp?path=${param.path}&amp;sortcolumn=${DELIVERED}"/>
                    <c:if test="${sortcolumn eq DELIVERED}">
                        <c:set var="sortLink" value="${sortLink}${reverseSortOrder}"/>
                        <c:set var="deliveryColumnIndicator" value=" ${sortIndicator}"/>
                    </c:if>
                    <a href="${sortLink}"><ib:message key="Delivered"/>${deliveryColumnIndicator}</a>
                </th>
            </c:if>
            <th class="header-column-trash"></th>
        </tr>
        
        <c:forEach items="${ib:getEmails(folder, sortcolumn, descending)}" var="email" varStatus="status">
            <c:set var="sender" value="${ib:getNameAndShortDestination(email.oneFromAddress)}"/>
            <c:if test="${empty sender}">
                <ib:message key="Anonymous" var="sender"/>
            </c:if>
            
            <c:set var="known" value=""/>
            <c:if test="${ib:isKnown(email.oneFromAddress)}">
                <c:set var="known" value="<div class='sender-known'>&#10004;</div>"/>
            </c:if>
            
            <c:if test="${isSentFolder}">
                <c:set var="delivered" value="<div class='deliveryComplete'>&#10004;</div>"/>
                <c:set var="deliveryPercentage" value="${email.deliveryPercentage}"/>
                <c:if test="${not email.delivered}">
                    <c:set var="delivered" value="<div class='deliveryIncomplete' title='${deliveryPercentage}%'><meter max='100' value='${deliveryPercentage}'>${deliveryPercentage}</meter></div>"/>
                </c:if>
            </c:if>
            
            <c:set var="recipient" value="${ib:getNameAndShortDestination(ib:getOneLocalRecipient(email))}"/>
            
            <c:set var="subject" value="${email.subject}"/>
            <c:if test="${empty subject}">
                <ib:message key="(No subject)" var="subject"/>
            </c:if>
            
            <c:set var="mailUrl" value="showEmail.jsp?folder=${folderName}&amp;messageID=${email.messageID}"/>
            
            <c:choose>
                <c:when test="${email.unread}"><c:set var="textClass" value="folder-item-new"/></c:when>
                <c:otherwise><c:set var="textClass" value="folder-item-old"/></c:otherwise>
            </c:choose>
            
            <c:set var="backgroundClass" value="even-table-cell"/>
            <c:if test="${status.index%2 != 0}">
                <c:set var="backgroundClass" value="odd-table-cell"/>
            </c:if>
            
            <tr class="${textClass} ${backgroundClass}">
            <td class="header-column-replied">
                <c:if test="${not email.signatureValid and not email.anonymous}">
                    <ib:message key="Signature is invalid or missing" var="sigMessage"/>
                    <div class="sig-invalid"><img src="${themeDir}/images/warning.png" title="${sigMessage}"/></div>
                </c:if>
                <c:if test="${email.signatureValid}">
                    <c:if test="${email.replied}">&#10550;</c:if>
                </c:if>
            </td>
            <td class="ellipsis"><a href="${mailUrl}">
                <c:if test="${email.anonymous}">
                    <div class="anon-sender">
                </c:if>
                ${fn:escapeXml(sender)}
                <c:if test="${empty sender}">
                    </div>
                </c:if>
            </a></td>
            <%-- Don't show the "known" column in the sent folder --%>
            <c:if test="${not isSentFolder}">
                <td><c:out value="${known}" escapeXml="false"/></td>
            </c:if>
            <td class="ellipsis"><a href="${mailUrl}">${fn:escapeXml(recipient)}</a></td>
            <td class="ellipsis"><a href="${mailUrl}">${fn:escapeXml(subject)}</a></td>
            <td><c:if test="${email.containingAttachments}">
                <img src="${themeDir}/images/attachment.png"/>
            </c:if></td>
            <td>
                <a href="${mailUrl}"><ib:printDate date="${email.sentDate}" timeStyle="short" printUnknown="true"/></a>
            </td>
            <%-- Show the "delivered" column only in the sent folder --%>
            <c:if test="${isSentFolder}">
                <td>${delivered}</td>
            </c:if>
            <td>
                <a href="deleteEmail.jsp?folder=${folderName}&amp;messageID=${email.messageID}">
                <img src="${themeDir}/images/delete.png" alt="<ib:message key='Delete'/>" title="<ib:message key='Delete this email'/>"/></a>
            </td>
            </tr>
        </c:forEach>
    </table>
</ib:requirePassword>

<jsp:include page="footer.jsp"/>
