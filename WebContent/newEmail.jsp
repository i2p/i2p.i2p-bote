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

<c:choose>
    <c:when test="${param.action eq 'send'}">
        <jsp:forward page="sendEmail.jsp"/>
    </c:when>
    <c:when test="${fn:startsWith(param.action, 'lookup')}">
        <jsp:forward page="addressBook.jsp">
            <jsp:param name="search" value="true"/>
            <jsp:param name="action" value=""/>
        </jsp:forward>
    </c:when>
</c:choose>

<ib:message key="New Email" var="title"scope="request"/>
<jsp:include page="header.jsp"/>

<div class="main">
    <form action="newEmail.jsp" method="post">
        <table>
            <tr>
                <td>
                    <ib:message key="From:"/>
                </td>
                <td>
                    <select name="sender">
                        <option value="anonymous">Anonymous</option>
                        <c:forEach items="${ib:getIdentities().all}" var="identity">
                            <c:set var="selected" value=""/>
                            <c:if test="${fn:contains(param.sender, identity.key)}">
                                <c:set var="selected" value=" selected"/>
                            </c:if>
                            <c:if test="${empty param.sender && identity.default}">
                                <c:set var="selected" value=" selected"/>
                            </c:if>
                            <option value="${identity.publicName} &lt;${identity.key}&gt;"${selected}>
                                ${identity.publicName}
                                <c:if test="${!empty identity.description}"> - ${identity.description}</c:if>
                            </option>
                        </c:forEach>
                    </select>
                </td>
            </tr>
            
            <c:set var="maxRecipientIndex" value="-1"/>
            <c:forEach var="parameter" items="${ib:getRecipients(param)}">
                <c:set var="isRecipient" value="${fn:startsWith(parameter.key, 'recipient') and !fn:contains(parameter.key, 'Type')}"/>
                <c:if test="${isRecipient && !empty parameter.value}">
                    <c:set var="recipientField" value="${parameter.key}"/>
                    <c:set var="recipient" value="${parameter.value}"/>
                    <c:set var="recipientIndex" value="${fn:substringAfter(recipientField, 'recipient')}"/>
                    <c:if test="${recipientIndex gt maxRecipientIndex}">
                        <c:set var="maxRecipientIndex" value="${recipientIndex}"/>
                    </c:if>
                    <tr><td>
                        <c:set var="recipientTypeField" value="recipientType${recipientIndex}"/>
                        <c:set var="recipientType" value="${param[recipientTypeField]}"/>
                        <select name="${recipientTypeField}">
                            <c:set var="toSelected" value="${recipientType eq 'to' ? ' selected' : ''}"/>
                            <c:set var="ccSelected" value="${recipientType eq 'cc' ? ' selected' : ''}"/>
                            <c:set var="bccSelected" value="${recipientType eq 'bcc' ? ' selected' : ''}"/>
                            <c:set var="replytoSelected" value="${recipientType eq 'replyto' ? ' selected' : ''}"/>
                            <option value="to"${toSelected}>To:</option>
                            <option value="cc"${ccSelected}>CC:</option>
                            <option value="bcc"${bccSelected}>BCC:</option>
                            <option value="replyto"${replytoSelected}>Reply To:</option>
                        </select>
                    </td><td>
                        <input type="text" size="80" name="${recipientField}" value="${recipient}"/>
                    </td></tr>
                </c:if>
            </c:forEach>
            <c:if test="${!empty param.selectedContact}">
                <c:forEach var="destination" items="${paramValues.selectedContact}">
                    <c:set var="maxRecipientIndex" value="${maxRecipientIndex+1}"/>
                    <tr><td>
                        <select name="recipientType${maxRecipientIndex}">
                            <option value="to"><ib:message key="To:"/></option>
                            <option value="cc"><ib:message key="CC:"/></option>
                            <option value="bcc"><ib:message key="BCC:"/></option>
                            <option value="replyto"><ib:message key="Reply To:"/></option>
                        </select>
                    </td><td>
                        <input type="text" size="80" name="recipient${maxRecipientIndex}" value="${destination}"/>
                    </td></tr>
                </c:forEach>
            </c:if>
            <c:set var="maxRecipientIndex" value="${maxRecipientIndex+1}"/>
            <tr><td>
                <select name="recipientType${maxRecipientIndex}">
                    <option value="to"><ib:message key="To:"/></option>
                    <option value="cc"><ib:message key="CC:"/></option>
                    <option value="bcc"><ib:message key="BCC:"/></option>
                    <option value="replyto"><ib:message key="Reply To:"/></option>
                </select>
            </td><td>
                <input type="text" size="80" name="recipient${maxRecipientIndex}"/>
            </td></tr>

            <tr>
                <td/>
                <td style="text-align: right;">
                    <button type="submit" name="action" value="addRecipient" disabled="disabled">+</button>
                    <button type="submit" name="action" value="lookup0"><ib:message key="Addr. Book..."/></button>
                </td>
            </tr>
            <tr>
                <td valign="top"><br/><ib:message key="Subject:"/></td>
                <td><input type="text" size="80" name="subject" value="${param.subject}"/></td>
            </tr>
            <tr>
                <td valign="top"><br/><ib:message key="Message:"/></td>
                <td>
                    <textarea rows="30" cols="80" name="message"><c:if test="${!empty param.quoteMsgId}">
<%-- The following lines are not indented because the indentation would show up as blank chars on the textarea --%>
<c:set var="origEmail" value="${ib:getEmail(param.quoteMsgFolder, param.quoteMsgId)}"/>
<ib:shortSenderName sender="${origEmail.sender}"/> wrote:
<ib:quote text="${origEmail.text}"/></c:if><c:if test="${empty param.quoteMsgId}">${param.message}</c:if></textarea>
                </td>
            </tr>
            <tr>
                <td colspan=3 align="center">
                    <button type="submit" name="action" value="send"><ib:message key="Send"/></button>
                    <button type="submit" name="action" disabled="disabled"><ib:message key="Save"/></button>
                </td>
            </tr>
        </table>
    </form>
</div>

<jsp:include page="footer.jsp"/>