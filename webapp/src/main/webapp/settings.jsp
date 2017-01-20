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

<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="csrf" uri="http://www.owasp.org/index.php/Category:OWASP_CSRFGuard_Project/Owasp.CsrfGuard.tld" %>
<%@ taglib prefix="ib" uri="I2pBoteTags" %>

<jsp:useBean id="jspHelperBean" class="i2p.bote.web.JSPHelper"/>
<c:set var="configuration" value="${jspHelperBean.configuration}"/>

<c:set var="action" value="${param.action}" scope="request"/>
<c:if test="${not empty action and pageContext.request.method ne 'POST'}">
    <c:set var="action" value="" scope="request"/>
    <ib:message key="Form must be submitted using POST." var="errorMessage" scope="request"/>
</c:if>

<c:if test="${action eq 'save'}">
    <jsp:setProperty name="configuration" property="autoMailCheckEnabled" value="${param.autoMailCheckEnabled eq 'on' ? 'true' : 'false'}"/>
    <jsp:setProperty name="configuration" property="mailCheckInterval" value="${param.mailCheckInterval}"/>
    <jsp:setProperty name="configuration" property="deliveryCheckEnabled" value="${param.deliveryCheckEnabled eq 'on' ? 'true' : 'false'}"/>
    <jsp:setProperty name="configuration" property="hideLocale" value="${param.hideLocale eq 'on' ? 'true' : 'false'}"/>
    <jsp:setProperty name="configuration" property="includeSentTime" value="${param.includeSentTime eq 'on' ? 'true' : 'false'}"/>
    <jsp:setProperty name="configuration" property="numStoreHops" value="${param.numStoreHops}"/>
    <jsp:setProperty name="configuration" property="relayMinDelay" value="${param.minDelay}"/>
    <jsp:setProperty name="configuration" property="relayMaxDelay" value="${param.maxDelay}"/>
    <jsp:setProperty name="configuration" property="gatewayEnabled" value="${param.gatewayEnabled eq 'on' ? 'true' : 'false'}"/>
    <jsp:setProperty name="configuration" property="gatewayDestination" value="${param.gatewayDestination}"/>
    <jsp:setProperty name="configuration" property="imapPort" value="${param.imapPort}"/>
    <%--
        Use the special property imapEnabled in JSPHelper which starts/stops the IMAP service.
        Do this after setting imapPort so it starts up on the new port if it changed.
     --%>
    <jsp:setProperty name="jspHelperBean" property="imapEnabled" value="${param.imapEnabled eq 'on' ? 'true' : 'false'}"/>
    <jsp:setProperty name="configuration" property="smtpPort" value="${param.smtpPort}"/>
    <%--
        Use the special property smtpEnabled in JSPHelper which starts/stops the SMTP service.
        Do this after setting smtpPort so it starts up on the new port if it changed.
     --%>
    <jsp:setProperty name="jspHelperBean" property="smtpEnabled" value="${param.smtpEnabled eq 'on' ? 'true' : 'false'}"/>
    <jsp:setProperty name="configuration" property="themeUrl" value="${param.theme}"/>
    <ib:saveConfiguration/>
    <ib:message key="Settings have been saved." var="infoMessage" scope="request"/>
    <c:set var="infoMessage" value="${infoMessage}"/>
</c:if>

<ib:message key="Settings" var="title" scope="request"/>
<c:set var="contentClass" value="main settings" scope="request"/>
<c:set var="navSelected" value="settings" scope="request"/>
<jsp:include page="header.jsp"/>

    <h1><ib:message key="Settings"/></h1>

    <csrf:form action="settings.jsp" method="POST">
        <input type="hidden" name="action" value="save"/>

        <h3><ib:message key="General"/></h3>

        <%-- Auto mail checking and delivery checking --%>
        <c:set var="checked" value="${configuration.autoMailCheckEnabled ? ' checked' : ''}"/>
        <input type="checkbox"${checked} name="autoMailCheckEnabled"/>
        <ib:message key="Check for mail every {0} minutes">
            <ib:param><input type="text" name="mailCheckInterval" size="3" value="${configuration.mailCheckInterval}"/></ib:param>
        </ib:message>
        <br/>
        <c:set var="checked" value="${configuration.deliveryCheckEnabled ? ' checked' : ''}"/>
        <input type="checkbox"${checked} name="deliveryCheckEnabled"/>
        <ib:message key="Check delivery status of sent emails"/>
        <br/>

        <%-- IMAP --%>
        <c:set var="checked" value="${configuration.imapEnabled ? ' checked' : ''}"/>
        <input type="checkbox"${checked} name="imapEnabled"/>
        <c:set var="imapPortField" value="<input type='text' name='imapPort' size='5' value='${configuration.imapPort}'/>"/>
        <ib:message key="Enable IMAP on port {0}">
            <ib:param value="${imapPortField}"/>
        </ib:message>
        <br/>

        <%-- SMTP --%>
        <c:set var="checked" value="${configuration.smtpEnabled ? ' checked' : ''}"/>
        <input type="checkbox"${checked} name="smtpEnabled"/>
        <c:set var="smtpPortField" value="<input type='text' name='smtpPort' size='5' value='${configuration.smtpPort}'/>"/>
        <ib:message key="Enable SMTP on port {0}">
            <ib:param value="${smtpPortField}"/>
        </ib:message>
        <br/>

        <%-- UI theme --%>
        <ib:message key="Theme:"/>
        <select name="theme">
            <c:set var="currentTheme" value="${configuration.theme}"/>
            <c:forEach items="${configuration.themes}" var="theme">
                <c:set var="selected" value=""/>
                <c:if test="${theme.id eq currentTheme}">
                    <c:set var="selected" value=" selected"/>
                </c:if>
                <option value="${theme.id}"${selected}>${theme.displayName}</option>
            </c:forEach>
        </select>
        <br/>

        <h3><ib:message key="Privacy"/></h3>

        <%-- Locale --%>
        <c:set var="checked" value="${configuration.hideLocale ? ' checked' : ''}"/>
        <input type="checkbox"${checked} name="hideLocale"/>
        <ib:message key="Use English for text added to outgoing email ('Re:', 'wrote:', etc.)"/>
        <br/>

        <%-- Send time --%>
        <c:set var="checked" value="${configuration.includeSentTime ? ' checked' : ''}"/>
        <input type="checkbox"${checked} name="includeSentTime"/>
        <ib:message key="Include send time in outgoing emails"/>
        <br/>

        <h3><ib:message key="Routing"/></h3>

        <%-- Relays --%>
        <ib:message key="Use relays when sending mail:"/>
        <select name="numStoreHops">
            <c:set var="selected" value=""/>
            <c:if test="${configuration.numStoreHops eq 0}">
                <c:set var="selected" value=" selected"/>
            </c:if>
            <option value="0"${selected}><ib:message key="None"/></option>
            <c:forEach var="hops" begin="1" end="3">
                <c:set var="selected" value=""/>
                <c:if test="${hops eq configuration.numStoreHops}">
                    <c:set var="selected" value=" selected"/>
                </c:if>
                <option value="${hops}"${selected}>${hops}</option>
            </c:forEach>
        </select>
        <br/>
        
        <ib:message key="Delay per relay hop: Between"/>
        <input type="text" name="minDelay" size="3" value="${configuration.relayMinDelay}"/>
        <ib:message key="and"/>
        <input type="text" name="maxDelay" size="3" value="${configuration.relayMaxDelay}"/>
        <ib:message key="minutes"/>
        <br/>
        
        <%-- Gateway --%>
        <c:set var="checked" value="${configuration.gatewayEnabled ? ' checked' : ''}"/>
        <input type="checkbox"${checked} name="gatewayEnabled"/>
        <ib:message key="Use a gateway when sending to non-I2P email addresses"/>
        <br/>
        <ib:message key="Email Destination of the gateway:"/>
        <input type="text" name="gatewayDestination" size="50" value="${configuration.gatewayDestination}"/>
        <br/>

        <p/>
        <button type="submit"><ib:message key="Save"/></button>
    </csrf:form> 

    <p><br/></p>
    <p><a href="setPassword.jsp"><ib:message key="Change Password"/></a></p>

<jsp:include page="footer.jsp"/>
