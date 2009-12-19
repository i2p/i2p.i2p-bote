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

<html>
<head>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
    <link rel="stylesheet" href="i2pbote.css" />
    <link rel="icon" type="image/png" href="images/favicon.png" />
    <title>${param.title} - I2P-Bote</title>
</head>

<body>

<div class="titlebar" style="cursor:pointer" onclick="document.location='.'">
    <div class="title">I2P-Bote</div>
    <br/>
    <div class="subtitle">Secure Distributed Email</div>
</div>

<div class="menubox">
    <iframe src="buttonFrame.jsp" width="100%" height="40px" scrolling="no" frameborder="0" allowtransparency="true"></iframe>
</div>

<div class="menubox">
    <h2>Folders</h2>
    <a href="folder.jsp?path=Inbox"><img src="images/folder.png"/>Inbox</a><br/>
    <img src="images/folder.png"/>Outbox<br/>
    <img src="images/folder.png"/>Sent<br/>
    <img src="images/folder.png"/>Drafts<br/>
    <img src="images/folder.png"/>Trash<br/>
    <hr/>
    <img src="images/folder.png"/>User_created_Folder_1<br/>
    <img src="images/folder.png"/>User_created_Folder_2<br/>
    <img src="images/folder.png"/>User_created_Folder_3<br/>
    <hr/>
    Manage Folders
</div>

<div class="menubox">
    <h2>Addresses</h2>
    <a href="identities.jsp">Identities</a><br/>
    Private Address Book<br/>
    Public Address Directory<br/>
</div>

<div class="menubox">
    <h2>Configuration</h2>
    Settings<br/>
</div>

<div class="menubox">
    <h2><a href="network.jsp">Network Status</a></h2>
    <iframe src="statusFrame.jsp" width="100%" height="40px" scrolling="no" frameborder="0" allowtransparency="true"></iframe>
</div>

<div class="menubox">
    <h2>Help</h2>
    User Guide<br/>
    <a href="about.jsp">About</a><br/>
</div>