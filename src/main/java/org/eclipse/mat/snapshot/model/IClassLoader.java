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

import java.util.List;

import org.eclipse.mat.SnapshotException;
import org.eclipse.mat.util.IProgressListener;

/**
 * An interface for class loader.
 * 
 * @noimplement
 */
public interface IClassLoader extends IInstance
{
    /**
     * Returns the retained size of all objects and classes loaded by this class
     * loader.
     */
    long getRetainedHeapSizeOfObjects(boolean calculateIfNotAvailable, boolean calculateMinRetainedSize,
                    IProgressListener listener) throws SnapshotException;

    /**
     * Returns the classes defined by this class loader instance.
     */
    List<IClass> getDefinedClasses() throws SnapshotException;
}
