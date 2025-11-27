# CI/CD Workflow Documentation

This document describes the Continuous Integration and Continuous Deployment workflows for the VS Code Extension Updater project.

## 🔄 CI Workflow (Build and Test)

### Workflow: `ci.yml`
**Triggers**: 
- Push to `main` or `develop` branches
- Pull requests targeting `main` or `develop`
- Manual trigger via GitHub Actions UI

### Jobs Overview

#### 1. **Cross-Platform Testing** (`test`)
- **Platforms**: Ubuntu, Windows, macOS
- **Java Versions**: 21, 25
- **Matrix Strategy**: Tests all combinations (6 total combinations)
- **Steps**:
  - Checkout code
  - Set up JDK with specified version
  - Cache Maven dependencies
  - Validate, compile, and test
  - Generate test reports
  - Upload test artifacts

#### 2. **Application Build** (`build`)
- **Platform**: Ubuntu (for consistency)
- **Dependencies**: Requires successful completion of all tests
- **Steps**:
  - Build application JAR
  - Verify JAR executability
  - Upload application artifact (90-day retention)

#### 3. **Code Quality Analysis** (`code-quality`)
- **Platform**: Ubuntu
- **Analysis**: 
  - Maven verify phase
  - OWASP dependency vulnerability check
- **Artifacts**: Dependency check reports

#### 4. **Integration Testing** (`integration-test`)
- **Platforms**: Ubuntu, macOS (Windows excluded due to VS Code installation complexity)
- **Dependencies**: Requires successful build
- **Steps**:
  - Install VS Code on the platform
  - Test VS Code detection functionality
  - Test application startup in headless mode

#### 5. **Dependency Analysis** (`dependency-analysis`)
- **Platform**: Ubuntu
- **Analysis**:
  - Dependency tree generation
  - Duplicate dependency detection
  - Unused dependency analysis
- **Artifacts**: Analysis reports

#### 6. **Build Summary** (`build-summary`)
- **Dependencies**: Runs after all other jobs complete
- **Purpose**: Provides comprehensive build status summary
- **Output**: GitHub step summary with overall status

### Command-Line Testing Support

The application now supports several command-line flags for CI testing:

```bash
# Show version information
java -jar extension-updater.jar --version

# Test VS Code detection
java -jar extension-updater.jar --test-detection

# Test application startup (exits after initialization)
java -jar extension-updater.jar --test-startup

# Allow headless environment
java -jar extension-updater.jar --allow-headless

# Show help
java -jar extension-updater.jar --help
```

### Matrix Testing Strategy

The CI workflow uses a matrix strategy to ensure compatibility:

| Platform | Java 21 | Java 25 |
|----------|---------|---------|
| Ubuntu   | ✅      | ✅      |
| Windows  | ✅      | ✅      |
| macOS    | ✅      | ✅      |

### Security & Quality Checks

- **OWASP Dependency Check**: Scans for known vulnerabilities
- **Maven Verify**: Runs all verification plugins
- **Cross-platform Testing**: Ensures compatibility across operating systems
- **Dependency Analysis**: Identifies unused or conflicting dependencies

### Artifacts Generated

1. **Test Results** (30-day retention):
   - JUnit XML reports
   - Test class files
   - Per-platform test results

2. **Application JAR** (90-day retention):
   - Executable application JAR
   - Available for download and testing

3. **Code Quality Reports** (30-day retention):
   - OWASP dependency check report
   - Dependency analysis results

## 🍎 macOS Installer Workflow

### Workflow: `macos-installer.yml`
**Triggers**:
- Tagged releases (`v*`)
- Manual trigger
- Pull requests affecting macOS installer files

**Purpose**: Builds signed macOS installer packages with embedded Java Runtime

**Key Features**:
- Creates self-contained `.pkg` installers
- Embeds custom JRE using `jlink`
- Supports code signing and notarization
- Generates universal binaries for Intel and Apple Silicon

## 📊 Build Status

### Success Criteria
- ✅ All cross-platform tests pass
- ✅ Application JAR builds successfully
- ✅ No critical security vulnerabilities
- ✅ Integration tests pass on Unix-like platforms
- ✅ No dependency conflicts detected

### Failure Handling
- **Test Failures**: Individual platform/Java version failures don't stop other combinations
- **Build Failures**: Stop the pipeline and require fixes
- **Security Issues**: OWASP check failures with CVSS >= 7 will fail the build
- **Integration Issues**: VS Code detection or startup failures require investigation

### Performance Targets
- **Total Build Time**: < 15 minutes
- **Test Execution**: < 5 minutes per platform/Java combination
- **Artifact Upload**: < 2 minutes

## 🔧 Local Testing

To run the same checks locally:

```bash
# Run all tests
mvn test

# Build application
mvn clean package

# Run security check
mvn org.owasp:dependency-check-maven:check

# Test command-line functionality
java -jar target/extension-updater-1.0.jar --test-detection
java -jar target/extension-updater-1.0.jar --version

# Test startup in CI mode
java -Djava.awt.headless=true -jar target/extension-updater-1.0.jar --test-startup --allow-headless
```

## 📋 Troubleshooting

### Common Issues

**1. Test Failures on Specific Platforms**
- Check platform-specific logs in test artifacts
- Verify Java version compatibility
- Review system-specific dependencies

**2. OWASP Security Warnings**
- Review `owasp-suppressions.xml` for false positives
- Update dependencies if real vulnerabilities exist
- Consider risk assessment for low-priority issues

**3. VS Code Detection Failures**
- Ensure VS Code is properly installed on the platform
- Check installation paths in test logs
- Verify executable permissions

**4. Integration Test Timeouts**
- Increase timeout values if tests are running slowly
- Check for system resource constraints
- Review log output for stuck processes

### Monitoring

The CI workflow provides comprehensive monitoring through:
- **GitHub Actions UI**: Real-time build status
- **Step Summaries**: Detailed build results
- **Artifact Downloads**: Access to build outputs and logs
- **Matrix View**: Per-platform/version results

This CI setup ensures high code quality, cross-platform compatibility, and reliable deployments for the VS Code Extension Updater project.