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
<%@ taglib prefix="ib" uri="I2pBoteTags" %>

<jsp:include page="getStatus.jsp"/>

<ib:message key="Network" var="title" scope="request"/>
<c:set var="navSelected" value="network" scope="request"/>
<jsp:include page="header.jsp"/>

    <c:choose>
        <c:when test="${connStatus==NOT_STARTED || connStatus==DELAY}">
            <span class="subheading">
                <ib:message key="Network information is not available because I2P-Bote hasn't started connecting to the network yet."/>
            </span>
        </c:when>
        <c:otherwise>
            <jsp:useBean id="jspHelperBean" class="i2p.bote.web.JSPHelper"/>
            <p><span class="subheading"><ib:message key="Local destination:"/></span>
            ${jspHelperBean.localDestination}</p>
            <ib:peerInfo/>
        </c:otherwise>
    </c:choose>

<jsp:include page="footer.jsp"/>