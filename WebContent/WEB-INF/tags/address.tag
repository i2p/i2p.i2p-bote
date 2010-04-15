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

<%@ attribute name="address" required="true" description="The email address to display" %>

<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>

<% jspContext.setAttribute("newline", "\n"); %>

<%-- If the address contains a name that is followed by an email destination in angle brackets, put a line break after the name --%>
<c:set var="gtHtml" value="${fn:escapeXml('<')}"/>
<c:set var="newlinePlusGt" value="${newline}${gtHtml}"/>
<c:set var="formattedAddress" value="${fn:escapeXml(address)}"/>
<c:set var="formattedAddress" value="${fn:replace(formattedAddress, gtHtml, newlinePlusGt)}"/>

<%-- if the address contains an email destination, use a textarea; otherwise just print it --%>
<c:if test="${fn:length(formattedAddress) ge 512}">
    <textarea cols="64" rows="9" readonly="yes" wrap="soft" class="nobordertextarea">${formattedAddress}</textarea>
</c:if>
<c:if test="${fn:length(formattedAddress) lt 512}">
    ${formattedAddress}
</c:if>