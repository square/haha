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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.mat.collect.SetInt;
import org.eclipse.mat.snapshot.model.IObject;

/**
 * This class holds the data of a dominators summary (see
 * ISnapshot.getDominatorsOf()). For a given list of objects the summary
 * contains the dominators of the single objects grouped by class or by class
 * loader.#
 * 
 * @noinstantiate
 */
public final class DominatorsSummary
{

    private ClassDominatorRecord[] classDominatorRecords;
    private ClassloaderDominatorRecord[] classloaderDominatorRecords;

    private ISnapshot snapshot;

    private Object data;

    public DominatorsSummary(ClassDominatorRecord[] classDominatorRecords, ISnapshot snapshot)
    {
        this.classDominatorRecords = classDominatorRecords;
        this.snapshot = snapshot;

        for (ClassDominatorRecord record : classDominatorRecords)
            record.summary = this;
    }

    /**
     * Returns data object attached to the summary. Needed for UI elements to
     * store context.
     */
    public Object getData()
    {
        return data;
    }

    /**
     * Attaches arbitrary data object to the summary. Needed for UI elements to
     * store context.
     */
    public void setData(Object data)
    {
        this.data = data;
    }

    /**
     * Get the dominators summary grouped by classes
     * 
     * @return ClassDominatorRecord[] the array of ClassDominatorRecords
     */
    public ClassDominatorRecord[] getClassDominatorRecords()
    {
        return classDominatorRecords;
    }

    /**
     * Get the dominators summary grouped by class loaders
     * 
     * @return ClassloaderDominatorRecord[] the array of
     *         ClassloaderDominatorRecords
     */
    public ClassloaderDominatorRecord[] getClassloaderDominatorRecords()
    {
        return getClassloaderDominatorRecords(ClassloaderDominatorRecord.class);
    }

    @SuppressWarnings("unchecked")
    public <C extends ClassloaderDominatorRecord> C[] getClassloaderDominatorRecords(Class<C> factoryClass)
    {
        synchronized (this)
        {
            if (classloaderDominatorRecords == null)
            {
                classloaderDominatorRecords = load((Class<ClassloaderDominatorRecord>) factoryClass);
            }
        }
        return (C[]) classloaderDominatorRecords;
    }

    private ClassloaderDominatorRecord[] load(Class<ClassloaderDominatorRecord> factoryClass)
    {
        try
        {

            Map<Integer, ClassloaderDominatorRecord> map = new HashMap<Integer, ClassloaderDominatorRecord>();
            for (ClassDominatorRecord record : classDominatorRecords)
            {
                ClassloaderDominatorRecord clr = map.get(record.getClassloaderId());
                if (clr == null)
                {
                    map.put(record.getClassloaderId(), clr = factoryClass.newInstance());

                    clr.setId(record.getClassloaderId());

                    if (clr.getId() == -1)
                    {
                        clr.name = "<ROOT>"; //$NON-NLS-1$
                    }
                    else
                    {
                        IObject object = snapshot.getObject(clr.id);
                        clr.name = object.getClassSpecificName();
                        if (clr.name == null)
                            clr.name = object.getTechnicalName();
                    }

                }

                clr.dominated += record.getDominatedCount();
                clr.dominator += record.getDominatorCount();
                clr.dominatedNetSize += record.getDominatedNetSize();
                clr.dominatorNetSize += record.getDominatorNetSize();

                clr.records.add(record);
            }

            return map.values().toArray(new ClassloaderDominatorRecord[map.size()]);

        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }

    /**
     * A record containing information for objects loaded by one class loader
     * dominating a set of other objects
     */
    public static class ClassloaderDominatorRecord
    {
        protected List<ClassDominatorRecord> records = new ArrayList<ClassDominatorRecord>();
        protected String name;
        protected int id;

        long dominatedNetSize;
        long dominatorNetSize;
        long dominatorRetainedSize;
        long dominatedRetainedSize;
        int dominated;
        int dominator;

        public String getName()
        {
            return name;
        }

        /**
         * Get the total net size of the dominated objects
         */
        public long getDominatedNetSize()
        {
            return dominatedNetSize;
        }

        /**
         * Get the number of dominated objects
         */
        public int getDominatedCount()
        {
            return dominated;
        }

        /**
         * Get the total net size of the dominators
         */
        public int getDominatorCount()
        {
            return dominator;
        }

        /**
         * Get the total net size of the dominators
         */
        public long getDominatorNetSize()
        {
            return dominatorNetSize;
        }

        public List<ClassDominatorRecord> getRecords()
        {
            return records;
        }

        public int getId()
        {
            return id;
        }

        public void setId(int id)
        {
            this.id = id;
        }

        public long getDominatedRetainedSize()
        {
            return dominatedRetainedSize;
        }

        public void setDominatedRetainedSize(long dominatedRetainedSize)
        {
            this.dominatedRetainedSize = dominatedRetainedSize;
        }

        public long getDominatorRetainedSize()
        {
            return dominatorRetainedSize;
        }

        public void setDominatorRetainedSize(long dominatorRetainedSize)
        {
            this.dominatorRetainedSize = dominatorRetainedSize;
        }

    }

    /**
     * A record containing information for objects of one class dominating a set
     * of other objects
     */
    public static class ClassDominatorRecord
    {
        DominatorsSummary summary;

        String className;
        int classloaderId;
        int classId;

        long dominatedNetSize;
        long dominatorNetSize;

        long dominatedRetainedSize;
        long dominatorRetainedSize;

        SetInt dominated = new SetInt(500);
        SetInt dominator = new SetInt(500);

        /**
         * Get the name of the class for this record
         */
        public String getClassName()
        {
            return className;
        }

        /**
         * Set the name of the class of this record
         * 
         * @param className
         */
        public void setClassName(String className)
        {
            this.className = className;
        }

        public int getClassId()
        {
            return classId;
        }

        public void setClassId(int classId)
        {
            this.classId = classId;
        }

        /**
         * Get the total net size of the dominated objects
         */
        public long getDominatedNetSize()
        {
            return dominatedNetSize;
        }

        /**
         * Get the total net size of the dominators
         */
        public long getDominatorNetSize()
        {
            return dominatorNetSize;
        }

        /**
         * Get the total retained heap size of the dominated objects
         */
        public long getDominatedRetainedSize()
        {
            return dominatedRetainedSize;
        }

        /**
         * Set the retained heap size of the dominated objects
         */
        public void setDominatedRetainedSize(long dominatedRetainedSize)
        {
            this.dominatedRetainedSize = dominatedRetainedSize;
        }

        /**
         * Get the retained heap size of the dominators
         */
        public long getDominatorRetainedSize()
        {
            return dominatorRetainedSize;
        }

        /**
         * Set the retained heap size of the dominators
         */
        public void setDominatorRetainedSize(long dominatorRetainedSize)
        {
            this.dominatorRetainedSize = dominatorRetainedSize;
        }

        /**
         * Get the number of dominated objects
         */
        public int getDominatedCount()
        {
            return dominated.size();
        }

        /**
         * Get the number of dominators
         * 
         * @return the number of dominators
         */
        public int getDominatorCount()
        {
            return dominator.size();
        }

        /**
         * get the id of the classloader of the dominators' class
         */
        public int getClassloaderId()
        {
            return classloaderId;
        }

        /**
         * set the id of the classloader of the dominators
         * 
         * @param classloaderId
         */
        public void setClassloaderId(int classloaderId)
        {
            this.classloaderId = classloaderId;
        }

        /**
         * Add a dominated object to the record
         * 
         * @param objectId
         */
        public boolean addDominated(int objectId)
        {
            return this.dominated.add(objectId);
        }

        /**
         * Add a dominator to the record
         */
        public boolean addDominator(int objectId)
        {
            return this.dominator.add(objectId);
        }

        /**
         * Increase the dominated net heap size
         */
        public void addDominatedNetSize(long size)
        {
            dominatedNetSize += size;
        }

        /**
         * Increase the dominators total size
         */
        public void addDominatorNetSize(long size)
        {
            dominatorNetSize += size;
        }

        /**
         * Get the dominated objects
         * 
         * @return int[] - an array with the ids of all dominated objects
         */
        public int[] getDominated()
        {
            return dominated.toArray();
        }

        /**
         * Get the dominator objects
         * 
         * @return int[] - an array with the ids of the dominators
         */
        public int[] getDominators()
        {
            return dominator.toArray();
        }

        /**
         * Get the DominatorsSummary to which this record belongs.
         * 
         * @return this records's DominatorsSummery
         */
        public DominatorsSummary getSummary()
        {
            return summary;
        }
    }

    /**
     * A comparator by name
     */
    public static final Comparator<Object> COMPARE_BY_NAME = new Comparator<Object>()
    {
        public int compare(Object o1, Object o2)
        {
            if (o1 instanceof ClassDominatorRecord)
                return ((ClassDominatorRecord) o1).getClassName().compareTo(((ClassDominatorRecord) o2).getClassName());
            else
                return ((ClassloaderDominatorRecord) o1).getName().compareTo(
                                ((ClassloaderDominatorRecord) o2).getName());
        }

    };

    /**
     * A comparator by number of dominators
     */
    public static final Comparator<Object> COMPARE_BY_DOMINATORS = new Comparator<Object>()
    {
        public int compare(Object o1, Object o2)
        {
            int c1 = 0;
            int c2 = 0;

            if (o1 instanceof ClassDominatorRecord)
            {
                c1 = ((ClassDominatorRecord) o1).getDominatorCount();
                c2 = ((ClassDominatorRecord) o2).getDominatorCount();
            }
            else
            {
                c1 = ((ClassloaderDominatorRecord) o1).getDominatorCount();
                c2 = ((ClassloaderDominatorRecord) o2).getDominatorCount();
            }

            return c1 > c2 ? 1 : c1 == c2 ? 0 : -1;
        }

    };

    /**
     * A comparator by number of dominated objects
     */
    public static final Comparator<Object> COMPARE_BY_DOMINATED = new Comparator<Object>()
    {
        public int compare(Object o1, Object o2)
        {
            int c1 = 0;
            int c2 = 0;

            if (o1 instanceof ClassDominatorRecord)
            {
                c1 = ((ClassDominatorRecord) o1).getDominatedCount();
                c2 = ((ClassDominatorRecord) o2).getDominatedCount();
            }
            else
            {
                c1 = ((ClassloaderDominatorRecord) o1).getDominatedCount();
                c2 = ((ClassloaderDominatorRecord) o2).getDominatedCount();
            }

            return c1 > c2 ? 1 : c1 == c2 ? 0 : -1;
        }

    };

    /**
     * A comparator by dominated heap size
     */
    public static final Comparator<Object> COMPARE_BY_DOMINATED_HEAP_SIZE = new Comparator<Object>()
    {
        public int compare(Object o1, Object o2)
        {
            long c1 = 0;
            long c2 = 0;

            if (o1 instanceof ClassDominatorRecord)
            {
                c1 = ((ClassDominatorRecord) o1).getDominatedNetSize();
                c2 = ((ClassDominatorRecord) o2).getDominatedNetSize();
            }
            else
            {
                c1 = ((ClassloaderDominatorRecord) o1).getDominatedNetSize();
                c2 = ((ClassloaderDominatorRecord) o2).getDominatedNetSize();
            }

            return c1 > c2 ? 1 : c1 == c2 ? 0 : -1;
        }

    };

    /**
     * A comparator by dominators heap size
     */
    public static final Comparator<Object> COMPARE_BY_DOMINATOR_HEAP_SIZE = new Comparator<Object>()
    {
        public int compare(Object o1, Object o2)
        {
            long c1 = 0;
            long c2 = 0;
            if (o1 instanceof ClassDominatorRecord)
            {
                c1 = ((ClassDominatorRecord) o1).getDominatorNetSize();
                c2 = ((ClassDominatorRecord) o2).getDominatorNetSize();
            }
            else
            {
                c1 = ((ClassloaderDominatorRecord) o1).getDominatorNetSize();
                c2 = ((ClassloaderDominatorRecord) o2).getDominatorNetSize();
            }
            return c1 > c2 ? 1 : c1 == c2 ? 0 : -1;
        }

    };

    /**
     * A comparator by dominated objects' retained size
     */
    public static final Comparator<Object> COMPARE_BY_DOMINATED_RETAINED_HEAP_SIZE = new Comparator<Object>()
    {
        public int compare(Object o1, Object o2)
        {
            long c1 = 0;
            long c2 = 0;

            if (o1 instanceof ClassDominatorRecord)
            {
                c1 = ((ClassDominatorRecord) o1).getDominatedRetainedSize();
                c2 = ((ClassDominatorRecord) o2).getDominatedRetainedSize();
            }
            else
            {
                c1 = ((ClassloaderDominatorRecord) o1).getDominatedRetainedSize();
                c2 = ((ClassloaderDominatorRecord) o2).getDominatedRetainedSize();
            }
            return c1 > c2 ? 1 : c1 == c2 ? 0 : -1;

        }

    };

    /**
     * A comparator by dominators' retained size
     */
    public static final Comparator<Object> COMPARE_BY_DOMINATOR_RETAINED_HEAP_SIZE = new Comparator<Object>()
    {
        public int compare(Object o1, Object o2)
        {
            long c1 = 0;
            long c2 = 0;

            if (o1 instanceof ClassDominatorRecord)
            {
                c1 = ((ClassDominatorRecord) o1).getDominatorRetainedSize();
                c2 = ((ClassDominatorRecord) o2).getDominatorRetainedSize();
            }
            else
            {
                c1 = ((ClassloaderDominatorRecord) o1).getDominatorRetainedSize();
                c2 = ((ClassloaderDominatorRecord) o2).getDominatorRetainedSize();
            }
            return c1 > c2 ? 1 : c1 == c2 ? 0 : -1;
        }

    };

    /**
     * Reverse the sort order.
     */
    public static Comparator<Object> reverseComparator(final Comparator<Object> comparator)
    {
        return new Comparator<Object>()
        {
            public int compare(Object o1, Object o2)
            {
                return comparator.compare(o2, o1);
            }
        };
    }

}
