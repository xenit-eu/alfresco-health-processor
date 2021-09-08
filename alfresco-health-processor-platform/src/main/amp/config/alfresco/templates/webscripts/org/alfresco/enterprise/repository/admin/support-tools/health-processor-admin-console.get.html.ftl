<#include "/org/alfresco/repository/admin/admin-template.ftl" />

<@page title="Alfresco Health Processor" readonly=true>

    <div class="column-full">
        <@field label="Alfresco Health Processor Version"
            description="The version of the Alfresco Health Processor module"
            value="${healthprocessor.version}" />
        <@field label="Status"
            description="Current status of the Health Processor"
            value="${healthprocessor.status}" />
    </div>

    <#macro map_dump name map>
        <#if name != "">
            <h4>${name}</h4>
        </#if>
        <ul>
            <#list map?keys as key>
                <li><b>${key}</b>: ${map[key]}</li>
            </#list>
        </ul>
    </#macro>

    <div class="column-full">
        <@section label="Cycle progress"/>
        <#assign progress=healthprocessor.indexing.progress>
        <#if !progress.isNone()>
            <@field label="Progress"
                description="Completion percentage for this iteration"
                value="${progress.progress}" />
            <@field label="Elapsed time"
                description="Started at ${progress.startTime?datetime}"
                value="${progress.elapsed}" />
            <#if progress.estimatedCompletionTime??>
                <@field label="Estimated completion"
                    description="Estimated to finish at ${progress.estimatedCompletionTime?datetime}"
                    value="${progress.estimatedCompletion}" />
            <#else>
                <@field label="Estimated completion"
                    description="Estimated completion time can not be calculated"
                    value="Unknown" />
            </#if>
        <#else>
            <p>No progress information is currently available,
            <#if healthprocessor.status != "ACTIVE">
                because there is no health-processor cycle active.
            <#else>
                because the current indexing strategy does not report progress information.
            </#if>
            </p>
        </#if>
    </div>

    <div class="column-full">
        <@section label="Indexing Strategy" />
        <@field label="ID"
            description="The ID of the Indexing Strategy in use"
            value="${healthprocessor.indexing.id}" />
        <div class="column-left">
            <@map_dump "State", healthprocessor.indexing.state />
        </div>
        <div class="column-right">
            <@map_dump "Configuration", healthprocessor.indexing.configuration />
        </div>
        <div style="clear:both"></div>
    </div>

    <#macro extensions_list type extensions>
            <#list extensions as extension>
                <div class="column-full">
                    <@section label="${type}: ${extension.name}" />
                    <#assign hasState = extension.state?size gt 0 />
                    <#if hasState>
                        <div class="column-left">
                            <@map_dump "Configuration", extension.configuration />
                        </div>
                        <div class="column-right">
                            <@map_dump "State", extension.state />
                        </div>
                    <#else>
                        <@map_dump "", extension.configuration />
                    </#if>
                </div>
           </#list>
    </#macro>

    <@extensions_list "Plugin", healthprocessor.plugins.extensions />

    <@extensions_list "Fixer", healthprocessor.fixers.extensions />

    <@extensions_list "Reporter", healthprocessor.reporters.extensions />

</@page>
