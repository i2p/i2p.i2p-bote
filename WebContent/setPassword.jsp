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

<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">

<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="ib" uri="I2pBoteTags" %>

<ib:message key="Set Password" var="title" scope="request"/>
<c:if test="${param.action eq 'set'}">
    <c:set var="errorMessage" value="${ib:changePassword(param.old, param.new, param.confirm)}" scope="request"/>
    <c:if test="${empty errorMessage}">
        <ib:message key="The password has been changed." var="infoMessage" scope="request"/>
        <jsp:forward page="index.jsp"/>
    </c:if>
</c:if>

<jsp:include page="header.jsp"/>

<div class="main">
    <h2><ib:message key="Set a new Password"/></h2>
    
    <p>
    <ib:message key="If you have not set a password, leave the old password blank."/>
    </p>
    
    <form action="setPassword.jsp" method="POST">
        <input type="hidden" name="action" value="set"/>
        <table>
            <tr>
                <td><ib:message key="Old password:"/></td>
                <td><input type="password" name="old"/></td>
            </tr><tr>
                <td><ib:message key="New password:"/></td>
                <td><input type="password" name="new"/></td>
            </tr><tr>
                <td><ib:message key="Confirm:"/></td>
                <td><input type="password" name="confirm"/></td>
            </tr>
        </table>
        <ib:message key="OK" var="ok"/>
        <input type="submit" value="${ok}"/>
    </form>
</div>

<jsp:include page="footer.jsp"/>