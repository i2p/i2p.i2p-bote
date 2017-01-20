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
<%@ taglib prefix="ib" uri="I2pBoteTags" %>

<ib:message key="About I2P-Bote" var="title" scope="request"/>
<c:set var="navSelected" value="about" scope="request"/>
<jsp:include page="header.jsp"/>

    <h2>
        <ib:message key="I2P-Bote Version {0}">
            <jsp:useBean id="jspHelperBean" class="i2p.bote.web.JSPHelper"/>
            <ib:param value="${jspHelperBean.appVersion}"/>
        </ib:message>
    </h2>

    <p><ib:message key="To submit bug reports or feature requests:"/> <a href="http://trac.i2p2.i2p/newticket?component=apps/plugins&keywords=I2P-Bote&owner=str4d"><ib:message key="click here"/></a>.</p>
    
    <div class="contributor-category"><ib:message key="Developers:"/></div>
    <div>
        <div class="contributor-name">
            <ib:message key="Click to send an email" var="linkTitle"/>
            <a href="newEmail.jsp?nofilter_recipient0=TzKO~FlShiQEOPkPn7eIOkxqBy6pGxk1NDkVLLwzGk~kNPwo8qvHoyk4vKOZVZNGklsU7iOndYeQofMZtADm5yqbUxhogTmxyu7VcNsw6mXBub26FAUEQADf4Uj4Ph0dGAMyPbWzDEFUibdJyjpLYS9AaHgf~EU8B49DP8rpkh8d0T&amp;recipientType0=to&amp;subject=${subject}" title="${linkTitle}">str4d</a>
        </div>
        <div class="contributor-role"><ib:message key="Maintenance, user experience, extended features, Android app"/></div>
    </div>
    <br/>

    <div class="contributor-category"><ib:message key="Past developers:"/></div>
    <div>
        <div class="contributor-name">
            <ib:message key="Feedback on I2P-Bote" var="subject"/>
            <a href="newEmail.jsp?nofilter_recipient0=hobo37SEJsEMfQHwcpVlvEgnrERGFz34GC1yjVyuRvl1QHnTi0UAoOtrLP~qkFY0oL59BBqj5sCep0RA8I5G8n&amp;recipientType0=to&amp;subject=${subject}" title="${linkTitle}">HungryHobo</a>
        </div>
        <div class="contributor-role"><ib:message key="Technical concept, implementation, user interface"/></div>
    </div>
    <br/>
    
    <div class="contributor-category"><ib:message key="Contributors:"/></div>
    <div>
        <div class="contributor-name">Mixxy</div>
        <div class="contributor-role"><ib:message key="Technical concept, translation, QA, usability, technical feedback"/></div>
    </div>
    <div>
        <div class="contributor-name">zzz</div>
        <div class="contributor-role"><ib:message key="Pluginization, technical feedback"/></div>
    </div>
    <div>
        <div class="contributor-name">sponge</div>
        <div class="contributor-role"><ib:message key="Seedless integration"/></div>
    </div>
    <div>
        <div class="contributor-name">suhr</div>
        <div class="contributor-role"><ib:message key="Russian translation"/></div>
    </div>
    <div>
        <div class="contributor-name">hiddenz</div>
        <div class="contributor-role"><ib:message key="Russian translation"/></div>
    </div>
    <div>
        <div class="contributor-name">albat</div>
        <div class="contributor-role"><ib:message key="French translation"/></div>
    </div>
    <div>
        <div class="contributor-name">redzara</div>
        <div class="contributor-role"><ib:message key="French translation"/></div>
    </div>
    <div>
        <div class="contributor-name">magma</div>
        <div class="contributor-role"><ib:message key="French translation"/></div>
    </div>
    <div>
        <div class="contributor-name">Jrnr601</div>
        <div class="contributor-role"><ib:message key="Dutch translation"/></div>
    </div>
    <div>
        <div class="contributor-name">KwukDuck</div>
        <div class="contributor-role"><ib:message key="Dutch translation"/></div>
    </div>
    <div>
        <div class="contributor-name">nej</div>
        <div class="contributor-role"><ib:message key="Norwegian translation"/></div>
    </div>
    <div>
        <div class="contributor-name">hottuna</div>
        <div class="contributor-role"><ib:message key="Swedish translation"/></div>
    </div>
    <div>
        <div class="contributor-name">walking</div>
        <div class="contributor-role"><ib:message key="Chinese translation"/></div>
    </div>
    <div>
        <div class="contributor-name">hamada</div>
        <div class="contributor-role"><ib:message key="Arabic translation"/></div>
    </div>
    <div>
        <div class="contributor-name">Waseihou Watashi</div>
        <div class="contributor-role"><ib:message key="Czech translation"/></div>
    </div>
    <div>
        <div class="contributor-name">D.A. Loader</div>
        <div class="contributor-role"><ib:message key="German translation"/></div>
    </div>
    <div>
        <div class="contributor-name">Warton</div>
        <div class="contributor-role"><ib:message key="Polish translation"/></div>
    </div>
    <div>
        <div class="contributor-name">digitalmannen</div>
        <div class="contributor-role"><ib:message key="Swedish translation"/></div>
    </div>
    <div>
        <div class="contributor-name">mkkid</div>
        <div class="contributor-role"><ib:message key="Italian translation"/></div>
    </div>
    <div>
        <div class="contributor-name">gringoire</div>
        <div class="contributor-role"><ib:message key="Ukrainian translation"/></div>
    </div>
    <div>
        <div class="contributor-name">AdminLMH</div>
        <div class="contributor-role"><ib:message key="Hungarian translation"/></div>
    </div>
    <div>
        <div class="contributor-name">Voulnet</div>
        <div class="contributor-role"><ib:message key="Arabic translation"/></div>
    </div>
    <div>
        <div class="contributor-name">Returning Novice</div>
        <div class="contributor-role"><ib:message key="Alpha testing"/></div>
    </div>
    <div>
        <div class="contributor-name">KillYourTV</div>
        <div class="contributor-role"><ib:message key="Code fixes, translation updates from tx"/></div>
    </div>
    <div>
        <div class="contributor-name">kay</div>
        <div class="contributor-role"><ib:message key="Code fixes"/></div>
    </div>
    <div>
        <div class="contributor-name">Jonathan Cross</div>
        <div class="contributor-role"><ib:message key="Usability improvement"/></div>
    </div>
    <div>
        <div class="contributor-name">Beardog</div>
        <div class="contributor-role"><ib:message key="Bug hunting"/></div>
    </div>

<jsp:include page="footer.jsp"/>
