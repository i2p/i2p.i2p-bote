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

<jsp:include page="header.jsp">
    <jsp:param name="title" value="Identities"/>
</jsp:include>

<div class="infoMessage">
    ${param.infoMessage}
</div>

<div class="main">
    <h2>
        Email Identities
    </h2>

    <c:if test="${empty ib:getIdentities().all}">
        No email identities are defined.
    </c:if>
    
    <div class="identities">
    <table>
    <tr>
        <th>Description</th>
        <th>Public Name</th>
        <th>Email Address</th>
        <th>Key</th>
        <th style="width: 20px; padding: 0px"></th>
    </tr>
    <c:forEach items="${ib:getIdentities().all}" var="identity">
        <tr>
        <td style="width: 100px;">
            <div class="ellipsis">
                <a href="editIdentity.jsp?new=false&key=${identity.key}&publicName=${identity.publicName}&description=${identity.description}&emailAddress=${identity.emailAddress}">
                    ${identity.publicName}
                </a>
            </div>
        </td>
        <td style="width: 150px;">
            <div class="ellipsis">
                ${identity.description}
            </div>
        </td>
        <td style="width: 150px;">
            <div class="ellipsis">
                ${identity.emailAddress}
            </div>
        </td>
        <td style="width: 100px;">
            <div class="ellipsis">
                ${identity.key}
            </div>
        </td>
        <td>
            <a href="deleteIdentity.jsp?key=${identity.key}"><img src="images/delete.png" alt="Delete" title="Delete this identity"/></a>
        </td>
        </tr>
    </c:forEach>
    </table>
    </div>
    
    <p/>
    <form action="editIdentity.jsp?new=true" method="POST">
        <button type="submit" value="New">New Identity</button>
    </form>
</div>

<jsp:include page="footer.jsp"/>