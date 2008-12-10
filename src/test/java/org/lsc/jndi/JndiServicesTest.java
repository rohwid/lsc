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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.BasicAttribute;
import javax.naming.directory.DirContext;
import javax.naming.directory.ModificationItem;
import javax.naming.directory.SearchControls;

import org.lsc.jndi.JndiModificationType;
import org.lsc.jndi.JndiModifications;
import org.lsc.jndi.JndiServices;

import junit.framework.TestCase;

/**
 * Launch the jndi tests.
 * 
 * @author Sebastien Bahloul &lt;seb@lsc-project.org&gt;
 */
public class JndiServicesTest extends TestCase {

    /**
     * Just check that the connection is ready.
     */
    public final void testConnection() {
        assertEquals(true, JndiServices.getDstInstance().exists(""));
    }

    public final void testGetAttrList() {
        Map values = null;
        try {
            values = JndiServices.getDstInstance().getAttrList("",
                    JndiServices.DEFAULT_FILTER, SearchControls.OBJECT_SCOPE,
            "objectClass");
            assertEquals(1, values.size());
            assertNotNull(values.get(values.keySet().iterator().next()));
            assertNotNull(JndiServices.getDstInstance().getSchema(
                    new String[] { "objectclasses" }));

        } catch (NamingException ne) {
            System.err.println(ne);
            assertNotNull(null);
        }
    }

    public final void testSup() {
        try {
            assertEquals(null, JndiServices.getDstInstance().sup("", -1));
            assertEquals(new ArrayList(), JndiServices.getDstInstance().sup(
                    "ou=People", 1));
            List<String> test2list = new ArrayList<String>();
            test2list.add("ou=test2,ou=test3");
            assertEquals(test2list, JndiServices.getDstInstance().sup(
                    "ou=test1,ou=test2,ou=test3", 1));
            test2list.add(0, "ou=test1,ou=test2,ou=test3");
            assertEquals(test2list, JndiServices.getDstInstance().sup(
                    "ou=test1,ou=test2,ou=test3", 0));
        } catch (NamingException ne) {
            System.err.println(ne);
            assertNotNull(null);
        }
    }

    public final void testGetDnList() {
        List<String> test2list = new ArrayList<String>();
        test2list.add("");
        try {
            assertEquals(test2list, JndiServices.getDstInstance().getDnList("",
                    JndiServices.DEFAULT_FILTER, SearchControls.OBJECT_SCOPE));
        } catch (NamingException ne) {
            System.err.println(ne);
            assertNotNull(null);
        }
    }

    public final void testReadEntry() {
        try {
            assertNotNull(JndiServices.getDstInstance().readEntry("", false));
        } catch (NamingException ne) {
            System.err.println(ne);
            assertNotNull(null);
        }
    }

    public final void testApplyModifications() {
        try {
            Map values = JndiServices.getDstInstance().getAttrList("ou=People",
                    JndiServices.DEFAULT_FILTER, SearchControls.OBJECT_SCOPE,
            "description");
            Attribute descAttr = new BasicAttribute("description");
            String descValue = (String) values.get(values.keySet().iterator()
                    .next());
            try {
                int n = Integer.parseInt(descValue
                        .substring(descValue.length() - 1));
                n += 1;
                descValue = descValue.substring(0, descValue.length() - 1) + n;
            } catch (NumberFormatException e) {
                descValue = descValue + "-1";
            }
            descAttr.add(descValue);
            JndiModifications jm = new JndiModifications(
                    JndiModificationType.MODIFY_ENTRY);
            jm.setDistinguishName("ou=People");
            ModificationItem mi = new ModificationItem(
                    DirContext.REPLACE_ATTRIBUTE, descAttr);
            List<ModificationItem> mis = new ArrayList<ModificationItem>();
            mis.add(mi);
            jm.setModificationItems(mis);
            assertNotNull(JndiServices.getDstInstance().apply(jm));
        } catch (NamingException ne) {
            System.err.println(ne);
            assertNotNull(null);
        }
    }

    /**
     * Test the retrieve of the complete directory.
     */
    public final void testAttrPagedResultsList() {
        try {
            System.out.println("Counting all the directory entries ...");
            Map<String, String> results = JndiServices.getDstInstance().
            getAttrList("", "objectClass=*", SearchControls.ONELEVEL_SCOPE,
            "objectClass");
            Iterator iter = results.keySet().iterator();
            int i = 0;
            for (; iter.hasNext(); i++) {
                String key = (String) iter.next();
                String value = results.get(key);
                System.out.println("key=" + key + ", value=" + value);
            }
            System.out.println(" Final count : " + i);
        } catch (NamingException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
}