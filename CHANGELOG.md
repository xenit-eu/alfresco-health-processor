---
title: Changelog - Alfred Health Processor
date: 4 March 2021
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