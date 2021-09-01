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
        <@section label="Indexing Strategy"/>
        <@field label="ID"
            description="The ID of the Indexing Strategy in use"
            value="${healthprocessor.indexing.id}" />
        <div class="column-left">
            <h4>State</h4>
            <ul>
                <#list healthprocessor.indexing.state?keys as key>
                    <li>${key}: <b>${healthprocessor.indexing.state[key]}</b></li>
                </#list>
            </ul>
        </div>
        <div class="column-right">
            <h4>Configuration</h4>
            <ul>
                <#list healthprocessor.indexing.configuration?keys as key>
                    <li>${key}: <b>${healthprocessor.indexing.configuration[key]}</b></li>
                </#list>
            </ul>
        </div>
    </div>

    <div class="column-full">
        <@section label="Plugins"/>
        <ul>
            <#list healthprocessor.plugins.plugins as plugin>
                <li>${plugin.name} [enabled: ${plugin.enabled?c}]</li>
            </#list>
        </ul>
    </div>

    <div class="column-full">
        <@section label="Reporters"/>
        <ul>
            <#list healthprocessor.reporters.reporters as reporter>
                <li>${reporter.name} [enabled: ${reporter.enabled?c}]</li>
            </#list>
        </ul>
    </div>

</@page>
