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

<c:set var="email" value="${ib:getEmail(param.folder, param.messageID)}"/>

<ib:setEmailRead folder="${ib:getMailFolder(param.folder)}" messageId="${param.messageID}" read="true"/>

<c:set var="title" value="${email.subject}" scope="request"/>
<jsp:include page="header.jsp"/>

<div class="emailtext">
    <table>
        <tr>
            <td valign="top"><strong><ib:message key="From:"/></strong></td>
            <td>
                <ib:address address="${email.sender}"/>
                <c:set var="senderDestination" value="${ib:extractEmailDestination(email.sender)}"/>
                <c:if test="${!empty senderDestination}">
                    <form action="editContact.jsp?new=true&destination=${senderDestination}&name=${ib:extractName(email.sender)}" method="POST">
                        <c:set var="disabled" value="${empty ib:getContact(senderDestination) ? '' : 'disabled=&quot; disabled&quot; title=&quot;The Email Destination already exists in the address book.&quot;'}"/>
                        <button type="submit"${disabled}><ib:message key="Add to Address Book"/></button>
                    </form>
                </c:if>
            </td>
        </tr>
        <tr>
            <td valign="top"><strong><ib:message key="Signature:"/></strong></td>
            <td>
                <c:if test="${email.signatureValid}"><ib:message key="Valid"/></c:if>
                <c:if test="${!email.signatureValid}"><div style="color: red;"><ib:message key="Invalid"/></div></c:if>
            </td>
        </tr>
        <tr>
            <td valign="top"><strong><ib:message key="To:"/></strong></td>
            <td>
                <c:forEach var="recipient" varStatus="status" items="${email.allRecipients}">
                    <ib:address address="${recipient}"/>
                    <c:if test="${!status.last}">,<p/></c:if>
                </c:forEach>
            </td>
        </tr>
        <tr>
            <td valign="top"><strong><ib:message key="Sent:"/></strong></td>
            <td><ib:emailDate email="${email}" timeStyle="full"/></td>
        </tr>
        <tr>
            <td valign="top"><strong><ib:message key="Subject:"/></strong></td>
            <td>${fn:escapeXml(email.subject)}</td>
        </tr>
        <tr>
            <td valign="top"><strong><ib:message key="Message:"/></strong></td>
            <td><ib:formatPlainText text="${fn:escapeXml(email.text)}"/></td>
        </tr>
        <tr>
            <td colspan="2">
                <table><tr>
                    <td>
                    <form action="newEmail.jsp" method="post">
                        <button type="submit"><ib:message key="Reply"/></button>
                        <input type="hidden" name="sender" value="${ib:getOneLocalRecipient(email)}"/>
                        <input type="hidden" name="recipient0" value="${email.sender}"/>
                        
                        <ib:message key="Re:" var="responsePrefix" hide="true"/>
                        <c:set var="responsePrefix" value="${responsePrefix} "/>
                        <c:if test="${fn:startsWith(email.subject, responsePrefix)}">
                            <c:set var="responsePrefix" value=""/>
                        </c:if>
                        <input type="hidden" name="subject" value="${responsePrefix}${email.subject}"/>
                        
                        <input type="hidden" name="quoteMsgFolder" value="${param.folder}"/>
                        <input type="hidden" name="quoteMsgId" value="${param.messageID}"/>
                    </form>
                    </td><td>
                    <form action="deleteEmail.jsp" method="post">
                        <button type="submit"><ib:message key="Delete"/></button>
                        <input type="hidden" name="folder" value="${param.folder}"/>
                        <input type="hidden" name="messageID" value="${email.messageID}"/>
                    </form>
                    </td>
                </tr></table>
            </td>
        </tr>
    </table>
</div>

<jsp:include page="footer.jsp"/>