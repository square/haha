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
package org.eclipse.mat.parser;

import java.util.List;

import org.eclipse.mat.collect.HashMapIntObject;
import org.eclipse.mat.parser.index.IIndexReader;
import org.eclipse.mat.parser.model.ClassImpl;
import org.eclipse.mat.parser.model.XGCRootInfo;
import org.eclipse.mat.parser.model.XSnapshotInfo;

/**
 * @noimplement
 */
public interface IPreliminaryIndex
{
    XSnapshotInfo getSnapshotInfo();

    void setClassesById(HashMapIntObject<ClassImpl> classesById);

    void setGcRoots(HashMapIntObject<List<XGCRootInfo>> gcRoots);

    void setThread2objects2roots(HashMapIntObject<HashMapIntObject<List<XGCRootInfo>>> thread2objects2roots);

    void setOutbound(IIndexReader.IOne2ManyIndex outbound);

    void setIdentifiers(IIndexReader.IOne2LongIndex identifiers);

    void setObject2classId(IIndexReader.IOne2OneIndex object2classId);

    void setArray2size(IIndexReader.IOne2OneIndex array2size);

}
