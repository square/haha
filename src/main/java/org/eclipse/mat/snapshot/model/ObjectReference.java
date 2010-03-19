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

import org.eclipse.mat.SnapshotException;
import org.eclipse.mat.snapshot.ISnapshot;

/**
 * The value of a field if it is an object reference.
 */
public class ObjectReference implements Serializable
{
    private static final long serialVersionUID = 1L;

    private transient ISnapshot snapshot;
    private long address;

    public ObjectReference(ISnapshot snapshot, long address)
    {
        this.snapshot = snapshot;
        this.address = address;
    }

    public long getObjectAddress()
    {
        return address;
    }

    public int getObjectId() throws SnapshotException
    {
        return snapshot.mapAddressToId(address);
    }

    public IObject getObject() throws SnapshotException
    {
        return snapshot.getObject(getObjectId());
    }

    public String toString()
    {
        return "0x" + Long.toHexString(address); //$NON-NLS-1$
    }
}
