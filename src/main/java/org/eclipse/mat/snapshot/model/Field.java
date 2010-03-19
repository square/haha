/*******************************************************************************
 * Copyright (c) 2008 SAP AG.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    SAP AG - initial API and implementation
 *******************************************************************************/
package org.eclipse.mat.snapshot.model;

import java.io.Serializable;

/**
 * Describes a member variable, i.e. name, signature and value.
 */
public final class Field extends FieldDescriptor implements Serializable
{
    private static final long serialVersionUID = 2L;

    protected Object value;

    public Field(String name, int type, Object value)
    {
        super(name, type);
        this.value = value;
    }

    public Object getValue()
    {
        return value;
    }

    public void setValue(Object object)
    {
        value = object;
    }

    public String toString()
    {
        return type + " " + name + ": \t" + value; //$NON-NLS-1$//$NON-NLS-2$
    }
}
