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

<%
    pageContext.setAttribute("NOT_STARTED", i2p.bote.network.NetworkStatus.NOT_STARTED, PageContext.REQUEST_SCOPE);
    pageContext.setAttribute("DELAY", i2p.bote.network.NetworkStatus.DELAY, PageContext.REQUEST_SCOPE);
    pageContext.setAttribute("CONNECTING", i2p.bote.network.NetworkStatus.CONNECTING, PageContext.REQUEST_SCOPE);
    pageContext.setAttribute("CONNECTED", i2p.bote.network.NetworkStatus.CONNECTED, PageContext.REQUEST_SCOPE);
    pageContext.setAttribute("ERROR", i2p.bote.network.NetworkStatus.ERROR, PageContext.REQUEST_SCOPE);
%> 
<jsp:useBean id="jspHelperBean" class="i2p.bote.web.JSPHelper"/>
<c:set var="connStatus" value="${jspHelperBean.networkStatus}" scope="request"/>
