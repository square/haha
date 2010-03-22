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
package org.eclipse.mat.parser.internal;

import java.util.List;

import org.eclipse.mat.collect.HashMapIntObject;
import org.eclipse.mat.parser.IPreliminaryIndex;
import org.eclipse.mat.parser.index.IIndexReader;
import org.eclipse.mat.parser.model.ClassImpl;
import org.eclipse.mat.parser.model.XGCRootInfo;
import org.eclipse.mat.parser.model.XSnapshotInfo;

/* package */class PreliminaryIndexImpl implements IPreliminaryIndex
{
    XSnapshotInfo snapshotInfo;

    // id -> class impl
    HashMapIntObject<ClassImpl> classesById;

    // GC roots (by ids)
    HashMapIntObject<List<XGCRootInfo>> gcRoots;
    HashMapIntObject<HashMapIntObject<List<XGCRootInfo>>> thread2objects2roots;

    // id -> id*
    IIndexReader.IOne2ManyIndex outbound = null;

    // id -> address
    IIndexReader.IOne2LongIndex identifiers = null;

    // id -> id
    IIndexReader.IOne2OneIndex object2classId = null;

    // id -> int (size)
    IIndexReader.IOne2OneIndex array2size = null;

    public PreliminaryIndexImpl(XSnapshotInfo snapshotInfo)
    {
        this.snapshotInfo = snapshotInfo;
    }

    public XSnapshotInfo getSnapshotInfo()
    {
        return snapshotInfo;
    }

    public void setClassesById(HashMapIntObject<ClassImpl> classesById)
    {
        this.classesById = classesById;
    }

    public void setGcRoots(HashMapIntObject<List<XGCRootInfo>> gcRoots)
    {
        this.gcRoots = gcRoots;
    }

    public void setThread2objects2roots(HashMapIntObject<HashMapIntObject<List<XGCRootInfo>>> thread2objects2roots)
    {
        this.thread2objects2roots = thread2objects2roots;
    }

    public void setOutbound(IIndexReader.IOne2ManyIndex outbound)
    {
        this.outbound = outbound;
    }

    public void setIdentifiers(IIndexReader.IOne2LongIndex identifiers)
    {
        this.identifiers = identifiers;
    }

    public void setObject2classId(IIndexReader.IOne2OneIndex object2classId)
    {
        this.object2classId = object2classId;
    }

    public void setArray2size(IIndexReader.IOne2OneIndex array2size)
    {
        this.array2size = array2size;
    }

    public void delete()
    {}
}
