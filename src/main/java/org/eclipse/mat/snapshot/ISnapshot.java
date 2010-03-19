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

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.eclipse.mat.SnapshotException;
import org.eclipse.mat.snapshot.model.GCRootInfo;
import org.eclipse.mat.snapshot.model.IClass;
import org.eclipse.mat.snapshot.model.IObject;
import org.eclipse.mat.snapshot.model.IThreadStack;
import org.eclipse.mat.util.IProgressListener;

/**
 * Interface for snapshots. It is the central API for handling HPROF snapshots.
 * <p>
 * A snapshot basically has an {@link SnapshotInfo} object describing it and
 * various methods operating on it. Be aware that such a snapshot potentially
 * holds quite a lot of memory, so try to keep not too many open at the same
 * time.
 * <p>
 * The basic "handle" you will encounter in the API is an <code>int</code>. It
 * is the id of an object and is always of the size of four bytes (no matter if
 * 32 or 64 bit). Operations are either performed on <code>int</code> or
 * <code>int[]</code> (addresses are stored in longs, but they should be used
 * only for display purposes). In order to simplify working with them and for
 * performance reasons compared to <code>Collection&lt;Integer&gt;</code> some
 * helper classes are provided and you are strongly advised to use them:
 * BitField (for huge fixed bit fields), ArrayIntBig and ArrayLongBig (for huge
 * growing arrays) and ArrayIntCompressed and ArrayLongCompressed (for
 * fixed-size compressed arrays).
 * 
 * @noimplement
 */
public interface ISnapshot
{

    /**
     * Get info object describing the snapshot.
     * <p>
     * Performance: Fast - in memory.
     * 
     * @return info object describing the snapshot
     */
    public SnapshotInfo getSnapshotInfo();

    /**
     * Get all GC roots.
     * <p>
     * A GC root is an object which doesn't need to be referenced to remain in
     * the heap, e.g. all objects created in the thread stack frame.
     * <p>
     * Performance: Fast - in memory.
     * 
     * @return int[] containing the objectIds of all GC roots
     * @throws SnapshotException
     */
    public int[] getGCRoots() throws SnapshotException;

    /**
     * Get all classes.
     * <p>
     * The returned class is a snapshot representation of a Java class. It
     * offers means to get to its name, class loader, fields, inheritance
     * relationships and most importantly to all of its objects found in the
     * heap.
     * <p>
     * Performance: Fast - in memory.
     * 
     * @return collection of all classes
     * @throws SnapshotException
     */
    public Collection<IClass> getClasses() throws SnapshotException;

    /**
     * Get all classes by name.
     * <p>
     * This method returns you for one full class name all classes with this
     * name. Usually you will get only one class back, but in the case of
     * multiple class loaders which have loaded a class with the same name twice
     * (or even the same class) you will get multiple snapshot class
     * representations back as only the combination of class and class loader is
     * unique, not the name alone.
     * <p>
     * Performance: Fast - in memory.
     * 
     * @param name
     *            name for the class
     * @param includeSubClasses
     *            flag indicating whether or not to include also classes derived
     *            from matching classes (the name isn't taken into account for
     *            sub classes anymore)
     * @return collection of matching classes
     * @throws SnapshotException
     */
    public Collection<IClass> getClassesByName(String name, boolean includeSubClasses) throws SnapshotException;

    /**
     * Get all classes by name pattern.
     * <p>
     * This method returns you all classes with a name matching the regular
     * expression pattern.
     * <p>
     * Performance: Fast - in memory, but needs iteration over all classes.
     * 
     * @param namePattern
     *            name pattern for the class (regular expression)
     * @param includeSubClasses
     *            flag indicating whether or not to include also classes derived
     *            from matching classes (the name isn't taken into account for
     *            sub classes anymore)
     * @return collection of matching classes
     * @throws SnapshotException
     */
    public Collection<IClass> getClassesByName(Pattern namePattern, boolean includeSubClasses) throws SnapshotException;

    /**
     * Get histogram for the whole snapshot.
     * <p>
     * This histogram lists all classes and class loaders and how many objects
     * of the classes exist in the snapshot along with the memory consumption.
     * <p>
     * Performance: Fast - in memory.
     * 
     * @param progressListener
     *            progress listener informing about the current state of
     *            execution
     * @return histogram
     * @throws SnapshotException
     */
//    public Histogram getHistogram(IProgressListener progressListener) throws SnapshotException;

    /**
     * Get histogram for some specific objects - usually the result of other
     * calls to the snapshot.
     * <p>
     * This histogram lists all classes and class loaders and how many objects
     * of the classes exist among the given objects along with the memory
     * consumption.
     * <p>
     * Performance: Fast to medium - on index (object id -> class id); depending
     * on the number of objects.
     * 
     * @param objectIds
     *            object ids for which the histogram should be computed
     * @param progressListener
     *            progress listener informing about the current state of
     *            execution
     * @return histogram
     * @throws SnapshotException
     */
//    public Histogram getHistogram(int[] objectIds, IProgressListener progressListener) throws SnapshotException;

    /**
     * Get all objects referencing the given object.
     * <p>
     * Performance: Relatively fast - single index operation.
     * 
     * @param objectId
     *            object which is referenced
     * @return objects referencing the given object
     * @throws SnapshotException
     */
    public int[] getInboundRefererIds(int objectId) throws SnapshotException;

    /**
     * Get all objects referenced by the given object.
     * <p>
     * Performance: Relatively fast - single index operation.
     * 
     * @param objectId
     *            object which is referencing
     * @return objects referenced by the given object
     * @throws SnapshotException
     */
    public int[] getOutboundReferentIds(int objectId) throws SnapshotException;

    /**
     * Get all objects referencing the given objects.
     * <p>
     * Hint: This method is handy if you want to learn which classes reference a
     * class. Therefore you would call this method with all objects of your
     * class of interest and get a histogram out of the result.
     * <p>
     * Performance: Fast to slow - on index; depending on the number of objects
     * and the references.
     * 
     * @param objectIds
     *            objects which are referenced
     * @param progressListener
     *            progress listener informing about the current state of
     *            execution
     * @return objects referencing the given objects
     * @throws SnapshotException
     */
    public int[] getInboundRefererIds(int[] objectIds, IProgressListener progressListener) throws SnapshotException;

    /**
     * Get all objects referenced by the given objects.
     * <p>
     * Hint: This method is handy if you want to learn which classes are
     * referenced by a class. Therefore you would call this method with all
     * objects of your class of interest and get a histogram out of the result.
     * <p>
     * Performance: Fast to slow - on index; depending on the number of objects
     * and the references.
     * 
     * @param objectIds
     *            objects which are referencing
     * @param progressListener
     *            progress listener informing about the current state of
     *            execution
     * @return objects referenced by the given objects
     * @throws SnapshotException
     */
    public int[] getOutboundReferentIds(int[] objectIds, IProgressListener progressListener) throws SnapshotException;

    /**
     * Get interactive computer for paths from GC roots to the given object.
     * <p>
     * Hint: This method is handy if you want to learn which objects are
     * responsible for the given object to remain in memory. Since the snapshot
     * implementation artificially creates references from the object to its
     * class and from the class to its class loader you can even see why a class
     * or class loaders remains in memory, i.e. which other objects hold
     * references to objects of the class or class loader of interest.
     * <p>
     * Performance: Fast to slow - on index; depending on the number and length
     * of GC root paths.
     * 
     * @param objectId
     *            object for which the GC root paths should be determined
     * @param excludeMap
     *            a map specifying paths through which objects have to be
     *            avoided and not reported. Each entry in the map has the IClass
     *            as a key, and a Set<String> set, specifying which fields
     *            exactly from this key class have to be avoided. If for a key
     *            IClass the value (Set<String>) null is specified, then paths
     *            through any of the fields will be avoided
     * @return interactive computer for paths from GC roots to the given object
     * @throws SnapshotException
     */
    public IPathsFromGCRootsComputer getPathsFromGCRoots(int objectId, Map<IClass, Set<String>> excludeMap)
                    throws SnapshotException;

    /**
     * Returns an IMultiplePathsFromGCRootsComputer object, which can be used to
     * calculate the shortest path from the GC roots to each of objects in the
     * specified object set.
     * 
     * @param objectIds
     *            the IDs of the objects for which a shortest path has to be
     *            calculated
     * @param excludeMap
     *            a map specifying paths through which objects have to be
     *            avoided and not reported. Each entry in the map has the IClass
     *            as a key, and a Set<String> set, specifying which fields
     *            exactly from this key class have to be avoided. If for a key
     *            IClass the value (Set<String>) null is specified, then paths
     *            through any of the fields will be avoided
     * @return IMultiplePathsFromGCRootsComputer The object which can be used to
     *         carry out the actual computation and
     * @throws SnapshotException
     */
    public IMultiplePathsFromGCRootsComputer getMultiplePathsFromGCRoots(int[] objectIds,
                    Map<IClass, Set<String>> excludeMap) throws SnapshotException;

    /**
     * Get retained set of objects for the given objects (including the given
     * objects).
     * <p>
     * The retained set includes the given objects and all objects which are
     * lifetime-dependent on them, i.e. which would be garbage collected if the
     * references to the given objects would be lost and the objects garbage
     * collected.
     * <p>
     * Performance: Usually extremely slow - on index; depending on the number
     * of objects and the references (deep).
     * 
     * @param objectIds
     *            objects on which the retained set should be determined
     * @param progressListener
     *            progress listener informing about the current state of
     *            execution
     * @return retained set of objects for the given objects
     * @throws SnapshotException
     */
    public int[] getRetainedSet(int[] objectIds, IProgressListener progressListener) throws SnapshotException;

    /**
     * Get retained set of objects for the given fields at the given objects
     * (excluding the given objects).
     * <p>
     * The retained set includes the objects referenced by the fields on the
     * given objects and all objects which are lifetime-dependent on them, i.e.
     * which would be garbage collected if the references at the given fields at
     * the given objects would be nulled.
     * <p>
     * Performance: Usually extremely slow - on index; depending on the number
     * of objects and the references (deep).
     * 
     * @param objectIds
     *            objects on which the retained set should be determined
     * @param fieldNames
     * @param progressMonitor
     *            progress listener informing about the current state of
     *            execution
     * @return retained set of objects for the given objects
     * @throws SnapshotException
     */
    public int[] getRetainedSet(int[] objectIds, String[] fieldNames, IProgressListener progressMonitor)
                    throws SnapshotException;

    public int[] getRetainedSet(int[] objectIds, ExcludedReferencesDescriptor[] excludedReferences,
                    IProgressListener progressMonitor) throws SnapshotException;

    /**
     * Calculate the minimum retained set of objects for the given objects
     * (including the given objects).
     * <p>
     * The minimum retained set includes the given objects and the union of the
     * retained sets for each of the given objects (see getRetainedSet() for an
     * explanation of a retained set). The union of the retained sets of the
     * single objects is potentially smaller than the retained set of all of
     * them, because some objects which are shared between two of the given
     * objects may not appear in the retained set of any of the single objects,
     * but will appear in the retained set if we take them as a whole. Because
     * of it's faster performance the method is suitable to "mine" for a
     * potentially big retained set (e.g. execute this method for all class
     * loaders and see for potentially big ones). One can use the
     * getRetainedSet() method afterwards to get the correct retained set.
     * <p>
     * Performance: Usually fast - for smaller sets this method is much faster
     * than getRetainedSet
     * 
     * @param objectIds
     *            objects on which the minimum retained set should be determined
     * @param progressListener
     *            progress listener informing about the current state of
     *            execution
     * @return the minimum retained set of objects for the given objects
     * @throws SnapshotException
     */
    public int[] getMinRetainedSet(int[] objectIds, IProgressListener progressListener) throws SnapshotException;

    /**
     * Calculate the minimum retained size for the given objects. Works much
     * faster than getting the min. retained set by getMinRetainedSet() and
     * calculating the size of the min. retained set.
     * 
     * @param objectIds
     *            objects on which the minimum retained set should be determined
     * @param listener
     *            progress listener informing about the current state of
     *            execution
     * @return the minimum retained set of objects for the given objects
     * @throws SnapshotException
     */
    public long getMinRetainedSize(int[] objectIds, IProgressListener listener) throws SnapshotException;

    /**
     * Get objects the given object directly dominates, i.e. the objects which
     * are life-time dependent on the given object (not recursively, so just the
     * directly dependent objects), i.e. the objects which would get collected
     * as garbage if the given object would be gone from the heap.
     * 
     * @param objectId
     *            object for which the directly dominated objects should be
     *            returned
     * @return objects the given object directly dominates
     * @throws SnapshotException
     */
    public int[] getImmediateDominatedIds(int objectId) throws SnapshotException;

    /**
     * Get object which directly dominates the given object, i.e. the object
     * which controls the life-time of the given object (not recursively, so
     * just the directly controlling object), i.e. the object which would cause
     * the given object to be collected as garbage if it would be gone from the
     * heap.
     * 
     * @param objectId
     *            object for which the directly dominated objects should be
     *            returned
     * @return Object id of the dominator. -1 if the object is dominated by the
     *         root of the dominator tree.
     * @throws SnapshotException
     */
    public int getImmediateDominatorId(int objectId) throws SnapshotException;

    /**
     * Get a summary of the dominators for all the given objects. The summary
     * can be viewed on grouped by classes or class loaders
     * <p>
     * If an exclude pattern is provided, instead of returning the immediate
     * dominator right away, its class name will be checked against the exclude
     * pattern. If it matches the pattern, the dominator of this dominator will
     * be taken and checked, and so on ... until a dominator not matching the
     * pattern is found or the dominator tree root is reached.
     * 
     * @param objectIds
     *            the objects for which we want the dominator summary (e.g. all
     *            objects of a given class)
     * @param excludePattern
     *            An exclude pattern. Domminators whose class name matches the
     *            pattern will be omitted and their dominator will be taken
     * @param progressListener
     *            progress listener informing about the current state of
     *            execution
     * @return DominatorsSummary the returned DominatorSummary contains the
     *         summary of the dominators grouped by classes or class loaders
     * @throws SnapshotException
     */
    public DominatorsSummary getDominatorsOf(int[] objectIds, Pattern excludePattern, IProgressListener progressListener)
                    throws SnapshotException;

    /**
     * Get the top-ancestors in the dominator tree from the supplied objectIds.
     * The result will be a list of objects (int[]), such that no object from
     * the return list is parent of another object in the returned list. I.e.
     * from a list of objects this method will return only the ones which are
     * independent on one another. It is then correct to sum the retained sizes
     * of the returned objects.
     * 
     * @param objectIds
     *            the objects for which the top-ancestors in the Dominator tree
     *            have to be found
     * @param listener
     *            progress listener informing about the current state of
     *            execution
     * @return int[] the objects which not in a parent/child relation in the
     *         dominator tree
     * @throws SnapshotException
     */
    public int[] getTopAncestorsInDominatorTree(int[] objectIds, IProgressListener listener) throws SnapshotException;

    /**
     * Get object abstracting the real Java Object from the heap dump identified
     * by the given id.
     * <p>
     * Performance: Relatively fast - single index operation.
     * 
     * @param objectId
     *            id of object you want a convenient object abstraction for
     * @return object abstracting the real Java Object from the heap dump
     *         identified by the given id
     * @throws SnapshotException
     */
    public IObject getObject(int objectId) throws SnapshotException;

    /**
     * Get the GC root info for an object. If the provided object is no GC root
     * null will be returned otherwise a GCRootInfo[]. An object can be a GC
     * root for more than one reason and the returned array will contain one
     * instance of GCRootInfo for each of the reasons (e.g. one GCRootInfo for
     * every thread where an object is a java local variable)
     * <p>
     * Performance: Fast - in memory.
     * 
     * @param objectId
     *            id of object you want the GC root info for
     * @return null if this object is no GC root or GCRootInfo[] if it is
     * @throws SnapshotException
     */
    public GCRootInfo[] getGCRootInfo(int objectId) throws SnapshotException;

    /**
     * Get object abstracting the real Java Class this object was an instance of
     * in the heap dump identified by the given id.
     * <p>
     * Performance: Relatively fast - single index operation.
     * 
     * @param objectId
     *            id of object you want the convenient class abstraction for
     * @return object abstracting the real Java Class this object was an
     *         instance of in the heap dump identified by the given id
     * @throws SnapshotException
     */
    public IClass getClassOf(int objectId) throws SnapshotException;

    /**
     * Get heap size for the given object.
     * <p>
     * Performance: Usually fast - in memory for non-array objects and single
     * index operation for array objects.
     * 
     * @param objectId
     *            id of object for which you want the heap size for
     * @return heap size for the given object
     * @throws SnapshotException
     */
    public int getHeapSize(int objectId) throws SnapshotException;

    /**
     * Get the total shallow heap size for a set of objects.
     * <p>
     * Performance: Relatively fast - using this method to calculate the total
     * size of a set of objects is much faster than iterating over the ids and
     * calling getHeapSize for each single object
     * 
     * @param objectIds
     *            ids of the objects for which you want the heap size for
     * @return total heap size for the given object set
     * @throws SnapshotException
     */
    public long getHeapSize(int[] objectIds) throws SnapshotException;

    /**
     * Get retained heap size for the given object.
     * <p>
     * The retained heap size is the memory which would be freed if all
     * references to the given object would be released. It is extracted from
     * the dominator tree and isn't available if the dominator tree isn't
     * available itself.
     * <p>
     * Performance: Relatively fast - single index operation.
     * 
     * @param objectId
     *            id of object for which you want the retained heap size for
     * @return retained heap size for the given object or 0 if no dominator tree
     *         was calculated
     * @throws SnapshotException
     */
    public long getRetainedHeapSize(int objectId) throws SnapshotException;

    /**
     * Returns true if the object by this id is a class.
     * <p>
     * Performance: Very fast.
     */
    boolean isClass(int objectId);

    /**
     * Returns true if the object by this id is a class loader.
     * <p>
     * Performance: Very fast.
     */
    boolean isClassLoader(int objectId);

    /**
     * Returns true if the object by this id is an array.
     * <p>
     * Performance: Very fast.
     */
    boolean isArray(int objectId);

    /**
     * Returns true if the object by this id is a garbage collection root.
     * <p>
     * Performance: Very fast.
     */
    boolean isGCRoot(int objectId);

    /**
     * Map object id (snapshot internal identity assigned during parsing) to
     * object address (memory address where the object was stored).
     * <p>
     * Performance: Fast - in memory.
     * 
     * @param objectId
     *            id of object you want the address for
     * @return object address
     * @throws SnapshotException
     */
    public long mapIdToAddress(int objectId) throws SnapshotException;

    /**
     * Map object address (memory address where the object was stored) to object
     * id (snapshot internal identity assigned during parsing).
     * <p>
     * Performance: Fast - binary search in memory.
     * 
     * @param objectAddress
     *            address of object you want the id for
     * @return object id
     * @throws SnapshotException
     */
    public int mapAddressToId(long objectAddress) throws SnapshotException;

    /**
     * Dispose the whole snapshot.
     * <p>
     * Please call this method prior to dropping the last reference to the
     * snapshot as this method ensures the proper return of all resources (e.g.
     * main memory, file and socket handles...). After calling this method the
     * snapshot can't be used anymore.
     */
    public void dispose();

    /**
     * Get additional JVM information, if available.
     * <p>
     * 
     * @return SnapshotAddons - extended information, e.g. perm info, OoM stack
     *         trace info, JVM arguments, etc.
     * @throws SnapshotException
     */
    public <A> A getSnapshotAddons(Class<A> addon) throws SnapshotException;

    /**
     * Get a the stack trace information for a given thread object, if thread
     * stack information is available in this snapshot.
     * <p>
     * 
     * @return IThreadStack - an object representing the call stack of the
     *         thread. Returns null if no info is available for the object, or
     *         no stack info is available at all
     * @throws SnapshotException
     * @since 0.8
     */
    public IThreadStack getThreadStack(int objectId) throws SnapshotException;

}
