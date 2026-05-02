# SDKMAN Submission Guide for CloudCLI

## What is SDKMAN?
SDKMAN is the package manager for Java tools (like pip for Python).
Once approved, users can install with: `sdk install cloudcli`

## Submission Steps

### 1. Prepare Release
- Ensure GitHub releases are stable and versioned (v1.0.0, v1.1.0, etc.)
- JAR must be downloadable from GitHub releases
- Must have a proper version scheme

### 2. Submit to SDKMAN
Go to: https://github.com/sdkman/sdkman-candidates

Create a new issue with title: "Add CloudCLI as a new candidate"

Use this template:
```
## Candidate Details

- **Name**: CloudCLI
- **Description**: Multi-Database Backup CLI Tool
- **Homepage**: https://github.com/saqlainansari/cloudcli
- **Download URL**: https://github.com/saqlainansari/cloudcli/releases/latest/download/cloudcli.jar
- **Version**: 1.0.0
- **License**: MIT
- **Author**: Saqlain Zarjis Ansari
```

### 3. Wait for Approval
SDKMAN team reviews submissions. Usually takes 1-2 weeks.

### 4. After Approval
Users can install with:
```bash
sdk install cloudcli
```
