/*
 * $Id: ComponentContext.java 471754 2006-11-06 14:55:09Z husted $
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

import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import javax.servlet.ServletRequest;
import javax.servlet.jsp.PageContext;

import org.apache.struts.tiles.taglib.ComponentConstants;

/**
 * Component context.
 */
public class ComponentContext implements Serializable {

    /**
     * Component attributes.
     */
    private Map attributes=null;

    /**
     * Constructor.
     */
    public ComponentContext() {
        super();
    }

    /**
     * Constructor.
     * Create a context and set specified attributes.
     * @param attributes Attributes to initialize context.
     */
    public ComponentContext(Map attributes) {
        if (attributes != null) {
            this.attributes = new HashMap(attributes);
        }
    }

    /**
     * Add all attributes to this context.
     * Copies all of the mappings from the specified attribute map to this context.
     * New attribute mappings will replace any mappings that this context had for any of the keys
     * currently in the specified attribute map.
     * @param newAttributes Attributes to add.
     */
    public void addAll(Map newAttributes) {
        if (attributes == null) {
            attributes = new HashMap(newAttributes);
            return;
        }

        attributes.putAll(newAttributes);
    }

    /**
     * Add all missing attributes to this context.
     * Copies all of the mappings from the specified attributes map to this context.
     * New attribute mappings will be added only if they don't already exist in
     * this context.
     * @param defaultAttributes Attributes to add.
     */
    public void addMissing(Map defaultAttributes) {
        if (defaultAttributes == null) {
            return;
        }

        if (attributes == null) {
            attributes = new HashMap(defaultAttributes);
            return;
        }

        Set entries = defaultAttributes.entrySet();
        Iterator iterator = entries.iterator();
        while (iterator.hasNext()) {
            Map.Entry entry = (Map.Entry) iterator.next();
            if (!attributes.containsKey(entry.getKey())) {
                attributes.put(entry.getKey(), entry.getValue());
            }
        }
    }

    /**
     * Get an attribute from context.
     * @param name Name of the attribute.
     * @return <{Object}>
     */
    public Object getAttribute(String name) {
        if (attributes == null){
            return null;
        }

        return attributes.get(name);
    }

    /**
     * Get names of all attributes.
     * @return <{Object}>
     */
    public Iterator getAttributeNames() {
        if (attributes == null) {
            return Collections.EMPTY_LIST.iterator();
        }

        return attributes.keySet().iterator();
    }

    /**
     * Put a new attribute to context.
     * @param name Name of the attribute.
     * @param value Value of the attribute.
     */
    public void putAttribute(String name, Object value) {
        if (attributes == null) {
            attributes = new HashMap();
        }

        attributes.put(name, value);
    }

    /**
     * Find object in one of the contexts.
     * Order : component then pageContext.findAttribute()
     * @param beanName Name of the bean to find.
     * @param pageContext Page context.
     * @return Requested bean or <code>null</code> if not found.
     */
    public Object findAttribute(String beanName, PageContext pageContext) {
        Object attribute = getAttribute(beanName);
        if (attribute == null) {
            attribute = pageContext.findAttribute(beanName);
        }

        return attribute;
    }

    /**
     * Get object from requested context.
     * Context can be 'component'.
     * @param beanName Name of the bean to find.
     * @param scope Search scope (see {@link PageContext}).
     * @param pageContext Page context.
     * @return requested bean or <code>null</code> if not found.
     */
    public Object getAttribute(
        String beanName,
        int scope,
        PageContext pageContext) {

        if (scope == ComponentConstants.COMPONENT_SCOPE){
            return getAttribute(beanName);
        }

        return pageContext.getAttribute(beanName, scope);
    }

    /**
     * Get component context from request.
     * @param request ServletRequest.
     * @return ComponentContext or null if context is not found or an
     * jspException is present in the request.
     */
    static public ComponentContext getContext(ServletRequest request) {
       if (request.getAttribute("javax.servlet.jsp.jspException") != null) {
           return null;
        }        return (ComponentContext) request.getAttribute(
            ComponentConstants.COMPONENT_CONTEXT);
    }

    /**
     * Store component context into request.
     * @param context ComponentContext to store.
     * @param request Request to store ComponentContext.
     */
    static public void setContext(
        ComponentContext context,
        ServletRequest request) {

        request.setAttribute(ComponentConstants.COMPONENT_CONTEXT, context);
    }
}
