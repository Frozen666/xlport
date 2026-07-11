# xlPort

A Java service for bidirectional conversion between Excel spreadsheets and JSON. Use Excel templates to generate spreadsheets from JSON data (export), or extract structured JSON from Excel files (import).

## Features

- **Export (JSON to Excel):** Populate Excel templates with JSON data, supporting named ranges, tables, formulas, data validation, conditional formatting, and multi-sheet templates.
- **Import (Excel to JSON):** Extract properties and table data from Excel files as structured JSON, with wildcard support.
- **Multi-sheet templates:** Create multiple sheets from a single template sheet, each with its own data.
- **PDF export:** Optional PDF generation via Google Sheets API.
- **Workbook protection:** Lock exported workbooks with a password.
- **Deployable as a web service (WAR) or embeddable as a library (JAR).**

## Requirements

- Java 17+
- Maven 3.x

## Quick Start

### Build

```bash
mvn clean verify
```

### Run locally

```bash
mvn jetty:run
```

This starts the service on `http://localhost:8082`.

### Export example (JSON to Excel)

```bash
curl -X PUT http://localhost:8082/export \
  -d @src/test/resources/export1.json \
  --header "Content-Type: application/json" \
  -O -J
```

### Import example (Excel to JSON)

```bash
curl -X PUT http://localhost:8082/import \
  -F file=@path/to/workbook.xlsx \
  -F request='{"properties":["*"],"tables":["*"]}'
```

## Use as a Library

Add xlPort as a dependency and use the core classes directly:

```java
// Export: populate a template with JSON data
Template template = TemplateManager.getTemplate("my-template.xlsx");
JSONObject data = new JSONObject(jsonString);
JSONArray errors = new JSONArray();
Exporter.exportToExcel(data.getJSONObject("data"), template, errors, true);

// Import: extract JSON from an Excel file
XSSFWorkbook workbook = new XSSFWorkbook(new File("data.xlsx"));
JSONArray errors = new JSONArray();
JSONObject result = Importer.importAllData(workbook, errors, true);
```

To build as a JAR instead of WAR, change `<packaging>` in `pom.xml`:

```xml
<packaging>jar</packaging>
```

## API Reference

### `PUT /export`

Accepts a JSON payload with:

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `templateId` | string | yes | Template file name or Google Sheets URL |
| `data` | object | yes | Key-value pairs matching named ranges/tables in the template |
| `filename` | string | no | Output file name (without extension) |
| `overwriteFormatting` | boolean | no | Copy cell formatting from template row (default: true) |
| `protectWorkbook` | boolean | no | Lock sheets in the output workbook |
| `workbookPassword` | string | no | Password for workbook protection |
| `format` | string | no | Set to `"pdf"` for PDF output |

Returns the generated Excel (or PDF) file.

### `PUT /import`

Accepts either:
- **Multipart form:** `file` (Excel) + `request` (JSON)
- **Binary body:** Raw Excel file (imports all data)

Request JSON:

| Field | Type | Description |
|-------|------|-------------|
| `properties` | array | Named ranges to extract (`["*"]` for all) |
| `tables` | array | Table names to extract (`["*"]` for all) |

Returns JSON with `properties` and/or `tables` keys.

### Health checks

- `GET /alive` - Returns 200 when the service is running.
- `GET /ready` - Returns 200 when fully initialized, 503 otherwise.

## Configuration

Configuration is via environment variables:

| Variable | Description |
|----------|-------------|
| `XLPORT_API_KEY` | If set, requires `Authorization: xlport apikey <key>` header |
| `XLPORT_USE_CORS` | Set to `FALSE` to disable CORS (enabled by default) |
| `XLPORT_USE_LOCAL_TEMPLATES` | Set to `TRUE` to load templates from local filesystem instead of GCS |
| `XLPORT_GCS_BUCKET_NAME` | GCS bucket name for template storage (default: `xlport-templates`) |
| `XLPORT_GCS_PATH` | Path prefix within the GCS bucket (default: `xlport/`) |
| `XLPORT_gcs_*` | Google Cloud credentials for GCS template storage and PDF export (see below) |

### Google Cloud (optional)

For Google Sheets templates, GCS template storage, or PDF export, set these environment variables:

- `XLPORT_gcs_project_id`
- `XLPORT_gcs_private_key_id`
- `XLPORT_gcs_private_key`
- `XLPORT_gcs_client_email`
- `XLPORT_gcs_client_id`

Or call `InitXlPort.setGoogleCredential(jsonString)` when using xlPort as a library.

## Running Tests

```bash
mvn test
```

Tests that require Google credentials will be automatically skipped if credentials are not configured.

## Docker Deployment

The repository includes a multi-stage `Dockerfile` (Ubuntu 26.04, OpenJDK 25 JRE, Jetty 12.1) that builds the WAR inside Docker, so no local Maven setup is needed:

```bash
docker build -t xlport .
docker run -p 8080:8080 xlport
```

Set environment variables (`XLPORT_API_KEY`, `XLPORT_gcs_*`, etc.) via `docker run -e` or your orchestrator.

## License

Licensed under the [Apache License 2.0](LICENSE).
