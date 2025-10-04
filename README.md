# NekoHTML [![Java CI with Maven](https://github.com/codelibs/nekohtml/actions/workflows/maven.yml/badge.svg)](https://github.com/codelibs/nekohtml/actions/workflows/maven.yml)

**A pure Java HTML parser with zero dependencies**

NekoHTML is a lightweight, tolerant HTML parser for Java that generates well-formed XML/DOM output from legacy and malformed HTML. Originally forked from CyberNeko HTML Parser 1.9.22, version 3.0 has been completely rewritten to eliminate all external dependencies and use only standard Java APIs.

## ✨ Key Features

- **Zero Dependencies** - Pure Java 17+ with no transitive dependencies (JAR size ~50KB)
- **Standard APIs** - Uses only `javax.xml` SAX and DOM APIs
- **Backward Compatible** - Existing DOMParser and SAXParser code works unchanged
- **Flexible Parsing** - DOM tree building and event-based SAX parsing
- **Tolerant** - Handles malformed HTML gracefully
- **Modern Java** - Requires Java 17+, uses modern language features
- **Well Tested** - Comprehensive unit test coverage with JUnit 5

## 🚀 Quick Start

### Installation

Add to your `pom.xml`:

```xml
<dependency>
  <groupId>org.codelibs</groupId>
  <artifactId>nekohtml</artifactId>
  <version>3.0.0-SNAPSHOT</version>
</dependency>
```

**No other dependencies needed!** ✅

### Basic Usage - DOM Parser

```java
import org.codelibs.nekohtml.parsers.DOMParser;
import org.xml.sax.InputSource;
import org.w3c.dom.Document;
import java.io.StringReader;

// Parse HTML to DOM
DOMParser parser = new DOMParser();
parser.parse(new InputSource(new StringReader("<html><body><h1>Hello</h1></body></html>")));
Document doc = parser.getDocument();

// Query elements
System.out.println(doc.getElementsByTagName("h1").item(0).getTextContent());
```

### SAX-Based Parsing

```java
import org.codelibs.nekohtml.parsers.SAXParser;
import org.xml.sax.helpers.DefaultHandler;
import org.xml.sax.Attributes;

SAXParser parser = new SAXParser();
parser.setContentHandler(new DefaultHandler() {
    @Override
    public void startElement(String uri, String localName, String qName,
                           Attributes attributes) {
        System.out.println("Element: " + qName);
    }
});

parser.parse(new InputSource(new StringReader(html)));
```

## 📚 Documentation

- **[USAGE_EXAMPLES.md](USAGE_EXAMPLES.md)** - Comprehensive code examples
- **[MIGRATION_GUIDE.md](MIGRATION_GUIDE.md)** - Upgrading from v2.x
- **[SAX_MIGRATION_STATUS.md](SAX_MIGRATION_STATUS.md)** - v3.0 architecture details
- **API Docs** - `doc/usage.html`, `doc/settings.html`, `doc/filters.html`

## 🏗️ Project Structure

```
src/main/java/org/codelibs/nekohtml/
├── parsers/              # Parser implementations
│   ├── DOMParser.java    # DOM-based HTML parser
│   ├── SAXParser.java    # SAX-based HTML parser
│   └── SAXToDOMHandler.java
├── sax/                  # Pure SAX implementation (v3.0)
│   ├── HTMLSAXParser.java          # New SAX parser
│   ├── HTMLSAXConfiguration.java   # Configuration/pipeline
│   ├── HTMLSAXScanner.java         # Scanner wrapper
│   ├── SimpleHTMLScanner.java      # Regex-based scanner
│   ├── HTMLTagBalancerFilter.java  # Tag balancing
│   ├── HTMLQName.java              # Qualified names
│   ├── HTMLAttributesImpl.java     # Attributes
│   └── ...                         # Support classes
├── HTMLElements.java     # HTML element definitions
├── HTMLEntities.java     # Entity references
└── HTMLErrorReporter.java # Error reporting

src/test/java/            # Comprehensive test suite
└── org/codelibs/nekohtml/
    ├── parsers/          # Parser tests
    ├── sax/              # SAX implementation tests
    └── ...               # Core functionality tests
```

## 🔧 Building & Development

### Prerequisites

- **Java 17 or higher**
- **Maven 3.6+**

### Build Commands

```bash
# Compile
mvn clean compile

# Run all tests
mvn test

# Run specific test
mvn test -Dtest=DOMParserTest

# Generate coverage report
mvn verify
# Report at: target/site/jacoco/index.html

# Build JAR
mvn package

# Format code
mvn formatter:format

# Apply license headers
mvn license:format

# Generate Javadoc
mvn javadoc:javadoc
```

### Running Tests

The project uses JUnit 5 with Mockito for testing:

```bash
# All 21+ tests across the codebase
mvn test

# Test categories:
# - Parser tests (DOMParser, SAXParser)
# - SAX implementation tests
# - HTML elements and entities
# - Error handling
# - Configuration and features
```

## 🎯 Use Cases

### Extract Links from HTML

```java
DOMParser parser = new DOMParser();
parser.parse(new InputSource(new StringReader(html)));
Document doc = parser.getDocument();

NodeList links = doc.getElementsByTagName("a");
for (int i = 0; i < links.getLength(); i++) {
    Element link = (Element) links.item(i);
    System.out.println(link.getAttribute("href"));
}
```

### Parse HTML from URL

```java
import java.net.URL;

DOMParser parser = new DOMParser();
URL url = new URL("https://example.com");
parser.parse(new InputSource(url.openStream()));
Document doc = parser.getDocument();
```

### Count HTML Elements

```java
SAXParser parser = new SAXParser();
Map<String, Integer> counts = new HashMap<>();

parser.setContentHandler(new DefaultHandler() {
    @Override
    public void startElement(String uri, String localName, String qName,
                           Attributes attributes) {
        counts.merge(qName, 1, Integer::sum);
    }
});

parser.parse(new InputSource(new StringReader(html)));
counts.forEach((tag, count) -> System.out.println(tag + ": " + count));
```

## 🏛️ Architecture

### Core Components

- **HTMLSAXParser** - Pure SAX interface for HTML parsing
- **HTMLSAXConfiguration** - Pipeline orchestrator and feature management
- **SimpleHTMLScanner** - Regex-based HTML tokenizer
- **HTMLTagBalancerFilter** - SAX filter for tag balancing
- **DOMParser/SAXParser** - Backward-compatible parser interfaces

### Parsing Pipeline

```
HTML Input → SimpleHTMLScanner → HTMLTagBalancerFilter → SAX Events → DOM/Handler
```

## 📋 Requirements

- **Runtime:** Java 17 or higher
- **Build:** Maven 3.6+
- **Dependencies:** None (pure Java)

## 📦 Releases

Download from [Maven Central](https://repo1.maven.org/maven2/org/codelibs/nekohtml/)

## 🤝 Contributing

Contributions welcome! The pure Java codebase makes it easy to contribute.

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Run `mvn verify` to ensure tests pass
5. Format code: `mvn formatter:format`
6. Submit a pull request

### Code Style

- Follow existing code conventions
- Use Eclipse formatter: `src/config/eclipse/formatter/java.xml`
- Maintain test coverage
- Add tests for new features

## 📄 License

Apache License 2.0 - See [LICENSE.txt](LICENSE.txt)

