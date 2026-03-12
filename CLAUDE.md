# CLAUDE.md - AI Assistant Guide for NekoHTML

## Project Overview

**NekoHTML** is a pure Java HTML parser for Java 17+ with zero external dependencies. It provides both DOM and SAX-based parsing interfaces with full backward compatibility. Originally forked from CyberNeko HTML Parser 1.9.22, version 3.0+ has been completely rewritten to eliminate Xerces XNI dependencies and uses only standard Java APIs.

## Build Commands

```bash
mvn clean compile          # Compile
mvn test                   # Run all tests
mvn test -Dtest=ClassName  # Run specific test
mvn package -DskipTests    # Build JAR
mvn verify                 # Coverage report (target/site/jacoco/index.html)
mvn formatter:format       # Format code (required before commits)
mvn license:format         # Apply license headers
```

## Directory Structure

```
src/main/java/org/codelibs/nekohtml/
├── parsers/                 # Public parser APIs
│   ├── DOMParser.java       # DOM-based HTML parser
│   ├── SAXParser.java       # SAX wrapper for backward compatibility
│   └── SAXToDOMHandler.java # Converts SAX events to DOM
├── sax/                     # Core SAX implementation
│   ├── HTMLSAXParser.java           # Main SAX parser (XMLReader)
│   ├── HTMLSAXConfiguration.java    # Pipeline orchestrator
│   ├── SimpleHTMLScanner.java       # Regex-based tokenizer
│   ├── HTMLTagBalancerFilter.java   # Tag balancing filter
│   ├── HTMLSAXScanner.java          # Base scanner with encoding detection
│   ├── HTMLAttributesImpl.java      # SAX Attributes implementation
│   ├── HTMLDocumentHandler.java     # Document handler interface
│   ├── EncodingMap.java             # Character encoding mappings
│   ├── HTMLQName.java               # Qualified name representation
│   ├── HTMLStringBuffer.java        # Mutable string buffer
│   ├── HTMLAugmentations.java       # Augmentation info for events
│   └── XMLChar.java                 # XML character validation
├── HTMLElements.java        # HTML element definitions
├── HTMLEntities.java        # Entity references and mapping
├── HTMLErrorReporter.java   # Error reporting interface
├── HTMLEventInfo.java       # Event information interface
├── ObjectFactory.java       # SAX parser factory (META-INF service)
└── SecuritySupport.java     # Security manager utilities

src/main/resources/org/codelibs/nekohtml/res/
├── HTML*.properties         # Entity definitions
└── ErrorMessages*.properties # Error messages

src/test/
├── java/org/codelibs/nekohtml/
│   ├── parsers/             # Parser tests
│   └── sax/                 # SAX implementation tests
└── resources/data/          # Test HTML fixtures
```

## CI/CD

GitHub Actions workflows in `.github/workflows/`:
- `maven.yml` — Build and test on push/PR
- `codeql-analysis.yml` — CodeQL security scanning

## Architecture

### Parsing Pipeline

```
HTML Input → SimpleHTMLScanner → HTMLTagBalancerFilter → ContentHandler → [SAXToDOMHandler → DOM]
```

### Key Classes

| Class | Purpose |
|-------|---------|
| `DOMParser` | Entry point for DOM parsing |
| `SAXParser` | Backward-compatible SAX parser wrapper |
| `HTMLSAXParser` | Core SAX parser implementing `XMLReader` |
| `HTMLSAXConfiguration` | Configuration and pipeline orchestration |
| `SimpleHTMLScanner` | Regex-based HTML tokenizer |
| `HTMLTagBalancerFilter` | HTML5 tag balancing (Adoption Agency Algorithm) |
| `HTMLElements` | Static HTML element definitions |
| `HTMLEntities` | HTML entity mappings |

## Code Conventions

- **Formatter:** `src/config/eclipse/formatter/java.xml` (DBFlute style)
- **Indentation:** 4 spaces
- **Run before commits:** `mvn formatter:format`
- **Classes:** PascalCase
- **Methods:** camelCase
- **Constants:** UPPER_SNAKE_CASE
- **Field prefixes:** `f` for protected/private fields (legacy convention)

## Common Tasks

### Adding a New HTML Element
1. Edit `HTMLElements.java`
2. Add tests in `HTMLElementsTest.java`

### Adding a New Entity
1. Edit properties file in `src/main/resources/org/codelibs/nekohtml/res/`
2. Add tests in `HTMLEntitiesTest.java`

### Modifying Tag Balancing
1. Edit `HTMLTagBalancerFilter.java`
2. Add tests in `HTMLTagBalancerFilterTest.java`

### Adding Parser Features
1. Edit `HTMLSAXConfiguration.java`
2. Update `HTMLSAXParser.java` if needed
3. Add tests in `HTMLSAXConfigurationTest.java`

## Gotchas

- `mvn formatter:format` and `mvn license:format` must both pass before committing — CI will fail otherwise
- The `ObjectFactory` class is registered as a SAX parser via `META-INF/services` — don't remove it
- `HTMLSAXScanner` is the base scanner class; `SimpleHTMLScanner` extends it with regex-based tokenization

## Important Notes

- **Zero runtime dependencies** - Only use standard Java APIs (`javax.xml.parsers`, `org.xml.sax`, `org.w3c.dom`, `java.util.logging`)
- **Backward compatibility** - `parsers` package maintains API compatibility with original CyberNeko HTML Parser
- **System property:** `nekohtml.dom.strict` enables strict DOM mode
- **Error handling:** via `HTMLErrorReporter` interface; exceptions are `SAXException`, `IOException`, `ParserConfigurationException`
