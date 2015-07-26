/*
 * $Id: TilesRequestProcessor.java 471754 2006-11-06 14:55:09Z husted $
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

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.struts.action.ActionServlet;
import org.apache.struts.action.RequestProcessor;
import org.apache.struts.config.ForwardConfig;
import org.apache.struts.config.ModuleConfig;

/**
 * <p><strong>RequestProcessor</strong> contains the processing logic that
 * the Struts controller servlet performs as it receives each servlet request
 * from the container.</p>
 * <p>This processor subclasses the Struts RequestProcessor in order to intercept calls to forward
 * or include. When such calls are done, the Tiles processor checks if the specified URI
 * is a definition name. If true, the definition is retrieved and included. If
 * false, the original URI is included or a forward is performed.
 * <p>
 * Actually, catching is done by overloading the following methods:
 * <ul>
 * <li>{@link #processForwardConfig(HttpServletRequest,HttpServletResponse,ForwardConfig)}</li>
 * <li>{@link #internalModuleRelativeForward(String, HttpServletRequest , HttpServletResponse)}</li>
 * <li>{@link #internalModuleRelativeInclude(String, HttpServletRequest , HttpServletResponse)}</li>
 * </ul>
 * </p>
 * @since Struts 1.1
 */
public class TilesRequestProcessor extends RequestProcessor {

    /**
     * Definitions factory.
     */
    protected DefinitionsFactory definitionsFactory = null;

    /**
     * Commons Logging instance.
     */
    protected static Log log = LogFactory.getLog(TilesRequestProcessor.class);

    /**
     * Initialize this request processor instance.
     *
     * @param servlet The ActionServlet we are associated with.
     * @param moduleConfig The ModuleConfig we are associated with.
     * @throws ServletException If an error occurs during initialization.
     */
    public void init(ActionServlet servlet, ModuleConfig moduleConfig)
        throws ServletException {

        super.init(servlet, moduleConfig);
        this.initDefinitionsMapping();
    }

    /**
     * Read component instance mapping configuration file.
     * This is where we read files properties.
     */
    protected void initDefinitionsMapping() throws ServletException {
        // Retrieve and set factory for this modules
        definitionsFactory =
            (
                (TilesUtilStrutsImpl) TilesUtil
                    .getTilesUtil())
                    .getDefinitionsFactory(
                getServletContext(),
                moduleConfig);

        if (definitionsFactory == null) { // problem !

            log.info(
                "Definition Factory not found for module '"
                    + moduleConfig.getPrefix()
                    + "'. "
                    + "Have you declared the appropriate plugin in struts-config.xml ?");

            return;
        }

        log.info(
            "Tiles definition factory found for request processor '"
                + moduleConfig.getPrefix()
                + "'.");

    }

    /**
     * Process a Tile definition name.
     * This method tries to process the parameter <code>definitionName</code>
     * as a definition name.
     * It returns <code>true</code> if a definition has been processed, or
     * <code>false</code> otherwise.
     * This method is deprecated; the method without the
     * <code>contextRelative</code> parameter should be used instead.
     *
     * @param definitionName Definition name to insert.
     * @param contextRelative Is the definition marked contextRelative ?
     * @param request Current page request.
     * @param response Current page response.
     * @return <code>true</code> if the method has processed uri as a
     * definition name, <code>false</code> otherwise.
     * @deprecated use processTilesDefinition(definitionName, request, response)
     *  instead.  This method will be removed in a version after 1.3.0.
     */
    protected boolean processTilesDefinition(
        String definitionName,
        boolean contextRelative,
        HttpServletRequest request,
        HttpServletResponse response)
        throws IOException, ServletException {

        return processTilesDefinition(definitionName, request, response);

    }

    /**
     * Process a Tile definition name.
     * This method tries to process the parameter <code>definitionName</code>
     * as a definition name.
     * It returns <code>true</code> if a definition has been processed, or
     * <code>false</code> otherwise.
     *
     * @param definitionName Definition name to insert.
     * @param request Current page request.
     * @param response Current page response.
     * @return <code>true</code> if the method has processed uri as a
     * definition name, <code>false</code> otherwise.
     */
    protected boolean processTilesDefinition(
        String definitionName,
        HttpServletRequest request,
        HttpServletResponse response)
        throws IOException, ServletException {

        // Do we do a forward (original behavior) or an include ?
        boolean doInclude = false;

        // Controller associated to a definition, if any
        Controller controller = null;

        // Computed uri to include
        String uri = null;

        ComponentContext tileContext = null;

        try {
            // Get current tile context if any.
            // If context exist, we will do an include
            tileContext = ComponentContext.getContext(request);
            doInclude = (tileContext != null);
            ComponentDefinition definition = null;

            // Process tiles definition names only if a definition factory exist,
            // and definition is found.
            if (definitionsFactory != null) {
                // Get definition of tiles/component corresponding to uri.
                try {
                    definition =
                        definitionsFactory.getDefinition(
                            definitionName,
                            request,
                            getServletContext());
                } catch (NoSuchDefinitionException ex) {
                    // Ignore not found
                    log.debug("NoSuchDefinitionException " + ex.getMessage());
                }
                if (definition != null) { // We have a definition.
                    // We use it to complete missing attribute in context.
                    // We also get uri, controller.
                    uri = definition.getPath();
                    controller = definition.getOrCreateController();

                    if (tileContext == null) {
                        tileContext =
                            new ComponentContext(definition.getAttributes());
                        ComponentContext.setContext(tileContext, request);

                    } else {
                        tileContext.addMissing(definition.getAttributes());
                    }
                }
            }

            // Process definition set in Action, if any.
            definition = DefinitionsUtil.getActionDefinition(request);
            if (definition != null) { // We have a definition.
                // We use it to complete missing attribute in context.
                // We also overload uri and controller if set in definition.
                if (definition.getPath() != null) {
                    uri = definition.getPath();
                }

                if (definition.getOrCreateController() != null) {
                    controller = definition.getOrCreateController();
                }

                if (tileContext == null) {
                    tileContext =
                        new ComponentContext(definition.getAttributes());
                    ComponentContext.setContext(tileContext, request);
                } else {
                    tileContext.addMissing(definition.getAttributes());
                }
            }

        } catch (java.lang.InstantiationException ex) {

            log.error("Can't create associated controller", ex);

            throw new ServletException(
                "Can't create associated controller",
                ex);
        } catch (DefinitionsFactoryException ex) {
            throw new ServletException(ex);
        }

        // Have we found a definition ?
        if (uri == null) {
            return false;
        }

        // Execute controller associated to definition, if any.
        if (controller != null) {
            try {
                controller.execute(
                    tileContext,
                    request,
                    response,
                    getServletContext());

            } catch (Exception e) {
                throw new ServletException(e);
            }
        }

        // If request comes from a previous Tile, do an include.
        // This allows to insert an action in a Tile.
        if (log.isDebugEnabled()) {
            log.debug("uri=" + uri + " doInclude=" + doInclude);
        }

        if (doInclude) {
            doInclude(uri, request, response);
        } else {
            doForward(uri, request, response); // original behavior
        }

        return true;
    }

    /**
     * Do a forward using request dispatcher.
     * Uri is a valid uri. If response has already been commited, do an include
     * instead.
     * @param uri Uri or Definition name to forward.
     * @param request Current page request.
     * @param response Current page response.
     */
    protected void doForward(
        String uri,
        HttpServletRequest request,
        HttpServletResponse response)
        throws IOException, ServletException {

        if (response.isCommitted()) {
            this.doInclude(uri, request, response);

        } else {
            super.doForward(uri, request, response);
        }
    }

    /**
     * Overloaded method from Struts' RequestProcessor.
     * Forward or redirect to the specified destination by the specified
     * mechanism.
     * This method catches the Struts' actionForward call. It checks if the
     * actionForward is done on a Tiles definition name. If true, process the
     * definition and insert it. If false, call the original parent's method.
     * @param request The servlet request we are processing.
     * @param response The servlet response we are creating.
     * @param forward The ActionForward controlling where we go next.
     *
     * @exception IOException if an input/output error occurs.
     * @exception ServletException if a servlet exception occurs.
     */
    protected void processForwardConfig(
        HttpServletRequest request,
        HttpServletResponse response,
        ForwardConfig forward)
        throws IOException, ServletException {

        // Required by struts contract
        if (forward == null) {
            return;
        }

        if (log.isDebugEnabled()) {
            log.debug(
                "processForwardConfig("
                    + forward.getPath()
                    + ")");
        }

        // Try to process the definition.
        if (processTilesDefinition(forward.getPath(),
            request,
            response)) {
            if (log.isDebugEnabled()) {
                log.debug(
                    "  '" + forward.getPath() + "' - processed as definition");
            }
            return;
        }

        if (log.isDebugEnabled()) {
            log.debug("  '" + forward.getPath() + "' - processed as uri");
        }

        // forward doesn't contain a definition, let parent do processing
        super.processForwardConfig(request, response, forward);
    }

    /**
     * Catch the call to a module relative forward.
     * If the specified uri is a tiles definition name, insert it.
     * Otherwise, parent processing is called.
     * Do a module relative forward to specified uri using request dispatcher.
     * Uri is relative to the current module. The real uri is computed by
     * prefixing the module name.
     * <strong>This method is used internally and is not part of the public
     * API. It is advised to not use it in subclasses.</strong>
     * @param uri Module-relative URI to forward to.
     * @param request Current page request.
     * @param response Current page response.
     * @since Struts 1.1
     */
    protected void internalModuleRelativeForward(
        String uri,
        HttpServletRequest request,
        HttpServletResponse response)
        throws IOException, ServletException {

        if (processTilesDefinition(uri, request, response)) {
            return;
        }

        super.internalModuleRelativeForward(uri, request, response);
    }

    /**
     * Do a module relative include to specified uri using request dispatcher.
     * Uri is relative to the current module. The real uri is computed by
     * prefixing the module name.
     * <strong>This method is used internally and is not part of the public
     * API. It is advised to not use it in subclasses.</strong>
     * @param uri Module-relative URI to forward to.
     * @param request Current page request.
     * @param response Current page response.
     * @since Struts 1.1
     */
    protected void internalModuleRelativeInclude(
        String uri,
        HttpServletRequest request,
        HttpServletResponse response)
        throws IOException, ServletException {

        if (processTilesDefinition(uri, request, response)) {
            return;
        }

        super.internalModuleRelativeInclude(uri, request, response);
    }

    /**
     * Get associated definition factory.
     */
    public DefinitionsFactory getDefinitionsFactory() {
        return definitionsFactory;
    }

}
