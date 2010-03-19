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
 * Describes a field of an object, i.e. its name and signature.
 */
public class FieldDescriptor implements Serializable
{
    private static final long serialVersionUID = 2L;

    protected String name;
    protected int type;

    public FieldDescriptor(String name, int type)
    {
        this.name = name;
        this.type = type;
    }

    public String getName()
    {
        return name;
    }

    public int getType()
    {
        return type;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    public void setType(int type)
    {
        this.type = type;
    }

    public String getVerboseSignature()
    {
        if (type == IObject.Type.OBJECT)
            return "ref"; //$NON-NLS-1$

        String t = IPrimitiveArray.TYPE[type];
        return t.substring(0, t.length() - 2);
    }
}
