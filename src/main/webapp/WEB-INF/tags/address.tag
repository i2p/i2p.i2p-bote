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
<%@ taglib prefix="csrf" uri="http://www.owasp.org/index.php/Category:OWASP_CSRFGuard_Project/Owasp.CsrfGuard.tld" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib prefix="ib" uri="I2pBoteTags" %>

<% jspContext.setAttribute("newline", "\n"); %>

<c:set var="address" value="${ib:getNameAndDestination(address)}"/>

<div>
    <c:set var="emailDestination" value="${ib:extractEmailDestination(address)}"/>
    <c:set var="name" value="${ib:extractName(address)}"/>
    
    <csrf:form action="editContact.jsp" method="POST">
        <input type="hidden" name="new" value="true"/>
        <input type="hidden" name="destination" value="${emailDestination}"/>
        <input type="hidden" name="name" value="${ib:escapeQuotes(name)}"/>
    
        <c:if test="${empty emailDestination}">
            ${address}
        </c:if>
        
        <c:if test="${not empty emailDestination}">
	        <%-- Print the shortened address which is always visible--%>
	        <c:set var="shortAdr" value="${fn:escapeXml(name)}"/>
	        <c:if test="${!empty name}">
	            <c:set var="shortAdr" value="${shortAdr} &lt;"/>
	        </c:if>
	        <c:set var="shortAdr" value="${shortAdr}${fn:substring(emailDestination, 0, 10)}..."/>
	        <c:if test="${!empty name}">
	            <c:set var="shortAdr" value="${shortAdr}&gt;"/>
	        </c:if>
	        ${shortAdr}
	        
	        <%-- Print the full email destination and the button only when expanded --%>
            <ib:expandable>
                <%-- put wbr tags in the address so the lines don't get too long --%>
                <c:set var="wbrDest" value=""/>
                <c:forEach begin="0" end="${fn:length(emailDestination)-1}" step="5" var="i">
                    <c:set var="wbrDest" value="${wbrDest}${fn:substring(emailDestination, i, i+5)}<wbr/>"/>
                </c:forEach>
    
                <strong><ib:message key="Email Destination: "/></strong> ${wbrDest}
                <c:if test="${!empty emailDestination}">
                    <c:set var="disabled" value="${ib:isKnown(emailDestination) ? 'disabled=&quot; disabled&quot; title=&quot;The Email Destination already exists in the address book.&quot;' : ''}"/>
                    <br/>
                    <button type="submit"${disabled}><ib:message key="Add to Address Book"/></button>
                </c:if>
            </ib:expandable>
        </c:if>
    </csrf:form>
</div>
