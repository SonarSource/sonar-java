/*
 * $Id: RedeployableActionServlet.java 481833 2006-12-03 17:32:52Z niallp $
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

import javax.servlet.ServletException;

import org.apache.struts.Globals;
import org.apache.struts.action.ActionServlet;
import org.apache.struts.action.RequestProcessor;
import org.apache.struts.config.ModuleConfig;
import org.apache.struts.tiles.DefinitionsFactory;
import org.apache.struts.tiles.DefinitionsFactoryException;
import org.apache.struts.tiles.TilesRequestProcessor;


/**
 * <p>
 * WebLogic (at least v6 and v7) attempts to serialize the TilesRequestProcessor
 * when re-deploying the Webapp in development mode. The TilesRequestProcessor
 * is not serializable, and loses the Tiles definitions. This results in
 * NullPointerException and/or NotSerializableException when using the app after
 * automatic redeploy.
 * </p>
 * <p>
 * This bug report proposes a workaround for this problem, in the hope it will
 * help others and maybe motivate an actual fix.
 * </p>
 * <p>
 * The attached class extends the Struts Action servlet and fixes the problem by
 * reloading the Tiles definitions when they have disappeared.
 * </p>
 * <p>
 * For background discussion see
 * http://issues.apache.org/bugzilla/show_bug.cgi?id=26322
 * </p>
 * @version $Rev: 481833 $ $Date: 2006-12-03 18:32:52 +0100 (Sun, 03 Dec 2006) $
 * @since 1.2.1
 */
public class RedeployableActionServlet extends ActionServlet {
    private TilesRequestProcessor tileProcessor;

    protected synchronized RequestProcessor
            getRequestProcessor(ModuleConfig config) throws ServletException {

        if (tileProcessor != null) {
            TilesRequestProcessor processor = (TilesRequestProcessor) super.getRequestProcessor(config);
            return processor;
        }

        // reset the request processor
        String requestProcessorKey = Globals.REQUEST_PROCESSOR_KEY +
                config.getPrefix();
        getServletContext().removeAttribute(requestProcessorKey);

        // create a new request processor instance
        TilesRequestProcessor processor = (TilesRequestProcessor) super.getRequestProcessor(config);

        tileProcessor = processor;

        try {
            // reload Tiles defs
            DefinitionsFactory factory = processor.getDefinitionsFactory();
            factory.init(factory.getConfig(), getServletContext());
            // System.out.println("reloaded tiles-definitions");
        } catch (DefinitionsFactoryException e) {
            e.printStackTrace();
        }

        return processor;
    }
}
