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

<ib:requirePassword>
<c:set var="email" value="${ib:getEmail(param.folder, param.messageID)}"/>

<c:choose><c:when test="${empty email}">
    <c:choose>
        <c:when test="${param.folder == 'Outbox'}">
            <ib:message key="The email could not be found in this folder. It was probably moved to the Sent folder." var="errorMessage" scope="request"/>
        </c:when>
        <c:otherwise>
            <ib:message key="The email could not be found in this folder." var="errorMessage" scope="request"/>
        </c:otherwise>
    </c:choose>
    <jsp:forward page="folder.jsp">
        <jsp:param name="path" value="${param.folder}"/>
    </jsp:forward>
</c:when><c:otherwise>

<c:if test="${fn:toLowerCase(param.folder) ne 'outbox'}">
    <ib:setEmailRead folder="${ib:getMailFolder(param.folder)}" messageId="${param.messageID}" read="true"/>
</c:if>

<c:set var="title" value="${email.subject}" scope="request"/>
<c:set var="contentClass" value="main emailmain" scope="request"/>
<jsp:include page="header.jsp"/>

<article class="emailtext">
    <div class="email-form-label"><ib:message key="From:"/></div>
    <div class="show-email-value">
        <c:if test="${email.anonymous}">
            <ib:message key="Anonymous"/>
        </c:if>
        <c:if test="${!email.anonymous}">
            <ib:address address="${email.oneFromAddress}"/>
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
        <div class="show-email-value">${ib:getEmailStatusText(email)}</div>
    </c:if>
    
    <br/>
    <div class="show-email-reply">
    <csrf:form action="newEmail.jsp" method="POST">
        <c:set var="replyDisabled" value="${email.anonymous ? 'disabled=&quot;disabled&quot;' : ''}"/>
        <button type="submit"${replyDisabled}><ib:message key="Reply"/></button>
        <input type="hidden" name="nofilter_sender" value="${ib:escapeQuotes(ib:getOneLocalRecipient(email))}"/>
        <jsp:useBean id="jspHelperBean" class="i2p.bote.web.JSPHelper"/>
        <input type="hidden" name="nofilter_recipient0" value="${ib:escapeQuotes(ib:getReplyAddress(email, jspHelperBean.identities))}"/>
        <input type="hidden" name="recipientType0" value="to"/>
        
        <ib:message key="Re:" var="responsePrefix" hide="true"/>
        <c:set var="responsePrefix" value="${responsePrefix} "/>
        <c:if test="${fn:startsWith(email.subject, responsePrefix)}">
            <c:set var="responsePrefix" value=""/>
        </c:if>
        <input type="hidden" name="subject" value="${responsePrefix}${ib:escapeQuotes(email.subject)}"/>
        
        <input type="hidden" name="quoteMsgFolder" value="${param.folder}"/>
        <input type="hidden" name="quoteMsgId" value="${param.messageID}"/>
    </csrf:form>
    </div>
    <div class="show-email-delete">
    <csrf:form action="deleteEmail.jsp" method="POST">
        <button type="submit"><ib:message key="Delete"/></button>
        <input type="hidden" name="folder" value="${param.folder}"/>
        <input type="hidden" name="messageID" value="${email.messageID}"/>
    </csrf:form>
    </div>
</article>

</c:otherwise></c:choose>
</ib:requirePassword>

<jsp:include page="footer.jsp"/>
