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

<ib:message key="Add Email Destination to Directory" var="title" scope="request"/>
<jsp:include page="header.jsp"/>

<div class="main">
    <c:if test="${param.confirm eq 'true'}">
        <c:set var="picFilename" value="${requestScope['picture'].tempFilename}"/>
        <ib:publishDestination destination="${param.destination}" pictureFilename="${picFilename}" text="${param.text}"/>
        <ib:message var="infoMessage" scope="request" key="The identity has been added to the address directory."/>
        <jsp:useBean id="jspHelperBean" class="i2p.bote.web.JSPHelper"/>
        <jsp:forward page="editIdentity.jsp?rnd=${jspHelperBean.randomNumber}&new=false&key=${param.destination}"/>
    </c:if>
    <c:if test="${param.confirm ne 'true'}">
        <h2><ib:message key="Publish to the Address Directory"/></h2>
        <form action="publishDestination.jsp?confirm=true" method="post" enctype="multipart/form-data" accept-charset="UTF-8">
            <input type="hidden" name="destination" value="${param.key}"/>
            <div class="publish-form-label">
                <ib:message key="Picture to upload:"/>
                <div class="addtl-text"><ib:message key="(optional, 7.5 kBytes max)"/></div>
            </div>
            <div class="publish-form-value">
                <input type="file" name="picture"/>
            </div>
            <div class="publish-form-label">
                <ib:message key="Text to include:"/>
                <div class="addtl-text"><ib:message key="(optional, 2000 chars max)"/></div>
            </div>
            <div class="publish-form-value">
                <textarea rows="15" cols="50" name="text"></textarea>
            </div>
            <button type="submit"><ib:message key="Publish"/></button>
        </form>
    </c:if>
</div>

<jsp:include page="footer.jsp"/>