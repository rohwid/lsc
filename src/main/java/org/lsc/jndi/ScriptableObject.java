/*
 ****************************************************************************
 * Ldap Synchronization Connector provides tools to synchronize
 * electronic identities from a list of data sources including
 * any database with a JDBC connector, another LDAP directory,
 * flat files...
 *
 *                  ==LICENSE NOTICE==
 * 
 * Copyright (c) 2008, LSC Project 
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:

 *    * Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 *     * Neither the name of the LSC Project nor the names of its
 * contributors may be used to endorse or promote products derived from
 * this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS
 * IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
 * PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER
 * OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 *                  ==LICENSE NOTICE==
 *
 *               (c) 2008 - 2009 LSC Project
 *         Sebastien Bahloul <seb@lsc-project.org>
 *         Thomas Chemineau <thomas@lsc-project.org>
 *         Jonathan Clarke <jon@lsc-project.org>
 *         Remy-Christophe Schermesser <rcs@lsc-project.org>
 ****************************************************************************
 */
package org.lsc.jndi;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.naming.NamingException;

import org.apache.log4j.Logger;
import org.mozilla.javascript.NativeJavaObject;


/**
 * Based on rhino, this class is able to understand your LQL requests.
 * @author Sebastien Bahloul &lt;seb@lsc-project.org&gt;
 */
public class ScriptableObject {
    /** The local static LOG4J logger. */
    private static final Logger LOGGER = 
	Logger.getLogger(ScriptableObject.class);
    
    @SuppressWarnings("unchecked")
    public List<String> wrap(String methodName, final Object a, final Object b,
                              boolean listable) throws NamingException {
        Method method = null;

        try {
            List<String> aList = getList(a);
            List<String> bList = getList(b);

            if (listable) {
                method = this.getClass()
                             .getDeclaredMethod( methodName,
                                        new Class[] { List.class, List.class });

                return (List<String>) method.invoke(this,
                                                    new Object[] { aList, bList });
            } else {
                if ((aList == null) || (bList == null)) {
                    return null;
                }

                method = this.getClass()
                             .getDeclaredMethod(methodName,
                                                new Class[] {
                                                    String.class, String.class
                                                });

                List<String> results = new ArrayList<String>();
                Iterator<String> aListIter = aList.iterator();

                while (aListIter.hasNext()) {
                    String aValue = aListIter.next();
                    Iterator<String> bListIter = bList.iterator();

                    while (bListIter.hasNext()) {
                        String bValue = bListIter.next();
                        List<String> res = (List<String>) method.invoke(this,
                                                                        new Object[] {
                                                                            aValue,
                                                                            bValue
                                                                        });

                        if (res != null) {
                            results.addAll(res);
                        }
                    }
                }

                return results;
            }
        } catch (SecurityException e) {
            LOGGER.error("Programmatic error : " + e, e);
        } catch (NoSuchMethodException e) {
            LOGGER.error("Programmatic error : " + e, e);
        } catch (IllegalArgumentException e) {
            LOGGER.error("Programmatic error : " + e, e);
        } catch (IllegalAccessException e) {
            LOGGER.error("Programmatic error : " + e, e);
        } catch (InvocationTargetException e) {
            LOGGER.error("Programmatic error : " + e, e);
        }

        return null;
    }

    /**
     * Convert objects to Strings list
     *
     * @param a the original object
     *
     * @return the strings list
     */
    @SuppressWarnings("unchecked")
    public List<String> getList(final Object a) {
        List<String> aList = null;

        if (a == null) {
            return null;
        } else if (String.class.isAssignableFrom(a.getClass())) {
            aList = new ArrayList<String>();
            aList.add((String) a);

            return aList;
        } else if (List.class.isAssignableFrom(a.getClass())) {
            aList = (List<String>) a;

            return aList;
        } else if (NativeJavaObject.class.isAssignableFrom(a.getClass())) {
            aList = getList(((NativeJavaObject) a).unwrap());

            return aList;
        }

        return null;
    }

    public List<String> wrapList(String methodName, final Object a, final Object b)
                           throws NamingException {
        return wrap(methodName, a, b, true);
    }

    public List<String> wrapString(String methodName, final Object a, final Object b)
                             throws NamingException {
        return wrap(methodName, a, b, false);
    }
}
