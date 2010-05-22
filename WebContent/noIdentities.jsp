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

<c:set var="title" value="No Identity" scope="request"/>
<jsp:include page="header.jsp"/>

<div class="main">
    <h2><ib:message key="No Email Identity Defined"/></h2>
    <p>
    <ib:message>
        In order to receive email from other people, you need to create an email identity
        first.
    </ib:message>
    </p><p>
    <ib:message>
        Every email identity is associated with an email destination. Anybody can send email
        to the email destination, but only the identity holder can read it.
    </ib:message>
    <br/>
    <ib:message>
        In a sense, email identities are the equivalent to traditional email accounts -
        except that there is no provider that can read all your email because I2P-Bote
        stores all emails encrypted on the network.
    </ib:message>
    </p><p>
    <ib:message>
        I2P-Bote automatically decrypts emails sent to you, using the email identity you
        created. Email identities are stored in a file named identities.txt; never give
        this file to anyone or they will be able to read all your emails.
    </ib:message>
    <br/>
    <ib:message>
        Do give out the email destination so others can contact you.
    </ib:message>
    <br/>
    <ib:message>
        It is easy to create multiple identities for different purposes, or different
        contacts.
    </ib:message>
    <form action="editIdentity.jsp?new=true" method="POST">
        <button type="submit" value="New"><ib:message key="Create a New Email Identity"/></button>
    </form>
</div>

<jsp:include page="footer.jsp"/>