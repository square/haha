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
package org.eclipse.mat.snapshot;

import org.eclipse.mat.SnapshotException;
import org.eclipse.mat.util.IProgressListener;

/**
 * Objects of this type can be used to work with paths to many objects
 * 
 * @noimplement
 */
public interface IMultiplePathsFromGCRootsComputer
{
    /**
     * Calculates (if not yet calculated) and returns all the paths. The paths
     * are grouped by the GC root object, i.e. all paths starting from one and
     * the same GC root will be packed in one MultiplePathsFromGCRootsRecord.
     * This record can be used to get the objects at the next level in the path,
     * etc...
     * 
     * @param progressListener
     *            - used to track the progress of the computation
     * @return MultiplePathsFromGCRootsRecord[] one record for each group of
     *         paths starting from the same GC root
     * @throws SnapshotException
     */
    public MultiplePathsFromGCRootsRecord[] getPathsByGCRoot(IProgressListener progressListener)
                    throws SnapshotException;

    /**
     * Calculates (if not yet calculated) and returns all the paths. Each
     * element in the Object[] is an int[] representing the path. The first
     * element in the int[] is the specified object, and the last is the GC root
     * object
     * 
     * @param progressListener
     *            - used to track the progress of the computation
     * @return Object[] - each element in the array is an int[] representing a
     *         path
     * @throws SnapshotException
     */
    public Object[] getAllPaths(IProgressListener progressListener) throws SnapshotException;

}
