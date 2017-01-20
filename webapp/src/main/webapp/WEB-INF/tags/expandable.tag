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

<%--
    Shows a "More..." link that unhides the body content of the tag,
    i.e. everything between <ib:expandable> and </ib:expandable>.
    If JavaScript is disabled, the body content is always shown and
    there is no link.
--%>

<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="ib" uri="I2pBoteTags" %>

<ib:message key="More..." var="moreText"/>
<ib:message key="Less..." var="lessText"/>

<c:set var="expandableTagCounter" value="${expandableTagCounter + 1}" scope="request"/>

<script>
    // Expands / collapses the help text
    function toggle(textId, linkId) {
        var text = document.getElementById(textId);
        var link = document.getElementById(linkId);
        if(text.style.display == "block") {
            text.style.display = "none";
            link.innerHTML = "${moreText}";
        }
        else {
            text.style.display = "block";
            link.innerHTML = "${lessText}";
        }
        link.blur();
    }
    
    // fills in the link text
    function insertToggleLink() {
        var numTags = ${expandableTagCounter};
        for (var i=1; i<=numTags; i++) {
            var toggleLink = document.getElementById('toggle-link' + i);
            toggleLink.innerHTML = "${moreText}";
        };
    }
    
    window.onload = insertToggleLink;
</script>

<div id="hidden-text${expandableTagCounter}" class="hidden-text">
    <jsp:doBody/>
</div>
<a href="#" id="toggle-link${expandableTagCounter}" onclick="toggle('hidden-text${expandableTagCounter}', 'toggle-link${expandableTagCounter}')"></a>
<noscript>
    <br/>
    <jsp:doBody/>
</noscript>