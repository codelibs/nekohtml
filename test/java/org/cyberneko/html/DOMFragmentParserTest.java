package org.cyberneko.html;

import java.io.StringReader;

import junit.framework.TestCase;

import org.apache.html.dom.HTMLDocumentImpl;
import org.cyberneko.html.parsers.DOMFragmentParser;
import org.w3c.dom.DocumentFragment;
import org.w3c.dom.Node;
import org.w3c.dom.bootstrap.DOMImplementationRegistry;
import org.w3c.dom.html.HTMLDocument;
import org.w3c.dom.ls.DOMImplementationLS;
import org.w3c.dom.ls.LSSerializer;
import org.xml.sax.InputSource;

/**
 * Unit tests for {@link DOMFragmentParser}.
 * @author Marc Guillemot
 *
 */
public class DOMFragmentParserTest extends TestCase {
	/**
	 * See <a href="http://sourceforge.net/support/tracker.php?aid=2828553">Bug 2828553</a>.
	 */
    public void testInvalidProcessingInstruction() throws Exception {
    	doTest("<html><?9 ?></html>", "<HTML/>");
    }

	/**
	 * See <a href="http://sourceforge.net/support/tracker.php?aid=2828534">Bug 2828534</a>.
	 */
    public void testInvalidAttributeName() throws Exception {
    	doTest("<html 9='id'></html>", "<HTML/>");
    }

    private void doTest(final String html, final String expected) throws Exception {
        DOMFragmentParser parser = new DOMFragmentParser();
        HTMLDocument document = new HTMLDocumentImpl();

        DocumentFragment fragment = document.createDocumentFragment();
        InputSource source = new InputSource(new StringReader(html));
        parser.parse(source, fragment);
//        final OutputFormat of = new OutputFormat();
//        of.setOmitXMLDeclaration(true);
//        XMLSerializer s = new XMLSerializer(of);
//        StringWriter sw = new StringWriter();
//        s.setOutputCharStream(sw);
//        s.serialize(fragment);
        DOMImplementationRegistry registry = DOMImplementationRegistry.newInstance();

        DOMImplementationLS impl = 
            (DOMImplementationLS)registry.getDOMImplementation("LS");

        LSSerializer writer = impl.createLSSerializer();
        String str = writer.writeToString(fragment);
        
        final String xmlDecl = "<?xml version=\"1.0\" encoding=\"UTF-16\"?>" + System.getProperty("line.separator");
        assertEquals(xmlDecl + expected, str);
    }

    public static void print(Node node, String indent) {
        System.out.println(indent+node.getClass().getName());
        Node child = node.getFirstChild();
        while (child != null) {
            print(child, indent+" ");
            child = child.getNextSibling();
        }
    }
}

