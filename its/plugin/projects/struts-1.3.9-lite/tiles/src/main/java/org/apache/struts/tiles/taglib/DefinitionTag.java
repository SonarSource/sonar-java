/*
 * $Id: DefinitionTag.java 471754 2006-11-06 14:55:09Z husted $
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

import javax.servlet.jsp.JspException;

import org.apache.struts.tiles.taglib.util.TagUtils;
import org.apache.struts.tiles.AttributeDefinition;
import org.apache.struts.tiles.ComponentDefinition;
import org.apache.struts.tiles.UntypedAttribute;

/**
 * This is the tag handler for &lt;tiles:definition&gt;, which defines
 * a tiles (or template / component). Definition is put in requested context and can be
 * used in &lt;tiles:insert&gt.
 *
 * @version $Rev: 471754 $ $Date: 2006-11-06 15:55:09 +0100 (Mon, 06 Nov 2006) $
 */
public class DefinitionTag
    extends DefinitionTagSupport
    implements PutTagParent, PutListTagParent {

    /* JSP Tag attributes */
    /**
     * Definition identifier.
     */
    private String id = null;

    /**
     * Scope into which definition will be saved.
     */
    private String scope = null;

    /**
     * Extends attribute value.
     */
    private String extendsDefinition = null;

    /* Internal properties */
    /**
     * Template definition
     */
    private ComponentDefinition definition = null;

    /**
     * Reset member values for reuse. This method calls super.release(),
     * which invokes TagSupport.release(), which typically does nothing.
     */
    public void release() {
        super.release();
        id = null;
        page = null;
        scope = null;
        role = null;
        extendsDefinition = null;
    }

    /**
     * Release internal references.
     */
    protected void releaseInternal() {
        definition = null;
    }

    /**
     * This method is a convenience for other tags for
     * putting content into the tile definition.
     * Content is already typed by caller.
     */
    public void putAttribute(String name, Object content) {
        definition.putAttribute(name, content);
    }

    /**
     * Process nested &lg;put&gt; tag.
     * Method is called from nested &lg;put&gt; tags.
     * Nested list is added to current list.
     * If role is defined, nested attribute is wrapped into an untyped definition
     * containing attribute value and role.
     */
    public void processNestedTag(PutTag nestedTag) throws JspException {
        // Get real value and check role
        // If role is set, add it in attribute definition if any.
        // If no attribute definition, create untyped one and set role.
        Object attributeValue = nestedTag.getRealValue();
        AttributeDefinition def;

        if (nestedTag.getRole() != null) {
            try {
                def = ((AttributeDefinition) attributeValue);
            } catch (ClassCastException ex) {
                def = new UntypedAttribute(attributeValue);
            }
            def.setRole(nestedTag.getRole());
            attributeValue = def;
        }

        // now add attribute to enclosing parent (i.e. : this object)
        putAttribute(nestedTag.getName(), attributeValue);
    }

    /**
     * Process nested &lg;putList&gt; tag.
     * Method is called from nested &lg;putList&gt; tags.
     * Nested list is added to current list.
     * If role is defined, nested attribute is wrapped into an untyped definition
     * containing attribute value and role.
     */
    public void processNestedTag(PutListTag nestedTag) throws JspException {
        // Get real value and check role
        // If role is set, add it in attribute definition if any.
        // If no attribute definition, create untyped one and set role.
        Object attributeValue = nestedTag.getList();

        if (nestedTag.getRole() != null) {
            AttributeDefinition def = new UntypedAttribute(attributeValue);
            def.setRole(nestedTag.getRole());
            attributeValue = def;
        }

        // Check if a name is defined
        if (nestedTag.getName() == null) {
            throw new JspException("Error - PutList : attribute name is not defined. It is mandatory as the list is added to a 'definition'.");
        }

        // now add attribute to enclosing parent (i.e. : this object).
        putAttribute(nestedTag.getName(), attributeValue);
    }

    /**
     * Get the ID.
     * @return ID
     */
    public String getId() {
        return id;
    }

    /**
     * Set the ID.
     * @param id New ID.
     */
    public void setId(String id) {
        this.id = id;
    }

    /**
     * Get the scope.
     * @return Scope.
     */
    public String getScope() {
        return scope;
    }

    /**
     * Set the scope.
     * @param aScope Scope.
     */
    public void setScope(String aScope) {
        scope = aScope;
    }

    /**
     * Set <code>extends</code> (parent) definition name.
     * @param definitionName Name of parent definition.
     */
    public void setExtends(String definitionName) {
        this.extendsDefinition = definitionName;
    }

    /**
     * Get <code>extends</code> (parent) definition name.
     * @return Name of parent definition.
     */
    public String getExtends() {
        return extendsDefinition;
    }

    /**
     * Process the start tag by creating a new definition.
     * @throws JspException On errors processing tag.
     */
    public int doStartTag() throws JspException {
        // Do we extend a definition ?
        if (extendsDefinition != null && !extendsDefinition.equals("")) {
            ComponentDefinition parentDef =
                TagUtils.getComponentDefinition(extendsDefinition, pageContext);

            definition = new ComponentDefinition(parentDef);

        } else {
            definition = new ComponentDefinition();
        }

        // Set definitions attributes
        if (page != null) {
            definition.setTemplate(page);
        }

        if (role != null) {
            definition.setRole(role);
        }

        return EVAL_BODY_INCLUDE;
    }

    /**
     * Process the end tag by putting the definition in appropriate context.
     * @throws JspException On errors processing tag.
     */
    public int doEndTag() throws JspException {
        TagUtils.setAttribute(pageContext, id, definition, scope);

        releaseInternal();
        return EVAL_PAGE;
    }

}
