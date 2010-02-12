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
<%@ taglib prefix="ib" uri="I2pBoteTags" %>

<c:set var="title" value="New Email" scope="request"/>
<jsp:include page="header.jsp"/>

<div class="main">
    <form action="sendEmail.jsp" method="post">
        <table>
            <tr>
                <td>
                    From:
                </td>
                <td>
                    <select name="sender">
                        <option value="anonymous">Anonymous</option>
                        <c:forEach items="${ib:getIdentities().all}" var="identity">
                            <c:set var="selected" value=""/>
                            <c:if test="${identity.key eq param.sender}">
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
            <tr>
                <td>
                    <select name="recipientType0">
                        <option value="to">To:</option>
                        <option value="cc">CC:</option>
                        <option value="bcc">BCC:</option>
                        <option value="reply_to">Reply To:</option>
                    </select>
                </td>
                <td>
                    <input type="text" size="80" name="recipient0" value="${param.recipient0}"/>
                </td>
            </tr>
            <tr>
                <td valign="top"><br/>Subject:</td>
                <td><input type="text" size="80" name="subject" value="${param.subject}"/></td>
            </tr>
            <tr>
                <td valign="top"><br/>Message:</td>
                <td>
                    <textarea rows="30" cols="80" name="message"><c:if test="${!empty param.quoteMsgId}">
<%-- The following lines are not indented because the indentation would show up as blank chars on the textarea --%>
<c:set var="origEmail" value="${ib:getEmail(param.quoteMsgFolder, param.quoteMsgId)}"/>
<ib:shortSenderName sender="${origEmail.sender}"/> wrote:
<ib:quote text="${origEmail.text}"/></c:if></textarea>
                </td>
            </tr>
            <tr>
                <td colspan=2 align="center">
                    <button type="submit">Send</button>
                    <button type="submit" disabled="disabled">Save</button>
                </td>
            </tr>
        </table>
    </form>
</div>

<jsp:include page="footer.jsp"/>