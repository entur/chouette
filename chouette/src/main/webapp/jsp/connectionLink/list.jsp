<%@ taglib uri="/struts-tags" prefix="s" %>
<%@ taglib uri="http://displaytag.sf.net" prefix="display" %>

<%-- Titre et barre de navigation --%>	
<title><s:text name="text.connectionlink.list.title" /></title>
<s:url id="urlCorrespondances" value="/connectionLink/list" includeParams="none"/>
<s:property value="filAriane.addElementFilAriane(getText('text.connectionlink.list.title'), '', #urlCorrespondances)"/>
<div class="panelData">
  <s:property value="filAriane.texteFilAriane" escape="false"/>
</div>
<br>
<%-- Ajouter une correspondance --%>
<div class="actions">
  <s:url action="add" namespace="/connectionLink" id="createCorrespondance"/>
  <s:a href="%{createCorrespondance}"><b><s:text name="text.connectionlink.create.button"/></b></s:a>
</div>

<br>
<div>
  <s:if test="useHastus == 'true'">
    <FIELDSET align="center" style="width: 500px;">
      <LEGEND><b><s:text name="text.connectionlink.import"/></b></LEGEND>
      <s:form id="uploadCorrespondancesForm1" enctype="multipart/form-data" method="POST">
        <s:file name="fichier" />
        <s:hidden name="fichierContentType" value="text/xml; charset=UTF-8"/>
        <s:submit value="%{getText('action.import.connectionLink')}" formId="uploadCorrespondancesForm1" action="Import_importCorrespondances" />
      </s:form>
    </FIELDSET>
    <br>
    <FIELDSET align="center" style="width: 500px;">
      <LEGEND><b><s:text name="text.connectionlink.export"/></b></LEGEND>
      <s:form id="uploadCorrespondancesForm2" enctype="multipart/form-data" method="POST" namespace="/export">
        <s:submit value="%{getText('action.export.connectionLink')}" formId="uploadCorrespondancesForm2" action="exportCorrespondances" />
      </s:form>
    </FIELDSET>
  </s:if>
</div>
<br>
<div class="panel" id="displaytag"> 
  
  <display:table name="connectionLinks" pagesize="20"  requestURI="" id="connectionLink" export="false">
    <display:column titleKey="table.title.action" sortable="false">
      
      <%-- Delete action--%>
      <s:url id="removeUrl" action="delete" namespace="/connectionLink">
        <s:param name="idCorrespondance">${connectionLink.id}</s:param>
        <s:param name="operationMode">${ops.delete}</s:param>
      </s:url>
      
      <%-- Edit action--%>
      <s:url id="editUrl" action="edit" namespace="/connectionLink">
        <s:param name="idCorrespondance">${connectionLink.id}</s:param>
      </s:url>
      
      <s:a href="%{editUrl}">
        <img border="0" alt="Edit" src="<s:url value='/images/editer.png'/>" title="<s:text name="tooltip.edit"/>">
      </s:a>
      
      &nbsp;&nbsp;            
      <s:a href="%{removeUrl}" onclick="return confirm('%{getText('connectionlink.delete.confirmation')}');">
        <img border="0" alt="Delete" src="<s:url value='/images/supprimer.png'/>" title="<s:text name="tooltip.delete"/>">
      </s:a>
        
    </display:column>
    <display:column titleKey="table.title.name" property="name"/>
    <display:column titleKey="table.title.comment" property="comment"/>
    <display:column titleKey="table.title.type">
      <s:text name="%{#attr.correspondance.linkType}"/>
    </display:column>
  </display:table>
</div>
