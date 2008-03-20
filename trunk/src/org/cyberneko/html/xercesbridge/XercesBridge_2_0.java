package org.cyberneko.html.xercesbridge;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.apache.xerces.impl.Version;
import org.apache.xerces.xni.Augmentations;
import org.apache.xerces.xni.NamespaceContext;
import org.apache.xerces.xni.XMLDocumentHandler;
import org.apache.xerces.xni.XMLLocator;

/**
 * Xerces bridge for use with Xerces 2.0.<br/>
 * This file won't compile with recent versions of Xerces, this is normal.
 * @author Marc Guillemot
 */
public class XercesBridge_2_0 extends XercesBridge 
{
	protected XercesBridge_2_0() {
		// nothing, this is the last one that will be tried
	}

	public String getVersion() {
		return Version.fVersion;
	}
	public void XMLDocumentHandler_startPrefixMapping(
			XMLDocumentHandler documentHandler, String prefix, String uri,
			Augmentations augs) {

		documentHandler.startPrefixMapping(prefix, uri, augs);
	}
	
	public void XMLDocumentHandler_endPrefixMapping(
			XMLDocumentHandler documentHandler, String prefix,
			Augmentations augs) {
		documentHandler.endPrefixMapping(prefix, augs);
	}

	public void XMLDocumentHandler_startDocument(XMLDocumentHandler documentHandler, XMLLocator locator,
			String encoding, NamespaceContext nscontext, Augmentations augs) {

		documentHandler.startDocument(locator, encoding, augs);
     }
}
