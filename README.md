NekoHTML [![Java CI with Maven](https://github.com/codelibs/nekohtml/actions/workflows/maven.yml/badge.svg)](https://github.com/codelibs/nekohtml/actions/workflows/maven.yml)
========

NekoHTML is a tolerant HTML parser and tag balancer for Java. It provides SAX and DOM parsers, a small filter pipeline, and robust handling of legacy and malformed HTML. This fork is based on CyberNeko HTML Parser 1.9.22 with comprehensive HTML5 support.

Features
- Tag balancing for malformed HTML; tolerant scanning.
- **HTML5 Support**: DOCTYPE recognition, void elements, semantic elements, and advanced parsing features.
- **HTML5 Advanced Features**: Optional tag omission, foster parenting, active formatting elements reconstruction, and template element processing.
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

HTML5 Features
NekoHTML now supports modern HTML5 parsing with:
- **DOCTYPE Detection**: Automatic HTML5 mode when `<!DOCTYPE html>` is detected
- **Void Elements**: Proper handling of HTML5 void elements (area, base, br, col, embed, hr, img, input, link, meta, param, source, track, wbr)
- **Semantic Elements**: Full support for HTML5 semantic elements (article, aside, header, footer, nav, main, section, etc.)
- **Optional Tag Omission**: Smart auto-closing of optional tags following HTML5 specification
- **Foster Parenting**: Correct handling of misplaced content in table contexts
- **Active Formatting Elements**: Reconstruction of formatting elements across block boundaries
- **Template Elements**: Special processing for HTML5 template elements

Example with HTML5:
```java
String html5 = "<!DOCTYPE html><article><header><h1>Title</h1></header><p>Content<p>More content</article>";
DOMParser parser = new DOMParser();
parser.parse(new InputSource(new StringReader(html5)));
Document doc = parser.getDocument();
```

Build & Test
- `mvn clean test` — compile and run tests (JUnit 5).
- `mvn test -Dtest="HTML5*Test"` — run HTML5-specific tests.
- `mvn package` — build the JAR in `target/`.
- `mvn verify` — full lifecycle with JaCoCo report at `target/site/jacoco/index.html`.
- `mvn formatter:format` — apply the Eclipse formatter.

Documentation
- Usage: `doc/usage.html`, `doc/settings.html`, `doc/filters.html`, and samples in `doc/sample/`.
- Release versions: https://repo1.maven.org/maven2/org/codelibs/nekohtml/

License
Apache License 2.0. See `LICENSE.txt`.
