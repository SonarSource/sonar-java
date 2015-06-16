/*
 * $Id: XmlAttribute.java 471754 2006-11-06 14:55:09Z husted $
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

package org.apache.struts.tiles.xmlDefinition;

import org.apache.struts.tiles.DefinitionNameAttribute;
import org.apache.struts.tiles.DirectStringAttribute;
import org.apache.struts.tiles.PathAttribute;
import org.apache.struts.tiles.UntypedAttribute;

/**
 * A property key-value pair.  This class is used to read configuration files.
 */
public class XmlAttribute {

    /**
     * Attribute name or key.
     */
    private String name = null;

    /**
     * Attribute value.
     * Value read from description file.
     */
    private Object value = null;

    /**
     * Attribute value.
     */
    private String direct = null;

    /**
     * Attribute value.
     */
    private String valueType = null;

    /**
     * Attribute value.
     */
    private String role = null;

    /**
     * Real attribute value.
     * Real value is the value after processing of valueType.
     * I.e. if a type is defined, realValue contains wrapper for this type.
     */
    private Object realValue = null;

    /**
     * Constructor.
     */
    public XmlAttribute() {
        super();
    }

    /**
     * Constructor.
     */
    public XmlAttribute(String name, Object value) {
        this.name = name;
        this.value = value;
    }

    /**
     * Access method for the name property.
     *
     * @return The current value of the name property.
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the value of the name property.
     *
     * @param role the new value of the name property
     */
    public void setRole(String role) {
        this.role = role;
    }

    /**
     * Access method for the name property.
     *
     * @return The current value of the name property.
     */
    public String getRole() {
        return role;
    }

    /**
     * Sets the value of the name property.
     *
     * @param aName the new value of the name property.
     */
    public void setName(String aName) {
        name = aName;
    }

    /**
     * Another access method for the name property.
     *
     * @return   the current value of the name property
     */
    public String getAttribute() {
        return name;
    }

    /**
     * Sets the value of the name property.
     *
     * @param aName the new value of the name property
     */
    public void setAttribute(String aName) {
        name = aName;
    }

    /**
     * Access method for the value property. Return the value or a
     * QualifiedAttribute containing the value if 'direct' is set.
     *
     * @return The current value of the value property.
     */
    public Object getValue() {
        // Compatibility with JSP Template
        if (this.realValue == null) {
            this.realValue = this.computeRealValue();
        }

        return this.realValue;
    }

    /**
     * Sets the value of the value property.
     *
     * @param aValue the new value of the value property
     */
    public void setValue(Object aValue) {
        realValue = null;
        value = aValue;
    }

    /**
     * Sets the value of the value property.
     *
     * @param aValue the new value of the value property
     */
    public void setContent(Object aValue) {
        setValue(aValue);
    }

    /**
     * Sets the value of the value property.
     *
     * @param body the new value of the value property
     */
    public void setBody(String body) {
        if (body.length() == 0) {
            return;
        }

        setValue(body);
    }

    /**
     * Sets the value of the value property.
     *
     * @param value the new value of the value property
     */
    public void setDirect(String value) {
        this.direct = value;
    }

    /**
     * Sets the value of the value property.
     *
     * @param value the new value of the value property
     */
    public void setType(String value) {
        this.valueType = value;
    }

    /**
     * Compute  real value from attributes setting.
     */
    protected Object computeRealValue() {
        Object realValue = value;
        // Is there a type set ?
        // First check direct attribute, and translate it to a valueType.
        // Then, evaluate valueType, and create requested typed attribute.
        if (direct != null) {
            this.valueType =
                Boolean.valueOf(direct).booleanValue() ? "string" : "path";
        }

        if (value != null && valueType != null) {
            String strValue = value.toString();

            if (valueType.equalsIgnoreCase("string")) {
                realValue = new DirectStringAttribute(strValue);

            } else if (valueType.equalsIgnoreCase("page")) {
                realValue = new PathAttribute(strValue);

            } else if (valueType.equalsIgnoreCase("template")) {
                realValue = new PathAttribute(strValue);

            } else if (valueType.equalsIgnoreCase("instance")) {
                realValue = new DefinitionNameAttribute(strValue);
            }

            // Set realValue's role value if needed
            if (role != null) {
                ((UntypedAttribute) realValue).setRole(role);
            }
        }

        // Create attribute wrapper to hold role if role is set and no type
        // specified
        if (role != null && value != null && valueType == null) {
            realValue = new UntypedAttribute(value.toString(), role);
        }

        return realValue;
    }
}
