package org.cyberneko.html.xercesbridge;

import org.apache.xerces.impl.Version;
import org.apache.xerces.xni.Augmentations;
import org.apache.xerces.xni.NamespaceContext;
import org.apache.xerces.xni.XMLDocumentHandler;
import org.apache.xerces.xni.XMLLocator;
import org.apache.xerces.xni.parser.XMLDocumentFilter;
import org.apache.xerces.xni.parser.XMLDocumentSource;

/**
 * Xerces bridge for use with Xerces 2.1.
 * @author Marc Guillemot
 */
public class XercesBridge_2_1 extends XercesBridge
{
	/**
	 * Should fail for Xerces version less than 2.1 
	 * @throws InstantiationException if instantiation failed 
	 */
	public XercesBridge_2_1() throws InstantiationException {
        try {
            // Just try and see if if we're called with Xerces 2.1 or higher
            getVersion();
        } catch (final Error e) {
            throw new InstantiationException(e.getMessage());
        }
	}

	public String getVersion() {
		return new Version().getVersion();
	}

	public void XMLDocumentHandler_startDocument(XMLDocumentHandler documentHandler, XMLLocator locator,
			String encoding, NamespaceContext nscontext, Augmentations augs) {

		documentHandler.startDocument(locator, encoding, augs);
     }

	public void XMLDocumentFilter_setDocumentSource(XMLDocumentFilter filter,
			XMLDocumentSource lastSource) {
		filter.setDocumentSource(lastSource);
	}
}
