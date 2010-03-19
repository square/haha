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

import org.eclipse.mat.snapshot.ISnapshot;

/**
 * A named reference.
 */
public class NamedReference extends ObjectReference
{
    private static final long serialVersionUID = 1L;

    private String name;

    public NamedReference(ISnapshot snapshot, long address, String name)
    {
        super(snapshot, address);
        this.name = name;
    }

    public String getName()
    {
        return name;
    }
}
