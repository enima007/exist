/*
 * Copyright © 2001, Adam Retter
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in the
 *       documentation and/or other materials provided with the distribution.
 *     * Neither the name of the <organization> nor the
 *       names of its contributors may be used to endorse or promote products
 *       derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL <COPYRIGHT HOLDER> BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.exist.extensions.exquery.restxq.impl;

import org.apache.http.HttpHost;
import org.apache.http.client.fluent.Executor;
import org.exist.TestUtils;
import org.exist.test.ExistWebServer;
import org.junit.Before;
import org.junit.Rule;
import org.w3c.dom.NodeList;

import java.io.IOException;

public abstract class AbstractInstanceIntegrationTest extends AbstractIntegrationTest {

    @Rule
    public ExistWebServer existWebServer = new ExistWebServer(true, false, true, true);

    protected Executor executor = null;

    @Before
    public void setupExecutor() {
        executor = Executor.newInstance()
                .auth(TestUtils.ADMIN_DB_USER, TestUtils.ADMIN_DB_PWD)
                .authPreemptive(new HttpHost("localhost", existWebServer.getPort()));
    }

    protected String getServerUri() {
        return getServerUri(existWebServer);
    }

    protected String getRestUri() {
        return getRestUri(existWebServer);
    }

    protected String getRestXqUri() {
        return getRestXqUri(existWebServer);
    }

    protected void enableRestXqTrigger(final String collectionPath) throws IOException {
        enableRestXqTrigger(existWebServer, executor, collectionPath);
    }

    protected void storeXquery(final String collectionPath, final String xqueryFilename, final String xquery) throws IOException {
        storeXquery(existWebServer, executor, collectionPath, xqueryFilename, xquery);
    }

    protected void removeXquery(final String collectionPath, final String xqueryFilename) throws IOException {
        removeXquery(existWebServer, executor, collectionPath, xqueryFilename);
    }

    protected void assertRestXqResourceFunctionsCount(final int expectedCount) throws IOException {
        assertRestXqResourceFunctionsCount(existWebServer, executor, expectedCount);
    }

    protected NodeList getRestXqResourceFunctions() throws IOException {
        return getRestXqResourceFunctions(existWebServer, executor);
    }
}
