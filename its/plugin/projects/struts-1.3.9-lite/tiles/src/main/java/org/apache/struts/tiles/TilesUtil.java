/*
 * $Id: TilesUtil.java 471754 2006-11-06 14:55:09Z husted $
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

package org.apache.struts.tiles;

import java.io.IOException;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.jsp.PageContext;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Class containing utility methods for Tiles.
 * Methods of this class are static and thereby accessible from anywhere.
 * The underlying implementation can be changed with
 * {@link #setTilesUtil(TilesUtilImpl)}.
 * <br>
 * Real implementation classes should derive from the {@link TilesUtilImpl} class.
 * <br>
 * Some methods are specified to throw the <code>UnsupportedOperationException</code>
 * if the underlying implementation doesn't support the operation.
 */
public class TilesUtil {

    /** Commons Logging instance.*/
    protected static Log log = LogFactory.getLog(TilesUtil.class);

    /** The implementation of tilesUtilImpl */
    protected static TilesUtilImpl tilesUtilImpl = new TilesUtilImpl();

    /**
     * Get the real implementation.
     * @return The underlying implementation object.
     */
    static public TilesUtilImpl getTilesUtil() {
        return tilesUtilImpl;
    }

    /**
     * Set the real implementation.
     * This method should be called only once.
     * Successive calls have no effect.
     * @param tilesUtil The implementaion.
     */
    static public void setTilesUtil(TilesUtilImpl tilesUtil) {
        if (implAlreadySet) {
            return;
        }
        tilesUtilImpl = tilesUtil;
        implAlreadySet = true;
    }

    /**
     * Getter to know if the underlying implementation is already set to another
     * value than the default value.
     * @return <code>true</code> if {@link #setTilesUtil} has already been called.
     */
    static boolean isTilesUtilImplSet() {
        return implAlreadySet;
    }

    /** Flag to know if internal implementation has been set by the setter method */
    private static boolean implAlreadySet = false;

    /**
     * Do a forward using request dispatcher.
     *
     * This method is used by the Tiles package anytime a forward is required.
     * @param uri Uri or Definition name to forward.
     * @param request Current page request.
     * @param response Current page response.
     * @param servletContext Current servlet context.
     */
    public static void doForward(
        String uri,
        HttpServletRequest request,
        HttpServletResponse response,
        ServletContext servletContext)
        throws IOException, ServletException {

        tilesUtilImpl.doForward(uri, request, response, servletContext);
    }

    /**
     * Do an include using request dispatcher.
     *
     * This method is used by the Tiles package when an include is required.
     * The Tiles package can use indifferently any form of this method.
     * @param uri Uri or Definition name to forward.
     * @param request Current page request.
     * @param response Current page response.
     * @param servletContext Current servlet context.
     */
    public static void doInclude(
        String uri,
        HttpServletRequest request,
        HttpServletResponse response,
        ServletContext servletContext)
        throws IOException, ServletException {

        tilesUtilImpl.doInclude(uri, request, response, servletContext);
    }

    /**
     * Do an include using PageContext.include().
     *
     * This method is used by the Tiles package when an include is required.
     * The Tiles package can use indifferently any form of this method.
     * @param uri Uri or Definition name to forward.
     * @param pageContext Current page context.
     */
    public static void doInclude(String uri, PageContext pageContext)
        throws IOException, ServletException {
        doInclude(uri, pageContext, true);
    }

    /**
     * Do an include using PageContext.include().
     *
     * This method is used by the Tiles package when an include is required.
     * The Tiles package can use indifferently any form of this method.
     * @param uri Uri or Definition name to forward.
     * @param flush If the writer should be flushed before the include
     * @param pageContext Current page context.
     */
    public static void doInclude(String uri, PageContext pageContext, boolean flush)
        throws IOException, ServletException {
        tilesUtilImpl.doInclude(uri, pageContext, flush);
    }

    /**
     * Get definition factory from appropriate servlet context.
     * @return Definitions factory or <code>null</code> if not found.
     */
    public static DefinitionsFactory getDefinitionsFactory(
        ServletRequest request,
        ServletContext servletContext) {
        return tilesUtilImpl.getDefinitionsFactory(request, servletContext);
    }

    /**
     * Create Definition factory from specified configuration object.
     * Create a ConfigurableDefinitionsFactory and initialize it with the configuration
     * object. This later can contain the factory classname to use.
     * Factory is made accessible from tags.
     * <p>
     * Fallback of several factory creation methods.
     *
     * @param servletContext Servlet Context passed to newly created factory.
     * @param factoryConfig Configuration object passed to factory.
     * @return newly created factory of type ConfigurableDefinitionsFactory.
     * @throws DefinitionsFactoryException If an error occur while initializing factory
     */
    public static DefinitionsFactory createDefinitionsFactory(
        ServletContext servletContext,
        DefinitionsFactoryConfig factoryConfig)
        throws DefinitionsFactoryException {
        return tilesUtilImpl.createDefinitionsFactory(servletContext, factoryConfig);
    }

    /**
     * Get a definition by its name.
     * First, retrieve definition factory and then get requested definition.
     * Throw appropriate exception if definition or definition factory is not found.
     * @param definitionName Name of requested definition.
     * @param request Current servelet request.
     * @param servletContext current servlet context.
     * @throws FactoryNotFoundException Can't find definition factory.
     * @throws DefinitionsFactoryException General error in factory while getting definition.
     * @throws NoSuchDefinitionException No definition found for specified name
     */
    public static ComponentDefinition getDefinition(
        String definitionName,
        ServletRequest request,
        ServletContext servletContext)
        throws FactoryNotFoundException, DefinitionsFactoryException {

        try {
            return getDefinitionsFactory(request, servletContext).getDefinition(
                definitionName,
                (HttpServletRequest) request,
                servletContext);

        } catch (NullPointerException ex) { // Factory not found in context
            throw new FactoryNotFoundException("Can't get definitions factory from context.");
        }
    }

    /**
     * Reset internal state.
     * This method is used by test suites to reset the class to its original state.
     */
    protected static void testReset() {
        implAlreadySet = false;
        tilesUtilImpl = new TilesUtilImpl();
    }

}
