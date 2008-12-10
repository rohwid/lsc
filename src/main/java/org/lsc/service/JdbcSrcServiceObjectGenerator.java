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
package org.lsc.service;

import org.lsc.AbstractGenerator;
import org.lsc.jndi.parser.LdapAttributeType;
import org.lsc.jndi.parser.LdapObjectClass;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import java.lang.reflect.Field;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import javax.naming.NamingException;


/**
 * This class containts the JDBC Source Service generator.  In order to
 * get the right stuff for synchronization from the enhanced schema this
 * class is fine to generate the corresponding JDBC Service. Then this object
 * have to be complete.
 *
 * @author Thomas Chemineau &lt;tchemineau@linagora.com&gt;
 */
public class JdbcSrcServiceObjectGenerator extends AbstractGenerator {
    //    /** This is the bean related class name. */
    //    private String initialName;
    /** This it the mono valued attributes in list bean related class name. */
    private List<String> monoAttrs;

    /** This is the multi valued attributes list in bean related class name. */
    private List<String> multiAttrs;

    /** This is the objectname. */
    private String objectName;

    /** This is the flat objectname. */
    private String flatName;

    /** This is the local objectclass representation. */
    private LdapObjectClass objectClass;

    /**
     * Generate bean file.
     *
     * @param className the related class name
     *
     * @return the generation status
     *
     * @throws NamingException thrown if an directory exception is encountered
     *         while generating the new bean
     */
    public final boolean generate(final String className)
                           throws NamingException {
        setClassName(className);

        if (!((getOcs() != null) && (getOcs().size() > 0)
                && (getAttrs() != null) && (getAttrs().size() > 0))) {
            LOGGER.error("Generator have to be initialized");

            return false;
        }

        objectName = className.substring(0, 1).toUpperCase()
                     + className.substring(1);
        flatName = "f" + className.substring(0, 1).toUpperCase()
                   + className.substring(1);

        setClassName(className.substring(0, 1).toUpperCase()
                     + className.substring(1) + "JDBCService");
        setPackageName(getGenericPackageName());

        // Get ldap object class of corresponding new Service bean
        final Map<String, LdapAttributeType> ats = 
            new HashMap<String, LdapAttributeType>();
        final Iterator<String> atIter = getAttrs().iterator();

        while (atIter.hasNext()) {
            final String atStr = atIter.next();
            final LdapAttributeType lat = LdapAttributeType.parse(atStr);

            if (lat != null) {
                ats.put(lat.getName(), lat);
            }
        }

        final Iterator<String> ocIter = getOcs().iterator();

        while (ocIter.hasNext() && (objectClass == null)) {
            final String ocStr = ocIter.next();
            final LdapObjectClass loc = LdapObjectClass.parse(ocStr, ats);

            if ((loc != null)
                    && (loc.getName().compareToIgnoreCase(className) == 0)) {
                objectClass = loc;
            }
        }

        // Try to generate files
        boolean ret = true;

        if (objectClass != null) {
            this.monoAttrs = objectClass.getMonoAttrs();
            this.multiAttrs = objectClass.getMultiAttrs();

            // Generate Class content
            if (!writeContent(generateContent())) {
                LOGGER.info("JndiObject generation failed for "
                            + this.getFileName());
                ret &= false;
            } else {
                LOGGER.info("JndiObject generation successed for "
                            + this.getFileName());
                ret &= true;
            }

            // OK -> Generate XML file
            String myFlatPackage = this.getClass().getPackage().getName();
            myFlatPackage = myFlatPackage.substring(0,
        	    myFlatPackage.lastIndexOf(".")) + ".objects.flat";

            final String inheritFrom = myFlatPackage + ".f"
                                       + objectClass.getInheritFrom()
                                                    .substring(0, 1)
                                                    .toUpperCase()
                                       + objectClass.getInheritFrom()
                                                    .substring(1);
            final String myXMLFilename = getMyXMLFileName();

            if (!writeXMLContent(myXMLFilename,
                             generateAssociatedXMLContent(inheritFrom))) {
                LOGGER.info("Associated XML file generation failed for "
                            + myXMLFilename);
                ret &= false;
            } else {
                LOGGER.info("Associated XML file generation successed for "
                            + myXMLFilename);
                ret &= true;
            }
        } else {
            LOGGER.error("JndiObject generation failed : LDAP objectClass ("
                         + className
                         + ") could not be found in LDAP directory.");
            ret &= false;
        }

        return ret;
    }

    /**
     * Generate the xml persistence filename according to environment
     * and destination, if set.
     *
     * @return the xml persistence filename
     */
    public final String getMyXMLFileName() {
        String myXMLPackage = this.getClass().getPackage().getName();
        myXMLPackage = myXMLPackage.substring(0, myXMLPackage.lastIndexOf("."))
                       + ".persistence.xml";

        String mainLocation = null;

        if (getDestination() != null) {
            mainLocation = getDestination();
        } else {
            mainLocation = System.getProperty("user.dir") + getSeparator()
                           + "src" + getSeparator() + "main" + getSeparator()
                           + "java";
        }

        return mainLocation + getSeparator()
               + myXMLPackage.replaceAll("\\.", getSeparator())
               + getSeparator() + this.objectName + ".xml";
    }

    /**
     * Generate java content.
     *
     * @return Content within string representation.
     */
    protected final String generateContent() {
        String serviceClassName = super.getClassName();
        String ctn = "";

        // Content
        ctn += ("package " + this.getPackageName() + ";\n\n");

        ctn += "import java.sql.SQLException;\n";
        ctn += "import java.util.Iterator;\n\n";
        ctn += "import org.apache.log4j.Logger;\n";
        ctn += "import org.lsc.persistence.DaoConfig;\n\n";
        ctn += "import org.lsc.objects.flat.fTop;\n";
        ctn += ("import org.lsc.objects.flat." + flatName + ";\n\n");
        ctn += "import com.ibatis.sqlmap.client.SqlMapClient;\n\n";

        ctn += ("public class " + serviceClassName
               + " implements IJdbcSrcService {\n\n");
        ctn += ("	private Logger LOGGER = Logger.getLogger("
               + serviceClassName + ".class);\n\n");
        ctn += ("	private static " + serviceClassName + " INSTANCE;\n");
        ctn += "	private SqlMapClient sqlMapper;\n\n";

        ctn += ("	public " + serviceClassName + "() {\n"
               + "		sqlMapper = DaoConfig.getSqlMapClient();\n" + "	}\n\n");

        ctn += ("	public static " + serviceClassName + " getInstance() {\n"
               + "		if(INSTANCE == null) {\n\t\t\tINSTANCE = new "
               + serviceClassName + "();\n"
               + "			INSTANCE.sqlMapper = DaoConfig.getSqlMapClient();\n"
               + "\t\t}\n" + "			return INSTANCE;\n" + "	}\n\n");

        ctn += ("	public final fTop getFlatObject(final String id) {\n"
               + "		try {\n" + "			return (" + this.flatName
               + ") sqlMapper.queryForObject(\"get" + this.objectName
               + "\", id);\n" + "		} catch (SQLException e) {\n"
               + "			LOGGER.warn(\"Unable to found " + getClassName()
               + " with id=\" + id + \" (\" + e + \")\", e);\n" + "		}\n"
               + "		return null;\n" + "	}\n\n");

        ctn += ("		@SuppressWarnings(\"unchecked\")\n"
               + "	public final Iterator<String> getIdsList() {\n"
               + "		try {\n" + "			return sqlMapper.queryForList(\"get"
               + this.objectName + "List\", null).iterator();\n"
               + "		} catch (SQLException e) {\n"
               + "			LOGGER.warn(\"Unable to return objects list (\" + e + \")\", e);\n"
               + "		}\n" + "		return null;\n" + "	}\n\n");

        ctn += "}";

        return ctn;
    }

    /**
     * Generate the XML persistant map file.
     *
     * @param inheritFrom Full class name from inherited class.
     *
     * @return Content within String representation.
     */
    protected final String generateAssociatedXMLContent(final String inheritFrom) {
        Vector<String> allInheritAttrs = new Vector<String>();

        String xml = "";

        // Init XML document.
        xml = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>\n";
        xml += "<!DOCTYPE sqlMap PUBLIC \"-//iBATIS.com//DTD SQL Map 2.0//EN\" \"http://www.ibatis.com/dtd/sql-map-2.dtd\">\n\n";
        xml += ("<sqlMap namespace=\"" + getClassName() + "\">\n\n");
        xml += ("\t<typeAlias alias=\"" + objectName
               + "\" type=\"org.lsc.objects.flat." + flatName
               + "\" />\n\n");

        // Fixe resultMap :
        if ((multiAttrs.size() != 0) || (monoAttrs.size() != 0)) {
            xml += ("\t<resultMap id=\"" + this.objectName
                   + "Result\" class=\"" + this.objectName + "\">\n");

            // Get all inherit attributes
            // Stop when superclass is "Object".
            String superClass = inheritFrom;

            while ((superClass != null)
                       && (superClass.substring(superClass.lastIndexOf(".")
                                                    + 1)
                                         .compareToIgnoreCase("Object") != 0)) {
                LOGGER.debug("Get inherited attributes from " + superClass);

                try {
                    Class<?> superClassObj = Class.forName(superClass);
                    Field[] superAttrs = superClassObj.getDeclaredFields();

                    for (int i = 0; i < superAttrs.length; i++) {
                        String attr = superAttrs[i].getName();

                        if (!allInheritAttrs.contains(attr)
                                && !attr.equalsIgnoreCase("logger")) {
                            allInheritAttrs.add(attr);
                        }
                    }

                    superClass = superClassObj.getSuperclass().getSimpleName();

                    if ((superClassObj.getPackage().getName() != null)
                            && (superClassObj.getPackage().getName().length() > 0)) {
                        superClass = superClassObj.getPackage().getName()
                                     + "." + superClass;
                    }
                } catch (Exception e) {
                    LOGGER.error("Reflective Exception : " + e, e);
                    superClass = null;
                }
            }

            Iterator<String> monoAttrsIter = monoAttrs.iterator();

            while (monoAttrsIter.hasNext()) {
                String attribute = monoAttrsIter.next();

                if (!allInheritAttrs.contains(attribute)) {
                    allInheritAttrs.add(attribute);
                }
            }

            Iterator<String> multiAttrsIter = multiAttrs.iterator();

            while (multiAttrsIter.hasNext()) {
                String attribute = multiAttrsIter.next();

                if (!allInheritAttrs.contains(attribute)) {
                    allInheritAttrs.add(attribute);
                }
            }

            Iterator<String> allInheritAttrsIter = allInheritAttrs.iterator();

            while (allInheritAttrsIter.hasNext()) {
                String attr = allInheritAttrsIter.next();
                xml += ("\t\t<result property=\"" + attr + "\" column=\""
                       + attr + "\"/>\n");
            }

            xml += "\t</resultMap>\n\n";
        }

        // Fixe select :
        xml += ("\t<select id=\"get" + this.objectName + "\" resultMap=\""
               + this.objectName + "Result\" parameterClass=\"string\">\n");
        xml += "\t\t<!-- FILL IT, BE CAREFULL AT SPECIAL CHARACTER REPRESENTATION -->\n";
        xml += "\t\tSelect\n";

        Iterator<String> allInheritAttrsIter = allInheritAttrs.iterator();

        while (allInheritAttrsIter.hasNext()) {
            String attr = allInheritAttrsIter.next();
            xml += ("\t\t\t" + attr + ",\n");
        }

        xml += "\t\t\t...\n";
        xml += "\t\tFROM ...\n\t\t\tWHERE ...\n\t</select>\n\n";
        xml += ("\t<select id=\"get" + this.objectName
               + "List\" resultClass=\"string\">\n");
        xml += "\t\t<!-- FILL IT, BE CAREFULL AT SPECIAL CHARACTER REPRESENTATION -->\n";
        xml += "\t\tSelect ...\n\t\tFROM ...\n\t\t\tWHERE ...\n\t</select>\n\n</sqlMap>";

        return xml;
    }

    /**
     * Return a generic package name.
     *
     * @return A generic package name.
     */
    public final String getGenericPackageName() {
        return this.getClass().getPackage().getName();
    }

    /**
     * Write XML content in a particular file.
     *
     * @param fileName XML Filename ;
     * @param content XML content.
     *
     * @return True if the operation successed.
     */
    protected final boolean writeXMLContent(final String fileName,
                                            final String content) {
        File file = new File(fileName);

        LOGGER.info("Creating file (" + fileName + ") ...");

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("\n---------------\n" + content + "---------------\n");
        }

        try {
            if (file.exists()) {
                LOGGER.warn("XML File generation failed: file (" + fileName
                            + ") already exists.");
            } else if (file.createNewFile()) {
                FileOutputStream os = new FileOutputStream(file);
                os.write(content.getBytes());

                return true;
            } else {
                LOGGER.error("XML File generation failed: file (" + fileName
                       + ") could not be created (probably a rights issue).");
            }
        } catch (FileNotFoundException fnfe) {
            LOGGER.error(fnfe, fnfe);
        } catch (IOException e) {
            LOGGER.error(e, e);
        }

        return false;
    }

    /**
     * Launch the whole bean generation process.
     * @param className the bean related class name
     * @param destination the destination directory
     * @return the generated jdbc source service status
     * @throws NamingException thrown if an directory exception is encountered
     *         while generating the new bean
     */
    public final String run(final String className, final String destination)
                      throws NamingException {
        init(false);
        setDestination(destination);
        generate(className);
        return getPackageName() + "." + getClassName();
    }

    /**
     * Return a generic file name for latest generated file.
     * @return A java generic file name.
     */
    public final String getFileName() {
	return getStandardFileName();
    }
}