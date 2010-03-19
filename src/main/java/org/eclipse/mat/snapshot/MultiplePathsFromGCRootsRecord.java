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

/**
 * This class represents a set of paths from the GC roots to different objects,
 * which go through one and the same object.
 */
public class MultiplePathsFromGCRootsRecord
{
    final ISnapshot snapshot;
    final int objectId;
    final int level;

    List<int[]> paths;
    long referencedSize = -1;
    long referencedRetainedSize;

    /**
     * Get the total retained heap size of the referenced objects
     */
    public long getReferencedRetainedSize()
    {
        return referencedRetainedSize;
    }

    /**
     * Set the retained heap size of the referenced objects
     */
    public void setReferencedRetainedSize(long referencedRetainedSize)
    {
        this.referencedRetainedSize = referencedRetainedSize;
    }

    /**
     * A constructor to create the record
     * 
     * @param objectId
     *            - the ID of the object which is common for all the paths
     * @param level
     *            - the level (depth) in the paths where this objects appears
     * @param snapshot
     *            - an ISnapshot object used for further calculation
     */
    public MultiplePathsFromGCRootsRecord(int objectId, int level, ISnapshot snapshot)
    {
        this.level = level;
        this.objectId = objectId;
        paths = new ArrayList<int[]>();
        this.snapshot = snapshot;
    }

    /**
     * Returns the next level of the paths. For all paths going through this
     * object the next objects in the paths are taken and grouped again (i.e.
     * the lenght of the returned array will be equal to the number of different
     * objects at the next depth level of the paths). The direction is from the
     * GC roots to the objects
     * 
     * @return MultiplePathsFromGCRootsRecord[] Each record in the result
     *         represents again paths going through one and the same object
     */
    public MultiplePathsFromGCRootsRecord[] nextLevel()
    {
        int new_level = level + 1;
        HashMapIntObject<MultiplePathsFromGCRootsRecord> nextLevelRecords = new HashMapIntObject<MultiplePathsFromGCRootsRecord>();
        for (int[] path : paths)
        {
            if (path != null && (path.length - new_level - 1 >= 0))
            {
                MultiplePathsFromGCRootsRecord record = nextLevelRecords.get(path[path.length - new_level - 1]);
                if (record == null)
                {
                    record = new MultiplePathsFromGCRootsRecord(path[path.length - new_level - 1], new_level, snapshot);
                    nextLevelRecords.put(path[path.length - new_level - 1], record);
                }
                record.addPath(path);
            }
        }

        return nextLevelRecords.getAllValues(new MultiplePathsFromGCRootsRecord[0]);

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
     * Get the id of the object which is common for all the paths
     */
    public int getObjectId()
    {
        return objectId;
    }

    /**
     * Get the number of paths going through this object
     */
    public int getCount()
    {
        return paths.size();
    }

    /**
     * Get the level of this record
     */
    public int getLevel()
    {
        return level;
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
     * Returns a Comparator ordering the records descending by the number of
     * referenced objects.
     */
    public static Comparator<MultiplePathsFromGCRootsRecord> getComparatorByNumberOfReferencedObjects()
    {
        return new Comparator<MultiplePathsFromGCRootsRecord>()
        {

            public int compare(MultiplePathsFromGCRootsRecord o1, MultiplePathsFromGCRootsRecord o2)
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
    public static Comparator<MultiplePathsFromGCRootsRecord> getComparatorByReferencedHeapSize()
    {
        return new Comparator<MultiplePathsFromGCRootsRecord>()
        {

            public int compare(MultiplePathsFromGCRootsRecord o1, MultiplePathsFromGCRootsRecord o2)
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

    /**
     * Returns a Comparator ordering the records descending by the total size of
     * referenced objects.
     */
    public static Comparator<MultiplePathsFromGCRootsRecord> getComparatorByReferencedRetainedSize()
    {
        return new Comparator<MultiplePathsFromGCRootsRecord>()
        {

            public int compare(MultiplePathsFromGCRootsRecord o1, MultiplePathsFromGCRootsRecord o2)
            {
                if (o1.getReferencedRetainedSize() < o2.getReferencedRetainedSize())
                    return 1;
                if (o1.getReferencedRetainedSize() > o2.getReferencedRetainedSize())
                    return -1;
                return 0;
            }

        };
    }
}
