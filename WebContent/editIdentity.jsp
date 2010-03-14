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
        <ib:message key="New Email Identity" var="title"/>" scope="request"/>
        <ib:message key="Create" var="commitAction"/>"/>
    </c:when>
    <c:otherwise>
        <ib:message key="Edit Email Identity" var="title"/>
        <ib:message key="Save" var="commitAction"/>
    </c:otherwise>
</c:choose>

<jsp:include page="header.jsp"/>

<div class="errorMessage">
    ${fn:escapeXml(param.errorMessage)}
</div>

<div class="main">
    <form name="form" action="saveIdentity.jsp">
        <table>
            <tr>
                <td>
                    <div style="font-weight: bold;"><ib:message key="Public Name:"/></div>
                    <div style="font-size: 0.8em;"><ib:message key="(required field, shown to recipients)"/></div>
                </td>
                <td>
                    <input type="text" size="25" name="publicName" value="${param.publicName}"/>
                </td>
            </tr>
            <tr>
                <td>
                    <div style="font-weight: bold;"><ib:message key="Description:"/></div>
                    <div style="font-size: 0.8em;"><ib:message key="(optional, kept private)"/></div>
                </td>
                <td>
                    <input type="text" size="25" name="description" value="${param.description}"/>
                </td>
            </tr>
            <tr>
                <td>
                    <div style="font-weight: bold;"><ib:message key="Email Address:"/></div>
                    <div style="font-size: 0.8em;"><ib:message key="(optional)"/></div>
                </td>
                <td>
                    <input type="text" size="50" name="emailAddress" value="${param.emailAddress}"/>
                </td>
            </tr>
            <c:if test="${!empty param.key}">
            <tr>
                <td style="font-weight: bold; vertical-align: top;">
                    <ib:message key="Email Destination:"/>
                </td>
                <td>
                    <textarea cols="64" rows="9" readonly="yes" wrap="soft" class="destinationtextarea">${param.key}</textarea>
                </td>
            </tr>
            <tr>
                <td style="font-weight: bold; vertical-align: top;">
                    Default Identity:
                </td>
                <td>
                    <c:if test="${param.isDefault}">
                        <c:set var="checked" value="checked"/>
                    </c:if>
                    <input type="checkbox" name="isDefault" ${checked}/>
                </td>
            </tr>
            </c:if>
        </table>
        <input type="hidden" name="key" value="${param.key}"/>
        <input type="submit" name="action" value="${commitAction}"/>
        <input type="submit" name="action" value="<ib:message key='Cancel'/>"/>
    </form>

    <script type="text/javascript" language="JavaScript">
        document.forms['form'].elements['publicName'].focus();
    </script>
</div>

<jsp:include page="footer.jsp"/>