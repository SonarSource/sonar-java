/*
 * $Id: TagUtils.java 471754 2006-11-06 14:55:09Z husted $
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.struts.tiles.taglib.util;

import java.lang.reflect.InvocationTargetException;
import java.util.Map;
import java.util.HashMap;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.PageContext;

import org.apache.commons.beanutils.PropertyUtils;
import org.apache.struts.Globals;
import org.apache.struts.tiles.taglib.ComponentConstants;
import org.apache.struts.tiles.ComponentContext;
import org.apache.struts.tiles.ComponentDefinition;
import org.apache.struts.tiles.DefinitionsFactoryException;
import org.apache.struts.tiles.FactoryNotFoundException;
import org.apache.struts.tiles.NoSuchDefinitionException;
import org.apache.struts.tiles.TilesUtil;

/**
 * Collection of utilities.
 * This class also serves as an interface between Components and Struts. If
 * you want to rip away Struts, simply reimplement some methods in this class.
 * You can copy them from Struts.
 *
 */
public class TagUtils {

    /** Debug flag */
    public static final boolean debug = true;

    /**
     * Maps lowercase JSP scope names to their PageContext integer constant
     * values.
     */
    private static final Map scopes = new HashMap();

    /**
     * Initialize the scope names map and the encode variable with the
     * Java 1.4 method if available.
     */
    static {
        scopes.put("page", new Integer(PageContext.PAGE_SCOPE));
        scopes.put("request", new Integer(PageContext.REQUEST_SCOPE));
        scopes.put("session", new Integer(PageContext.SESSION_SCOPE));
        scopes.put("application", new Integer(PageContext.APPLICATION_SCOPE));
    }


    /**
    * Get scope value from string value
    * @param scopeName Scope as a String.
    * @param defaultValue Returned default value, if not found.
    * @return Scope as an <code>int</code>, or <code>defaultValue</code> if scope is <code>null</code>.
    * @throws JspException Scope name is not recognized as a valid scope.
    */
    public static int getScope(String scopeName, int defaultValue) throws JspException {
        if (scopeName == null) {
            return defaultValue;
        }

        if (scopeName.equalsIgnoreCase("component")) {
            return ComponentConstants.COMPONENT_SCOPE;

        } else if (scopeName.equalsIgnoreCase("template")) {
            return ComponentConstants.COMPONENT_SCOPE;

        } else if (scopeName.equalsIgnoreCase("tile")) {
            return ComponentConstants.COMPONENT_SCOPE;

        } else {
            return getScope(scopeName);
        }
    }

    /**
     * Converts the scope name into its corresponding PageContext constant value.
     * @param scopeName Can be "page", "request", "session", or "application" in any
     * case.
     * @return The constant representing the scope (ie. PageContext.REQUEST_SCOPE).
     * @throws JspException if the scopeName is not a valid name.
     */
    public static int getScope(String scopeName) throws JspException {
        Integer scope = (Integer) scopes.get(scopeName.toLowerCase());

        if (scope == null) {
            //throw new JspException(messages.getMessage("lookup.scope", scope));
            throw new JspException("Unable to retrieve the scope "+scopeName);
        }

        return scope.intValue();
    }


    /**
     * Retrieve bean from page context, using specified scope.
     * If scope is not set, use <code>findAttribute()</code>.
     *
     * @param beanName Name of bean to retrieve.
     * @param scopeName Scope or <code>null</code>. If <code>null</code>, bean is searched using
     *  findAttribute().
     * @param pageContext Current pageContext.
     * @return Requested bean or <code>null</code> if not found.
     * @throws JspException Scope name is not recognized as a valid scope.
     */
    public static Object retrieveBean(String beanName, String scopeName, PageContext pageContext)
        throws JspException {

        if (scopeName == null) {
            return findAttribute(beanName, pageContext);
        }

        // Default value doesn't matter because we have already check it
        int scope = getScope(scopeName, PageContext.PAGE_SCOPE);

        //return pageContext.getAttribute( beanName, scope );
        return getAttribute(beanName, scope, pageContext);
    }

    /**
     * Search attribute in different contexts.
     * First, check in component context, then use pageContext.findAttribute().
     * @param beanName Name of bean to retrieve.
     * @param pageContext Current pageContext.
     * @return Requested bean or <code>null</code> if not found.
     */
    public static Object findAttribute(String beanName, PageContext pageContext) {
        ComponentContext compContext = ComponentContext.getContext(pageContext.getRequest());

        if (compContext != null) {
            Object attribute = compContext.findAttribute(beanName, pageContext);
            if (attribute != null) {
                return attribute;
            }
        }

        // Search in pageContext scopes
        return pageContext.findAttribute(beanName);
    }

    /**
     * Get object from requested context. Return <code>null</code> if not found.
     * Context can be "component" or normal JSP contexts.
     * @param beanName Name of bean to retrieve.
     * @param scope Scope from which bean must be retrieved.
     * @param pageContext Current pageContext.
     * @return Requested bean or <code>null</code> if not found.
     */
    public static Object getAttribute(String beanName, int scope, PageContext pageContext) {
        if (scope == ComponentConstants.COMPONENT_SCOPE) {
            ComponentContext compContext = ComponentContext.getContext(pageContext.getRequest());
            return compContext.getAttribute(beanName);
        }
        return pageContext.getAttribute(beanName, scope);
    }

    /**
     * Locate and return the specified property of the specified bean, from
     * an optionally specified scope, in the specified page context.
     *
     * @param pageContext Page context to be searched.
     * @param beanName Name of the bean to be retrieved.
     * @param beanProperty Name of the property to be retrieved, or
     *  <code>null</code> to retrieve the bean itself.
     * @param beanScope Scope to be searched (page, request, session, application)
     *  or <code>null</code> to use <code>findAttribute()</code> instead.
     *
     * @exception JspException Scope name is not recognized as a valid scope
     * @exception JspException if the specified bean is not found
     * @exception JspException if accessing this property causes an
     *  IllegalAccessException, IllegalArgumentException,
     *  InvocationTargetException, or NoSuchMethodException
     */
    public static Object getRealValueFromBean(
        String beanName,
        String beanProperty,
        String beanScope,
        PageContext pageContext)
        throws JspException {

        try {
            Object realValue;
            Object bean = retrieveBean(beanName, beanScope, pageContext);
            if (bean != null && beanProperty != null) {
                realValue = PropertyUtils.getProperty(bean, beanProperty);
            } else {
                realValue = bean; // value can be null
            }
            return realValue;

        } catch (NoSuchMethodException ex) {
            throw new JspException(
                "Error - component.PutAttributeTag : Error while retrieving value from bean '"
                    + beanName
                    + "' with property '"
                    + beanProperty
                    + "' in scope '"
                    + beanScope
                    + "'. (exception : "
                    + ex.getMessage(), ex);

        } catch (InvocationTargetException ex) {
            throw new JspException(
                "Error - component.PutAttributeTag : Error while retrieving value from bean '"
                    + beanName
                    + "' with property '"
                    + beanProperty
                    + "' in scope '"
                    + beanScope
                    + "'. (exception : "
                    + ex.getMessage(), ex);

        } catch (IllegalAccessException ex) {
            throw new JspException(
                "Error - component.PutAttributeTag : Error while retrieving value from bean '"
                    + beanName
                    + "' with property '"
                    + beanProperty
                    + "' in scope '"
                    + beanScope
                    + "'. (exception : "
                    + ex.getMessage(), ex);
        }
    }

    /**
     * Store bean in requested context.
     * If scope is <code>null</code>, save it in REQUEST_SCOPE context.
     *
     * @param pageContext Current pageContext.
     * @param name Name of the bean.
     * @param scope Scope under which bean is saved (page, request, session, application)
     *  or <code>null</code> to store in <code>request()</code> instead.
     * @param value Bean value to store.
     *
     * @exception JspException Scope name is not recognized as a valid scope
     */
    public static void setAttribute(
        PageContext pageContext,
        String name,
        Object value,
        String scope)
        throws JspException {

        if (scope == null)
            pageContext.setAttribute(name, value, PageContext.REQUEST_SCOPE);
        else if (scope.equalsIgnoreCase("page"))
            pageContext.setAttribute(name, value, PageContext.PAGE_SCOPE);
        else if (scope.equalsIgnoreCase("request"))
            pageContext.setAttribute(name, value, PageContext.REQUEST_SCOPE);
        else if (scope.equalsIgnoreCase("session"))
            pageContext.setAttribute(name, value, PageContext.SESSION_SCOPE);
        else if (scope.equalsIgnoreCase("application"))
            pageContext.setAttribute(name, value, PageContext.APPLICATION_SCOPE);
        else {
            throw new JspException("Error - bad scope name '" + scope + "'");
        }
    }

    /**
     * Store bean in REQUEST_SCOPE context.
     *
     * @param pageContext Current pageContext.
     * @param name Name of the bean.
     * @param beanValue Bean value to store.
     *
     * @exception JspException Scope name is not recognized as a valid scope
     */
    public static void setAttribute(PageContext pageContext, String name, Object beanValue)
        throws JspException {
        pageContext.setAttribute(name, beanValue, PageContext.REQUEST_SCOPE);
    }

    /**
     * Save the specified exception as a request attribute for later use.
     *
     * @param pageContext The PageContext for the current page.
     * @param exception The exception to be saved.
     */
    public static void saveException(PageContext pageContext, Throwable exception) {
        pageContext.setAttribute(Globals.EXCEPTION_KEY, exception, PageContext.REQUEST_SCOPE);
    }

    /**
     * Get component definition by its name.
     * @param name Definition name.
     * @param pageContext The PageContext for the current page.
     * @throws JspException -
     */
    public static ComponentDefinition getComponentDefinition(String name, PageContext pageContext)
        throws JspException {

        try {
            return TilesUtil.getDefinition(
                name,
                pageContext.getRequest(),
                pageContext.getServletContext());

        } catch (NoSuchDefinitionException ex) {
            throw new JspException(
                "Error : Can't get component definition for '"
                    + name
                    + "'. Check if this name exist in component definitions.",ex);
        } catch (FactoryNotFoundException ex) { // factory not found.
            throw new JspException(ex);

        } catch (DefinitionsFactoryException ex) {
            if (debug)
                ex.printStackTrace();
            // Save exception to be able to show it later
            saveException(pageContext, ex);
            throw new JspException(ex);
        }
    }

}
