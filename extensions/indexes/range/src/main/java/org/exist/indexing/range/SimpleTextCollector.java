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
package org.exist.indexing.range;

import org.exist.dom.persistent.AttrImpl;
import org.exist.dom.persistent.AbstractCharacterData;
import org.exist.dom.QName;
import org.exist.storage.NodePath;
import org.exist.util.XMLString;

import java.util.ArrayList;
import java.util.List;

public class SimpleTextCollector implements TextCollector {

    private boolean includeNested = true;
    private RangeIndexConfigElement config = null;
    private XMLString buf = new XMLString();
    private int wsTreatment = XMLString.SUPPRESS_NONE;
    private boolean caseSensitive = true;

    public SimpleTextCollector(RangeIndexConfigElement config, boolean includeNested, int wsTreatment, boolean caseSensitive) {
        this.config = config;
        this.includeNested = includeNested;
        this.wsTreatment = wsTreatment;
        this.caseSensitive = caseSensitive;
    }

    public SimpleTextCollector(String content) {
        buf.append(content);
    }

    @Override
    public void startElement(QName qname, NodePath path) {
    }

    @Override
    public void endElement(QName qname, NodePath path) {
    }

    @Override
    public void characters(AbstractCharacterData text, NodePath path) {
        if (includeNested || config.match(path)) {
            buf.append(text.getXMLString());
        }
    }

    @Override
    public void attribute(AttrImpl attribute, NodePath path) {
    }

    @Override
    public int length() {
        return buf.length();
    }

    @Override
    public boolean hasFields() {
        return false;
    }

    @Override
    public List<Field> getFields() {
        List<Field> fields = new ArrayList<>(1);
        fields.add(new Field(buf, wsTreatment, caseSensitive));
        return fields;
    }
}
