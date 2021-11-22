<import resource="classpath:alfresco/templates/webscripts/org/alfresco/repository/admin/admin-common.lib.js">

function main()
{
   model.tools = Admin.getConsoleTools("health-processor");
   model.metadata = Admin.getServerMetaData();
}

main();