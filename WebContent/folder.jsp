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
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">

<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib prefix="ib" uri="I2pBoteTags" %>

<%
    pageContext.setAttribute("FROM", i2p.bote.email.EmailAttribute.FROM, PageContext.PAGE_SCOPE);
    pageContext.setAttribute("TO", i2p.bote.email.EmailAttribute.TO, PageContext.PAGE_SCOPE);
    pageContext.setAttribute("SUBJECT", i2p.bote.email.EmailAttribute.SUBJECT, PageContext.PAGE_SCOPE);
    pageContext.setAttribute("DATE", i2p.bote.email.EmailAttribute.DATE, PageContext.PAGE_SCOPE);
%> 

<%-- The outbox is implemented in a separate JSP --%>
<c:if test="${param.path eq 'Outbox'}">
    <jsp:forward page="outbox.jsp"/>
</c:if>

<%-- Autorefresh inbox and sent folders --%>
<c:if test="${param.path eq 'Inbox' or param.path eq 'Sent'}">
    <c:set var="refreshInterval" value="60" scope="request"/>
    <c:set var="refreshUrl" value="folder.jsp?path=${param.path}&sortcolumn=${param.sortcolumn}&descending=${param.descending}" scope="request"/>
</c:if>
<c:set var="title" value="${param.path}" scope="request"/>
<jsp:include page="header.jsp"/>

<c:set var="folderName" value="${param.path}"/>
<c:if test="${empty folderName}">
    <c:set var="folderName" value="Inbox"/>
</c:if>
<c:if test="${folderName == 'Inbox'}">
    <div id="inboxFlag"></div>
</c:if>

<%-- Don't show signature info in the sent folder because sent emails are saved without a signature --%>
<c:set var="showSignatureColumn" value="true"/>
<c:if test="${folderName eq 'Sent'}">
    <c:set var="showSignatureColumn" value="false"/>
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
    <c:set var="reverseSortOrder" value="&descending=true"/>
</c:if>
<c:if test="${descending}">
    <c:set var="sortIndicator" value="&#x25be;"/>
    <c:set var="reverseSortOrder" value="&descending=false"/>
</c:if>

<div class="main">
<div class="folder">
    <table class="table">
        <c:set var="folder" value="${ib:getMailFolder(folderName)}"/>
        <tr>
            <th style="width: 100px;">
                <c:set var="sortLink" value="folder.jsp?path=${param.path}&sortcolumn=${FROM}"/>
                <c:if test="${sortcolumn eq FROM}">
                    <c:set var="sortLink" value="${sortLink}${reverseSortOrder}"/>
                    <c:set var="fromColumnIndicator" value=" ${sortIndicator}"/>
                </c:if>
                <a href="${sortLink}"><ib:message key="From"/>${fromColumnIndicator}</a>
            </th>
            <th style="width: 30px; text-align: center;"><ib:message key="Know"/></th>
            <c:if test="${showSignatureColumn}">
                <th style="width: 20px; text-align: center;"><ib:message key="Sig"/></th>
            </c:if>
            <th style="width: 100px;">
                <c:set var="sortLink" value="folder.jsp?path=${param.path}&sortcolumn=${TO}"/>
                <c:if test="${sortcolumn eq TO}">
                    <c:set var="sortLink" value="${sortLink}${reverseSortOrder}"/>
                    <c:set var="toColumnIndicator" value=" ${sortIndicator}"/>
                </c:if>
                <a href="${sortLink}"><ib:message key="To"/>${toColumnIndicator}</a>
            </th>
            <th style="width: 150px;">
                <c:set var="sortLink" value="folder.jsp?path=${param.path}&sortcolumn=${SUBJECT}"/>
                <c:if test="${sortcolumn eq SUBJECT}">
                    <c:set var="sortLink" value="${sortLink}${reverseSortOrder}"/>
                    <c:set var="subjectColumnIndicator" value=" ${sortIndicator}"/>
                </c:if>
                <a href="${sortLink}"><ib:message key="Subject"/>${subjectColumnIndicator}</a>
            </th>
            <th style="width: 120px;">
                <c:set var="sortLink" value="folder.jsp?path=${param.path}&sortcolumn=${DATE}"/>
                <c:if test="${sortcolumn eq DATE}">
                    <c:set var="sortLink" value="${sortLink}${reverseSortOrder}"/>
                    <c:set var="dateColumnIndicator" value=" ${sortIndicator}"/>
                </c:if>
                <a href="${sortLink}"><ib:message key="Sent"/>${dateColumnIndicator}</a>
            </th>
            <th style="width: 20px;"></th>
        </tr>
        
        <c:forEach items="${ib:getEmails(folder, sortcolumn, descending)}" var="email" varStatus="status">
            <c:set var="sender" value="${ib:getNameAndDestination(email.sender)}"/>
            <c:if test="${empty sender}">
                <ib:message key="Anonymous" var="sender"/>
            </c:if>
            
            <c:if test="${email.signatureValid}">
                <c:set var="signature" value="<div style='color: green;'>&#10004;</div>"/>
            </c:if>
            <c:if test="${!email.signatureValid}">
                <c:set var="signature" value="<div style='color: red;'>&#10008;</div>"/>
            </c:if>
            
            <c:set var="recipient" value="${ib:getNameAndDestination(ib:getOneLocalRecipient(email))}"/>
            
            <c:set var="subject" value="${email.subject}"/>
            <c:if test="${empty subject}">
                <ib:message key="(No subject)" var="subject"/>
            </c:if>
            
            <c:set var="mailUrl" value="showEmail.jsp?folder=${folderName}&messageID=${email.messageID}"/>
            
            <c:choose>
                <c:when test="${email.new}"><c:set var="fontWeight" value="bold"/></c:when>
                <c:otherwise><c:set var="fontWeight" value="normal"/></c:otherwise>
            </c:choose>
            
            <c:set var="class" value=""/>
            <c:if test="${status.index%2 != 0}">
                <c:set var="class" value=" class=\"alttablecell\""/>
            </c:if>
            
            <tr>
            <td><div${class}><a href="${mailUrl}" style="font-weight: ${fontWeight}">${fn:escapeXml(sender)}</a></div></td>
            <td><div${class} style="text-align: center;">${ib:isKnown(email.sender) ? '&#10004;' : '&nbsp;'}</div></td>
            <c:if test="${showSignatureColumn}">
                <td><div${class} style="text-align: center;"><c:out value="${signature}" escapeXml="false"/></div></td>
            </c:if>
            <td><div${class}><a href="${mailUrl}" style="font-weight: ${fontWeight}">${fn:escapeXml(recipient)}</a></div></td>
            <td><div${class}><a href="${mailUrl}" style="font-weight: ${fontWeight}">${fn:escapeXml(subject)}</a></div></td>
            <td>
	            <span${class} style="display: block;">
	                <a href="${mailUrl}" style="font-weight: ${fontWeight}; float: left"><ib:printDate date="${email.sentDate}" type="date" timeStyle="short"/></a>
	                <a href="${mailUrl}" style="font-weight: ${fontWeight}; float: right"><ib:printDate date="${email.sentDate}" type="time" timeStyle="short"/></a>
	            </span>
            </td>
            <td>
                <div${class}>
                <a href="deleteEmail.jsp?folder=${folderName}&messageID=${email.messageID}">
                <img src="images/delete.png" alt="<ib:message key='Delete'/>" title="<ib:message key='Delete this email'/>"/></a>
                </div>
            </td>
            </tr>
        </c:forEach>
    </table>
</div>
</div>

<jsp:include page="footer.jsp"/>