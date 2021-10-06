---
title: Changelog - Alfred Health Processor
date: 31 May 2021
report: true
colorlinks: true
---
<!--
Changelog for Alfred Telemetry

See http://keepachangelog.com/en as reference
Version template:

## [X.X.X] - yyyy-MM-dd
### Added (for new features)
### Changed (for changes in existing functionality)
### Deprecated (for soon-to-be removed features)
### Removed (for now removed features)
### Fixed (for any bug fixes)
### Security (in case of vulnerabilities)
### YANKED (for reverted functionality in)
 -->

# Alfresco Health Processor Changelog

## [0.5.0] - UNRELEASED

### Added

* [[#33](https://github.com/xenit-eu/alfresco-health-processor/pull/33)] Validate that plugins send reports for all nodes.
* [[#34](https://github.com/xenit-eu/alfresco-health-processor/pull/34)] Solr index validation plugin
* [[#35](https://github.com/xenit-eu/alfresco-health-processor/pull/35)] Last N transactions indexing strategy
* [[#36](https://github.com/xenit-eu/alfresco-health-processor/pull/36)] User-friendly progress reporting
* [[#40](https://github.com/xenit-eu/alfresco-health-processor/pull/40)] Add health processor fixers to automatically fix unhealthy nodes

### Fixed

* [[#37]](https://github.com/xenit-eu/alfresco-health-processor/pull/37)]  Export all node health statuses in micrometer
* [[#42](https://github.com/xenit-eu/alfresco-health-processor/pull/42)] Change response check of SolrIndexValidationHealthProcessorPlugin to work with ASS versions < 2.0
* [[#43](https://github.com/xenit-eu/alfresco-health-processor/pull/43)] Reindex complete transaction when a missing node is found

### Changed

* [[#38](https://github.com/xenit-eu/alfresco-health-processor/pull/38)] Moved public APIs to separate artifact
* [[#39](https://github.com/xenit-eu/alfresco-health-processor/pull/39)] Rename IndexingProgress to CycleProgress and remove default methods

## [0.4.0] - 2021-07-08
### Added
* Alfresco 7 support

## [0.3.1] - 2021-06-24
### Fixed
* Make sure persisting of attributes is done in a write transaction
* Unable to start in a clustered environment due to invalid cache settings

## [0.3.0] - 2021-05-28
### Changed
* [[#20](https://github.com/xenit-eu/alfresco-health-processor/issues/20)] 
  Make (subset of) state more persistent using the Alfresco AttributeService
* AlfredTelemetryHealthReporter: reset report counts when cycle starts

## [0.2.1] - 2021-03-31
### Fixed
* [[#24](https://github.com/xenit-eu/alfresco-health-processor/issues/24)] 
  Admin Console dashboard: status stays ACTIVE after failure
* [[#25](https://github.com/xenit-eu/alfresco-health-processor/issues/25)] 
  ContentValidationHealthProcessorPlugin performance improvements

## [0.2.0] - 2021-03-23
### Added
* [[#17](https://github.com/xenit-eu/alfresco-health-processor/issues/17)] 
  AlfredTelemetryHealthReporter
* [[#21](https://github.com/xenit-eu/alfresco-health-processor/issues/21)] 
  Admin console dashboard

## [0.1.0] - 2021-03-04

Initial, early access release containing core framework, `SummaryLoggingHealthReporter`, 
`ContentValidationHealthProcessorPlugin`, ...
