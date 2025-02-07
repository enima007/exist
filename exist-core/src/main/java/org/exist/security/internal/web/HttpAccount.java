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
package org.exist.security.internal.web;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.exist.security.Subject;

import jakarta.servlet.http.HttpServletRequest;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.security.Principal;

public class HttpAccount {

    private final static Logger LOG = LogManager.getLogger(HttpAccount.class);

    public static Subject getUserFromServletRequest(final HttpServletRequest request) {
        final Principal principal = request.getUserPrincipal();
        if(principal instanceof Subject) {
            return (Subject) principal;
        } else if(principal != null && "org.eclipse.jetty.plus.jaas.JAASUserPrincipal".equals(principal.getClass().getName())) {

            //workaroud strange jetty authentication method, why encapsulate user object??? -shabanovd

            try {
                final Method method = principal.getClass().getMethod("getSubject");
                final Object obj = method.invoke(principal);
                if(obj instanceof javax.security.auth.Subject) {
                    final javax.security.auth.Subject subject = (javax.security.auth.Subject) obj;
                    for(final Principal _principal_ : subject.getPrincipals()) {
                        if(_principal_ instanceof Subject) {
                            return (Subject) _principal_;
                        }
                    }
                }
            } catch(final SecurityException | InvocationTargetException | NoSuchMethodException | IllegalAccessException | IllegalArgumentException e) {
                LOG.error(e);
            }
        }
        return null;
    }
}
