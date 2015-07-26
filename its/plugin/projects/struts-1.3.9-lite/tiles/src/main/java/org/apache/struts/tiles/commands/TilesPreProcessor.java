/*
 * $Id: TilesPreProcessor.java 471754 2006-11-06 14:55:09Z husted $
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
package org.apache.struts.tiles.commands;

import java.io.IOException;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.chain.Command;
import org.apache.commons.chain.Context;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.struts.chain.contexts.ServletActionContext;
import org.apache.struts.config.ForwardConfig;
import org.apache.struts.tiles.ComponentContext;
import org.apache.struts.tiles.ComponentDefinition;
import org.apache.struts.tiles.Controller;
import org.apache.struts.tiles.DefinitionsUtil;
import org.apache.struts.tiles.FactoryNotFoundException;
import org.apache.struts.tiles.NoSuchDefinitionException;
import org.apache.struts.tiles.TilesUtil;
import org.apache.struts.upload.MultipartRequestWrapper;


/**
 * <p>Command class intended to perform responsibilities of the
 * TilesRequestProcessor in Struts 1.1.  Does not actually dispatch requests,
 * but simply prepares the chain context for a later forward as
 * appropriate.  Should be added to a chain before something which
 * would handle a conventional ForwardConfig.</p>
 *
 * <p>This class will never have any effect on the chain unless a
 * <code>TilesDefinitionFactory</code> can be found; however it does not
 * consider the absence of a definition factory to be a fatal error; the
 * command simply returns false and lets the chain continue.</p>
 *
 * <p>To initialize the <code>TilesDefinitionFactory</code>, use
 * <code>org.apache.struts.chain.commands.legacy.TilesPlugin</code>.  This class
 * is a simple extension to <code>org.apache.struts.tiles.TilesPlugin</code>
 * which simply does not interfere with your choice of <code>RequestProcessor</code>
 * implementation.
 *  </p>
 *
 *
 */
public class TilesPreProcessor implements Command
{


    // ------------------------------------------------------ Instance Variables


    private static final Log log = LogFactory.getLog(TilesPreProcessor.class);

    // ---------------------------------------------------------- Public Methods


    /**
     * <p>If the current <code>ForwardConfig</code> is using "tiles",
     * perform necessary pre-processing to set up the <code>TilesContext</code>
     * and substitute a new <code>ForwardConfig</code> which is understandable
     * to a <code>RequestDispatcher</code>.</p>
     *
     * <p>Note that if the command finds a previously existing
     * <code>ComponentContext</code> in the request, then it
     * infers that it has been called from within another tile,
     * so instead of changing the <code>ForwardConfig</code> in the chain
     * <code>Context</code>, the command uses <code>RequestDispatcher</code>
     * to <em>include</em> the tile, and returns true, indicating that the processing
     * chain is complete.</p>
     *
     * @param context The <code>Context</code> for the current request
     *
     * @return <code>false</code> in most cases, but true if we determine
     * that we're processing in "include" mode.
     */
    public boolean execute(Context context) throws Exception {

        // Is there a Tiles Definition to be processed?
        ServletActionContext sacontext = (ServletActionContext) context;
        ForwardConfig forwardConfig = sacontext.getForwardConfig();
        if (forwardConfig == null || forwardConfig.getPath() == null)
        {
            log.debug("No forwardConfig or no path, so pass to next command.");
            return (false);
        }


        ComponentDefinition definition = null;
        try
        {
            definition = TilesUtil.getDefinition(forwardConfig.getPath(),
                    sacontext.getRequest(),
                    sacontext.getContext());
        }
        catch (FactoryNotFoundException ex)
        {
            // this is not a serious error, so log at low priority
            log.debug("Tiles DefinitionFactory not found, so pass to next command.");
            return false;
        }
        catch (NoSuchDefinitionException ex)
        {
            // ignore not found
            log.debug("NoSuchDefinitionException " + ex.getMessage());
        }

        // Do we do a forward (original behavior) or an include ?
        boolean doInclude = false;
        ComponentContext tileContext = null;

        // Get current tile context if any.
        // If context exists, or if the response has already been committed we will do an include
        tileContext = ComponentContext.getContext(sacontext.getRequest());
        doInclude = (tileContext != null || sacontext.getResponse().isCommitted());

        // Controller associated to a definition, if any
        Controller controller = null;

        // Computed uri to include
        String uri = null;

        if (definition != null)
        {
            // We have a "forward config" definition.
            // We use it to complete missing attribute in context.
            // We also get uri, controller.
            uri = definition.getPath();
            controller = definition.getOrCreateController();

            if (tileContext == null) {
                tileContext =
                        new ComponentContext(definition.getAttributes());
                ComponentContext.setContext(tileContext, sacontext.getRequest());

            } else {
                tileContext.addMissing(definition.getAttributes());
            }
        }

        // Process definition set in Action, if any.  This may override the
        // values for uri or controller found using the ForwardConfig, and
        // may augment the tileContext with additional attributes.
        // :FIXME: the class DefinitionsUtil is deprecated, but I can't find
        // the intended alternative to use.
        definition = DefinitionsUtil.getActionDefinition(sacontext.getRequest());
        if (definition != null) { // We have a definition.
                // We use it to complete missing attribute in context.
                // We also overload uri and controller if set in definition.
                if (definition.getPath() != null) {
                    log.debug("Override forward uri "
                              + uri
                              + " with action uri "
                              + definition.getPath());
                        uri = definition.getPath();
                }

                if (definition.getOrCreateController() != null) {
                    log.debug("Override forward controller with action controller");
                        controller = definition.getOrCreateController();
                }

                if (tileContext == null) {
                        tileContext =
                                new ComponentContext(definition.getAttributes());
                        ComponentContext.setContext(tileContext, sacontext.getRequest());
                } else {
                        tileContext.addMissing(definition.getAttributes());
                }
        }


        if (uri == null) {
            log.debug("no uri computed, so pass to next command");
            return false;
        }

        // Execute controller associated to definition, if any.
        if (controller != null) {
            log.trace("Execute controller: " + controller);
            controller.execute(
                    tileContext,
                    sacontext.getRequest(),
                    sacontext.getResponse(),
                    sacontext.getContext());
        }

        // If request comes from a previous Tile, do an include.
        // This allows to insert an action in a Tile.

        if (doInclude) {
            log.info("Tiles process complete; doInclude with " + uri);
            doInclude(sacontext, uri);
        } else {
            log.info("Tiles process complete; forward to " + uri);
            doForward(sacontext, uri);
        }

        log.debug("Tiles processed, so clearing forward config from context.");
        sacontext.setForwardConfig( null );
        return (false);
    }


    // ------------------------------------------------------- Protected Methods

    /**
     * <p>Do an include of specified URI using a <code>RequestDispatcher</code>.</p>
     *
     * @param context a chain servlet/web context
     * @param uri Context-relative URI to include
     */
    protected void doInclude(
        ServletActionContext context,
        String uri)
        throws IOException, ServletException {

        RequestDispatcher rd = getRequiredDispatcher(context, uri);

        if (rd != null) {
            rd.include(context.getRequest(), context.getResponse());
        }
    }

    /**
     * <p>Do an include of specified URI using a <code>RequestDispatcher</code>.</p>
     *
     * @param context a chain servlet/web context
     * @param uri Context-relative URI to include
     */
    protected void doForward(
        ServletActionContext context,
        String uri)
        throws IOException, ServletException {

        RequestDispatcher rd = getRequiredDispatcher(context, uri);

        if (rd != null) {
            rd.forward(context.getRequest(), context.getResponse());
        }
    }

    /**
     * <p>Get the <code>RequestDispatcher</code> for the specified <code>uri</code>.  If it is not found,
     * send a 500 error as a response and return null;
     *
     * @param context the current <code>ServletActionContext</code>
     * @param uri the ServletContext-relative URI of the request dispatcher to find.
     * @return the <code>RequestDispatcher</code>, or null if none is returned from the <code>ServletContext</code>.
     * @throws IOException if <code>getRequestDispatcher(uri)</code> has an error.
     */
    private RequestDispatcher getRequiredDispatcher(ServletActionContext context, String uri) throws IOException {
        RequestDispatcher rd = context.getContext().getRequestDispatcher(uri);
        if (rd == null) {
            log.debug("No request dispatcher found for " + uri);
            HttpServletResponse response = context.getResponse();
            response.sendError(
                HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                "Error getting RequestDispatcher for " + uri);
        }
        return rd;
    }

}
