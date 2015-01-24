<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>
<!DOCTYPE html>

<%@ taglib prefix="ib" uri="I2pBoteTags" %>

<html>
<head>
<meta charset="utf-8">
</head>

<body>
    <p>
    <ib:message>
        In order to receive email from other people, you need to create an email identity
        first.
    </ib:message>
    </p>
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
    <p>
    <ib:message>
        I2P-Bote automatically decrypts emails sent to you, using the email identity you
        created. Email identities are stored in a file named identities; never give
        this file to anyone or they will be able to read all your emails.
    </ib:message>
    <br/>
    <ib:message>
        Do give out the email destination so others can contact you.
    </ib:message>
    </p>
    <p>
    <ib:message>
        It is easy to create multiple identities for different purposes, or different
        contacts.
    </ib:message>
    </p>
    <ib:message>
        When you create a new email identity, you can choose the type of encryption that
        will be used by the email identity. While all encryption algorithms offered by
        I2P-Bote provide a high level of privacy, the length of an email destination
        depends on the encryption used. The choice of encryption also determines what
        signing algorithm is used. 
    </ib:message>
    <br/>
    <ib:message>
        It is generally recommended to choose 256-bit elliptic curve encryption because
        it produces the shortest email destinations (86 characters) while still being
        highly secure.
    </ib:message>
</body>
</html>