# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Chouette is a Java application for importing, exporting, and validating public transport (PT) data in Neptune, NeTEx, and GTFS formats. It runs as a REST web service on WildFly 26.1.3 (Jakarta EE 8) and deploys as an EAR archive. The Rutebanken fork focuses on the NeTEx Nordic Profile.

## Build Commands

```bash
# Full build (skips tests)
mvn clean install -DskipTests

# Full build with tests (requires PostgreSQL with PostGIS running)
mvn clean install

# Build skipping WildFly download and Lombok delombok (CI-style)
mvn -DskipWildfly -DskipDelombok -B clean install

# Run a single test class
mvn test -pl mobi.chouette.exchange.netexprofile -Dtest=JtsGmlConverterTest

# Run a single test method
mvn test -pl mobi.chouette.exchange.netexprofile -Dtest=JtsGmlConverterTest#testIgnoreEmptyLineString

# Skip database initialization during tests
mvn test -DskipInitDb
```

## Test Requirements

- **PostgreSQL** with PostGIS extension must be running (default: `localhost:5432`)
- **Database**: `chouette_test` with user `chouette`/password `chouette`
- DB config can be overridden: `-Ddb.host=... -Ddb.port=... -Ddb.name=...`
- Schema is initialized automatically via SQL Maven plugin (`src/test/sql/chouette_test.sql`)
- Tests use **TestNG** (not JUnit) with **Arquillian** for integration tests using an embedded WildFly container

## Architecture

The project follows a **command pattern** with stateless EJBs. Data exchange (import/export/validate) is orchestrated through processing commands.

### Key Modules

- **`mobi.chouette.model`** - JPA entities for the public transport data model (lines, routes, stop points, timetables, etc.)
- **`mobi.chouette.exchange`** - Core exchange framework: base commands, parsers, and producers
- **`mobi.chouette.exchange.netexprofile`** - NeTEx Nordic Profile import/export/validation (primary format for Rutebanken)
- **`mobi.chouette.exchange.gtfs`** - GTFS import/export
- **`mobi.chouette.exchange.neptune`** - Neptune import/export
- **`mobi.chouette.exchange.validator`** - Common data validation
- **`mobi.chouette.ws`** - JAX-RS REST API (entry point: `Application.java`, context root: `/chouette_iev`)
- **`chouette_iev`** - EAR packaging module that bundles everything for WildFly deployment
- **`mobi.chouette.dao`** / **`mobi.chouette.jdbc`** - Data access layer (Hibernate + raw JDBC)
- **`mobi.chouette.googlecloudsql`** - Google Cloud SQL socket factory integration

### Exchange Flow Pattern

Each format module follows the same pattern:
1. **`*ImporterCommand`** (entry point EJB) orchestrates import via `*ImporterProcessingCommands`
2. **Parsers** convert format-specific XML/data into the internal `Referential` model
3. **`*ExporterCommand`** orchestrates export via `*ExporterProcessingCommands`
4. **Producers/Writers** convert the internal model back to format-specific output

### REST API

Main endpoints registered in `mobi.chouette.ws.Application`:
- `RestService` at `/referentials` - core referential CRUD and job management
- `RestAdmin` - admin operations
- `RestNetexStopPlaceService` - stop place operations
- `HealthResource` - health checks

## Key Technologies

- Java 17, Maven, WildFly 26.1.3
- Hibernate 5.3.28 with Spatial extensions
- PostgreSQL with PostGIS
- JAXB for XML marshalling/unmarshalling (NeTEx, Neptune)
- Saxon for XPath processing
- Lombok for boilerplate reduction
- NeTEx Java Model (`org.rutebanken.netex`) for NeTEx data structures

## Branch and CI

- Main branch: `rutebanken_develop`
- CI: GitHub Actions (`.github/workflows/push.yml`)
- Docker: Debian-based image with WildFly, built from `Dockerfile`
