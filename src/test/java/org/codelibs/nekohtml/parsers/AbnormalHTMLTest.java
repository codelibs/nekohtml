package org.codelibs.nekohtml.parsers;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

/**
 * Tests for abnormal/unusual HTML patterns that might appear in the wild.
 * These tests verify the parser can handle edge cases beyond standard malformed HTML.
 */
public class AbnormalHTMLTest {

    @Test
    public void testEmptyDocument() throws Exception {
        // Completely empty input
        final String html = "";

        final DOMParser parser = new DOMParser();
        parser.parse(new InputSource(new java.io.StringReader(html)));

        final Document doc = parser.getDocument();
        assertNotNull(doc, "Document should not be null even for empty input");
    }

    @Test
    public void testOnlyWhitespace() throws Exception {
        // Document with only whitespace
        final String html = "   \n\t\r\n   ";

        final DOMParser parser = new DOMParser();
        parser.parse(new InputSource(new java.io.StringReader(html)));

        final Document doc = parser.getDocument();
        assertNotNull(doc, "Document should not be null for whitespace-only input");
    }

    @Test
    public void testOnlyText() throws Exception {
        // No HTML tags at all, just plain text
        final String html = "This is just plain text without any HTML tags.";

        final DOMParser parser = new DOMParser();
        parser.parse(new InputSource(new java.io.StringReader(html)));

        final Document doc = parser.getDocument();
        assertNotNull(doc, "Document should not be null for plain text");
    }

    @Test
    public void testInvalidTagNames() throws Exception {
        // Tags with numbers, special characters
        final String html =
                "<html><body><123>Text</123><div-custom>Content</div-custom><tag_underscore>Test</tag_underscore></body></html>";

        final DOMParser parser = new DOMParser();
        parser.parse(new InputSource(new java.io.StringReader(html)));

        final Document doc = parser.getDocument();
        assertNotNull(doc, "Document should not be null");
    }

    @Test
    public void testVeryLongAttributeValue() throws Exception {
        // Extremely long attribute value (10000 characters)
        final StringBuilder longValue = new StringBuilder();
        for (int i = 0; i < 10000; i++) {
            longValue.append("x");
        }

        final String html = "<html><body><div data-test='" + longValue.toString() + "'>Content</div></body></html>";

        final DOMParser parser = new DOMParser();
        parser.parse(new InputSource(new java.io.StringReader(html)));

        final Document doc = parser.getDocument();
        assertNotNull(doc, "Document should handle very long attributes");
    }

    @Test
    public void testVeryDeeplyNested() throws Exception {
        // 50 levels of nesting
        final StringBuilder html = new StringBuilder("<html><body>");
        for (int i = 0; i < 50; i++) {
            html.append("<div>");
        }
        html.append("Deep content");
        for (int i = 0; i < 50; i++) {
            html.append("</div>");
        }
        html.append("</body></html>");

        final DOMParser parser = new DOMParser();
        parser.parse(new InputSource(new java.io.StringReader(html.toString())));

        final Document doc = parser.getDocument();
        assertNotNull(doc, "Document should handle very deep nesting");

        final javax.xml.xpath.XPathFactory factory = javax.xml.xpath.XPathFactory.newInstance();
        final javax.xml.xpath.XPath xpath = factory.newXPath();
        final NodeList divs = (NodeList) xpath.evaluate("//DIV", doc, javax.xml.xpath.XPathConstants.NODESET);
        assertEquals(50, divs.getLength(), "Should have 50 DIV elements");
    }

    @Test
    public void testManyAttributes() throws Exception {
        // Element with many attributes (100 attributes)
        final StringBuilder html = new StringBuilder("<html><body><div ");
        for (int i = 0; i < 100; i++) {
            html.append("attr").append(i).append("='value").append(i).append("' ");
        }
        html.append(">Content</div></body></html>");

        final DOMParser parser = new DOMParser();
        parser.parse(new InputSource(new java.io.StringReader(html.toString())));

        final Document doc = parser.getDocument();
        assertNotNull(doc, "Document should handle elements with many attributes");
    }

    @Test
    public void testDuplicateAttributes() throws Exception {
        // Same attribute specified multiple times
        final String html = "<html><body><div class='one' class='two' id='test' id='duplicate'>Text</div></body></html>";

        final DOMParser parser = new DOMParser();
        parser.parse(new InputSource(new java.io.StringReader(html)));

        final Document doc = parser.getDocument();
        assertNotNull(doc, "Document should handle duplicate attributes");
    }

    @Test
    public void testCommentsInUnexpectedPlaces() throws Exception {
        // Comments in various unexpected locations
        final String html =
                "<html><!-- comment in html --><head><!-- comment in head --></head><!-- comment between head and body --><body><!-- comment in body --><div><!-- comment in div -->Text<!-- another comment --></div><!-- final comment --></body></html>";

        final DOMParser parser = new DOMParser();
        parser.parse(new InputSource(new java.io.StringReader(html)));

        final Document doc = parser.getDocument();
        assertNotNull(doc, "Document should handle comments everywhere");
    }

    @Test
    public void testProcessingInstructions() throws Exception {
        // XML processing instructions in HTML
        final String html = "<?xml version='1.0'?><?custom-pi data?><html><body><?another-pi?><p>Text</p></body></html>";

        final DOMParser parser = new DOMParser();
        parser.parse(new InputSource(new java.io.StringReader(html)));

        final Document doc = parser.getDocument();
        assertNotNull(doc, "Document should handle processing instructions");
    }

    @Test
    public void testMixedCaseTags() throws Exception {
        // Tags with mixed case (HTML is case-insensitive)
        final String html = "<HTML><BODY><DiV><p>Text</P></dIv></Body></html>";

        final DOMParser parser = new DOMParser();
        parser.parse(new InputSource(new java.io.StringReader(html)));

        final Document doc = parser.getDocument();
        assertNotNull(doc, "Document should handle mixed case tags");

        final javax.xml.xpath.XPathFactory factory = javax.xml.xpath.XPathFactory.newInstance();
        final javax.xml.xpath.XPath xpath = factory.newXPath();
        assertEquals(1, ((NodeList) xpath.evaluate("//DIV", doc, javax.xml.xpath.XPathConstants.NODESET)).getLength());
        assertEquals(1, ((NodeList) xpath.evaluate("//P", doc, javax.xml.xpath.XPathConstants.NODESET)).getLength());
    }

    @Test
    public void testUnicodeContent() throws Exception {
        // Various Unicode characters including emoji, CJK, etc.
        final String html = "<html><body><p>Hello 世界 🌍 Привет مرحبا</p></body></html>";

        final DOMParser parser = new DOMParser();
        parser.parse(new InputSource(new java.io.StringReader(html)));

        final Document doc = parser.getDocument();
        assertNotNull(doc, "Document should handle Unicode content");
    }

    @Test
    public void testHTMLEntities() throws Exception {
        // Various HTML entities
        final String html = "<html><body><p>&lt;&gt;&amp;&quot;&apos;&nbsp;&copy;&reg;&trade;</p></body></html>";

        final DOMParser parser = new DOMParser();
        parser.parse(new InputSource(new java.io.StringReader(html)));

        final Document doc = parser.getDocument();
        assertNotNull(doc, "Document should handle HTML entities");
    }

    @Test
    public void testInvalidHTMLEntities() throws Exception {
        // Invalid entity references
        final String html = "<html><body><p>&invalidEntity; &123; &; &&</p></body></html>";

        final DOMParser parser = new DOMParser();
        parser.parse(new InputSource(new java.io.StringReader(html)));

        final Document doc = parser.getDocument();
        assertNotNull(doc, "Document should handle invalid entities gracefully");
    }

    @Test
    public void testNumericCharacterReferences() throws Exception {
        // Numeric character references (decimal and hex)
        final String html = "<html><body><p>&#65; &#x41; &#9731; &#x263A;</p></body></html>";

        final DOMParser parser = new DOMParser();
        parser.parse(new InputSource(new java.io.StringReader(html)));

        final Document doc = parser.getDocument();
        assertNotNull(doc, "Document should handle numeric character references");
    }

    @Test
    public void testSpecialCharactersInAttributes() throws Exception {
        // Special characters in attribute values
        final String html = "<html><body><div data-test='<>&\"' onclick='alert(\"test\")'>Text</div></body></html>";

        final DOMParser parser = new DOMParser();
        parser.parse(new InputSource(new java.io.StringReader(html)));

        final Document doc = parser.getDocument();
        assertNotNull(doc, "Document should handle special characters in attributes");
    }

    @Test
    public void testConsecutiveTagsWithoutWhitespace() throws Exception {
        // Multiple tags without any whitespace between them
        final String html =
                "<html><head><title>Test</title></head><body><div><span>A</span><span>B</span><span>C</span></div></body></html>";

        final DOMParser parser = new DOMParser();
        parser.parse(new InputSource(new java.io.StringReader(html)));

        final Document doc = parser.getDocument();
        assertNotNull(doc, "Document should handle consecutive tags");

        final javax.xml.xpath.XPathFactory factory = javax.xml.xpath.XPathFactory.newInstance();
        final javax.xml.xpath.XPath xpath = factory.newXPath();
        final NodeList spans = (NodeList) xpath.evaluate("//SPAN", doc, javax.xml.xpath.XPathConstants.NODESET);
        assertEquals(3, spans.getLength(), "Should have 3 SPAN elements");
    }

    @Test
    public void testSelfClosingSyntaxOnNonVoidElements() throws Exception {
        // Using self-closing syntax on elements that aren't void elements
        final String html = "<html><body><div /><span /><p /></body></html>";

        final DOMParser parser = new DOMParser();
        parser.parse(new InputSource(new java.io.StringReader(html)));

        final Document doc = parser.getDocument();
        assertNotNull(doc, "Document should handle self-closing syntax on non-void elements");
    }

    @Test
    public void testMultipleRootElements() throws Exception {
        // Multiple HTML elements (invalid but might occur)
        final String html = "<html><body>First</body></html><html><body>Second</body></html>";

        final DOMParser parser = new DOMParser();
        parser.parse(new InputSource(new java.io.StringReader(html)));

        final Document doc = parser.getDocument();
        assertNotNull(doc, "Document should handle multiple root elements");
    }

    @Test
    public void testBareAngleBrackets() throws Exception {
        // Angle brackets not part of tags
        final String html = "<html><body><p>5 < 10 and 10 > 5</p></body></html>";

        final DOMParser parser = new DOMParser();
        parser.parse(new InputSource(new java.io.StringReader(html)));

        final Document doc = parser.getDocument();
        assertNotNull(doc, "Document should handle bare angle brackets");
    }

    @Test
    public void testUnterminatedComment() throws Exception {
        // Comment without proper closing
        final String html = "<html><body><!-- This comment never ends<p>Text</p></body></html>";

        final DOMParser parser = new DOMParser();
        parser.parse(new InputSource(new java.io.StringReader(html)));

        final Document doc = parser.getDocument();
        assertNotNull(doc, "Document should handle unterminated comments");
    }

    @Test
    public void testNestedComments() throws Exception {
        // Attempt to nest comments (not valid in HTML)
        final String html = "<html><body><!-- outer <!-- inner --> outer --><p>Text</p></body></html>";

        final DOMParser parser = new DOMParser();
        parser.parse(new InputSource(new java.io.StringReader(html)));

        final Document doc = parser.getDocument();
        assertNotNull(doc, "Document should handle nested comment attempts");
    }

    @Test
    public void testBogusCommentLike() throws Exception {
        // Bogus comment-like constructs
        final String html = "<html><body><?comment?><! comment ><p>Text</p></body></html>";

        final DOMParser parser = new DOMParser();
        parser.parse(new InputSource(new java.io.StringReader(html)));

        final Document doc = parser.getDocument();
        assertNotNull(doc, "Document should handle bogus comment constructs");
    }

    @Test
    public void testNullCharacters() throws Exception {
        // Null characters in content (U+0000)
        final String html = "<html><body><p>Text\u0000with\u0000nulls</p></body></html>";

        final DOMParser parser = new DOMParser();
        parser.parse(new InputSource(new java.io.StringReader(html)));

        final Document doc = parser.getDocument();
        assertNotNull(doc, "Document should handle null characters");
    }

    @Test
    public void testControlCharacters() throws Exception {
        // Various control characters
        final String html = "<html><body><p>Text\u0001\u0002\u0003\u0004with\u001F\u007Fcontrols</p></body></html>";

        final DOMParser parser = new DOMParser();
        parser.parse(new InputSource(new java.io.StringReader(html)));

        final Document doc = parser.getDocument();
        assertNotNull(doc, "Document should handle control characters");
    }

    @Test
    public void testTagsInScriptContent() throws Exception {
        // HTML-like content inside script tags
        final String html =
                "<html><body><script>var html = '<div>test</div>'; if (x < 5 && y > 3) { alert('test'); }</script></body></html>";

        final DOMParser parser = new DOMParser();
        parser.parse(new InputSource(new java.io.StringReader(html)));

        final Document doc = parser.getDocument();
        assertNotNull(doc, "Document should handle tag-like content in scripts");
    }

    @Test
    public void testTagsInStyleContent() throws Exception {
        // Selector content in style tags
        final String html = "<html><body><style>div > p { color: red; } a[href] { text-decoration: none; }</style></body></html>";

        final DOMParser parser = new DOMParser();
        parser.parse(new InputSource(new java.io.StringReader(html)));

        final Document doc = parser.getDocument();
        assertNotNull(doc, "Document should handle selector syntax in style tags");
    }

    @Test
    public void testZeroWidthCharacters() throws Exception {
        // Zero-width characters
        final String html = "<html><body><p>Text\u200B\u200C\u200D\uFEFFwith zero-width chars</p></body></html>";

        final DOMParser parser = new DOMParser();
        parser.parse(new InputSource(new java.io.StringReader(html)));

        final Document doc = parser.getDocument();
        assertNotNull(doc, "Document should handle zero-width characters");
    }

    @Test
    public void testRightToLeftAndBidiMarks() throws Exception {
        // Right-to-left and bidi marks
        final String html = "<html><body><p>Text\u202E\u202A\u202B\u202C\u202D\u202Ewith RTL marks</p></body></html>";

        final DOMParser parser = new DOMParser();
        parser.parse(new InputSource(new java.io.StringReader(html)));

        final Document doc = parser.getDocument();
        assertNotNull(doc, "Document should handle RTL and bidi marks");
    }

    @Test
    public void testManyNestingsOfSameElement() throws Exception {
        // Many nestings of the same element type
        final String html = "<html><body><b><b><b><b><b>Bold</b></b></b></b></b></body></html>";

        final DOMParser parser = new DOMParser();
        parser.parse(new InputSource(new java.io.StringReader(html)));

        final Document doc = parser.getDocument();
        assertNotNull(doc, "Document should handle many nestings of same element");
    }

    @Test
    public void testIncompleteTag() throws Exception {
        // Tags that are incomplete
        final String html = "<html><body><div<p>Text</p></body></html>";

        final DOMParser parser = new DOMParser();
        parser.parse(new InputSource(new java.io.StringReader(html)));

        final Document doc = parser.getDocument();
        assertNotNull(doc, "Document should handle incomplete tags");
    }
}
