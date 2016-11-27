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

<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="csrf" uri="http://www.owasp.org/index.php/Category:OWASP_CSRFGuard_Project/Owasp.CsrfGuard.tld" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib prefix="ib" uri="I2pBoteTags" %>

<ib:message key="More..." var="moreText"/>
<ib:message key="Less..." var="lessText"/>

<ib:requirePassword>
<jsp:useBean id="jspHelperBean" class="i2p.bote.web.JSPHelper"/>

<c:choose>
    <c:when test="${param.createNew}">
        <ib:message key="New Email Identity" var="title" scope="request"/>
        <ib:message key="Create" var="commitAction"/>
        <c:set var="publicName" value="${param.publicName}"/>
        <c:set var="description" value="${param.description}"/>
    </c:when>
    <c:otherwise>
        <ib:message key="Edit Email Identity" var="title" scope="request"/>
        <ib:message key="Save" var="commitAction"/>
        <c:set var="key" value="${param.key}"/>
        <c:set var="identity" value="${ib:getIdentity(key)}"/>
        <c:set var="publicName" value="${ib:escapeQuotes(identity.publicName)}"/>
        <c:set var="description" value="${ib:escapeQuotes(identity.description)}"/>
    </c:otherwise>
</c:choose>

<jsp:include page="header.jsp"/>

    <h1>${title}</h1>
    <c:if test="${param.createNew}">
        <ib:message>An Email Identity lets you receive email from other I2P-Bote users.</ib:message>
        <ib:expandable>
            <jsp:include page="identitiesHelp.jsp"/>
        </ib:expandable>
    </c:if>
    <csrf:form name="form" method="POST" action="submitIdentity.jsp">
        <div class="identity-form-label">
            <div class="field-label"><ib:message key="Public Name:"/></div>
            <div class="addtl-text"><ib:message key="(required field, shown to recipients)"/></div>
        </div>
        <div class="identity-form-value">
            <input type="text" size="40" name="publicName" value="${publicName}"/>
        </div>
    
        <div class="identity-form-label">
            <div class="field-label"><ib:message key="Description:"/></div>
            <div class="addtl-text"><ib:message key="(optional, kept private)"/></div>
        </div>
        <div class="identity-form-value">
            <input type="text" size="40" name="description" value="${description}"/>
        </div>
    
        <div class="identity-form-label">
            <div class="field-label"><ib:message key="Picture:"/></div>
            <div class="addtl-text"><ib:message key="(published to the address directory)"/></div>
        </div>
        <div class="identity-form-picture">
            <c:if test="${not empty identity.picture}">
                <img src="data:${identity.pictureType};base64,${identity.pictureBase64}"/>
            </c:if>
            <c:if test="${empty identity.picture}">
                &nbsp;<br/>&nbsp;<br/>
            </c:if>
        </div>
    
        <div class="identity-form-label">
            <div class="field-label"><ib:message key="Text:"/></div>
            <div class="addtl-text"><ib:message key="(published to the address directory)"/></div>
        </div>
        <div class="identity-form-text"><c:if test="${not empty identity.text}">${fn:escapeXml(identity.text)}</c:if><c:if test="${empty identity.text}">&nbsp;</c:if></div>
    
        <div class="identity-form-label">
            <div class="field-label"><ib:message key="Encryption:"/></div>
            <c:if test="${param.createNew}">
                <div class="addtl-text"><ib:message key="(If unsure, leave the default)"/></div>
            </c:if>
        </div>
        <div class="identity-form-value">
            <c:if test="${param.createNew}">
                <c:set var="selectedCryptoImplId" value="${param.cryptoImpl}"/>
                <c:if test="${empty param.cryptoImpl}">
                    <c:set var="selectedCryptoImplId" value="2"/>
                </c:if>
                <select name="cryptoImpl">
                    <c:forEach items="${jspHelperBean.cryptoImplementations}" var="cryptoImpl">
                        <c:set var="selected" value=""/>
                        <c:if test="${selectedCryptoImplId eq cryptoImpl.id}">
                            <c:set var="selected" value=" selected"/>
                        </c:if>
                        <option value="${cryptoImpl.id}"${selected}>
                            ${cryptoImpl.name}
                        </option>
                    </c:forEach>
                </select>
            </c:if>
            <c:if test="${not param.createNew}">
                <c:set var="cryptoImpl" value="${ib:getCryptoImplementation(identity.cryptoImpl.id)}"/>
                ${cryptoImpl.name}
            </c:if>
        </div>

        <c:if test="${param.createNew}">
            <div class="identity-form-label">
                <ib:message key="Vanity Destination:"/>
                <div class="addtl-text"><ib:message key="(2 chars takes seconds, 3 takes minutes, etc.)"/></div>
            </div>
            <div class="identity-form-value">
                <input type="text" size="10" name="vanityPrefix" value="${vanityPrefix}"/>
            </div>
        </c:if>
    
        <c:if test="${not empty param.key}">
            <div class="identity-form-label">
                <ib:message key="Email Destination:"/>
            </div>
            <div class="identity-form-value">
                <div class="destination">${param.key}</div>
            </div>
        </c:if>
    
        <c:if test="${not empty param.key}">
            <div class="identity-form-label">
                <ib:message key="Fingerprint:"/>
            </div>
            <c:set var="uiLocaleCode" value="${jspHelperBean.language}"/>
            <c:set var="fingerprint" value="${ib:getIdentityFingerprint(identity, uiLocaleCode)}"/>
            <div class="identity-form-value">
                <c:if test="${not empty fingerprint}">
                    ${fingerprint}
                    <ib:expandable>
                        <c:forEach items="${jspHelperBean.wordListLocales}" var="localeCode">
                            <c:if test="${localeCode ne uiLocaleCode}">
                                &nbsp;&nbsp;<b>${localeCode}</b>: ${ib:getIdentityFingerprint(identity, localeCode)}<br/>
                            </c:if>
                        </c:forEach>
                    </ib:expandable>
                </c:if>
            </div>
            <c:if test="${empty fingerprint}">
                &nbsp;
            </c:if>
        </c:if>
        
        <div class="identity-form-label">
            <ib:message key="Default Identity:"/>
        </div>
        <div class="identity-form-checkbox">
            <c:if test="${jspHelperBean.identities.size le 1 and not param.createNew}">
                <c:set var="disabled" value="disabled='disabled'"/>
            </c:if>
            <c:if test="${param.defaultIdentity or not empty disabled}">
                <c:set var="checked" value="checked='checked'"/>
            </c:if>
            <input type="checkbox" name="defaultIdentity" ${disabled} ${checked}/>
        </div>

        <div class="identity-form-label">
            <ib:message key="Include in global check:"/>
        </div>
        <div class="identity-form-checkbox">
            <c:if test="${empty identity or identity.includeInGlobalCheck}">
                <c:set var="checked" value="checked='checked'"/>
            </c:if>
            <input type="checkbox" name="includeInGlobalCheck" ${checked}/>
        </div>
        
        <c:if test="${not empty param.key}">
            <div class="identity-form-label">
                <div class="field-label"><ib:message key="Private keys:"/></div>
                <div class="warning addtl-text"><ib:message key="(Never reveal to anyone!)"/></div>
            </div>
            <div class="identity-form-value">
                <a href="showFullIdentity.jsp?key=${key}"><ib:message key="Show"/></a>
            </div>
        </c:if>
        
        <p><br/></p>
        <div class="identity-buttons">
            <input type="hidden" name="createNew" value="${param.createNew}"/>
            <input type="hidden" name="key" value="${param.key}"/>
            <button name="action" value="${commitAction}">${commitAction}</button>
            <button name="action" value="cancel"><ib:message key="Cancel"/></button>
        </div>
        <c:if test="${not param.createNew}">
            <div class="identity-delete">
                <jsp:include page="getStatus.jsp"/>
                <c:if test="${connStatus ne CONNECTED}">
                    <ib:message var="publishTooltip" key="Cannot publish until connected."/>
                    <c:set var="publishDisabled" value=" disabled='disabled' title='${publishTooltip}'"/>
                </c:if>
                <button name="action" value="publish"${publishDisabled}><ib:message key="Publish"/></button>
                <button name="action" value="delete" title="<ib:message key='Delete this identity'/>"><ib:message key="Delete"/></button>
            </div>
        </c:if>
    </csrf:form>

    <script type="text/javascript" language="JavaScript">
        document.forms['form'].elements['publicName'].focus();
    </script>
</ib:requirePassword>

<jsp:include page="footer.jsp"/>
