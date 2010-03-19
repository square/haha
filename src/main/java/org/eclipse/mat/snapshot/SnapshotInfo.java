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

import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Class describing an {@link ISnapshot}. Each snapshot has some core data, like
 * the date when it was acquired/parsed, the number of objects inside... This
 * data is available with the snapshot info which is usually serialized along
 * with the snapshot.
 */
public class SnapshotInfo implements Serializable
{
    private static final long serialVersionUID = 4L;

    protected String path;
    protected String prefix;
    protected String jvmInfo;
    protected int identifierSize;
    protected Date creationDate;
    protected int numberOfObjects;
    protected int numberOfGCRoots;
    protected int numberOfClasses;
    protected int numberOfClassLoaders;
    protected long usedHeapSize;
    protected Map<String, Serializable> properties;

    /**
     * Construct a snapshot info.
     * 
     * @param path
     *            path from where the snapshot was acquired
     * @param jvmInfo
     *            version of the JVM from which it was acquired
     * @param identifierSize
     *            size of the internal identifiers in the heap dump, i.e. 32 or
     *            64 bit
     * @param creationDate
     *            date when the snapshot was acquired/parsed
     * @param numberOfObjects
     *            number of Java objects found in the snapshot
     * @param numberOfGCRoots
     *            number of GC roots found in the snapshot
     * @param numberOfClasses
     *            number of Java Classes found in the snapshot
     * @param numberOfClassLoaders
     *            number of ClassLoaders found in the snapshot
     * @param usedHeapSize
     *            number of bytes used in the heap (the allocated memory may be
     *            higher)
     */
    public SnapshotInfo(String path, String prefix, String jvmInfo, int identifierSize, Date creationDate,
                    int numberOfObjects, int numberOfGCRoots, int numberOfClasses, int numberOfClassLoaders,
                    long usedHeapSize)
    {
        this.path = path;
        this.prefix = prefix;
        this.jvmInfo = jvmInfo;
        this.identifierSize = identifierSize;
        this.creationDate = creationDate != null ? new Date(creationDate.getTime()) : null;
        this.numberOfObjects = numberOfObjects;
        this.numberOfGCRoots = numberOfGCRoots;
        this.numberOfClasses = numberOfClasses;
        this.numberOfClassLoaders = numberOfClassLoaders;
        this.usedHeapSize = usedHeapSize;
        this.properties = new HashMap<String, Serializable>();
    }

    @Deprecated
    public SnapshotInfo(String path, String jvmInfo, int identifierSize, Date creationDate, int numberOfObjects,
                    int numberOfGCRoots, int numberOfClasses, int numberOfClassLoaders, long usedHeapSize)
    {
        this(path, prefix(path), jvmInfo, identifierSize, creationDate, numberOfObjects, numberOfGCRoots,
                        numberOfClasses, numberOfClassLoaders, usedHeapSize);
    }

    private static String prefix(String path)
    {
        int p = path.lastIndexOf('.');
        return p >= 0 ? path.substring(0, p + 1) : path + '.';
    }

    public Serializable getProperty(String name)
    {
        return properties.get(name);
    }

    public Serializable setProperty(String name, Serializable value)
    {
        return properties.put(name, value);
    }

    /**
     * Get the absolute path of the heap dump file.
     * 
     * @return absolute path of the heap dump file.
     */
    public String getPath()
    {
        return path;
    }

    /**
     * Get the common prefix used to name all additional (e.g. index) files. The
     * prefix includes the directory path.
     * 
     * @return common prefix used to name additional files
     */
    public String getPrefix()
    {
        return prefix;
    }

    /**
     * Get version of the JVM from which it was acquired.
     * 
     * @return version of the JVM from which it was acquired
     */
    public String getJvmInfo()
    {
        return jvmInfo;
    }

    /**
     * Get size of the internal identifiers in the heap dump, i.e. 32 or 64 bit.
     * 
     * @return size of the internal identifiers in the heap dump, i.e. 32 or 64
     *         bit
     */
    public int getIdentifierSize()
    {
        return identifierSize;
    }

    /**
     * Get date when the snapshot was acquired/parsed.
     * 
     * @return creation date, or <code>null</code> if the creation date is not
     *         known
     */
    public Date getCreationDate()
    {
        return creationDate != null ? new Date(creationDate.getTime()) : null;
    }

    /**
     * Get number of Java objects found in the snapshot.
     * 
     * @return number of Java objects found in the snapshot
     */
    public int getNumberOfObjects()
    {
        return numberOfObjects;
    }

    /**
     * Get number of GC roots found in the snapshot.
     * 
     * @return number of GC roots found in the snapshot
     */
    public int getNumberOfGCRoots()
    {
        return numberOfGCRoots;
    }

    /**
     * Get number of Java Classes found in the snapshot.
     * 
     * @return number of Java Classes found in the snapshot
     */
    public int getNumberOfClasses()
    {
        return numberOfClasses;
    }

    /**
     * Get number of ClassLoaders found in the snapshot
     * 
     * @return number of ClassLoaders found in the snapshot
     */
    public int getNumberOfClassLoaders()
    {
        return numberOfClassLoaders;
    }

    /**
     * Get number of bytes used in the heap (the allocated memory may be higher)
     * 
     * @return number of bytes used in the heap (the allocated memory may be
     *         higher)
     */
    public long getUsedHeapSize()
    {
        return usedHeapSize;
    }

    @SuppressWarnings("nls")
    @Override
    public String toString()
    {
        StringBuilder summary = new StringBuilder();
        summary.append("Path: ");
        summary.append(path);
        summary.append("\r\nJVM Info: ");
        summary.append(jvmInfo);
        summary.append("\r\nIdentifier Size: ");
        summary.append(identifierSize);
        summary.append("\r\nCreation Date: ");
        summary.append(creationDate);
        summary.append("\r\nNumber of Objects: ");
        summary.append(numberOfObjects);
        summary.append("\r\nNumber of GC roots: ");
        summary.append(numberOfGCRoots);
        summary.append("\r\nNumber of Classes: ");
        summary.append(numberOfClasses);
        summary.append("\r\nNumber of ClassLoaders: ");
        summary.append(numberOfClassLoaders);
        summary.append("\r\nUsed Heap Size: ");
        summary.append(usedHeapSize);
        return summary.toString();
    }
}
