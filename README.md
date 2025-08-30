NekoHTML [![Java CI with Maven](https://github.com/codelibs/nekohtml/actions/workflows/maven.yml/badge.svg)](https://github.com/codelibs/nekohtml/actions/workflows/maven.yml)
========

NekoHTML is a tolerant HTML parser and tag balancer for Java. It provides SAX and DOM parsers, a small filter pipeline, and robust handling of legacy and malformed HTML. This fork is based on CyberNeko HTML Parser 1.9.22.

Features
- Tag balancing for malformed HTML; tolerant scanning.
- DOM and SAX parsers (`parsers.DOMParser`, `parsers.SAXParser`).
- Pluggable filters (`filters.*`) for transformation.
- Entity support and localized messages (resources under `src/main/resources`).
- Shaded Xerces classes to reduce dependency conflicts.

Requirements
- Main branch targets Java 17 (builds with Maven).
- For Java 11 projects, use the 2.1.x line from Maven Central.

Installation (Maven)
Add this dependency (replace version with the latest from Maven Central):

```xml
<dependency>
  <groupId>org.codelibs</groupId>
  <artifactId>nekohtml</artifactId>
  <version><!-- see Maven Central link below --></version>
  <!-- https://repo1.maven.org/maven2/org/codelibs/nekohtml/ -->
  
</dependency>
```

Quick Start
```java
import org.codelibs.nekohtml.parsers.DOMParser;
import org.xml.sax.InputSource;
import org.w3c.dom.Document;
import java.io.StringReader;

DOMParser parser = new DOMParser();
parser.parse(new InputSource(new StringReader("<html><title>T</title><body>Hi")));
Document doc = parser.getDocument();
```

Build & Test
- `mvn clean test` — compile and run tests (JUnit 5).
- `mvn package` — build the JAR in `target/`.
- `mvn verify` — full lifecycle with JaCoCo report at `target/site/jacoco/index.html`.
- `mvn formatter:format` — apply the Eclipse formatter.

Documentation
- Usage: `doc/usage.html`, `doc/settings.html`, `doc/filters.html`, and samples in `doc/sample/`.
- Release versions: https://repo1.maven.org/maven2/org/codelibs/nekohtml/

License
Apache License 2.0. See `LICENSE.txt`.
