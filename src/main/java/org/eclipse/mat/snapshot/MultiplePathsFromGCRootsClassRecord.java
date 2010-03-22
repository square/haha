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

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.eclipse.mat.SnapshotException;
import org.eclipse.mat.collect.HashMapIntObject;
import org.eclipse.mat.collect.SetInt;
import org.eclipse.mat.snapshot.model.IClass;

public class MultiplePathsFromGCRootsClassRecord
{
    private List<int[]> paths = new ArrayList<int[]>();
    private SetInt distinctObjects;
    private int level;
    private IClass clazz;
    private long referencedSize = -1;
    private ISnapshot snapshot;
    private boolean fromRoots;

    public MultiplePathsFromGCRootsClassRecord(IClass clazz, int level, boolean fromRoots, ISnapshot snapshot)
    {
        this.clazz = clazz;
        this.level = level;
        this.fromRoots = fromRoots;
        this.snapshot = snapshot;
    }

    public MultiplePathsFromGCRootsClassRecord[] nextLevel() throws SnapshotException
    {
        int nextLevel = level + 1;
        if (nextLevel < 0)
            return null;

        HashMapIntObject<MultiplePathsFromGCRootsClassRecord> nextLevelRecords = new HashMapIntObject<MultiplePathsFromGCRootsClassRecord>();
        for (int[] path : paths)
        {
            if (path != null)
            {
                int newIndex = fromRoots ? path.length - nextLevel - 1 : nextLevel;
                // check if one of the ends is reached
                if (newIndex < 0 || newIndex >= path.length)
                {
                    continue;
                }
                IClass clazz = snapshot.getClassOf(path[newIndex]);
                MultiplePathsFromGCRootsClassRecord record = nextLevelRecords.get(clazz.getObjectId());
                if (record == null)
                {
                    record = new MultiplePathsFromGCRootsClassRecord(clazz, nextLevel, fromRoots, snapshot);
                    nextLevelRecords.put(clazz.getObjectId(), record);
                }
                record.addPath(path);
            }
        }

        return nextLevelRecords.getAllValues(new MultiplePathsFromGCRootsClassRecord[0]);

    }

    /**
     * This method is used only when the record is built. Adds one path to the
     * set of paths
     * 
     * @param path
     */
    public void addPath(int[] path)
    {
        paths.add(path);
    }

    /**
     * Get all the paths going through the object (getObjectId())
     * 
     * @return List<int[]> each element in the list is an int[] representing a
     *         path
     */
    public List<int[]> getPaths()
    {
        return paths;
    }

    /**
     * Get the number of paths going through this object
     */
    public int getCount()
    {
        return paths.size();
    }

    /**
     * Get the number of distinct objects of this class
     */
    public int getDistinctCount()
    {
        if (distinctObjects == null) // lazy init
        {
            distinctObjects = new SetInt();
            for (int[] path : paths)
            {
                int index = fromRoots ? path.length - level - 1 : level;
                distinctObjects.add(path[index]);
            }
        }
        return distinctObjects.size();
    }

    /**
     * Get the total net heap size of all referenced objects (see
     * getReferencedObjects())
     * 
     * @return - the total heap size of all referenced objects
     * @throws SnapshotException
     */
    public long getReferencedHeapSize() throws SnapshotException
    {
        if (referencedSize == -1)
        {
            referencedSize = snapshot.getHeapSize(getReferencedObjects());
        }
        return referencedSize;
    }

    /**
     * Get the "end" objects for each path. This is equal to getting all the
     * paths and looking at their element [0]
     * 
     * @return - an array with all the objects at the end of the paths
     */
    public int[] getReferencedObjects()
    {
        int[] result = new int[paths.size()];

        int i = 0;
        for (int[] path : paths)
        {
            result[i++] = path[0];
        }

        return result;
    }

    public static Comparator<MultiplePathsFromGCRootsClassRecord> getComparatorByNumberOfReferencedObjects()
    {
        return new Comparator<MultiplePathsFromGCRootsClassRecord>()
        {

            public int compare(MultiplePathsFromGCRootsClassRecord o1, MultiplePathsFromGCRootsClassRecord o2)
            {
                if (o1.paths.size() < o2.paths.size())
                    return 1;
                if (o1.paths.size() > o2.paths.size())
                    return -1;
                return 0;
            }

        };
    }

    /**
     * Returns a Comparator ordering the records descending by the total size of
     * referenced objects.
     */
    public static Comparator<MultiplePathsFromGCRootsClassRecord> getComparatorByReferencedHeapSize()
    {
        return new Comparator<MultiplePathsFromGCRootsClassRecord>()
        {

            public int compare(MultiplePathsFromGCRootsClassRecord o1, MultiplePathsFromGCRootsClassRecord o2)
            {
                try
                {
                    if (o1.getReferencedHeapSize() < o2.getReferencedHeapSize())
                        return 1;
                    if (o1.getReferencedHeapSize() > o2.getReferencedHeapSize())
                        return -1;
                    return 0;
                }
                catch (SnapshotException e)
                {
                    // $JL-EXC$
                    return 0;
                }
            }

        };
    }

    public IClass getClazz()
    {
        return clazz;
    }

    public boolean isFromRoots()
    {
        return fromRoots;
    }

    public int getLevel()
    {
        return level;
    }

}
