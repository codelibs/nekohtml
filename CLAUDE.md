# CLAUDE.md - AI Assistant Guide for NekoHTML

## Project Overview

**NekoHTML** is a pure Java HTML parser for Java 17+ with zero external dependencies. It provides both DOM and SAX-based parsing interfaces with full backward compatibility. Originally forked from CyberNeko HTML Parser 1.9.22, version 3.0+ has been completely rewritten to eliminate Xerces XNI dependencies and uses only standard Java APIs.

- **Current Version:** 3.0.3-SNAPSHOT
- **License:** Apache License 2.0
- **Organization:** CodeLibs Project
- **Repository:** https://github.com/codelibs/nekohtml

## Quick Reference

### Build Commands

```bash
# Compile the project
mvn clean compile

# Run all tests
mvn test

# Run a specific test class
mvn test -Dtest=DOMParserTest

# Build JAR (skip tests for speed)
mvn package -DskipTests

# Generate code coverage report (output: target/site/jacoco/index.html)
mvn verify

# Format code (required before commits)
mvn formatter:format

# Apply license headers
mvn license:format

# Generate Javadoc
mvn javadoc:javadoc
```

### Technology Stack

| Component | Technology |
|-----------|------------|
| Language | Java 17+ |
| Build System | Maven 3.6+ |
| Testing | JUnit 5 (Jupiter) + Mockito 5.14.2 |
| Runtime Dependencies | None (pure Java, standard SAX/DOM APIs only) |
| Code Formatter | Eclipse formatter (DBFlute style) |

## Directory Structure

```
nekohtml/
├── pom.xml                          # Maven build configuration
├── README.md                        # Project documentation
├── LICENSE.txt                      # Apache License 2.0
├── .github/workflows/               # CI/CD pipelines
│   ├── maven.yml                    # Build pipeline
│   └── codeql-analysis.yml          # Security analysis
├── doc/                             # Legacy documentation and samples
│   └── sample/                      # Example Java code
├── src/
│   ├── config/eclipse/formatter/
│   │   └── java.xml                 # Eclipse code formatter config
│   ├── main/java/org/codelibs/nekohtml/
│   │   ├── parsers/                 # Public parser APIs
│   │   │   ├── DOMParser.java       # DOM-based HTML parser
│   │   │   ├── SAXParser.java       # SAX wrapper for backward compatibility
│   │   │   └── SAXToDOMHandler.java # Converts SAX events to DOM
│   │   ├── sax/                     # Core SAX implementation (v3.0)
│   │   │   ├── HTMLSAXParser.java           # Main SAX parser (XMLReader)
│   │   │   ├── HTMLSAXConfiguration.java    # Pipeline orchestrator
│   │   │   ├── SimpleHTMLScanner.java       # Regex-based tokenizer
│   │   │   ├── HTMLTagBalancerFilter.java   # Tag balancing filter
│   │   │   ├── HTMLSAXScanner.java          # Scanner wrapper
│   │   │   ├── HTMLQName.java               # Qualified names
│   │   │   ├── HTMLAttributesImpl.java      # SAX attributes
│   │   │   ├── HTMLAugmentations.java       # Extra parser info
│   │   │   ├── HTMLDocumentHandler.java     # Document event handler
│   │   │   ├── HTMLStringBuffer.java        # String handling
│   │   │   ├── EncodingMap.java             # Character encoding mappings
│   │   │   └── XMLChar.java                 # XML character utilities
│   │   ├── HTMLElements.java        # HTML element definitions (~1,083 lines)
│   │   ├── HTMLEntities.java        # Entity references and mapping
│   │   ├── HTMLErrorReporter.java   # Error reporting interface
│   │   ├── HTMLEventInfo.java       # Event information
│   │   ├── ObjectFactory.java       # JAXP factory utilities
│   │   └── SecuritySupport.java     # Security utilities
│   ├── main/resources/org/codelibs/nekohtml/res/
│   │   ├── HTMLlat1.properties      # Latin-1 entities
│   │   ├── HTMLspecial.properties   # Special HTML entities
│   │   ├── HTMLsymbol.properties    # Symbol entities
│   │   ├── XMLbuiltin.properties    # Built-in XML entities
│   │   ├── ErrorMessages.properties # Error messages (English)
│   │   └── ErrorMessages_ja.properties # Error messages (Japanese)
│   └── test/
│       ├── java/org/codelibs/nekohtml/
│       │   ├── parsers/             # Parser tests (9 test files)
│       │   ├── sax/                 # SAX implementation tests (17 test files)
│       │   └── *.java               # Core functionality tests
│       └── resources/data/          # Test HTML fixtures (100+ files)
│           ├── canonical/           # Expected parse outputs
│           └── [various]/           # Categorized test cases
```

## Architecture

### Parsing Pipeline

```
HTML Input
    ↓
SimpleHTMLScanner (regex-based tokenization)
    ↓
HTMLTagBalancerFilter (tag balancing, HTML5 Adoption Agency Algorithm)
    ↓
ContentHandler (SAX events)
    ↓
[Optional: SAXToDOMHandler → DOM Document]
```

### Key Classes

| Class | Purpose |
|-------|---------|
| `DOMParser` | High-level DOM-based parser, entry point for DOM parsing |
| `SAXParser` | Backward-compatible SAX parser wrapper |
| `HTMLSAXParser` | Core SAX parser implementing `XMLReader` |
| `HTMLSAXConfiguration` | Configuration and pipeline orchestration |
| `SimpleHTMLScanner` | Regex-based HTML tokenizer |
| `HTMLTagBalancerFilter` | Implements HTML5 tag balancing algorithm |
| `SAXToDOMHandler` | Converts SAX events to DOM tree |
| `HTMLElements` | Static HTML element definitions and metadata |
| `HTMLEntities` | HTML entity mappings (name ↔ character) |

## Code Conventions

### Style Guidelines

- **Formatter:** Eclipse formatter config at `src/config/eclipse/formatter/java.xml` (DBFlute style)
- **Indentation:** 4 spaces (no tabs in Java code)
- **Run before commits:** `mvn formatter:format`

### Naming Conventions

- **Classes:** PascalCase (e.g., `HTMLSAXParser`, `SimpleHTMLScanner`)
- **Methods:** camelCase (e.g., `setContentHandler`, `parse`)
- **Constants:** UPPER_SNAKE_CASE (e.g., `VOID_ELEMENTS`, `MARKER`)
- **Packages:** lowercase (e.g., `org.codelibs.nekohtml.sax`)
- **Field prefixes:** `f` for protected/private fields (legacy: `fConfiguration`, `fFeatures`)

### License Headers

All Java source files must have the Apache 2.0 license header:

```java
/*
 * Copyright 2012-2025 CodeLibs Project and the Others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * ...
 */
```

Apply with: `mvn license:format`

## Testing

### Test Structure

- **35 test files** covering parsers, SAX implementation, and core utilities
- **100+ HTML fixture files** in `src/test/resources/data/`
- Uses JUnit 5 (Jupiter) and Mockito

### Common Test Patterns

```java
@Test
public void testBasicParsing() throws Exception {
    final String html = "<html><body><p>Hello</p></body></html>";
    final DOMParser parser = new DOMParser();
    parser.parse(new InputSource(new StringReader(html)));
    final Document doc = parser.getDocument();
    assertNotNull(doc, "Document should not be null");
    // ... assertions
}
```

### Test Categories

| Location | Purpose |
|----------|---------|
| `parsers/DOMParserTest.java` | DOM parsing (77+ test methods) |
| `parsers/SAXParserTest.java` | SAX event-based parsing |
| `parsers/MalformedHTMLTest.java` | Broken HTML handling |
| `sax/HTMLTagBalancerFilterTest.java` | Tag balancing logic |
| `sax/SimpleHTMLScannerTest.java` | Tokenization tests |
| `PerformanceStressTest.java` | Performance testing |
| `ThreadSafetyTest.java` | Concurrency tests |

## Common Tasks

### Adding a New HTML Element

1. Edit `src/main/java/org/codelibs/nekohtml/HTMLElements.java`
2. Add element constant and metadata
3. Add tests in `HTMLElementsTest.java`
4. Run `mvn test` to verify

### Adding a New Entity

1. Edit appropriate properties file in `src/main/resources/org/codelibs/nekohtml/res/`
2. Add tests in `HTMLEntitiesTest.java`
3. Run `mvn test` to verify

### Modifying Tag Balancing Behavior

1. Edit `src/main/java/org/codelibs/nekohtml/sax/HTMLTagBalancerFilter.java`
2. Understand the Adoption Agency Algorithm implementation
3. Add tests in `HTMLTagBalancerFilterTest.java`
4. Run `mvn test` to verify

### Adding Parser Features/Properties

1. Edit `src/main/java/org/codelibs/nekohtml/sax/HTMLSAXConfiguration.java`
2. Add feature/property handling
3. Update `HTMLSAXParser.java` if needed
4. Add tests in `HTMLSAXConfigurationTest.java`

## Important Notes

### No External Dependencies

This project has **zero runtime dependencies**. Only use standard Java APIs:
- `javax.xml.parsers` (SAXParserFactory, DocumentBuilderFactory)
- `org.xml.sax` (SAX API)
- `org.w3c.dom` (DOM API)
- `java.util.logging` (logging)

### Backward Compatibility

The `parsers` package maintains API compatibility with the original CyberNeko HTML Parser. Changes to `DOMParser` and `SAXParser` must preserve existing method signatures.

### System Properties

- `nekohtml.dom.strict` - Enables strict DOM mode in `SAXToDOMHandler`

### Error Handling

- Parse errors are reported via `HTMLErrorReporter` interface
- Exceptions: `SAXException`, `IOException`, `ParserConfigurationException`
- Use `java.util.logging.Logger` for debug logging

## CI/CD

- **GitHub Actions:** `.github/workflows/maven.yml`
- **Triggers:** Push to master, pull requests
- **Security:** CodeQL analysis in `.github/workflows/codeql-analysis.yml`
- **JDK:** Temurin 17

## Release Process

Releases are managed via Maven Release Plugin:
```bash
mvn release:prepare
mvn release:perform
```

Published to Maven Central via `central-publishing-maven-plugin`.
