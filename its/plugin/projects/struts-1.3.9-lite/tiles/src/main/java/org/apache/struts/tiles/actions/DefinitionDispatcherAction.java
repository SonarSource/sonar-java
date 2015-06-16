/*
 * $Id: DefinitionDispatcherAction.java 471754 2006-11-06 14:55:09Z husted $
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

package org.apache.struts.tiles.actions;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.struts.action.Action;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.tiles.ComponentDefinition;
import org.apache.struts.tiles.DefinitionsFactoryException;
import org.apache.struts.tiles.DefinitionsUtil;
import org.apache.struts.tiles.FactoryNotFoundException;
import org.apache.struts.tiles.NoSuchDefinitionException;
import org.apache.struts.tiles.TilesUtil;

/**
 * <p>An <strong>Action</strong> that dispatches to a Tiles Definition
 * that is named by the request parameter whose name is specified
 * by the <code>parameter</code> property of the corresponding
 * ActionMapping.
 * This action is useful in following situations:
 * <li>
 * <ul>To associate an Url to a definition</ul>
 * <ul>To use Struts &lt;html:link&gt; tag on a definition</ul>
 * </li>
 * <p>To configure the use of this action in your
 * <code>struts-config.xml</code> file, create an entry like this:</p>
 *
 * <code>
 *   &lt;action path="/saveSubscription"
 *           type="org.apache.struts.tiles.actions.DefinitionDispatcherAction"
 *           parameter="def"/&gt;
 *     &lt;forward name="success"   path="anything" //&gt;
 *     &lt;forward name="error"     path="path.to.error.page" //&gt;
 * </code>
 *
 * <p>which will use the value of the request parameter named "def"
 * to pick the appropriate definition name.
 * <p>  The value for success doesn't matter. The forward will forward to
 * appropriate definition.
 * <p> The value for error should denote a valid jsp path or definition name.
 *
 * @version $Rev: 471754 $ $Date: 2006-11-06 15:55:09 +0100 (Mon, 06 Nov 2006) $
 */
public class DefinitionDispatcherAction extends Action {

    /**
     * Commons Logging instance.
     */
    protected static Log log = LogFactory.getLog(DefinitionDispatcherAction.class);

    /**
     * Process the specified HTTP request, and create the corresponding HTTP
     * response (or forward to another web component that will create it),
     * with provision for handling exceptions thrown by the business logic.
     *
     * @param mapping The ActionMapping used to select this instance
     * @param form The optional ActionForm bean for this request (if any)
     * @param request The HTTP request we are processing
     * @param response The HTTP response we are creating
     *
     * @exception Exception if the application business logic throws
     *  an exception
     * @since Struts 1.1
     */
    public ActionForward execute(
        ActionMapping mapping,
        ActionForm form,
        HttpServletRequest request,
        HttpServletResponse response)
        throws Exception {

        // Identify the request parameter containing the method name
        // If none defined, use "def"
        String parameter = mapping.getParameter();
        if (parameter == null) {
            parameter = "def";
        }

        // Identify the method name to be dispatched to
        String name = request.getParameter(parameter);
        if (name == null) {
            log.error("Can't get parameter '" + parameter + "'.");

            return mapping.findForward("error");
        }

        // Try to dispatch to requested definition
        try {
            // Read definition from factory, but we can create it here.
            ComponentDefinition definition =
                TilesUtil.getDefinition(
                    name,
                    request,
                    getServlet().getServletContext());

            if (log.isDebugEnabled()) {
                log.debug("Get Definition " + definition);
            }

            DefinitionsUtil.setActionDefinition(request, definition);

        } catch (FactoryNotFoundException e) {
            log.error("Can't get definition factory.", e);
            return mapping.findForward("error");

        } catch (NoSuchDefinitionException e) {
            log.error("Can't get definition '" + name + "'.", e);
            return mapping.findForward("error");

        } catch (DefinitionsFactoryException e) {
            log.error("General Factory error '" + e.getMessage() + "'.", e);
            return mapping.findForward("error");

        } catch (Exception e) {
            log.error("General error '" + e.getMessage() + "'.", e);
            return mapping.findForward("error");
        }

        return mapping.findForward("success");

    }

    /**
     * @deprecated This will be removed after Struts 1.2.
     */
    protected void printError(HttpServletResponse response, String msg)
        throws IOException {
        response.setContentType("text/plain");
        PrintWriter writer = response.getWriter();
        writer.println(msg);
        writer.flush();
        writer.close();
    }
}
