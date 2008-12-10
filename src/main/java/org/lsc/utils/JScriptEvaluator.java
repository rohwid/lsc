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
package org.lsc.utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.lsc.jndi.JndiServices;
import org.lsc.jndi.ScriptableJndiServices;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Script;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;

/**
 * This is the Rhino Java Script evaluation context.
 * 
 * @author Sebastien Bahloul &lt;seb@lsc-project.org&gt;
 */
public final class JScriptEvaluator {

    /** The private unique instance. */
    private static JScriptEvaluator instance;
    /** The precompiled Javascript cache. */
    private Map<String, Script> cache;
    /** The local Rhino context. */
    private Context cx;

    /**
     * Default private constructor.
     * 
     * @see getInstance()
     */
    private JScriptEvaluator() {
        cache = new HashMap<String, Script>();
        cx = Context.enter();
    }

    /**
     * Local instance getter.
     * 
     * @return the instance
     */
    public static JScriptEvaluator getInstance() {
        if (instance == null) {
            instance = new JScriptEvaluator();
        }
        return instance;
    }

    /**
     * Please consider using evalToString
     * @deprecated
     * @param expression
     * @param params
     * @return the string result
     */
    public static String eval(final String expression,
            final Map<String, Object> params) {
        return evalToString( expression, params);
    }

    /**
     * Evaluate your Ecma script expression (manage pre-compiled expressions
     * cache).
     * 
     * @param expression
     *                the expression to eval
     * @param params
     *                the keys are the name used in the
     * @return the evaluation result
     */
    public static String evalToString(final String expression,
            final Map<String, Object> params) {
        return Context.toString(getInstance().instanceEval(expression, params));
    }

    @SuppressWarnings("unchecked")
    public static List<String> evalToStringList(final String expression,
            final Map<String, Object> params) {
        Object result = getInstance().instanceEval(expression, params);
        if(result.getClass().isAssignableFrom(List.class)) {
            return (List<String>)result;
        } else {
            List<String> resultsArray = new ArrayList<String>();
            resultsArray.add(Context.toString(result));
            return resultsArray;
        }
    }
    
    public static Boolean evalToBoolean(final String expression, final Map<String, Object> params) {
        return Context.toBoolean(getInstance().instanceEval(expression, params));
    }

    /**
     * Local instance evaluation.
     * 
     * @param expression
     *                the expression to eval
     * @param params
     *                the keys are the name used in the
     * @return the evaluation result
     */
    private Object instanceEval(final String expression,
            final Map<String, Object> params) {
        Script script = null;
        Scriptable scope = cx.initStandardObjects();

        /* Allow to have shorter names for function in the package org.lsc.utils.directory */
        String expressionImport = 
            "with (new JavaImporter(Packages.org.lsc.utils.directory)) {" + 
            "with (new JavaImporter(Packages.org.lsc.utils)) { " + expression + "}}";

        if (cache.containsKey(expressionImport)) {
            script = cache.get(expressionImport);
        } else {
            script = cx.compileString(expressionImport, "<cmd>", 1, null);
            cache.put(expressionImport, script);
        }

	// add LDAP interface for destination
        ScriptableJndiServices dstSjs = new ScriptableJndiServices();
        dstSjs.setJndiServices(JndiServices.getDstInstance());
        params.put("ldap", dstSjs);

	// add LDAP interface for source if available
	JndiServices srcInstance = JndiServices.getSrcInstance();
	if (srcInstance != null) {
		ScriptableJndiServices srcSjs = new ScriptableJndiServices();
		srcSjs.setJndiServices(srcInstance);
		params.put("srcLdap", srcSjs);
	}

        Iterator<String> paramsIter = params.keySet().iterator();
        while (paramsIter.hasNext()) {
            String name = paramsIter.next();
            Object value = params.get(name);

            Object jsObj = Context.javaToJS(value, scope);
            ScriptableObject.putProperty(scope, name, jsObj);
        }

        return script.exec(cx, scope);
    }
}