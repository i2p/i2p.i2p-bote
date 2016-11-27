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

<%--
    Valid actions:
        <default>           - show the "new email" form
        send                - send an email using the request data
        addToAddrBook       - add a recipient to the address book and return here
        lookup              - add one or more address book entries as recipients and return here
        addRecipientField   - add a recipient field
        removeRecipient<i>  - remove the recipient field with index i
        attach              - add the file given in the parameter "file" as an attachment
        removeAttachment<i> - remove the attachment with index i
        
    Other parameters:
        new    - true for new contact, false for existing contact
--%>

<c:set var="action" value="${param.action}" scope="request"/>
<c:if test="${not empty action and pageContext.request.method ne 'POST'}">
    <c:set var="action" value="" scope="request"/>
    <ib:message key="Form must be submitted using POST." var="errorMessage" scope="request"/>
</c:if>

<c:choose>
    <c:when test="${action eq 'send'}">
        <jsp:forward page="sendEmail.jsp"/>
    </c:when>
    <c:when test="${action eq 'addToAddrBook'}">
        <c:set var="destparam" value="${param.destparamname}"/>
        <jsp:forward page="editContact.jsp">
            <jsp:param name="new" value="true"/>
            <jsp:param name="destination" value="${ib:escapeQuotes(param[destparam])}"/>
            <jsp:param name="forwardUrl" value="newEmail.jsp"/>
            <jsp:param name="backUrl" value="newEmail.jsp"/>
            <jsp:param name="paramsToCopy" value="nofilter_sender,nofilter_recipient*,to*,cc*,bcc*,replyto*,subject,message,attachmentNameOrig*,attachmentNameTemp*,forwardUrl,backUrl,paramsToCopy"/>
        </jsp:forward>
    </c:when>
    <c:when test="${action eq 'lookup'}">
        <jsp:forward page="addressBook.jsp">
            <jsp:param name="select" value="true"/>
            <jsp:param name="forwardUrl" value="newEmail.jsp"/>
            <jsp:param name="nofilter_paramsToCopy" value="nofilter_sender,nofilter_recipient*,to*,cc*,bcc*,replyto*,subject,message,attachmentNameOrig*,attachmentNameTemp*,forwardUrl"/>
        </jsp:forward>
    </c:when>
</c:choose>

<%--
    The newAttachment request attribute contains a UploadedFile object, see MultipartFilter.java.
    When action='attach', originalAttachmentFilename contains the name of the file selected by the user.
--%>
<c:set var="originalAttachmentFilename" value="${requestScope['newAttachment'].originalFilename}"/>

<ib:message key="New Email" var="title" scope="request"/>
<c:if test="${action eq 'attach' and empty originalAttachmentFilename}">
    <ib:message key="Please select a file to attach and try again." var="noAttachmentMsg"/>
    <c:set var="errorMessage" value="${noAttachmentMsg}" scope="request"/>
</c:if>
<jsp:include page="header.jsp"/>

<ib:requirePassword>
    <c:set var="csrf_tokenname"><csrf:tokenname/></c:set>
    <c:set var="csrf_tokenvalue"><csrf:tokenvalue uri="newEmail.jsp"/></c:set>
    <form id="emailform" action="newEmail.jsp?${csrf_tokenname}=${csrf_tokenvalue}" method="post" enctype="multipart/form-data" accept-charset="UTF-8">
        <div class="email-form-button-send">
            <button type="submit" name="action" value="send">&#x2794; <ib:message key="Send"/></button>
        </div>

        <div class="email-form-label">
            <ib:message key="From:"/>
        </div>
        <div class="email-form-value">
            <select name="nofilter_sender">
                <option value="anonymous"><ib:message key="Anonymous"/></option>
                <jsp:useBean id="jspHelperBean" class="i2p.bote.web.JSPHelper"/>
                <c:forEach items="${jspHelperBean.identities.all}" var="identity">
                    <c:set var="selected" value=""/>
                    <c:if test="${fn:contains(param.nofilter_sender, identity.key)}">
                        <c:set var="selected" value=" selected"/>
                    </c:if>
                    <c:if test="${empty param.nofilter_sender && identity.defaultIdentity}">
                        <c:set var="selected" value=" selected"/>
                    </c:if>
                    <option value="${identity.publicName} &lt;${identity.key}&gt;"${selected}>
                        ${identity.publicName} &lt;${fn:substring(identity.key, 0, 10)}...&gt;
                        <c:if test="${!empty identity.description}"> - ${identity.description}</c:if>
                    </option>
                </c:forEach>
            </select>
        </div>
        
        <%-- Add an address line for each recipient --%>
        <c:set var="recipients" value="${ib:mergeRecipientFields(pageContext.request)}"/>
        <c:forEach var="recipient" items="${recipients}" varStatus="status">
            <c:set var="recipientField" value="nofilter_recipient${status.index}"/>
            <div class="email-form-recipient-label">
                <c:set var="recipientTypeField" value="recipientType${status.index}"/>
                <c:set var="recipientType" value="${param[recipientTypeField]}"/>
                <select name="recipientType${status.index}">
                    <c:set var="toSelected" value="${recipientType eq 'to' ? ' selected' : ''}"/>
                    <c:set var="ccSelected" value="${recipientType eq 'cc' ? ' selected' : ''}"/>
                    <c:set var="bccSelected" value="${recipientType eq 'bcc' ? ' selected' : ''}"/>
                    <c:set var="replytoSelected" value="${recipientType eq 'replyto' ? ' selected' : ''}"/>
                    <option value="to"${toSelected}><ib:message key="To:"/></option>
                    <option value="cc"${ccSelected}><ib:message key="CC:"/></option>
                    <option value="bcc"${bccSelected}><ib:message key="BCC:"/></option>
                    <option value="replyto"${replytoSelected}><ib:message key="Reply To:"/></option>
                </select>
            </div>
            <div class="email-form-recipient-value">
                <ib:message key="Remove this recipient" var="removeRecipientTitle"/>
                <input type="text" size="48" class="email-form-recipient-field" name="${recipientField}" value="${ib:escapeQuotes(recipient.address)}"/>
                <c:choose>
                    <c:when test="${status.last}">
                        <input type="hidden" name="destparamname" value="${recipientField}"/>
                        <ib:message key="Add this recipient to the address book" var="addToAddrBookTitle"/>
                        <button type="submit" name="action" value="addToAddrBook" title="${addToAddrBookTitle}">&#x1F872;<img src="${themeDir}/images/addressbook.png"/></button>
                    </c:when>
                    <c:otherwise>
                        <button type="submit" name="action" value="removeRecipient${status.index}" title="${removeRecipientTitle}">-</button>
                    </c:otherwise>
                </c:choose>
            </div>
        </c:forEach>

        <div class="email-form-button-row">
            <ib:message key="Select recipients from address book" var="lookupTitle"/>
            <button type="submit" name="action" value="lookup" title="${lookupTitle}">&#x1F870;<img src="${themeDir}/images/addressbook.png"/></button>
            <ib:message key="Add another recipient field" var="addRecipientFieldTitle"/>
            <button type="submit" name="action" value="addRecipientField" title="${addRecipientFieldTitle}">+</button>
        </div>
        
        <div class="email-form-label">
            <ib:message key="Subject:"/>
        </div>
        <div class="email-form-value">
            <input type="text" size="48" class="email-form-subject-field" name="subject" value="${ib:escapeQuotes(param.subject)}"/>
        </div>
        
        <%-- Attachments --%>
        <div class="email-form-attach-label">
            <ib:message key="Attachments:"/>
        </div>
        <div class="email-form-value">
            <c:set var="maxAttachmentIndex" value="-1"/>
            <c:forEach items="${param}" var="parameter">
                <c:if test="${fn:startsWith(parameter.key, 'attachmentNameOrig')}">
                    <c:set var="attachmentIndex" value="${fn:substringAfter(parameter.key, 'attachmentNameOrig')}"/>
                    <c:set var="removeAction" value="removeAttachment${attachmentIndex}"/>
                    <c:set var="removed" value="${action eq removeAction}"/>
                    <c:if test="${!removed}">
                        <c:if test="${attachmentIndex gt maxAttachmentIndex}">
                            <c:set var="maxAttachmentIndex" value="${attachmentIndex}"/>
                        </c:if>
                        <div class="email-form-attach-files">
                            <c:set var="tempFileParamName" value="attachmentNameTemp${attachmentIndex}"/>
                            <c:set var="tempAttachmentFilename" value="${param[tempFileParamName]}"/>
                            <c:set var="filename" value="${parameter.value}"/>
                            <c:set var="filesize" value="(${ib:getFileSize(tempAttachmentFilename)})"/>
                            <div class="email-form-attach-item" title="${filename} ${filesize}">
                                ${filename}
                                <span class="email-form-attach-size">${filesize}</span>
                                <input type="hidden" name="attachmentNameOrig${attachmentIndex}" value="${parameter.value}"/>
                                <input type="hidden" name="attachmentNameTemp${attachmentIndex}" value="${tempAttachmentFilename}"/>
                            </div>
                            <div class="email-form-attach-remove">
                                <ib:message key="Remove this attachment" var="linkTitle"/>
                                <button type="submit" name="action" value="removeAttachment${attachmentIndex}" title="${linkTitle}">-</button>
                            </div>
                        </div>
                    </c:if>
                </c:if>
            </c:forEach>
            
            <c:if test="${action eq 'attach' and not empty originalAttachmentFilename}">
                <c:set var="tempAttachmentFilename" value="${requestScope['newAttachment'].tempFilename}"/>
                <c:set var="maxAttachmentIndex" value="${maxAttachmentIndex + 1}"/>
                <div class="email-form-attach-files">
                    <c:set var="filename" value="${originalAttachmentFilename}"/>
                    <c:set var="filesize" value="(${ib:getFileSize(tempAttachmentFilename)})"/>
                    <div class="email-form-attach-item" title="${filename} ${filesize}">
                        ${filename}
                        <span class="email-form-attach-size">${filesize}</span>
                        <input type="hidden" name="attachmentNameOrig${maxAttachmentIndex}" value="${requestScope['newAttachment'].originalFilename}"/>
                        <input type="hidden" name="attachmentNameTemp${maxAttachmentIndex}" value="${tempAttachmentFilename}"/>
                    </div>
                    <div class="email-form-attach-remove">
                        <c:remove var="newAttachment" scope="request"/>
                        <button type="submit" name="action" value="removeAttachment${maxAttachmentIndex}">-</button>
                    </div>
                </div>
            </c:if>
            
            <div>
                <input type="file" name="newAttachment" onchange="attachFile();"/>
                <ib:message key="Add another attachment" var="linkTitle"/>
                <button id="attachbutton" type="submit" name="action" value="attach" title="${linkTitle}"><ib:message key="Attach"/></button>
            </div>
            <div class="email-form-attach-small"><ib:message key="It is recommended to keep attachments below 500 kBytes."/></div>
        </div>
        
        <div class="email-form-label">
            <ib:message key="Message:"/>
        </div>
        <div class="email-form-value">
            <textarea rows="30" cols="70" name="message"><c:if test="${!empty param.quoteMsgId}">
<%-- The following lines are not indented because the indentation would show up as blank chars on the textarea --%>
<c:set var="origEmail" value="${ib:getEmail(param.quoteMsgFolder, param.quoteMsgId)}"/>
<ib:message key="{0} wrote:" hide="true">
<ib:param value="${ib:getShortSenderName(origEmail.sender, 50)}"></ib:param>
</ib:message><ib:quote text="${fn:escapeXml(origEmail.text)}"/></c:if><c:if test="${empty param.quoteMsgId}">${fn:escapeXml(param.message)}</c:if></textarea>
        </div>
        <c:if test="${!empty param.quoteMsgId}">
            <%-- Parameters needed by sendEmail if the user clicks delete after sending a reply --%>
            <input type="hidden" name="quoteMsgFolder" value="${param.quoteMsgFolder}"/>
            <input type="hidden" name="quoteMsgId" value="${param.quoteMsgId}"/>
        </c:if>
    </form>
</ib:requirePassword>

<script>
// simulates a click on the "attach" button
function attachFile() {
    var hiddenField = document.createElement("input");
    hiddenField.setAttribute("type", "hidden");
    hiddenField.setAttribute("name", "action");
    hiddenField.setAttribute("value", "attach");
    document.forms["emailform"].appendChild(hiddenField);
    document.forms["emailform"].submit();
}

function hideAttachButton() {
    document.getElementById("attachbutton").style.visibility="hidden";
}
window.onload = hideAttachButton;
</script>

<jsp:include page="footer.jsp"/>
