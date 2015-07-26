/*
 * $Id: TilesUtilStrutsModulesImpl.java 471754 2006-11-06 14:55:09Z husted $
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

import org.apache.struts.Globals;
import org.apache.struts.config.ModuleConfig;
import org.apache.struts.util.ModuleUtils;

/**
 * Implementation of TilesUtil for Struts multi modules.
 * Methods in this implementation are aware of the Struts module context.
 * <br>
 * <ul>
 * <li>The method getFactory(...) returns the factory for the current Struts
 * module.</li>
 * <li>Methods doForward() and doInclude() use their counterparts in the
 * current RequestProcessor (todo).</li>
 * <li>The method createFactory(...) creates a factory for the current module and
 * stores it under the appropriate property name.</li>
 * </ul>
 */
public class TilesUtilStrutsModulesImpl extends TilesUtilStrutsImpl {

    /**
     * Do a forward using request dispatcher.
     *
     * This method is used by the Tiles package anytime a forward is required.
     * @param uri Uri or Definition name to forward.
     * @param request Current page request.
     * @param response Current page response.
     * @param servletContext Current servlet context.
     */
    public void doForward(
        String uri,
        HttpServletRequest request,
        HttpServletResponse response,
        ServletContext servletContext)
        throws IOException, ServletException {

        request.getRequestDispatcher(uri).forward(request, response);
    }

    /**
     * Do an include using request dispatcher.
     *
     * This method is used by the Tiles package anytime an include is required.
     * @param uri Uri or Definition name to forward.
     * @param request Current page request.
     * @param response Current page response.
     * @param servletContext Current servlet context.
     */
    public void doInclude(
        String uri,
        HttpServletRequest request,
        HttpServletResponse response,
        ServletContext servletContext)
        throws IOException, ServletException {

        request.getRequestDispatcher(uri).include(request, response);
    }

    /**
     * Get the definition factory from appropriate servlet context.
     * @param request Current request.
     * @param servletContext Current servlet context.
     * @return Definitions factory or null if not found.
     */
    public DefinitionsFactory getDefinitionsFactory(
        ServletRequest request,
        ServletContext servletContext) {

        return getDefinitionsFactory(
            servletContext,
            getModuleConfig((HttpServletRequest) request, servletContext));
    }

    /**
     * Get definition factory for the module attached to specified moduleConfig.
     * @param servletContext Current servlet context.
     * @param moduleConfig Module config of the module for which the factory is requested.
     * @return Definitions factory or null if not found.
     */
    public DefinitionsFactory getDefinitionsFactory(
        ServletContext servletContext,
        ModuleConfig moduleConfig) {

        return (DefinitionsFactory) servletContext.getAttribute(
            DEFINITIONS_FACTORY + moduleConfig.getPrefix());
    }

    /**
     * Make definition factory accessible to tags.
     * Factory is stored in servlet context.
     * @param factory Factory to be made accessible.
     * @param servletContext Current servlet context.
     */
    protected void makeDefinitionsFactoryAccessible(
        DefinitionsFactory factory,
        ServletContext servletContext) {

        String prefix = factory.getConfig().getFactoryName();
        servletContext.setAttribute(DEFINITIONS_FACTORY + prefix, factory);
    }

    /**
     * Get Tiles RequestProcessor associated to the current module.
     * @param request Current request.
     * @param servletContext Current servlet context.
     * @return The {@link TilesRequestProcessor} for the current request.
     */
    protected TilesRequestProcessor getRequestProcessor(
        HttpServletRequest request,
        ServletContext servletContext) {

        ModuleConfig moduleConfig = getModuleConfig(request, servletContext);

        return (TilesRequestProcessor) servletContext.getAttribute(
            Globals.REQUEST_PROCESSOR_KEY + moduleConfig.getPrefix());
    }

    /**
     * Get the current ModuleConfig.
     * <br>
     * Lookup in the request and do selectModule if not found. The side effect
     * is, that the ModuleConfig object is set in the request if it was not present.
     * @param request Current request.
     * @param servletContext Current servlet context*.
     * @return The ModuleConfig for current request.
     */
    protected ModuleConfig getModuleConfig(
        HttpServletRequest request,
        ServletContext servletContext) {

        ModuleConfig moduleConfig =
            ModuleUtils.getInstance().getModuleConfig(request);

        if (moduleConfig == null) {
            // ModuleConfig not found in current request. Select it.
            ModuleUtils.getInstance().selectModule(request, servletContext);
            moduleConfig = ModuleUtils.getInstance().getModuleConfig(request);
        }

        return moduleConfig;
    }

}
