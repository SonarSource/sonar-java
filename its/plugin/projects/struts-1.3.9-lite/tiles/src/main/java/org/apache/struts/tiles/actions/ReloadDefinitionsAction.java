/*
 * $Id: ReloadDefinitionsAction.java 471754 2006-11-06 14:55:09Z husted $
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

import java.io.PrintWriter;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.struts.action.Action;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.tiles.DefinitionsFactory;
import org.apache.struts.tiles.DefinitionsFactoryException;
import org.apache.struts.tiles.TilesUtil;



/**
 * <p>A standard <strong>Action</strong> that calls the
 * <code>reload()</code> method of our controller servlet to
 * reload its configuration information from the configuration
 * files (which have presumably been updated) dynamically.</p>
 *
 * @version $Rev: 471754 $ $Date: 2006-11-06 15:55:09 +0100 (Mon, 06 Nov 2006) $
 */

public class ReloadDefinitionsAction extends Action {

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
    public ActionForward execute(ActionMapping mapping,
                                 ActionForm form,
                                 HttpServletRequest request,
                                 HttpServletResponse response)
        throws Exception
    {
        response.setContentType("text/plain");
        PrintWriter writer = response.getWriter();

        try {
          ServletContext context = getServlet().getServletContext();
            DefinitionsFactory factory = TilesUtil.getDefinitionsFactory(request, context);
            factory.setConfig(factory.getConfig(), context);
            writer.println("OK");
        } catch (ClassCastException e) {
            writer.println("FAIL - " + e.toString());
            getServlet().log("ReloadAction", e);
        } catch (DefinitionsFactoryException e) {
            writer.println("FAIL - " + e.toString());
            getServlet().log("ReloadAction", e);
        }

        writer.flush();
        writer.close();

        return (null);

    }

}
