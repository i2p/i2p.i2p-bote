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
    This page behaves differently depending on the "select" boolean parameter.
    If select is true, the user can select contacts and add them as recipients.
    If select is false, the user can view and edit the address book.
--%>
<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">

<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="ib" uri="I2pBoteTags" %>

<ib:message key="Address Book" var="title" scope="request"/>
<jsp:include page="header.jsp"/>

<ib:requirePassword>
<div class="main">
    <jsp:useBean id="jspHelperBean" class="i2p.bote.web.JSPHelper"/>
    <c:set var="contacts" value="${jspHelperBean.addressBook.all}"/>
    
    <h2>
        <c:if test="${!param.select}"><ib:message key="Private Address Book"/></c:if>
        <c:if test="${param.select && !empty contacts}"><ib:message key="Select One or More Entries"/></c:if>
    </h2>

    <c:if test="${empty contacts}">
        <ib:message key="The address book is empty."/>
    </c:if>
    
    <c:if test="${param.select}">
        <form action="${param.forwardUrl}" method="POST">
        <ib:copyParams paramsToCopy="${param.paramsToCopy}"/>
    </c:if>
    
    <table>
    <c:if test="${!empty contacts}">
        <tr>
            <c:if test="${param.select}"><th class="header-column-check"></th></c:if>
            <th class="header-column-name"><ib:message key="Name"/></th>
            <th class="header-column-addr-email-dest"><ib:message key="Email Destination"/></th>
            <c:if test="${not param.select}"><th class="header-column-trash"></th></c:if>
        </tr>
    </c:if>
    <c:forEach items="${contacts}" var="contact" varStatus="status">
        <c:set var="backgroundClass" value="even-table-cell"/>
        <c:if test="${status.index%2 != 0}">
            <c:set var="backgroundClass" value="odd-table-cell"/>
        </c:if>
        
        <tr class="${backgroundClass}">
        <c:if test="${param.select}">
            <td>
                <input type="checkbox" name="selectedContact" value="${ib:escapeQuotes(contact.name)} &lt;${contact.destination}&gt;"/>
            </td>
        </c:if>
        
        <td>
            <c:if test="${!param.select}">
                <%-- Insert a random number into the request string so others can't see contacts using the CSS history hack --%>
                <a href="editContact.jsp?rnd=${jspHelperBean.randomNumber}&amp;new=false&amp;destination=${contact.destination}">
            </c:if>
                ${contact.name}
            <c:if test="${!param.select}">
                </a>
            </c:if>
        </td>
        <td>
            ${contact.destination}
        </td>
        <c:if test="${!param.select}">
            <td>
                <a href="deleteContact.jsp?destination=${contact.destination}"><img src="${themeDir}/images/delete.png" alt="<ib:message key='Delete'/>" title='<ib:message key='Delete this contact'/>'/></a>
            </td>
        </c:if>
        </tr>
    </c:forEach>
    </table>
    
    <p/>
    <table>
        <c:if test="${!param.select}">
            <tr><td>
                <form action="editContact.jsp" method="POST">
                    <input type="hidden" name="new" value="true"/>
                    <button type="submit" value="New"><ib:message key="New Contact"/></button>
                </form>
            </td></tr>
        </c:if>
        <c:if test="${param.select}">
            <tr><td>
                <c:if test="${!empty contacts}">
                    <button type="submit" value="New"><ib:message key="Add Recipients"/></button>
                </c:if>
                <c:if test="${empty contacts}">
                    <button type="submit" value="New"><ib:message key="Return"/></button>
                </c:if>
            </td></tr>
        </c:if>
    </table>
    
    <c:if test="${param.select}">
        </form>
    </c:if>
    
</div>
</ib:requirePassword>

<jsp:include page="footer.jsp"/>
