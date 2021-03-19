<#include "/org/alfresco/repository/admin/admin-template.ftl" />

<@page title="Alfresco Health Processor" readonly=true>

    <div class="column-full">
        <@field label="Alfresco Health Processor Version" description="The version of the Alfresco Health Processor module" value="${healthprocessor.module.version}" />
    </div>

</@page>