/*
 * $Id: CustomI18nFactorySet.java 471754 2006-11-06 14:55:09Z husted $
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

import java.util.Map;
import javax.servlet.ServletContext;
import javax.servlet.ServletRequest;
import org.apache.struts.tiles.xmlDefinition.I18nFactorySet;
/**
 * <p>Test I18nFactorySet.</p>
 *
 * @version $Rev: 471754 $ $Date: 2006-11-06 15:55:09 +0100 (Mon, 06 Nov 2006) $
 */

public class CustomI18nFactorySet extends I18nFactorySet {

    /**
     * Constructor.
     * Init the factory by reading appropriate configuration file.
     * @param servletContext Servlet context.
     * @param properties Map containing all properties.
     * @throws FactoryNotFoundException Can't find factory configuration file.
     */
    public CustomI18nFactorySet(ServletContext servletContext, Map properties)
        throws DefinitionsFactoryException {
        super(servletContext, properties);
    }

    public org.apache.struts.tiles.xmlDefinition.DefinitionsFactory createFactory(
        Object key,
        ServletRequest request,
        ServletContext servletContext)
        throws DefinitionsFactoryException {
        return super.createFactory(key, request, servletContext);
    }



}
