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
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;

import org.apache.log4j.Logger;
import org.lsc.Configuration;
import org.lsc.LscAttributes;
import org.lsc.LscObject;

/**
 * This class is an abstract generic but configurable implementation to get data
 * from the directory.
 * 
 * You can specify where (baseDn) and what (filterId & attr) information will be
 * read on which type of entries (filterAll and attrId).
 * 
 * @author Sebastien Bahloul &lt;seb@lsc-project.org&gt;
 */
public abstract class AbstractSimpleJndiService {
    
    /** This is the local LOG4J logger. */
    protected static final Logger LOGGER = 
        Logger.getLogger(AbstractSimpleJndiService.class);

	/**
	 * The filter to be completed by replacing {0} by the id to find a
	 * unique entry.
	 */
	private String filterId;

	/**
	 * The filter used to identify all the entries that have to be
	 * synchronized by this JndiSrcService.
	 */
	private String filterAll;

	/** Where to find the entries. */
	private String baseDn;

	/**
	 * When finding entries with 'filterAll' filter, the attribute to read
	 * to reuse it in 'filterId' filter.
	 */
	private List<String> attrsId;

	/**
	 * When a single entry is read in the directory, the attributes array to
	 * read - Used to limit at the source, the synchronization perimeter.
	 */
	private List<String> attrs;

	/**
	 * The default initializer.
	 * 
	 * @param serviceProps The default simple JNDI properties
	 */
	public AbstractSimpleJndiService(final Properties serviceProps) {
		baseDn = serviceProps.getProperty("baseDn");
		filterId = serviceProps.getProperty("filterId");
		filterAll = serviceProps.getProperty("filterAll");
		
		String attrsValue = serviceProps.getProperty("attrs");
		if (attrsValue != null) {
			attrs = Configuration.getListFromString(attrsValue);
		}

		String attrsIdValue = serviceProps.getProperty("pivotAttrs");
		if(attrsIdValue != null) {
		    attrsId = Configuration.getListFromString(attrsIdValue);
		}
	}

    /**
     * Map the ldap search result into a top derivated object.
     *
     * @param sr the ldap search result
     * @param objToFill the original object to fill
     *
     * @return the object modified
     *
     * @throws NamingException thrown if a directory exception is encountered
     *         while switching to the Java POJO
     */
    public final LscObject getObjectFromSR(final SearchResult sr, final LscObject objToFill)
                              throws NamingException {
        Method[] methods = objToFill.getClass().getMethods();
        Map<String, Method> localMethods = new HashMap<String, Method>();

        if (sr==null) return null;
        
        for (int i = 0; i < methods.length; i++) {
            localMethods.put(methods[i].getName(), methods[i]);
        }

        NamingEnumeration<?> ne = sr.getAttributes().getAll();

        while (ne.hasMore()) {
            Attribute attr = (Attribute) ne.next();
            String methodName = "set"
                                + attr.getID().substring(0, 1).toUpperCase()
                                + attr.getID().substring(1);

            if (localMethods.containsKey(methodName)) {
                try {
                    Class<?>[] paramsType = localMethods.get(methodName)
                                                     .getParameterTypes();

                    if (List.class.isAssignableFrom(paramsType[0])) {
                        localMethods.get(methodName)
                                    .invoke(objToFill, new Object[] { getValue(attr.getAll()) });
                    } else if (String.class.isAssignableFrom(paramsType[0])) {
                        localMethods.get(methodName)
                                    .invoke(objToFill, new Object[] { getValue(attr.getAll()).get(0) });
                    } else {
                        throw new RuntimeException("Unable to manage data type !");
                    }
                } catch (IllegalArgumentException e) {
                    throw new RuntimeException(e);
                } catch (IllegalAccessException e) {
                    throw new RuntimeException(e);
                } catch (InvocationTargetException e) {
                    throw new RuntimeException(e);
                }
            } else {
                LOGGER.debug("Unable to map search result attribute to "
                            + attr.getID() + " attribute object !");
            }
        }

        return objToFill;
    }

    /**
     * Get a list of object values from the NamingEnumeration.
     *
     * @param ne the naming enumeration
     *
     * @return the object list
     *
     * @throws NamingException thrown if a directory exception is encountered
     *         while switching to the Java POJO
     */
    protected static List<?> getValue(final NamingEnumeration<?> ne)
                            throws NamingException {
        List<Object> l = new ArrayList<Object>();

        while (ne.hasMore()) {
            l.add(ne.next());
        }
        return l;
    }
	
	/**
	 * Get the ldap search result according the specified identifier.
	 * 
	 * @param id The object identifier - used in the directory filter as {0}
	 * @return The ldap search result
	 * @throws NamingException
	 *                 thrown if an directory exception is encountered while
	 *                 getting the identified object
	 */
	public final SearchResult get(final LscAttributes ids) throws NamingException {
		SearchControls sc = new SearchControls();
		String[] attributes = new String[attrs.size()];
		attributes = attrs.toArray(attributes);
		sc.setReturningAttributes(attributes);
		
		String searchString = filterId;
		Iterator<String> ite = ids.getAttributesNames().iterator();
		while (ite.hasNext()) {
            String id = ite.next();
            searchString = searchString.replaceAll("\\{" + id + "\\}", ids.getStringValueAttribute(id));
        }
		
		return getJndiServices().getEntry(baseDn, searchString, sc);
	}

	/**
	 * LDAP Services getter to fit to the context - source or destination. 
	 * @return the JndiServices object used to apply directory operations
	 */
	public abstract JndiServices getJndiServices();

	/**
	 * Default attrId getter.
	 * @return the attrId value
	 */
	public final List<String> getAttrsId() {
		return attrsId;
	}

	/**
	 * Default attributes getter.
	 * @return the attrs array
	 */
	public final List<String> getAttrs() {
		return attrs;
	}

	/**
	 * Default base distinguish name getter.
	 * @return the baseDn value
	 */
	public final String getBaseDn() {
		return baseDn;
	}

	/**
	 * Default filter getter, for all corresponding entries.
	 * @return the filterAll value
	 */
	public final String getFilterAll() {
		return filterAll;
	}

	/**
	 * Default filter getter, for one corresponding entry.
	 * @return the attrId value
	 */
	public final String getFilterId() {
		return filterId;
	}
}