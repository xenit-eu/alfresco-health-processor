# Alfresco Health Processor

[![CI](https://github.com/xenit-eu/alfresco-health-processor/workflows/CI/badge.svg)](https://github.com/xenit-eu/alfresco-health-processor/actions?query=workflow%3ACI+branch%3Amaster)
[![License: LGPL v3](https://img.shields.io/badge/License-LGPL%20v3-blue.svg)](https://www.gnu.org/licenses/lgpl-3.0)

A background processor capable of performing various operations on the nodes in your Alfresco repository, in a batched
and controlled manner.

## Usage

Alfresco Health Processor is available in Maven Central as an
[Alfresco Module Package](https://docs.alfresco.com/6.2/concepts/dev-extensions-packaging-techniques-amps.html).

### Alfresco Gradle SDK

```groovy
// Alfresco Module Package:
alfrescoAmp "eu.xenit.alfresco:alfresco-health-processor-platform:${last - version}@amp"
```

### Alfresco Maven SDK

```xml
<!-- Alfresco Module Package: -->
<dependency>
    <groupId>eu.xenit.alfresco</groupId>
    <artifactId>alfresco-health-processor-platform</artifactId>
    <version>${last-version}</version>
    <type>amp</type>
</dependency>
```

### Manual download and install

Please consult the official Alfresco documentation on how to install Module Packages manually.

### Supported Alfresco versions

The module is systematically integration tested against Alfresco 5.2, 6.0, 6.1 and 6.2.

## Overview

Alfresco Health Processor is a custom
[Alfresco subsystem](https://docs.alfresco.com/content-services/latest/develop/repo-ext-points/subsystems/)
that is able to perform various node related operations such as integrity validation or even rectification.

![overview](images/health-processor-overview.png "Health Processor Overview")

Triggered as
a [scheduled job](https://docs.alfresco.com/content-services/community/develop/repo-ext-points/scheduled-jobs/), the
processor will iterate over (a subset of) the nodes in Alfresco. The nodes are processed in batches of a predefined size
and are offered to plugins for processing. Each plugin can optionally return reports that contain information about the
health status of the nodes in the processed batch. After the batch has been processed, the platform will offer the
reports to available health reporters.

All interactions done by the Health Processor are highly customizable and extensible. To iterate over batches of nodes,
a single `IndexingStrategy` is used. Batches of nodes are offered for processing to all enabled `HealthProcessorPlugin`
implementations. Any reports received will be offered for processing to all available `HealthReporter` implementations.

### Core configuration

* `eu.xenit.alfresco.healthprocessor.task.cron=* * * * * ? 2099`  
  The cron schedule that will initially startup the process.
* `eu.xenit.alfresco.healthprocessor.task.delay=0`  
  An optional delay before the processor starts.
* `eu.xenit.alfresco.healthprocessor.processing.node-batch-size=1000`  
  Node batch size of the processing. This is the size of the batch that will be requested from the `IndexingStategy`
  and the size of the batches offered to the `HealthProcessorPlugin` implementations
* `eu.xenit.alfresco.healthprocessor.processing.max-batches-per-second=-1`
  Optional rate limitation to minimize load on the system. This number indicates the maximum number of batches each
  plugin may process each second. A number smaller than or equal to 0 indicates no rate limiting is applied.
* `eu.xenit.alfresco.healthprocessor.processing.read-only=true`  
  The Health Processor wraps the processing of each batch for each plugin in a new transaction. This property configures
  if this transaction should be read-only.
* `eu.xenit.alfresco.healthprocessor.processing.run-as-user=System`  
  Since the Alfresco Health Processor is basically a scheduled job, it needs to run as a certain user. The default
  is `System` but it is possible to assign a dedicated user.

## Out of the box functionality

Next to the core framework, this module contains a list of useful tools that contain basic integrity check for your
Alfresco repository. Please note that all available plugins and reporters are disabled by default and should be enabled
as desired.

### IndexingStrategy implementations

The Health Processor uses a single indexing strategy to iterate over (a subset of) nodes in Alfresco. The active
strategy can be controlled with following property:

```properties
eu.xenit.alfresco.healthprocessor.indexing.strategy=txn-id
```

For now, the `txn-id` is the only available strategy.

#### Indexing based on transaction ID

Strategy id: `txn-id`

Loops over (a subset of) nodes based on the ID of transactions in Alfresco.

```properties
eu.xenit.alfresco.healthprocessor.indexing.txn-id.start=-1
eu.xenit.alfresco.healthprocessor.indexing.txn-id.stop=9223372036854775807
eu.xenit.alfresco.healthprocessor.indexing.txn-id.txn-batch-size=5000
```

### HealthProcessorPlugin implementations

#### Content Validation

Activation property: `eu.xenit.alfresco.healthprocessor.plugin.content-validation.enabled=true`

Validates that properties of type `d:content` point to content that actually exists in the Alfresco content store.

By default, the plugin will process all properties with type `d:content`. It is also possible to limit this validation
to specific, predefined properties:

```properties
eu.xenit.alfresco.healthprocessor.plugin.content-validation.properties=cm:content,{foobar}baz
```

If this property is not set (which is the default), the plugin will request all properties of type `d:content`
from Alfresco's `DictionaryService`.

### HealthReporter implementations

#### Logging

Activation property: `eu.xenit.alfresco.healthprocessor.reporter.log.summary.enabled=true`

A simple implementation that writes, once a Health Processor cycle is completed, a summary and unhealthy nodes to the
Alfresco logs.

Relevant logger: `log4j.logger.eu.xenit.alfresco.healthprocessor.reporter.SummaryLoggingHealthReporter=INFO`

Example output:

```text
 2021-03-03 12:40:40,273  INFO  [healthprocessor.reporter.SummaryLoggingHealthReporter] [DefaultScheduler_Worker-2] SUMMARY ---
 2021-03-03 12:40:40,273  INFO  [healthprocessor.reporter.SummaryLoggingHealthReporter] [DefaultScheduler_Worker-2] Plugin[ContentValidationHealthProcessorPlugin] generated reports: {HEALTHY=230, UNHEALTHY=1, NONE=630}
 2021-03-03 12:40:40,273  INFO  [healthprocessor.reporter.SummaryLoggingHealthReporter] [DefaultScheduler_Worker-2]  --- 
 2021-03-03 12:40:40,273  WARN  [healthprocessor.reporter.SummaryLoggingHealthReporter] [DefaultScheduler_Worker-2] UNHEALTHY NODES ---
 2021-03-03 12:40:40,274  WARN  [healthprocessor.reporter.SummaryLoggingHealthReporter] [DefaultScheduler_Worker-2] Plugin[ContentValidationHealthProcessorPlugin] (#1): 
 2021-03-03 12:40:40,275  WARN  [healthprocessor.reporter.SummaryLoggingHealthReporter] [DefaultScheduler_Worker-2] 	workspace://SpacesStore/86796712-4dc6-4b8d-973f-a943ef7f23ed: [Property: '{http://www.alfresco.org/model/content/1.0}content', contentUrl: 'store://2021/3/3/12/27/cb664208-abae-4da9-b7ee-81167a43041a.bin']
 2021-03-03 12:40:40,275  WARN  [healthprocessor.reporter.SummaryLoggingHealthReporter] [DefaultScheduler_Worker-2]  --- 
```

## Extension points

Besides out of the box functionality, it is very easy to provide custom plugins and reporters. All the
`HealthProcessorPlugin` or `HealthReporter` implementations available in the Spring context will be detected and used by
the Health Processor platform.

The API, containing required interfaces and helpful classes is available in Maven Central and can be added as a
dependency in your project:

```groovy
implementation "eu.xenit.alfresco:alfresco-health-processor-platform:${last - version}"
```

```xml

<dependency>
    <groupId>eu.xenit.alfresco</groupId>
    <artifactId>alfresco-health-processor-platform</artifactId>
    <version>${last-version}</version>
</dependency>
```

Please notice that the HealthProcessor is implemented as an Alfresco subsystem. Therefore you can choose to make your
customizations available in the main Alfresco Spring context or as an extension of the HealthProcessor Spring context.
For the former, configuration files (XML / properties) should be available in:
`classpath:alfresco/extension/subsystems/HealthProcessor/default/default` (first default for subsystem type, second
default for subsystem ID).

### HealthProcessorPlugin

An [example plugin](integration-tests/src/main/java/eu/xenit/alfresco/healthprocessor/example/ExampleHealthProcessorPlugin.java)
is included as part of the integration tests of this project.

### HealthReporter

## Configuration reference

The [default property file of the HealthProcessor subsystem](alfresco-health-processor-platform/src/main/amp/config/alfresco/subsystems/HealthProcessor/default/heathprocessor.properties)
contains an overview of all configuration and their default values. 