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
    Makes a hidden input field for each item in the "paramsToCopy" string.
    Prefix matching is supported (parameter names can end with an asterisk). 
--%>

<%@ attribute name="paramsToCopy" required="true" description="Comma-separated parameter names" %>
<%@ attribute name="paramsToExclude" required="false" description="Comma-separated parameter names to exclude" %>

<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib prefix="ib" uri="I2pBoteTags" %>

<c:set var="paramArray" value="${fn:split(paramsToCopy, ',')}"/>
<c:set var="excludedArray" value="${fn:split(paramsToExclude, ',')}"/>

<c:forEach var="parameter" items="${param}">
    <%-- Test if the parameter matches one of the excluded params --%>
    <c:set var="exactMatch" value="false"/>
    <c:set var="prefixMatch" value="false"/>
    <c:set var="isExcluded" value="false"/>
    <c:forEach var="paramToExclude" items="${excludedArray}">
        <c:if test="${parameter.key eq paramToExclude}">
            <c:set var="exactMatch" value="true"/>
        </c:if>
        <c:if test="${fn:endsWith(paramToExclude, '*') and fn:startsWith(parameter.key, fn:substringBefore(paramToExclude, '*'))}">
            <c:set var="prefixMatch" value="true"/>
        </c:if>
    </c:forEach>

    <%-- If not excluded, test for a match with paramArray --%>
    <c:if test="${not exactMatch and not prefixMatch}">
        <c:forEach var="paramToCopy" items="${paramArray}">
            <c:if test="${parameter.key eq paramToCopy}">
                <c:set var="exactMatch" value="true"/>
            </c:if>
            <c:if test="${fn:endsWith(paramToCopy, '*') and fn:startsWith(parameter.key, fn:substringBefore(paramToCopy, '*'))}">
                <c:set var="prefixMatch" value="true"/>
            </c:if>
        </c:forEach>
        <c:if test="${exactMatch or prefixMatch}">
            <input type="hidden" name="${parameter.key}" value="${ib:escapeQuotes(parameter.value)}"/>
        </c:if>
    </c:if>
</c:forEach>