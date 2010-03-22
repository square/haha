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

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.eclipse.mat.SnapshotException;
import org.eclipse.mat.collect.BitField;
import org.eclipse.mat.collect.HashMapIntObject;
import org.eclipse.mat.parser.IObjectReader;
import org.eclipse.mat.parser.index.IndexManager;
import org.eclipse.mat.parser.internal.util.ParserRegistry.Parser;
import org.eclipse.mat.parser.model.ClassImpl;
import org.eclipse.mat.parser.model.XGCRootInfo;
import org.eclipse.mat.parser.model.XSnapshotInfo;
import org.eclipse.mat.snapshot.model.IClass;
import org.eclipse.mat.util.IProgressListener;

public class SnapshotImplBuilder
{
    private XSnapshotInfo snapshotInfo;
    /* package */HashMapIntObject<ClassImpl> classCache;
    /* package */Map<String, List<IClass>> classCacheByName;
    private HashMapIntObject<XGCRootInfo[]> roots;
    private HashMapIntObject<HashMapIntObject<XGCRootInfo[]>> rootsPerThread;

    /* package */BitField arrayObjects;

    /* package */IndexManager indexManager;

    public SnapshotImplBuilder(XSnapshotInfo snapshotInfo)
    {
        this.snapshotInfo = snapshotInfo;
    }

    public XSnapshotInfo getSnapshotInfo()
    {
        return snapshotInfo;
    }

    public void setIndexManager(IndexManager indexManager)
    {
        this.indexManager = indexManager;
    }

    public IndexManager getIndexManager()
    {
        return indexManager;
    }

    public void setClassCache(HashMapIntObject<ClassImpl> classCache)
    {
        this.classCache = classCache;
    }

    public HashMapIntObject<ClassImpl> getClassCache()
    {
        return classCache;
    }

    public void setRoots(HashMapIntObject<XGCRootInfo[]> roots)
    {
        this.roots = roots;
    }

    public HashMapIntObject<XGCRootInfo[]> getRoots()
    {
        return roots;
    }

    public void setRootsPerThread(HashMapIntObject<HashMapIntObject<XGCRootInfo[]>> rootsPerThread)
    {
        this.rootsPerThread = rootsPerThread;
    }

    public void setArrayObjects(BitField arrayObjects)
    {
        this.arrayObjects = arrayObjects;
    }

    public SnapshotImpl create(Parser parser, IProgressListener listener) throws IOException, SnapshotException
    {
        IObjectReader heapObjectReader = parser.getObjectReader();
        return SnapshotImpl.create(snapshotInfo, parser.getUniqueIdentifier(), heapObjectReader, classCache, roots,
                        rootsPerThread, arrayObjects, indexManager, listener);
    }

}
