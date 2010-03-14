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
    <c:when test="${param.new}">
        <ib:message key="New Contact" var="title"/>
        <c:set var="title" value="${title}" scope="request"/>
        <ib:message key="Add" var="action"/>
        <c:set var="commitAction" value="${action}"/>
    </c:when>
    <c:otherwise>
        <ib:message key="Edit Contact" var="title"/>
        <c:set var="title" value="${title}" scope="request"/>
        <ib:message key="Add" var="action"/>
        <c:set var="commitAction" value="${action}"/>
    </c:otherwise>
</c:choose>
<jsp:include page="header.jsp"/>

<div class="main">
    <form name="form" action="saveContact.jsp" method="post">
        <table>
            <tr>
                <td>
                    <div style="font-weight: bold;"><ib:message key="Email Destination"/></div>
                    <c:if test="${empty param.destination}">
                        <div style="font-size: 0.8em;"><ib:message key="(required field, must be 512 characters)"/></div>
                    </c:if>
                </td>
                <td>
                    <input type="text" size="80" name="destination" value="${param.destination}"/>
                </td>
            </tr>
            <tr>
                <td>
                    <div style="font-weight: bold;"><ib:message key="Name:"/></div>
                    <div style="font-size: 0.8em;"><ib:message key="(required field)"/></div>
                </td>
                <td>
                    <input type="text" size="25" name="name" value="${param.name}"/>
                </td>
            </tr>
        </table>
        <input type="hidden" name="destination" value="${param.destination}"/>
        <input type="submit" name="action" value="${commitAction}"/>
        <input type="submit" name="action" value="<ib:message key="Cancel"/>"/>
    </form>

    <script type="text/javascript" language="JavaScript">
        document.forms['form'].elements['name'].focus();
    </script>
</div>

<jsp:include page="footer.jsp"/>