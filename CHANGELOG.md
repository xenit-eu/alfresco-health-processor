---
title: Changelog - Alfred Health Processor
date: 07 January 2025
report: true
colorlinks: true

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

# Alfresco Health Processor Changelog

## [0.6.2] - 2025-01-27

### Added
* Added the `single-txns` indexing strategy, which ignores the amount of requested nodes
  and always returns a single transaction for each requested batch.
* Added the `SolrUndersizedTransactionsHealthProcessorPlugin` plugin, which adds a placeholder description
  to existing nodes in order to trigger ACS to merge the corresponding transactions.

## [0.6.1] - 2025-01-08

### Fixed
* Fixed a bug that caused the OOTB admin console to crash the health processor web scripts from the integration tests.

## [0.6.0] - 2025-01-07

### Added
* Alfresco 7.1, 7.2, 7.3, 7.4 support
* Added TLS support for Solr HTTP Client

### Removed
* Alfresco 5 & 6 support
* Removed secret support for Solr HTTP Client

### Fixed
* Fixed missing logging bug caused by Alfresco upgrade to log4j2 


## [0.5.6] - 2024-12-19

### Fixed

*[[#60](https://github.com/xenit-eu/alfresco-health-processor/pull/60)] Fix exception when searching for an empty list of transactions for nodes in Solr


## [0.5.5] - 2024-12-16

### Fixed

*[[#59](https://github.com/xenit-eu/alfresco-health-processor/pull/59)] Fix admin console breaking after changing configuration options/restarting subsystem


## [0.5.4] - 2022-03-02

## Added
* [[#55](https://github.com/xenit-eu/alfresco-health-processor/pull/55)] Add option for SolrRequestExecutor to also check for latest transaction

## [0.5.3] - 2021-11-23

### Fixed

* [[#51](https://github.com/xenit-eu/alfresco-health-processor/pull/51)] Fix NPE in health-processor admin screen before HP is started

## [0.5.2] - 2021-10-26

### Fixed

* [[#50](https://github.com/xenit-eu/alfresco-health-processor/pull/50)] Add description to api project


## [0.5.1] - 2021-10-25

**This release was not published due to an error**

### Fixed

* [[#49](https://github.com/xenit-eu/alfresco-health-processor/pull/49)] Fix release publishing procedure 

## [0.5.0] - 2021-10-25

**This release was not published due to an error**

### Added

* [[#33](https://github.com/xenit-eu/alfresco-health-processor/pull/33)] Validate that plugins send reports for all nodes.
* [[#34](https://github.com/xenit-eu/alfresco-health-processor/pull/34)] Solr index validation plugin
* [[#35](https://github.com/xenit-eu/alfresco-health-processor/pull/35)] Last N transactions indexing strategy
* [[#36](https://github.com/xenit-eu/alfresco-health-processor/pull/36)] User-friendly progress reporting
* [[#40](https://github.com/xenit-eu/alfresco-health-processor/pull/40)] Add health processor fixers to automatically fix unhealthy nodes

### Fixed

* [[#37](https://github.com/xenit-eu/alfresco-health-processor/pull/37)] Export all node health statuses in micrometer
* [[#42](https://github.com/xenit-eu/alfresco-health-processor/pull/42)] Change response check of SolrIndexValidationHealthProcessorPlugin to work with ASS versions < 2.0
* [[#43](https://github.com/xenit-eu/alfresco-health-processor/pull/43)] Reindex complete transaction when a missing node is found
* [[#44](https://github.com/xenit-eu/alfresco-health-processor/issues/44)] Alfresco OOM condition when there are many unhealthy nodes
* [[#47](https://github.com/xenit-eu/alfresco-health-processor/pull/47)] Document full alfresco-health-processor-api artifact

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
