package org.cyberneko.html;

import junit.framework.TestCase;

/**
 * Unit tests for {@link HTMLScanner}.
 * @author Marc Guillemot
 * @version $Id: HTMLScanner.java,v 1.19 2005/06/14 05:52:37 andyc Exp $
 */
public class HTMLScannerTest extends TestCase {

	public void testisEncodingCompatible() throws Exception {
		final HTMLScanner scanner = new HTMLScanner();
		assertTrue(scanner.isEncodingCompatible("ISO-8859-1","ISO-8859-1"));
		assertTrue(scanner.isEncodingCompatible("UTF-8","UTF-8"));
		assertTrue(scanner.isEncodingCompatible("UTF-16","UTF-16"));
		assertTrue(scanner.isEncodingCompatible("US-ASCII","ISO-8859-1"));
		assertTrue(scanner.isEncodingCompatible("UTF-8","ISO-8859-1"));

		assertFalse(scanner.isEncodingCompatible("UTF-8","UTF-16"));
		assertFalse(scanner.isEncodingCompatible("ISO-8859-1","UTF-16"));
		assertFalse(scanner.isEncodingCompatible("UTF-16","Cp1252"));
	}
}
