/*
 * $Id: PutListTag.java 471754 2006-11-06 14:55:09Z husted $
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

import java.util.ArrayList;
import java.util.List;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.tagext.TagSupport;

import org.apache.struts.tiles.AttributeDefinition;
import org.apache.struts.tiles.UntypedAttribute;

/**
 * PutList tag implementation.
 */
public class PutListTag
    extends TagSupport
    implements ComponentConstants, AddTagParent, PutListTagParent {

    /**
     * Name of this attribute.
     */
    private String attributeName = null;

    /**
     * The list itself.
     */
    private List list = null;

    /**
     * Role attribute.
     */
    private String role = null;

    /**
     * Default constructor.
     */
    public PutListTag() {
        super();
    }

    /**
     * Release all allocated resources.
     */
    public void release() {
        super.release();
        attributeName = null;
        role = null;
    }

    /**
     * Release all internal resources.
     */
    protected void releaseInternal() {
        list = null;
    }

    /**
     * Set property.
     */
    public void setName(String name) {
        this.attributeName = name;
    }

    /**
     * Get property.
     */
    public String getName() {
        return attributeName;
    }

    /**
     * Set role attribute.
     * @param role The role the user must be in to store content.
     */
    public void setRole(String role) {
        this.role = role;
    }

    /**
     * Get role attribute.
     */
    public String getRole() {
        return role;
    }

    /**
     * Get list defined in tag.
     */
    public List getList() {
        return list;
    }

    /**
     * Set property.
     */
    public void addElement(Object value) {
        if (list == null) {
            list = new ArrayList();
        }

        list.add(value);
    }

    /**
     * Process nested &lg;putList&gt; tag.
     * Method calls by nested &lg;putList&gt; tags.
     * Nested list is added to current list.
     * If role is defined, nested attribute is wrapped into an untypped definition
     * containing attribute value and role.
     */
    public void processNestedTag(PutListTag nestedTag) throws JspException {
        // Get real value and check role
        // If role is set, add it in attribute definition if any.
        // If no attribute definition, create untyped one, and set role.
        Object attributeValue = nestedTag.getList();

        if (nestedTag.getRole() != null) {
            AttributeDefinition def = new UntypedAttribute(attributeValue);
            def.setRole(nestedTag.getRole());
            attributeValue = def;
        }

        // now add attribute to enclosing parent (i.e. : this object)
        addElement(attributeValue);
    }

    /**
     * Process nested &lg;add&gt; tag.
     * Method calls by nested &lg;add&gt; tags.
     * Nested attribute is added to current list.
     * If role is defined, nested attribute is wrapped into an untypped definition
     * containing attribute value and role.
     */
    public void processNestedTag(AddTag nestedTag) throws JspException {
        // Get real value and check role
        // If role is set, add it in attribute definition if any.
        // If no attribute definition, create untyped one, and set role.
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
        addElement(attributeValue);
    }

    /**
     * Do start tag.
     */
    public int doStartTag() throws JspException {
        return EVAL_BODY_INCLUDE;
    }

    /**
     * Do end tag.
     */
    public int doEndTag() throws JspException {
        PutListTagParent enclosingParent = findEnclosingParent();
        enclosingParent.processNestedTag(this);
        // Clear list to avoid reuse
        releaseInternal();
        return EVAL_PAGE;
    }

    /**
     * Find enclosing parent tag accepting this tag.
     * @throws JspException If we can't find an appropriate enclosing tag.
     */
    protected PutListTagParent findEnclosingParent() throws JspException {
        try {
            PutListTagParent parent =
                (PutListTagParent) findAncestorWithClass(this,
                    PutListTagParent.class);

            if (parent == null) {
                throw new JspException("Error - tag putList : enclosing tag doesn't accept 'putList' tag.");
            }

            return parent;

        } catch (ClassCastException ex) {
            throw new JspException("Error - tag putList : enclosing tag doesn't accept 'putList' tag.", ex);
        }
    }

}
