/*
*  eXist Open Source Native XML Database
*  Copyright (C) 2001-04 Wolfgang M. Meier (wolfgang@exist-db.org) 
*  and others (see http://exist-db.org)
*
*  This program is free software; you can redistribute it and/or
*  modify it under the terms of the GNU Lesser General Public License
*  as published by the Free Software Foundation; either version 2
*  of the License, or (at your option) any later version.
*
*  This program is distributed in the hope that it will be useful,
*  but WITHOUT ANY WARRANTY; without even the implied warranty of
*  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
*  GNU Lesser General Public License for more details.
*
*  You should have received a copy of the GNU Lesser General Public License
*  along with this program; if not, write to the Free Software
*  Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
* 
*  $Id$
*/
package org.exist.storage.serializers;

import java.io.StringReader;
import java.util.HashMap;
import java.util.StringTokenizer;

import org.apache.log4j.Logger;
import org.exist.dom.DocumentImpl;
import org.exist.dom.QName;
import org.exist.dom.XMLUtil;
import org.exist.security.Permission;
import org.exist.security.PermissionDeniedException;
import org.exist.util.serializer.AttrList;
import org.exist.util.serializer.Receiver;
import org.exist.xquery.PathExpr;
import org.exist.xquery.XPathException;
import org.exist.xquery.XQueryContext;
import org.exist.xquery.parser.XQueryLexer;
import org.exist.xquery.parser.XQueryParser;
import org.exist.xquery.parser.XQueryTreeParser;
import org.exist.xquery.util.ExpressionDumper;
import org.exist.xquery.value.NodeValue;
import org.exist.xquery.value.Sequence;
import org.exist.xquery.value.SequenceIterator;
import org.exist.xquery.value.Type;
import org.xml.sax.SAXException;

import antlr.RecognitionException;
import antlr.TokenStreamException;
import antlr.collections.AST;

/**
 * A filter that listens for XInclude elements in the stream
 * of events generated by the {@link org.exist.storage.serializers.Serializer}.
 * 
 * XInclude elements are expanded at the position where they were found.
 */
public class XIncludeFilter implements Receiver {

	private final static Logger LOG = Logger.getLogger(XIncludeFilter.class);

	public final static String XINCLUDE_NS = "http://www.w3.org/2001/XInclude";
	
	private final static QName HREF_ATTRIB = new QName("href", "");
	
	private Receiver receiver;
	private Serializer serializer;
	private DocumentImpl document = null;
	private HashMap namespaces = new HashMap(10);

	public XIncludeFilter(Serializer serializer, Receiver receiver) {
		this.receiver = receiver;
		this.serializer = serializer;
	}

	public XIncludeFilter(Serializer serializer) {
		this(serializer, null);
	}

	public void setReceiver(Receiver handler) {
		this.receiver = handler;
	}

	public Receiver getReceiver() {
		return receiver;
	}

	public void setDocument(DocumentImpl doc) {
		document = doc;
	}

	/* (non-Javadoc)
	 * @see org.exist.util.serializer.Receiver#characters(java.lang.CharSequence)
	 */
	public void characters(CharSequence seq) throws SAXException {
		receiver.characters(seq);
	}
	
	/* (non-Javadoc)
	 * @see org.exist.util.serializer.Receiver#comment(char[], int, int)
	 */
	public void comment(char[] ch, int start, int length) throws SAXException {
		receiver.comment(ch, start, length);
	}
	
	/**
	 * @see org.xml.sax.ContentHandler#endDocument()
	 */
	public void endDocument() throws SAXException {
		receiver.endDocument();
	}

	/* (non-Javadoc)
	 * @see org.exist.util.serializer.Receiver#endElement(org.exist.dom.QName)
	 */
	public void endElement(QName qname) throws SAXException {
		if(!qname.getNamespaceURI().equals(XINCLUDE_NS))
			receiver.endElement(qname);
	}
	
	public void endPrefixMapping(String prefix) throws SAXException {
		namespaces.remove(prefix);
		receiver.endPrefixMapping(prefix);
	}

	/**
	 * @see org.xml.sax.ContentHandler#processingInstruction(java.lang.String, java.lang.String)
	 */
	public void processingInstruction(String target, String data) throws SAXException {
		receiver.processingInstruction(target, data);
	}

	/**
	 * @see org.xml.sax.ContentHandler#startDocument()
	 */
	public void startDocument() throws SAXException {
		receiver.startDocument();
	}

	/* (non-Javadoc)
	 * @see org.exist.util.serializer.Receiver#attribute(org.exist.dom.QName, java.lang.String)
	 */
	public void attribute(QName qname, String value) throws SAXException {
		receiver.attribute(qname, value);
	}
	
	/* (non-Javadoc)
	 * @see org.exist.util.serializer.Receiver#startElement(org.exist.dom.QName, org.exist.util.serializer.AttrList)
	 */
	public void startElement(QName qname, AttrList attribs) throws SAXException {
		if (qname.getNamespaceURI() != null && qname.getNamespaceURI().equals(XINCLUDE_NS)) {
			if (qname.getLocalName().equals("include")) {
				LOG.debug("processing include ...");
				processXInclude(attribs.getValue(HREF_ATTRIB));
			}
		} else {
			//LOG.debug("start: " + qName);
			receiver.startElement(qname, attribs);
		}
	}
	
	protected void processXInclude(String href) throws SAXException {
		if(href == null)
			throw new SAXException("No href attribute found in XInclude include element");
		// save some settings
		DocumentImpl prevDoc = document;
		boolean createContainerElements = serializer.createContainerElements;
		serializer.createContainerElements = false;

		// parse the href attribute
		if (href != null) {
			LOG.debug("found href=\"" + href + "\"");
			String xpointer = null;
			String docName = href;
			// try to find xpointer part
			int p = href.indexOf('#');
			if (-1 < p) {
				docName = href.substring(0, p);
				xpointer = XMLUtil.decodeAttrMarkup(href.substring(p + 1));
				LOG.debug("found xpointer: " + xpointer);
			}
			// if docName has no collection specified, assume
			// current collection 
			p = docName.lastIndexOf('/');
			if (p < 0 && document != null)
				docName = document.getCollection().getName() + '/' + docName;
			// retrieve the document
			LOG.debug("loading " + docName);
			DocumentImpl doc = null;
			try {
				doc = (DocumentImpl) serializer.broker.getDocument(docName);
				if(doc != null && !doc.getPermissions().validate(serializer.broker.getUser(), Permission.READ))
					throw new PermissionDeniedException("Permission denied to read xincluded resource");
			} catch (PermissionDeniedException e) {
				LOG.warn("permission denied", e);
				throw new SAXException(e);
			}
			/* if document has not been found and xpointer is
			 * null, throw an exception. If xpointer != null
			 * we retry below and interpret docName as
			 * a collection.
			 */
			if (doc == null && xpointer == null)
				throw new SAXException("document " + docName + " not found");
			if (xpointer == null)
				// no xpointer found - just serialize the doc
				serializer.serializeToReceiver(doc, false);
			else {
				// process the xpointer
				try {
					XQueryContext context = new XQueryContext(serializer.broker);
					if(doc != null)
						context.setStaticallyKnownDocuments(new String[] { doc.getName() } );
					else
						context.setStaticallyKnownDocuments(new String[] { docName });
					xpointer = checkNamespaces(context, xpointer);
					context.declareNamespaces(namespaces);
					
					XQueryLexer lexer = new XQueryLexer(context, new StringReader(xpointer));
					XQueryParser parser = new XQueryParser(lexer);
					XQueryTreeParser treeParser = new XQueryTreeParser(context);
					parser.xpointer();
					if (parser.foundErrors()) {
						throw new SAXException(parser.getErrorMessage());
					}

					AST ast = parser.getAST();

					PathExpr expr = new PathExpr(context);
					treeParser.xpointer(ast, expr);
					if (treeParser.foundErrors()) {
						throw new SAXException(treeParser.getErrorMessage());
					}
					LOG.info("xpointer query: " + ExpressionDumper.dump(expr));
					long start = System.currentTimeMillis();
					expr.analyze(null, 0);
					expr.reset();
					Sequence seq = expr.eval(null, null);
					if(Type.subTypeOf(seq.getItemType(), Type.NODE)) {
						LOG.info("xpointer found: " + seq.getLength());
						
						NodeValue node;
						for (SequenceIterator i = seq.iterate(); i.hasNext();) {
							node = (NodeValue) i.nextItem();
							serializer.serializeToReceiver(node, false);
						}
					} else {
						String val;
						for (int i = 0; i < seq.getLength(); i++) {
							val = seq.itemAt(i).getStringValue();
							characters(val);
						}
					}

				} catch (RecognitionException e) {
					LOG.warn("xpointer error", e);
					throw new SAXException(e);
				} catch (TokenStreamException e) {
					LOG.warn("xpointer error", e);
					throw new SAXException(e);
				} catch (XPathException e) {
					throw new SAXException(e);
				}
			}
		}
		// restore settings
		document = prevDoc;
		serializer.createContainerElements = createContainerElements;
	}

	/**
	 * @see org.xml.sax.ContentHandler#startPrefixMapping(java.lang.String, java.lang.String)
	 */
	public void startPrefixMapping(String prefix, String uri) throws SAXException {
		namespaces.put(prefix, uri);
		receiver.startPrefixMapping(prefix, uri);
	}

	/**
	 * Process xmlns() schema. We process these here, because namespace mappings should
	 * already been known when parsing the xpointer() expression.
	 * 
	 * @param context
	 * @param xpointer
	 * @return
	 * @throws XPathException
	 */
	private String checkNamespaces(XQueryContext context, String xpointer) throws XPathException {
		int p0 = -1;
		while((p0 = xpointer.indexOf("xmlns(")) > -1) {
			if(p0 < 0)
				return xpointer;
			int p1 = xpointer.indexOf(')', p0 + 6);
			if(p1 < 0)
				throw new XPathException("expected ) for xmlns()");
			String mapping = xpointer.substring(p0 + 6, p1);
			xpointer = xpointer.substring(0, p0) + xpointer.substring(p1 + 1);
			StringTokenizer tok = new StringTokenizer(mapping, "= \t\n");
			if(tok.countTokens() < 2)
				throw new XPathException("expected prefix=namespace mapping in " + mapping);
			String prefix = tok.nextToken();
			String namespaceURI = tok.nextToken();
			namespaces.put(prefix, namespaceURI);
		}
		return xpointer;
	}
}
