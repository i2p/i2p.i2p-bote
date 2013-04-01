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

<ib:requirePassword>
<c:set var="email" value="${ib:getEmail(param.folder, param.messageID)}"/>

<c:if test="${fn:toLowerCase(param.folder) ne 'outbox'}">
    <ib:setEmailRead folder="${ib:getMailFolder(param.folder)}" messageId="${param.messageID}" read="true"/>
</c:if>

<c:set var="title" value="${email.subject}" scope="request"/>
<jsp:include page="header.jsp"/>

<div class="emailtext">
    <div class="email-form-label"><ib:message key="From:"/></div>
    <div class="show-email-value">
        <c:if test="${email.anonymous}">
            <ib:message key="Anonymous"/>
        </c:if>
        <c:if test="${!email.anonymous}">
            <ib:address address="${email.sender}"/>
        </c:if>
    </div>

    <div class="email-form-label"><ib:message key="Signature:"/></div>
    <div class="show-email-value">
        <c:if test="${email.signatureValid}"><ib:message key="Valid"/></c:if>
        <c:if test="${!email.signatureValid}">
            <c:if test="${email.anonymous}">
                <ib:message key="N/A (sender is anonymous)"/>
            </c:if>
            <c:if test="${!email.anonymous}">
                <div class="show-email-invalid-sig"><ib:message key="Invalid or missing"/></div>
            </c:if>
        </c:if>
    </div>

    <c:forEach var="replyToAddress" varStatus="status" items="${email.replyToAddresses}">
        <div class="email-form-label"><ib:message key="Reply To:"/></div>
        <div class="show-email-value"><ib:address address="${replyToAddress}"/></div>
    </c:forEach>
    
    <c:forEach var="toAddress" varStatus="status" items="${email.toAddresses}">
        <div class="email-form-label"><ib:message key="To:"/></div>
        <div class="show-email-value"><ib:address address="${toAddress}"/></div>
    </c:forEach>
    
    <c:forEach var="ccAddress" varStatus="status" items="${email.CCAddresses}">
        <div class="email-form-label"><ib:message key="CC:"/></div>
        <div class="show-email-value"><ib:address address="${ccAddress}"/></div>
    </c:forEach>
    
    <c:forEach var="bccAddress" varStatus="status" items="${email.BCCAddresses}">
        <div class="email-form-label"><ib:message key="BCC:"/></div>
        <div class="show-email-value"><ib:address address="${bccAddress}"/></div>
    </c:forEach>
    
    <div class="email-form-label"><ib:message key="Sent:"/></div>
    <div class="show-email-value"><ib:printDate date="${email.sentDate}" timeStyle="full"/></div>
    
    <div class="email-form-label"><ib:message key="Received:"/></div>
    <div class="show-email-value"><ib:printDate date="${email.receivedDate}" timeStyle="full"/></div>
    
    <div class="email-form-label"><ib:message key="Subject:"/></div>
    <div class="show-email-value">${fn:escapeXml(email.subject)}</div>
    
    <div class="email-form-label"><ib:message key="Message:"/></div>
    <div class="show-email-value"><ib:formatPlainText text="${fn:escapeXml(email.text)}"/></div>
    
    <div class="email-form-label"><ib:message key="Attachments:"/></div>
    <div class="show-email-value"><ib:showAttachments email="${email}" folder="${param.folder}"/></div>
    
    <c:if test="${param.folder eq 'Outbox'}">
        <div class="email-form-label"><ib:message key="Status:"/></div>
        <div class="show-email-value">${ib:getEmailStatus(email)}</div>
    </c:if>
    
    <br/>
    <div class="show-email-reply">
    <form action="newEmail.jsp" method="post">
        <c:set var="replyDisabled" value="${email.anonymous ? 'disabled=&quot;disabled&quot;' : ''}"/>
        <button type="submit"${replyDisabled}><ib:message key="Reply"/></button>
        <input type="hidden" name="sender" value="${ib:escapeQuotes(ib:getOneLocalRecipient(email))}"/>
        <jsp:useBean id="jspHelperBean" class="i2p.bote.web.JSPHelper"/>
        <input type="hidden" name="recipient0" value="${ib:escapeQuotes(ib:getReplyAddress(email, jspHelperBean.identities))}"/>
        <input type="hidden" name="recipientType0" value="to"/>
        
        <ib:message key="Re:" var="responsePrefix" hide="true"/>
        <c:set var="responsePrefix" value="${responsePrefix} "/>
        <c:if test="${fn:startsWith(email.subject, responsePrefix)}">
            <c:set var="responsePrefix" value=""/>
        </c:if>
        <input type="hidden" name="subject" value="${responsePrefix}${ib:escapeQuotes(email.subject)}"/>
        
        <input type="hidden" name="quoteMsgFolder" value="${param.folder}"/>
        <input type="hidden" name="quoteMsgId" value="${param.messageID}"/>
    </form>
    </div>
    <div class="show-email-delete">
    <form action="deleteEmail.jsp" method="post">
        <button type="submit"><ib:message key="Delete"/></button>
        <input type="hidden" name="folder" value="${param.folder}"/>
        <input type="hidden" name="messageID" value="${email.messageID}"/>
    </form>
    </div>
</div>
</ib:requirePassword>

<jsp:include page="footer.jsp"/>