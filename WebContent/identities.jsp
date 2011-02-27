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

<c:set var="title" value="Identities" scope="request"/>
<jsp:include page="header.jsp"/>

<jsp:useBean id="jspHelperBean" class="i2p.bote.web.JSPHelper"/>
<div class="main">
    <h2>
        <ib:message key="Email Identities"/>
    </h2>

    <ib:requirePassword>
        <c:set var="identities" value="${jspHelperBean.identities.all}"/>
    </ib:requirePassword>
    <c:if test="${empty identities}">
        <ib:message key="No email identities are defined."/>
    </c:if>
    
    <table class="table">
    <c:if test="${!empty identities}">
        <tr>
            <th style="width: 20px;"><ib:message key="Def."/></th>
            <th><ib:message key="Public Name"/></th>
            <th><ib:message key="Description"/></th>
            <th><ib:message key="Email Address"/></th>
            <th><ib:message key="Email Destination"/></th>
            <th style="width: 20px"></th>
        </tr>
    </c:if>
    <c:forEach items="${identities}" var="identity" varStatus="loopStatus">
        <c:set var="class" value=""/>
        <c:if test="${loopStatus.index%2 != 0}">
            <c:set var="class" value=" class=\"alttablecell\""/>
        </c:if>
        
        <tr>
        <td style="width: 20px; text-align: right;">
            <div${class}>
            <c:if test="${identity.default}">
                <img src="images/asterisk.png"/>
            </c:if>
            </div>
        </td>
        <td style="width: 100px;">
            <div${class}>
                <%-- Insert a random number into the request string so others can't see contacts or identities using the CSS history hack --%>
                <a href="editIdentity.jsp?rnd=${jspHelperBean.randomNumber}&new=false&key=${identity.key}&cryptoImpl=${identity.cryptoImpl}&publicName=${ib:escapeQuotes(identity.publicName)}&description=${ib:escapeQuotes(identity.description)}&emailAddress=${ib:escapeQuotes(identity.emailAddress)}&isDefault=${identity.default}">
                    ${identity.publicName}
                </a>
            </div>
        </td>
        <td style="width: 150px;">
            <div${class}>
                ${identity.description}
            </div>
        </td>
        <td style="width: 150px;">
            <div${class}>
                ${identity.emailAddress}
            </div>
        </td>
        <td style="width: 100px;">
            <div${class}>
                ${identity.key}
            </div>
        </td>
        <td>
            <a href="deleteIdentity.jsp?key=${identity.key}"><img src="images/delete.png" alt="<ib:message key='Delete'/>" title="<ib:message key='Delete this identity'/>"/></a>
        </td>
        </tr>
    </c:forEach>
    </table>
    
    <p/>
    <form action="editIdentity.jsp?new=true" method="POST">
        <button type="submit" value="New"><ib:message key="New Identity"/></button>
    </form>
</div>

<jsp:include page="footer.jsp"/>