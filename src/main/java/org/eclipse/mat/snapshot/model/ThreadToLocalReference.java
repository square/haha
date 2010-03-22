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
 * The class represents a references from a running thread object to objects
 * which are local for this thread. Such objects could be for example java local
 * variables, objects used for synchronization in this thread, etc...
 */
public class ThreadToLocalReference extends PseudoReference
{
    private static final long serialVersionUID = 1L;
    private int localObjectId;
    private GCRootInfo[] gcRootInfo;

    public ThreadToLocalReference(ISnapshot snapshot, long address, String name, int localObjectId,
                    GCRootInfo[] gcRootInfo)
    {
        super(snapshot, address, name);
        this.localObjectId = localObjectId;
        this.gcRootInfo = gcRootInfo;
    }

    public int getObjectId()
    {
        return localObjectId;
    }

    public GCRootInfo[] getGcRootInfo()
    {
        return gcRootInfo;
    }
}
