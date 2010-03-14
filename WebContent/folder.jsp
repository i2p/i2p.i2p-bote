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

<c:set var="title" value="${param.path}" scope="request"/>
<jsp:include page="header.jsp"/>

<c:set var="folderName" value="Inbox"/>
<c:if test="${folderName == 'Inbox'}">
    <div id="inboxFlag"></div>
</c:if>

<div class="main">
<div class="folder">
	<table>
	    <tr>
	        <th style="width: 100px;"><ib:message key="From"/></th>
            <th style="width: 100px;"><ib:message key="To"/></th>
            <th style="width: 150px;"><ib:message key="Subject"/></th>
            <th style="width: 100px;"><ib:message key="Date"/></th>
            <th style="width: 20px;"></th>
	    </tr>
	    <c:forEach items="${ib:getMailFolder(folderName).elements}" var="email">
	        <tr>
                <c:set var="sender" value="${email.sender}"/>
                <c:if test="${empty sender}">
                    <ib:message key="Anonymous" var="sender"/>
                </c:if>
                
                <c:set var="recipient" value="${ib:getOneLocalRecipient(email)}"/>
                
                <c:set var="date" value="${email.sentDate}"/>
                <c:if test="${empty date}">
                    <ib:message key="Unknown" var="date"/>"/>
                </c:if>
                
                <c:set var="subject" value="${email.subject}"/>
                <c:if test="${empty subject}">
                    <ib:message key="(No subject)" var="subject"/>
                </c:if>
                
                <c:set var="mailUrl" value="showEmail.jsp?folder=${folderName}&messageID=${email.messageID}"/>
                
                <c:choose>
                    <c:when test="${email.new}"><c:set var="fontWeight" value="bold"/></c:when>
                    <c:otherwise><c:set var="fontWeight" value="normal"/></c:otherwise>
                </c:choose>
                
                <td><div class="ellipsis"><a href="${mailUrl}" style="font-weight: ${fontWeight}">${fn:escapeXml(sender)}</a></div></td>
                <td><div class="ellipsis"><a href="${mailUrl}" style="font-weight: ${fontWeight}">${fn:escapeXml(recipient)}</a></div></td>
                <td><div class="ellipsis"><a href="${mailUrl}" style="font-weight: ${fontWeight}">${fn:escapeXml(subject)}</a></div></td>
                <td><a href="${mailUrl}" style="font-weight: ${fontWeight}">${fn:escapeXml(date)}</a></td>
                <td>
                    <a href="deleteEmail.jsp?folder=${folderName}&messageID=${email.messageID}">
                    <img src="images/delete.png" alt="<ib:message key='Delete'/>" title="<ib:message key='Delete this email'/>"/></a>
                </td>
	        </tr>
	    </c:forEach>
	</table>
</div>
</div>

<jsp:include page="footer.jsp"/>