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
package org.eclipse.mat.hprof;

import java.io.IOException;
import java.util.List;

import org.eclipse.mat.SnapshotException;
import org.eclipse.mat.collect.ArrayLong;
import org.eclipse.mat.collect.HashMapLongObject;
import org.eclipse.mat.parser.IPreliminaryIndex;
import org.eclipse.mat.parser.index.IIndexReader.IOne2LongIndex;
import org.eclipse.mat.parser.model.ClassImpl;
import org.eclipse.mat.parser.model.XSnapshotInfo;
import org.eclipse.mat.snapshot.model.IClass;
import org.eclipse.mat.util.IProgressListener;

public interface IHprofParserHandler
{
    String IDENTIFIER_SIZE = "ID_SIZE"; //$NON-NLS-1$
    String CREATION_DATE = "CREATION_DATE"; //$NON-NLS-1$
    String VERSION = "VERSION";//$NON-NLS-1$

    public class HeapObject
    {
        public int objectId;
        public long objectAddress;
        public ClassImpl clazz;
        public int usedHeapSize;
        public ArrayLong references = new ArrayLong();
        public boolean isArray = false;

        public HeapObject(int objectId, long objectAddress, ClassImpl clazz, int usedHeapSize)
        {
            super();
            this.objectId = objectId;
            this.objectAddress = objectAddress;
            this.clazz = clazz;
            this.usedHeapSize = usedHeapSize;
            this.isArray = false;
        }
    }

    // //////////////////////////////////////////////////////////////
    // lifecycle
    // //////////////////////////////////////////////////////////////

    void beforePass1(XSnapshotInfo snapshotInfo) throws IOException;

    void beforePass2(IProgressListener monitor) throws IOException, SnapshotException;

    IOne2LongIndex fillIn(IPreliminaryIndex index) throws IOException;

    void cancel();

    // //////////////////////////////////////////////////////////////
    // report parsed entities
    // //////////////////////////////////////////////////////////////

    void addProperty(String name, String value) throws IOException;

    void addGCRoot(long id, long referrer, int rootType) throws IOException;

    void addClass(ClassImpl clazz, long filePosition) throws IOException;

    void addObject(HeapObject object, long filePosition) throws IOException;

    void reportInstance(long id, long filePosition);

    void reportRequiredObjectArray(long arrayClassID);

    void reportRequiredPrimitiveArray(int arrayType);

    // //////////////////////////////////////////////////////////////
    // lookup heap infos
    // //////////////////////////////////////////////////////////////

    int getIdentifierSize();

    HashMapLongObject<String> getConstantPool();

    IClass lookupClass(long classId);

    IClass lookupClassByName(String name, boolean failOnMultipleInstances);

    IClass lookupClassByIndex(int objIndex);

    List<IClass> resolveClassHierarchy(long classId);

    int mapAddressToId(long address);

    XSnapshotInfo getSnapshotInfo();
}
