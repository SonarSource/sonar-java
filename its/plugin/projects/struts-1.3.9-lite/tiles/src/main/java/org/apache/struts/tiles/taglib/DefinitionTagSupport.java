/*
 * $Id: DefinitionTagSupport.java 471754 2006-11-06 14:55:09Z husted $
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
package org.apache.struts.tiles.taglib;

import java.io.Serializable;
import javax.servlet.jsp.tagext.TagSupport;

/**
 * Common base class for tags dealing with Tiles definitions.
 * This class defines properties used in Definition Tags.
 * It also extends TagSupport.
 */
public class DefinitionTagSupport extends TagSupport implements Serializable {
    /**
     * Associated Controller type
     */
    protected String controllerType;
    /**
     * Associated Controller name (classname or url)
     */
    protected String controllerName;
    /**
     * Role associated to definition.
     */
    protected String role;
    /**
     * Uri of page assoicated to this definition.
     */
    protected String page;

    /**
     * Release class properties.
     */
    public void release() {
        super.release();
        controllerType = null;
        controllerName = null;
        role = null;
        page = null;
    }

    /**
     * Get controller type.
     * Type can be 'classname', 'url'.
     *
     * @return Controller type.
     */
    public String getControllerType() {
        return controllerType;
    }

    /**
     * Get controller name.
     * Name denotes a fully qualified classname, or an url.
     * Exact type can be specified with {@link #setControllerType}.
     *
     * @return Controller name.
     */
    public String getControllerName() {
        return controllerName;
    }

    /**
     * Set associated controller type.
     * Type denotes a fully qualified classname.
     *
     * @param controllerType Type of associated controller.
     */
    public void setControllerType(String controllerType) {
        this.controllerType = controllerType;
    }

    /**
     * Set associated controller name.
     * Name denotes a fully qualified classname, or an url.
     * Exact type can be specified with {@link #setControllerType}.
     *
     * @param controller Controller classname or url.
     */
    public void setController(String controller) {
        setControllerName(controller);
    }

    /**
     * Set associated controller name.
     * Name denote a fully qualified classname, or an url.
     * Exact type can be specified with setControllerType.
     *
     * @param controller Controller classname or url
     */
    public void setControllerName(String controller) {
        this.controllerName = controller;
    }

    /**
     * Set associated controller name as an url, and controller
     * type as "url".
     * Name must be an url (not checked).
     * Convenience method.
     *
     * @param controller Controller url
     */
    public void setControllerUrl(String controller) {
        setControllerName(controller);
        setControllerType("url");
    }

    /**
     * Set associated controller name as a classtype and controller
     * type as "classname".
     * Name denotes a fully qualified classname.
     * Convenience method.
     *
     * @param controller Controller classname.
     */
    public void setControllerClass(String controller) {
        setControllerName(controller);
        setControllerType("classname");
    }

    /**
     * Get associated role.
     *
     * @return Associated role.
     */
    public String getRole() {
        return role;
    }

    /**
     * Set associated role.
     *
     * @param role Associated role.
     */
    public void setRole(String role) {
        this.role = role;
    }

    /**
     * Set the page.
     *
     * @param page Page.
     */
    public void setPage(String page) {
        this.page = page;
    }

    /**
     * Get the page.
     *
     * @return Page.
     */
    public String getPage() {
        return page;
    }

    /**
     * Get the template.
     * Same as getPage().
     *
     * @return Template.
     */
    public String getTemplate() {
        return page;
    }

    /**
     * Set the template.
     * Same as setPage().
     *
     * @param template Template.
     */
    public void setTemplate(String template) {
        this.page = template;
    }
}
