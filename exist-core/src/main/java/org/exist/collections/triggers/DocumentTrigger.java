/*
 * eXist-db Open Source Native XML Database
 * Copyright (C) 2001 The eXist-db Authors
 *
 * info@exist-db.org
 * http://www.exist-db.org
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 */
package org.exist.collections.triggers;

import org.exist.collections.Collection;
import org.exist.dom.persistent.DocumentImpl;
import org.exist.storage.DBBroker;
import org.exist.storage.txn.Txn;
import org.exist.xmldb.XmldbURI;

import java.util.Map;

/**
 * Interface for triggers that react to document-related events.
 * 
 * Document triggers may have two roles:
 * 
 * <ol>
 *  <li>before the document is stored, updated or removed, the trigger's {@link org.exist.collections.triggers.XQueryTrigger#prepare(int, DBBroker, Txn, XmldbURI, XmldbURI, boolean)}
 *  method is called. The trigger code may take any action desired, for example, to ensure referential
 *  integrity on the database, issue XUpdate commands on other documents in the database...</li>
 *  <li>the trigger also functions as a filter: the trigger interface extends SAX {@link org.xml.sax.ContentHandler content handler} and
 *  {@link org.xml.sax.ext.LexicalHandler lexical handler}. It will thus receive any SAX events generated by the SAX parser. The default
 *  implementation just forwards the SAX events to the indexer, i.e. the output content handler. However,
 *  a trigger may also alter the received SAX events before it forwards them to the indexer, for example,
 *  by applying a stylesheet.</li>
 * </ol>
 * 
 * The DocumentTrigger interface is also called for binary resources. However, in this case, the trigger can not function as
 * a filter and the SAX-related methods are useless. Only {@link org.exist.collections.triggers.XQueryTrigger#prepare(int, DBBroker, Txn, XmldbURI, XmldbURI, boolean)} and
 * {@link  org.exist.collections.triggers.XQueryTrigger#finish(int, DBBroker, Txn, XmldbURI, XmldbURI, boolean)} will be called. To determine if the document is a binary resource,
 * call {@link org.exist.dom.persistent.DocumentImpl#getResourceType()}.
 * 
 * The general contract for a trigger is as follows:
 * 
 * <ol>
 *  <li>configuration phase: whenever the collection loads its configuration file, the trigger's 
 *  {@link org.exist.collections.triggers.XQueryTrigger#configure(DBBroker, Txn, Collection, Map)} method
 *  will be called once.</li>
 *  <li>pre-parse phase: before parsing the source document, the collection will call the trigger's
 *  {@link org.exist.collections.triggers.XQueryTrigger#prepare(int, DBBroker, Txn, XmldbURI, XmldbURI, boolean) prepare}
 *  method once for each document to be stored, removed or updated. The trigger may
 *  throw a TriggerException if the current action should be aborted.</li>
 *  <li>validation phase: during the validation phase, the document is parsed once by the SAX parser. During this
 *  phase, the trigger may decide to throw a SAXException to report a problem. Validation will fail and the action
 *  is aborted.</li>
 *  <li>storage phase: the document is again parsed by the SAX parser. The trigger will still receive all SAX events,
 *  but it is not allowed to throw an exception. Throwing an exception during the storage phase will result in an
 *  invalid document in the database.</li>
 *  <li>finalization: the method {@link org.exist.collections.triggers.XQueryTrigger#finish(int, DBBroker, Txn, XmldbURI, XmldbURI, boolean)} is called. At this point, the document
 *  has already been stored and is ready to be used or - for {@link #REMOVE_DOCUMENT_EVENT} - has been removed.
 *  </li>
 * </ol>
 * 
 * @author wolf
 */
public interface DocumentTrigger extends Trigger {

    /**
     * This method is called once before the database will actually parse the input data. You may take any action
     * here, using the supplied broker instance.
     * 
     * @param broker eXist-db DBBroker
     * @param txn transaction
     * @param uri the uri
     * @throws TriggerException in case of an error
     */
    void beforeCreateDocument(DBBroker broker, Txn txn, XmldbURI uri) throws TriggerException;
    
    /**
     * This method is called after the operation completed. At this point, the document has already
     * been stored.
     *
     * @param broker eXist-db DBBroker
     * @param txn transaction
     * @param document stored document
     * @throws TriggerException in case of an error
     */
    void afterCreateDocument(DBBroker broker, Txn txn, DocumentImpl document) throws TriggerException;

    void beforeUpdateDocument(DBBroker broker, Txn txn, DocumentImpl document) throws TriggerException;
    void afterUpdateDocument(DBBroker broker, Txn txn, DocumentImpl document) throws TriggerException;

    void beforeUpdateDocumentMetadata(DBBroker broker, Txn txn, DocumentImpl document) throws TriggerException;
    void afterUpdateDocumentMetadata(DBBroker broker, Txn txn, DocumentImpl document) throws TriggerException;

    void beforeCopyDocument(DBBroker broker, Txn txn, DocumentImpl document, XmldbURI newUri) throws TriggerException;
    void afterCopyDocument(DBBroker broker, Txn txn, DocumentImpl document, XmldbURI oldUri) throws TriggerException;

    void beforeMoveDocument(DBBroker broker, Txn txn, DocumentImpl document, XmldbURI newUri) throws TriggerException;
    void afterMoveDocument(DBBroker broker, Txn txn, DocumentImpl document, XmldbURI oldUri) throws TriggerException;

    void beforeDeleteDocument(DBBroker broker, Txn txn, DocumentImpl document) throws TriggerException;
    void afterDeleteDocument(DBBroker broker, Txn txn, XmldbURI uri) throws TriggerException;

    /**
     * Returns true if the SAX parser is currently in validation phase. During validation phase, the trigger
     * may safely throw a SAXException. However, if is {@link #isValidating() isValidating} returns false, no exceptions should be
     * thrown.
     * 
     * @return true if the parser is in validation phase.
     */
    boolean isValidating();

    /**
     * Called by the database to report that it is entering validation phase.
     * 
     * @param validating enable or disable validation
     */
    void setValidating(boolean validating);
}
