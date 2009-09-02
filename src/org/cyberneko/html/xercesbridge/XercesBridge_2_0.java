/* 
 * Copyright Marc Guillemot
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
