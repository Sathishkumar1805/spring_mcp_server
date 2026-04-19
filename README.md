# LinkedIn Profile MCP Server

Spring Boot [Model Context Protocol](https://modelcontextprotocol.io/) (MCP) server that exposes your **LinkedIn data export** (JSON files) to clients such as **Claude Desktop**. Tools return markdown-oriented text so models can answer questions about your work history, skills, certifications, and education—useful for resumes, LinkedIn posts, and interview prep.

## Prerequisites

- **JDK 17+** (project targets Java 17)
- **Maven 3.6+** recommended (the POM pins a few Maven plugins for older CLI compatibility)
- A **LinkedIn data export** in JSON form (see below)

## How to obtain LinkedIn data

1. Open LinkedIn → **Settings & Privacy** → **Data privacy** → **Get a copy of your data**.
2. Request an archive; choose **JSON** when offered.
3. Unzip the archive. Copy the JSON files you need into a single directory that you will reference via `LINKEDIN_DATA_PATH`.

**Files used by this server:**

| File | Purpose |
|------|---------|
| `Profile.json` | Name, headline, summary, location |
| `Positions.json` | Employment history |
| `Skills.json` | Skills (and endorsement counts when present) |
| `Education.json` | Schools and degrees |
| `Certifications.json` | Certifications (or `Licenses & Certifications.json`) |
| `Projects.json` | Loaded for completeness (no dedicated MCP tool in this version) |

Exports differ by locale and LinkedIn version; POJOs use `@JsonIgnoreProperties(ignoreUnknown = true)` and aliases where practical.

## Configuration

Key settings live in `src/main/resources/application.properties`:

- **`spring.ai.mcp.server.stdio=true`** — STDIO transport for Claude Desktop (required).
- **`spring.main.web-application-type=none`** — no embedded web server.
- **`spring.main.banner-mode=off`** — keeps stdout clean for MCP framing.
- **`logging.file.name=linkedin-mcp.log`** — log to file (not stdout).
- **`logging.pattern.console=`** — suppresses default console log lines on stdout.
- **`linkedin.data.path`** — directory containing the JSON files. Override with environment variable **`LINKEDIN_DATA_PATH`** or JVM `-Dlinkedin.data.path=...`.

**Note:** Some Spring AI docs mention `spring.ai.mcp.server.transport=stdio`. In **Spring AI 1.0.x**, STDIO is enabled with **`spring.ai.mcp.server.stdio=true`** (already set in this project).

### MCP tools (Spring AI)

Tools are declared with **`@Tool`** / **`@ToolParam`** from `org.springframework.ai.tool.annotation` and registered via **`MethodToolCallbackProvider`** (see `McpToolConfiguration`). This matches Spring AI **1.0.0**; newer docs may refer to `@McpTool`—that is a different annotation package used in newer Spring AI lines.

## Build

```bash
mvn clean package
```

Executable JAR:

`target/linkedin-profile-server-1.0.0-SNAPSHOT.jar`

The `pom.xml` pins `maven-compiler-plugin`, `maven-surefire-plugin`, and `maven-jar-plugin` so **older Maven installations** that cannot use Spring Boot 3.4’s default plugin versions still build.

## Run (manual)

Point `LINKEDIN_DATA_PATH` at your export folder (the one that contains `Profile.json`, etc.):

**Windows (PowerShell):**

```powershell
$env:LINKEDIN_DATA_PATH = "C:\path\to\your\linkedin\export"
java -jar target\linkedin-profile-server-1.0.0-SNAPSHOT.jar
```

**macOS / Linux:**

```bash
export LINKEDIN_DATA_PATH=/path/to/your/linkedin/export
java -jar target/linkedin-profile-server-1.0.0-SNAPSHOT.jar
```

Do **not** rely on `System.out` for logging when using STDIO; this project logs to **`linkedin-mcp.log`** (and avoids printing to stdout).

## Claude Desktop integration

Add an MCP server entry so Claude launches this JAR with STDIO.

### Windows

Edit:

`%APPDATA%\Claude\claude_desktop_config.json`

### macOS

Edit:

`~/Library/Application Support/Claude/claude_desktop_config.json`

### Example configuration

Adjust paths to your machine. Use **escaped backslashes** on Windows in JSON.

```json
{
  "mcpServers": {
    "linkedin-profile": {
      "command": "java",
      "args": [
        "-jar",
        "C:\\Sathish\\spring_mcp_server\\target\\linkedin-profile-server-1.0.0-SNAPSHOT.jar"
      ],
      "env": {
        "LINKEDIN_DATA_PATH": "C:\\path\\to\\your\\linkedin\\export"
      }
    }
  }
}
```

Restart Claude Desktop. You should see the MCP server connected (indicator in the UI). Then you can ask, for example: *“What did I work on at Test Organization?”*—answers come from your `Positions.json` when that company appears there.

## Example usage scenarios

- **Resume refresh** — Ask Claude to rewrite bullets using `get_work_experience` / `search_experience` grounded in your export.
- **LinkedIn content** — Use `get_profile_summary` and `get_current_role` for accurate tone and facts.
- **Skill inventory** — `get_skills` with optional category filter.
- **Credential list** — `get_certifications` for certificates and validity text.

## Troubleshooting

| Symptom | What to check |
|--------|----------------|
| Claude never connects | JAR path in `args`, `java` on `PATH`, restart Claude Desktop |
| Tools return empty sections | `LINKEDIN_DATA_PATH` points to the folder **containing** the JSON files (not the zip root unless files are there) |
| Missing sections | Filenames must match (or use the alternate certifications filename). Check `linkedin-mcp.log` for load warnings |
| Garbled MCP / protocol errors | Ensure nothing prints to **stdout**; keep file logging and empty `logging.pattern.console` as in `application.properties` |
| Build failures on old Maven | Use Maven **3.6.3+**, or rely on the pinned plugin versions in `pom.xml` |

## Sample JSON

- **`sample-linkedin-export/`** — copy of representative files for structure and local experiments.
- **`src/test/resources/sample-linkedin/`** — same bundle used by unit tests (`mvn test`).

## Tests

```bash
mvn test
```

Covers loading the sample export, keyword search, category filtering, missing-file handling, and text utilities.

## License

Apache 2.0 (same as Spring Boot / Spring AI ecosystem defaults; confirm your org’s policy before distribution).
