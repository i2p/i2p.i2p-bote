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

<jsp:useBean id="jspHelperBean" class="i2p.bote.web.JSPHelper"/>
<c:set var="configuration" value="${jspHelperBean.configuration}"/>
<c:if test="${param.action eq 'save'}">
    <jsp:setProperty name="configuration" property="autoMailCheckEnabled" value="${param.autoMailCheckEnabled eq 'on' ? 'true' : 'false'}"/>
    <jsp:setProperty name="configuration" property="mailCheckInterval" value="${param.mailCheckInterval}"/>
    <jsp:setProperty name="configuration" property="language" value="${param.language}"/>
    <jsp:setProperty name="configuration" property="hideLocale" value="${param.hideLocale eq 'on' ? 'true' : 'false'}"/>
    <jsp:setProperty name="configuration" property="includeSentTime" value="${param.includeSentTime eq 'on' ? 'true' : 'false'}"/>
    <jsp:setProperty name="configuration" property="numStoreHops" value="${param.numStoreHops}"/>
    <jsp:setProperty name="configuration" property="relayMinDelay" value="${param.minDelay}"/>
    <jsp:setProperty name="configuration" property="relayMaxDelay" value="${param.maxDelay}"/>
    <jsp:setProperty name="configuration" property="gatewayEnabled" value="${param.gatewayEnabled eq 'on' ? 'true' : 'false'}"/>
    <jsp:setProperty name="configuration" property="gatewayDestination" value="${param.gatewayDestination}"/>
    <ib:saveConfiguration/>
</c:if>

<ib:message key="Settings" var="title" scope="request"/>
<jsp:include page="header.jsp"/>

<div class="main">
    <h2>
        <ib:message key="Settings"/>
    </h2>
    <br/>
    
    <form action="settings.jsp" method="post">
        <input type="hidden" name="action" value="save"/>
        
        <%-- Auto mail checking --%>
        <c:set var="checked" value="${configuration.autoMailCheckEnabled ? ' checked' : ''}"/>
        <input type="checkbox"${checked} name="autoMailCheckEnabled"/>
        <ib:message key="Check for mail every {0} minutes">
            <ib:param><input type="text" name="mailCheckInterval" size="3" value="${configuration.mailCheckInterval}"/></ib:param>
        </ib:message>
        <br/>
        
        <%-- Relays --%>
        <ib:message key="Use relays when sending mail:"/>
        <select name="numStoreHops">
            <c:set var="selected" value=""/>
            <c:if test="${configuration.numStoreHops eq 0}">
                <c:set var="selected" value=" selected"/>
            </c:if>
            <option value=""${selected}><ib:message key="0 (off)"/></option>
            <c:forEach var="hops" begin="1" end="3">
                <c:set var="selected" value=""/>
                <c:if test="${hops eq configuration.numStoreHops}">
                    <c:set var="selected" value=" selected"/>
                </c:if>
                <option value="${hops}"${selected}>${hops}</option>
            </c:forEach>
        </select>
        <br/>
        
        <ib:message key="Delay per relay hop: Between "/>
        <input type="text" name="minDelay" size="3" value="${configuration.relayMinDelay}"/>
        <ib:message key=" and "/>
        <input type="text" name="maxDelay" size="3" value="${configuration.relayMaxDelay}"/>
        <ib:message key=" minutes "/>
        <br/>
        
        <%-- Gateway --%>
        <c:set var="checked" value="${configuration.gatewayEnabled ? ' checked' : ''}"/>
        <input type="checkbox"${checked} name="gatewayEnabled"/>
        <ib:message key="Use a gateway when sending to non-I2P email addresses"/>
        <br/>
        <ib:message key="Email Destination of the gateway:"/>
        <input type="text" name="gatewayDestination" size="50" value="${configuration.gatewayDestination}"/>
        <br/>
        
        <%-- Locale --%>
        <ib:message key="Language:"/>
        <select name="language">
            <c:set var="selected" value=""/>
            <c:if test="${empty configuration.language}">
                <c:set var="selected" value=" selected"/>
            </c:if>
            <option value=""${selected}><ib:message key="Default"/></option>
            <c:forEach var="locale" items="${configuration.allLocales}">
                <c:set var="selected" value=""/>
                <c:if test="${locale.language eq configuration.language}">
                    <c:set var="selected" value=" selected"/>
                </c:if>
                <option value="${locale.language}"${selected}><ib:localizedLanguageName locale="${locale}"/></option>
            </c:forEach>
        </select>
        <c:set var="checked" value="${configuration.hideLocale ? ' checked' : ''}"/>
        <input type="checkbox"${checked} name="hideLocale"/>
        <ib:message key="Use English for text added to outgoing email ('Re:', 'wrote:', etc.)"/>
        <br/>

        <%-- Send time --%>
        <c:set var="checked" value="${configuration.includeSentTime ? ' checked' : ''}"/>
        <input type="checkbox"${checked} name="includeSentTime"/>
        <ib:message key="Include send time in outgoing emails"/>
        
        <p/>
        <button type="submit"><ib:message key="Save"/></button>
    </form> 
</div>

<jsp:include page="footer.jsp"/>