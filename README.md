# Alfresco Health Processor

[![CI](https://github.com/xenit-eu/alfresco-health-processor/workflows/CI/badge.svg)](https://github.com/xenit-eu/alfresco-health-processor/actions?query=workflow%3ACI+branch%3Amaster)
[![License: LGPL v3](https://img.shields.io/badge/License-LGPL%20v3-blue.svg)](https://www.gnu.org/licenses/lgpl-3.0)

## Out Of The Box Plugins

### Content Validation

Can be used to validate that `d:content` properties of a node point to content that actually exists in the Alfresco
content store.

Enabled with property: `eu.xenit.alfresco.healthprocessor.plugin.content-validation.enabled=true`

By default, the plugin will process all properties with type `d:content`. It is also possible to limit this validation
to specific, predefined properties:

```properties
eu.xenit.alfresco.healthprocessor.plugin.content-validation.properties=cm:content,{foobar}baz
```

If this property is not set (which is the default), the plugin will request all properties of type `d:content`
from Alfresco's `DictionaryService`. 