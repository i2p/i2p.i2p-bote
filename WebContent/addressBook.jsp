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

<%--
    This page behaves differently depending on the "search" boolean parameter.
    If search is true, the user can select contacts and add them as recipients.
    If search is false, the user can view and edit the address book.
--%>
<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">

<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="ib" uri="I2pBoteTags" %>

<c:set var="title" value="Address Book" scope="request"/>
<jsp:include page="header.jsp"/>

<div class="infoMessage">
    ${param.infoMessage}
</div>

<div class="main">
    <c:set var="contacts" value="${ib:getAddressBook().all}"/>
    
    <h2>
        <c:if test="${!param.search}">Private Address Book</c:if>
        <c:if test="${param.search && !empty contacts}">Select One or More Entries</c:if>
    </h2>

    <c:if test="${empty contacts}">
        The address book is empty.
    </c:if>
    
    <div class="addressbook">

    <c:if test="${param.search}">
        <form action="newEmail.jsp" method="POST">
        <c:forEach var="parameter" items="${param}">
            <input type="hidden" name="${parameter.key}" value="${parameter.value}"/>
        </c:forEach>
    </c:if>
    
    <table>
    <c:if test="${!empty contacts}">
        <tr>
            <c:if test="${param.search}"><th style="width: 10px;"></th></c:if>
            <th>Name</th>
            <th>Email Destination</th>
            <th style="width: 20px; padding: 0px"></th>
        </tr>
    </c:if>
    <c:forEach items="${contacts}" var="contact" varStatus="loopStatus">
        <tr>
        <c:if test="${param.search}">
            <td>
                <input type="checkbox" name="selectedContact" value="${contact.name} &lt;${contact.destination}&gt;"/>
            </td>
        </c:if>
        <td style="width: 100px;">
            <div class="ellipsis">
                <c:if test="${!param.search}">
                    <a href="editContact.jsp?new=false&destination=${contact.destination}&name=${contact.name}">
                </c:if>
                    ${contact.name}
                <c:if test="${!param.search}">
                    </a>
                </c:if>
            </div>
        </td>
        <td style="width: 100px;">
            <div class="ellipsis">
                ${contact.destination}
            </div>
        </td>
        <td>
            <c:if test="${!param.search}">
                <a href="deleteContact.jsp?destination=${contact.destination}"><img src="images/delete.png" alt="Delete" title="Delete this contact"/></a>
            </c:if>
        </td>
        </tr>
    </c:forEach>
    </table>
    </div>
    
    <p/>
    <table>
        <c:if test="${!param.search}">
            <tr><td>
                <form action="editContact.jsp?new=true" method="POST">
                    <button type="submit" value="New">New Contact</button>
                </form>
            </td></tr>
        </c:if>
        <c:if test="${param.search}">
            <tr><td>
                <button type="submit" value="New">Add Recipients</button>
            </td></tr>
        </c:if>
    </table>
    
    <c:if test="${param.search}">
        </form>
    </c:if>
    
</div>

<jsp:include page="footer.jsp"/>