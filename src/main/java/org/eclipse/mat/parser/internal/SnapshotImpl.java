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

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.eclipse.mat.SnapshotException;
import org.eclipse.mat.collect.ArrayInt;
import org.eclipse.mat.collect.ArrayIntBig;
import org.eclipse.mat.collect.BitField;
import org.eclipse.mat.collect.HashMapIntObject;
import org.eclipse.mat.collect.IteratorInt;
import org.eclipse.mat.collect.SetInt;
import org.eclipse.mat.parser.IObjectReader;
import org.eclipse.mat.parser.index.IIndexReader;
import org.eclipse.mat.parser.index.IndexManager;
import org.eclipse.mat.parser.index.IIndexReader.IOne2OneIndex;
import org.eclipse.mat.parser.internal.snapshot.MultiplePathsFromGCRootsComputerImpl;
import org.eclipse.mat.parser.internal.snapshot.ObjectCache;
import org.eclipse.mat.parser.internal.snapshot.ObjectMarker;
import org.eclipse.mat.parser.internal.snapshot.PathsFromGCRootsTreeBuilder;
import org.eclipse.mat.parser.internal.snapshot.RetainedSizeCache;
import org.eclipse.mat.parser.internal.util.IntStack;
import org.eclipse.mat.parser.internal.util.ParserRegistry;
import org.eclipse.mat.parser.internal.util.ParserRegistry.Parser;
import org.eclipse.mat.parser.model.AbstractObjectImpl;
import org.eclipse.mat.parser.model.ClassImpl;
import org.eclipse.mat.parser.model.ClassLoaderImpl;
import org.eclipse.mat.parser.model.InstanceImpl;
import org.eclipse.mat.parser.model.XGCRootInfo;
import org.eclipse.mat.parser.model.XSnapshotInfo;
import org.eclipse.mat.snapshot.DominatorsSummary;
import org.eclipse.mat.snapshot.ExcludedReferencesDescriptor;
import org.eclipse.mat.snapshot.IMultiplePathsFromGCRootsComputer;
import org.eclipse.mat.snapshot.IPathsFromGCRootsComputer;
import org.eclipse.mat.snapshot.ISnapshot;
import org.eclipse.mat.snapshot.PathsFromGCRootsTree;
import org.eclipse.mat.snapshot.DominatorsSummary.ClassDominatorRecord;
import org.eclipse.mat.snapshot.model.GCRootInfo;
import org.eclipse.mat.snapshot.model.IClass;
import org.eclipse.mat.snapshot.model.IObject;
import org.eclipse.mat.snapshot.model.IThreadStack;
import org.eclipse.mat.snapshot.model.NamedReference;
import org.eclipse.mat.util.IProgressListener;
import org.eclipse.mat.util.MessageUtil;
import org.eclipse.mat.util.VoidProgressListener;
import org.eclipse.mat.util.IProgressListener.OperationCanceledException;

public final class SnapshotImpl implements ISnapshot
{

    // //////////////////////////////////////////////////////////////
    // factory methods
    // //////////////////////////////////////////////////////////////

    private static final String VERSION = "MAT_01";//$NON-NLS-1$

    @SuppressWarnings("unchecked")
    public static SnapshotImpl readFromFile(File file, String prefix, IProgressListener listener)
                    throws SnapshotException, IOException
    {
        FileInputStream fis = null;

        listener.beginTask(Messages.SnapshotImpl_ReopeningParsedHeapDumpFile, 9);

        try
        {
            fis = new FileInputStream(prefix + "index");//$NON-NLS-1$
            listener.worked(1);
            ObjectInputStream in = new ObjectInputStream(new BufferedInputStream(fis));

            String version = in.readUTF();
            if (!VERSION.equals(version))
                throw new IOException(MessageUtil.format(Messages.SnapshotImpl_Error_UnknownVersion, version));

            String objectReaderUniqueIdentifier = in.readUTF();
            Parser parser = ParserRegistry.lookupParser(objectReaderUniqueIdentifier);
            if (parser == null)
                throw new IOException(Messages.SnapshotImpl_Error_ParserNotFound + objectReaderUniqueIdentifier);
            listener.worked(1);
            IObjectReader heapObjectReader = parser.getObjectReader();

            XSnapshotInfo snapshotInfo = (XSnapshotInfo) in.readObject();
            snapshotInfo.setProperty("$heapFormat", parser.getId()); //$NON-NLS-1$
            HashMapIntObject<ClassImpl> classCache = (HashMapIntObject<ClassImpl>) in.readObject();

            if (listener.isCanceled())
                throw new IProgressListener.OperationCanceledException();

            HashMapIntObject<XGCRootInfo[]> roots = (HashMapIntObject<XGCRootInfo[]>) in.readObject();
            HashMapIntObject<HashMapIntObject<XGCRootInfo[]>> rootsPerThread = (HashMapIntObject<HashMapIntObject<XGCRootInfo[]>>) in
                            .readObject();

            listener.worked(1);
            if (listener.isCanceled())
                throw new IProgressListener.OperationCanceledException();

            HashMapIntObject<String> loaderLabels = (HashMapIntObject<String>) in.readObject();
            BitField arrayObjects = (BitField) in.readObject();
            listener.worked(3);

            snapshotInfo.setPrefix(prefix);
            snapshotInfo.setPath(file.getAbsolutePath());
            IndexManager indexManager = new IndexManager();
            indexManager.init(prefix);

            SnapshotImpl ret = new SnapshotImpl(snapshotInfo, heapObjectReader, classCache, roots, rootsPerThread, loaderLabels,
                            arrayObjects, indexManager);
            listener.worked(3);
            return ret;
        }
        catch (ClassNotFoundException e)
        {
            IOException ioe = new IOException(e.getMessage());
            ioe.initCause(e);
            throw ioe;
        }
        catch (ClassCastException e)
        {
            IOException ioe = new IOException(e.getMessage());
            ioe.initCause(e);
            throw ioe;
        }
        finally
        {
            if (fis != null)
                fis.close();
            listener.done();
        }
    }

    public static SnapshotImpl create(XSnapshotInfo snapshotInfo, //
                    String objectReaderUniqueIdentifier, //
                    IObjectReader heapObjectReader, //
                    HashMapIntObject<ClassImpl> classCache, //
                    HashMapIntObject<XGCRootInfo[]> roots, //
                    HashMapIntObject<HashMapIntObject<XGCRootInfo[]>> rootsPerThread, //
                    BitField arrayObjects, //
                    IndexManager indexManager, //
                    IProgressListener listener) throws IOException, SnapshotException
    {
        SnapshotImpl answer = new SnapshotImpl(snapshotInfo, heapObjectReader, classCache, roots, rootsPerThread, null,
                        arrayObjects, indexManager);

        answer.calculateLoaderLabels();

        FileOutputStream fos = null;
        ObjectOutputStream out = null;

        try
        {
            fos = new FileOutputStream(snapshotInfo.getPrefix() + "index");//$NON-NLS-1$
            out = new ObjectOutputStream(new BufferedOutputStream(fos));
            out.writeUTF(VERSION);
            out.writeUTF(objectReaderUniqueIdentifier);
            out.writeObject(answer.snapshotInfo);
            out.writeObject(answer.classCache);

            if (listener.isCanceled())
                throw new IProgressListener.OperationCanceledException();

            out.writeObject(answer.roots);
            out.writeObject(answer.rootsPerThread);

            if (listener.isCanceled())
                throw new IProgressListener.OperationCanceledException();

            out.writeObject(answer.loaderLabels);
            out.writeObject(answer.arrayObjects);
        }
        finally
        {
            if (out != null)
                out.close();

            if (fos != null)
                fos.close();
        }

        return answer;
    }

    // //////////////////////////////////////////////////////////////
    // member variables
    // //////////////////////////////////////////////////////////////

    // serialized data
    private XSnapshotInfo snapshotInfo;
    private HashMapIntObject<ClassImpl> classCache;
    private HashMapIntObject<XGCRootInfo[]> roots;
    private HashMapIntObject<HashMapIntObject<XGCRootInfo[]>> rootsPerThread;

    private HashMapIntObject<String> loaderLabels;
    private BitField arrayObjects;

    // stored in separate files
    private IndexManager indexManager;
    private RetainedSizeCache retainedSizeCache;

    // runtime data
    private IObjectReader heapObjectReader;
    private boolean dominatorTreeCalculated;
    private Map<String, List<IClass>> classCacheByName;
    private ObjectCache<IObject> objectCache;
    
    private boolean parsedThreads = false;
    HashMapIntObject<IThreadStack> threadId2stack;

    // //////////////////////////////////////////////////////////////
    // constructor
    // //////////////////////////////////////////////////////////////

    private SnapshotImpl(XSnapshotInfo snapshotInfo, //
                    IObjectReader heapObjectReader, //
                    HashMapIntObject<ClassImpl> classCache, //
                    HashMapIntObject<XGCRootInfo[]> roots, //
                    HashMapIntObject<HashMapIntObject<XGCRootInfo[]>> rootsPerThread, //
                    HashMapIntObject<String> loaderLabels, //
                    BitField arrayObjects, //
                    IndexManager indexManager) throws SnapshotException, IOException
    {
        this.snapshotInfo = snapshotInfo;
        this.heapObjectReader = heapObjectReader;
        this.classCache = classCache;
        this.roots = roots;
        this.rootsPerThread = rootsPerThread;
        this.loaderLabels = loaderLabels;
        this.arrayObjects = arrayObjects;
        this.indexManager = indexManager;

        // initialize data
        this.retainedSizeCache = new RetainedSizeCache(snapshotInfo);

        this.classCacheByName = new HashMap<String, List<IClass>>(this.classCache.size());
        for (Iterator<ClassImpl> iter = this.classCache.values(); iter.hasNext();)
        {
            ClassImpl clasz = iter.next();
            clasz.setSnapshot(this);

            List<IClass> list = classCacheByName.get(clasz.getName());
            if (list == null)
                classCacheByName.put(clasz.getName(), list = new ArrayList<IClass>());
            list.add(clasz);
        }

        this.dominatorTreeCalculated = indexManager.dominated() != null && indexManager.o2retained() != null
                        && indexManager.dominator() != null;

        this.objectCache = new HeapObjectCache(this, 1000);

        this.heapObjectReader.open(this);

//        Object unreach = snapshotInfo.getProperty(UnreachableObjectsHistogram.class.getName());
//        if (unreach instanceof UnreachableObjectsHistogram)
//            ((UnreachableObjectsHistogram)unreach).setSnapshot(this);
    }

    private void calculateLoaderLabels() throws SnapshotException
    {
        loaderLabels = new HashMapIntObject<String>();
        long usedHeapSize = 0;

        int systemClassLoaderId = indexManager.o2address().reverse(0);

        Object[] classes = classCache.getAllValues();
        for (int i = 0; i < classes.length; i++)
        {
            ClassImpl clasz = (ClassImpl) classes[i];
            usedHeapSize += clasz.getTotalSize();

            int classLoaderId = clasz.getClassLoaderId();
            String label = loaderLabels.get(classLoaderId);
            if (label != null)
                continue;

            if (classLoaderId == systemClassLoaderId)
            {
                label = "<system class loader>";//$NON-NLS-1$
            }
            else
            {
                IObject classLoader = getObject(classLoaderId);
                label = classLoader.getClassSpecificName();
                if (label == null)
                    label = ClassLoaderImpl.NO_LABEL;
            }

            loaderLabels.put(classLoaderId, label);
        }

        // now, let's go through all instances of all sub classes to attach
        // labels
        Collection<IClass> loaderClasses = getClassesByName(IClass.JAVA_LANG_CLASSLOADER, true);
        if (loaderClasses != null)
        {
            for (IClass clazz : loaderClasses)
            {
                for (int classLoaderId : clazz.getObjectIds())
                {
                    String label = loaderLabels.get(classLoaderId);
                    if (label != null)
                        continue;

                    if (classLoaderId == systemClassLoaderId)
                    {
                        label = "<system class loader>";//$NON-NLS-1$
                    }
                    else
                    {
                        IObject classLoader = getObject(classLoaderId);
                        label = classLoader.getClassSpecificName();
                        if (label == null)
                            label = ClassLoaderImpl.NO_LABEL;
                    }

                    loaderLabels.put(classLoaderId, label);
                }
            }
        }

        snapshotInfo.setUsedHeapSize(usedHeapSize);
		// numberOfObjects was previously calculated by summing getNumberOfObjects() for
		// each class. Sometimes there was a mismatch. See bug 294311
		snapshotInfo.setNumberOfObjects(indexManager.idx.size());
        snapshotInfo.setNumberOfClassLoaders(loaderLabels.size());
        snapshotInfo.setNumberOfGCRoots(roots.size());
        snapshotInfo.setNumberOfClasses(classCache.size());

        // important: refresh object cache. To calculate the loader labels, the
        // class loader instances are loaded. however, the object cache uses the
        // loader label to determine the implementation class. hence, cached
        // instances from creating the labels have the wrong implementation
        // class. hence, the refresh.
        objectCache.clear();
    }

    // //////////////////////////////////////////////////////////////
    // interface implementation
    // //////////////////////////////////////////////////////////////

    public XSnapshotInfo getSnapshotInfo()
    {
        return snapshotInfo;
    }

    public int[] getGCRoots() throws SnapshotException
    {
        return roots.getAllKeys();
        // return Arrays.asList((GCRootInfo[]) roots.getAllValues(new
        // GCRootInfo[roots.size()]));
    }

    public Collection<IClass> getClasses() throws SnapshotException
    {
        return Arrays.asList(classCache.getAllValues(new IClass[classCache.size()]));
    }

    public Collection<IClass> getClassesByName(String name, boolean includeSubClasses) throws SnapshotException
    {
        List<IClass> list = this.classCacheByName.get(name);
        if (list == null)
            return null;

        if (!includeSubClasses)
            return Collections.unmodifiableCollection(list);

        // use set to filter out duplicate subclasses
        Set<IClass> answer = new HashSet<IClass>();
        answer.addAll(list);
        for (IClass clazz : list)
            answer.addAll(clazz.getAllSubclasses());
        return answer;
    }

    public Collection<IClass> getClassesByName(Pattern namePattern, boolean includeSubClasses) throws SnapshotException
    {
        Set<IClass> result = new HashSet<IClass>();
        Object[] classes = classCache.getAllValues();
        for (int i = 0; i < classes.length; i++)
        {
            IClass clazz = (IClass) classes[i];
            if (namePattern.matcher(clazz.getName()).matches())
            {
                result.add(clazz);
                if (includeSubClasses)
                {
                    result.addAll(clazz.getAllSubclasses());
                }
            }
        }
        return result;
    }

//    public Histogram getHistogram(IProgressListener listener) throws SnapshotException
//    {
//        if (listener == null)
//            listener = new VoidProgressListener();
//
//        HistogramBuilder histogramBuilder = new HistogramBuilder(Messages.SnapshotImpl_Histogram);
//
//        Object[] classes = classCache.getAllValues();
//        for (int i = 0; i < classes.length; i++)
//        {
//            histogramBuilder.put(new XClassHistogramRecord((ClassImpl) classes[i]));
//        }
//
//        if (listener.isCanceled())
//            throw new IProgressListener.OperationCanceledException();
//
//        return histogramBuilder.toHistogram(this, true);
//    }
//
//    public Histogram getHistogram(int[] objectIds, IProgressListener progressMonitor) throws SnapshotException
//    {
//        if (progressMonitor == null)
//            progressMonitor = new VoidProgressListener();
//
//        HistogramBuilder histogramBuilder = new HistogramBuilder(Messages.SnapshotImpl_Histogram);
//
//        progressMonitor.beginTask(Messages.SnapshotImpl_BuildingHistogram, objectIds.length >>> 8);
//
//        // Arrays.sort(objectIds);
//        // int[] classIds = indexManager.o2class().getAll(objectIds);
//
//        IOne2OneIndex o2class = indexManager.o2class();
//
//        int classId;
//
//        for (int ii = 0; ii < objectIds.length; ii++)
//        {
//            classId = o2class.get(objectIds[ii]);
//
//            histogramBuilder.add(classId, objectIds[ii], getHeapSize(objectIds[ii]));
//
//            if ((ii & 0xff) == 0)
//            {
//                if (progressMonitor.isCanceled())
//                    return null;
//                progressMonitor.worked(1);
//            }
//        }
//
//        progressMonitor.done();
//        return histogramBuilder.toHistogram(this, false);
//    }

    public int[] getInboundRefererIds(int objectId) throws SnapshotException
    {
        return indexManager.inbound().get(objectId);
    }

    public int[] getOutboundReferentIds(int objectId) throws SnapshotException
    {
        return indexManager.outbound().get(objectId);
    }

    public int[] getInboundRefererIds(int[] objectIds, IProgressListener progressMonitor) throws SnapshotException
    {
        if (progressMonitor == null)
            progressMonitor = new VoidProgressListener();

        IIndexReader.IOne2ManyIndex inbound = indexManager.inbound();

        SetInt result = new SetInt();
        progressMonitor.beginTask(Messages.SnapshotImpl_ReadingInboundReferrers, objectIds.length / 100);

        for (int ii = 0; ii < objectIds.length; ii++)
        {
            int[] referees = inbound.get(objectIds[ii]);
            for (int refereeId : referees)
                result.add(refereeId);

            if (ii % 100 == 0)
            {
                if (progressMonitor.isCanceled())
                    return null;
                progressMonitor.worked(1);
            }
        }

        int[] endResult = result.toArray();
        // It used to be sorted before (TreeSet<Integer>) but I don't
        // remember if this is needed
        // Arrays.sort(endResult);

        progressMonitor.done();

        return endResult;
    }

    public int[] getOutboundReferentIds(int[] objectIds, IProgressListener progressMonitor) throws SnapshotException
    {
        if (progressMonitor == null)
            progressMonitor = new VoidProgressListener();

        IIndexReader.IOne2ManyIndex outbound = indexManager.outbound();

        SetInt result = new SetInt();
        progressMonitor.beginTask(Messages.SnapshotImpl_ReadingOutboundReferrers, objectIds.length / 100);

        for (int ii = 0; ii < objectIds.length; ii++)
        {
            int[] referees = outbound.get(objectIds[ii]);
            for (int refereeId : referees)
                result.add(refereeId);

            if (ii % 100 == 0)
            {
                if (progressMonitor.isCanceled())
                    return null;
                progressMonitor.worked(1);
            }
        }

        int[] endResult = result.toArray();

        progressMonitor.done();

        return endResult;
    }

    public IPathsFromGCRootsComputer getPathsFromGCRoots(int objectId, Map<IClass, Set<String>> excludeList)
                    throws SnapshotException
    {
        return new PathsFromGCRootsComputerImpl(objectId, excludeList);
    }

    public IMultiplePathsFromGCRootsComputer getMultiplePathsFromGCRoots(int[] objectIds,
                    Map<IClass, Set<String>> excludeList) throws SnapshotException
    {
        return new MultiplePathsFromGCRootsComputerImpl(objectIds, excludeList, this);
    }

    int[] getRetainedSetSingleThreaded(int[] objectIds, IProgressListener progressMonitor) throws SnapshotException
    {
        /* for empty initial set - return immediately an empty retained set */
        if (objectIds.length == 0) { return new int[0]; }

        /*
         * take the retained set of a single object out of the dominator tree -
         * it's faster
         */
        if (objectIds.length == 1) { return getSingleObjectRetainedSet(objectIds[0]); }

        int numberOfObjects = snapshotInfo.getNumberOfObjects();

        if (progressMonitor == null)
            progressMonitor = new VoidProgressListener();

        /* a bit field to mark all reached objects */
        boolean[] reachable = new boolean[numberOfObjects];

        /*
         * Initially mark all the objects whose retained set is to be calculated
         * Thus the dfs will not go through this objects, and all objects
         * retained from them will stay unmarked (the bits will be clear)
         */
        for (int objId : objectIds)
        {
            reachable[objId] = true;
        }

        /*
         * The dfs() will start from the GC roots, follow the outbound
         * references, and mark all unmarked objects. The retained set will
         * contain the unmarked objects
         */
        ObjectMarker marker = new ObjectMarker(roots.getAllKeys(), reachable, indexManager.outbound(), progressMonitor);
        int numReached;
        try
        {
            numReached = marker.markSingleThreaded();
        }
        catch (OperationCanceledException e)
        {
            // $JL-EXC$
            return null;
        }

        // int numReached = dfs(reachable);
        int[] retained = new int[numberOfObjects - numReached];

        /*
         * Unmark also the initial objects, as we want them to be included in
         * the retained set
         */
        for (int objId : objectIds)
        {
            reachable[objId] = false;
        }

        /* Put each unmarked bit into the retained set */
        int j = 0;
        for (int i = 0; i < numberOfObjects; i++)
        {
            if (!reachable[i])
            {
                retained[j++] = i;
            }
        }
        return retained;

    }

    private int[] getRetainedSetMultiThreaded(int[] objectIds, int availableProcessors,
                    IProgressListener progressMonitor) throws SnapshotException
    {
        /* for empty initial set - return immediately an empty retained set */
        if (objectIds.length == 0) { return new int[0]; }

        /*
         * take the retained set of a single object out of the dominator tree -
         * it's faster
         */
        if (objectIds.length == 1) { return getSingleObjectRetainedSet(objectIds[0]); }

        int numberOfObjects = snapshotInfo.getNumberOfObjects();

        if (progressMonitor == null)
            progressMonitor = new VoidProgressListener();

        /* a boolean[] to mark all reached objects */
        boolean[] reachable = new boolean[numberOfObjects];

        /*
         * Initially mark all the objects whose retained set is to be calculated
         * Thus the dfs will not go through this objects, and all objects
         * retained from them will stay unmarked (the bits will be clear)
         */
        for (int objId : objectIds)
        {
            reachable[objId] = true;
        }

        /*
         * Mark all the GC roots, and keep them in a stack. The worker threads
         * are going to pop() one by one the gc roots and do the marking from
         * them
         */
        int[] gcRoots = roots.getAllKeys();
        ObjectMarker marker = new ObjectMarker(gcRoots, reachable, indexManager.outbound(), progressMonitor);
        try
        {
            marker.markMultiThreaded(availableProcessors);
        }
        catch (InterruptedException e)
        {
            throw new SnapshotException(e);
        }

        /*
         * Unmark also the initial objects, as we want them to be included in
         * the retained set
         */
        for (int objId : objectIds)
        {
            reachable[objId] = false;
        }

        /*
         * build the result in an IntArray - the exact number of marked is not
         * known
         */
        ArrayIntBig retained = new ArrayIntBig();

        /* Put each unmarked object into the retained set */
        for (int i = 0; i < numberOfObjects; i++)
        {
            if (!reachable[i])
            {
                retained.add(i);
            }
        }
        return retained.toArray();

    }

    public int[] getRetainedSet(int[] objectIds, IProgressListener progressMonitor) throws SnapshotException
    {
        int availableProcessors = Runtime.getRuntime().availableProcessors();
        if (availableProcessors > 1)
        {
            return getRetainedSetMultiThreaded(objectIds, availableProcessors, progressMonitor);
        }
        else
        {
            return getRetainedSetSingleThreaded(objectIds, progressMonitor);
        }
    }

    public int[] getRetainedSet(int[] objectIds, String[] fieldNames, IProgressListener listener)
                    throws SnapshotException
    {
        if (objectIds.length == 0) { return new int[0]; }

        int numberOfObjects = indexManager.o2address().size();

        if (listener == null)
            listener = new VoidProgressListener();

        BitField initialSet = new BitField(numberOfObjects);
        for (int objId : objectIds)
            initialSet.set(objId);

        if (listener.isCanceled())
            return null;

        BitField reachable = new BitField(numberOfObjects);

        int markedObjects = dfs2(reachable, initialSet, fieldNames);

        int[] retained = new int[numberOfObjects - markedObjects];
        int j = 0;
        for (int i = 0; i < numberOfObjects; i++)
        {
            if (!reachable.get(i))
            {
                retained[j++] = i;
            }
        }

        return retained;
    }

    public int[] getRetainedSet(int[] objectIds, ExcludedReferencesDescriptor[] excludedReferences,
                    IProgressListener progressMonitor) throws SnapshotException
    {
        /*
         * first pass - mark starting from the GC roots, avoiding
         * excludedReferences, until initial are reached. The non-marked objects
         * will be a common retained set from the excluded and initial objects
         */
        boolean[] firstPass = new boolean[getSnapshotInfo().getNumberOfObjects()];
        // mark all initial
        for (int objId : objectIds)
        {
            firstPass[objId] = true;
        }
        ObjectMarker marker = new ObjectMarker(getGCRoots(), firstPass, getIndexManager().outbound,
                        new VoidProgressListener());
        marker.markSingleThreaded(excludedReferences, this);

        // un-mark initial - they have to go into the retained set
        for (int objId : objectIds)
        {
            firstPass[objId] = false;
        }

        /*
         * Second pass - from the non-marked objects mark the ones starting from
         * the initial set (objectIds)
         */
        boolean[] secondPass = new boolean[firstPass.length];
        System.arraycopy(firstPass, 0, secondPass, 0, firstPass.length);

        ObjectMarker secondMarker = new ObjectMarker(objectIds, secondPass, getIndexManager().outbound,
                        new VoidProgressListener());
        secondMarker.markSingleThreaded();

        /*
         * Have to merge the results of the two markings here
         */
        int numObjects = getSnapshotInfo().getNumberOfObjects();
        ArrayIntBig retainedSet = new ArrayIntBig();
        for (int i = 0; i < numObjects; i++)
        {
            if (!firstPass[i] && secondPass[i])
            {
                retainedSet.add(i);
            }
        }
        return retainedSet.toArray();
    }

    public long getMinRetainedSize(int[] objectIds, IProgressListener progressMonitor)
                    throws UnsupportedOperationException, SnapshotException
    {
        if (objectIds.length == 1) { return getRetainedHeapSize(objectIds[0]); }
        if (objectIds.length == 0) { return 0; }

        // to get the min.retained size we do not need to find the min.retain
        // set at all
        // all one needs is the distinct objects in the dominator tree. The
        // sum of their retained sizes is the correct value then
        int[] topAncestors = getTopAncestorsInDominatorTree(objectIds, progressMonitor);
        long result = 0;
        for (int topAncestorId : topAncestors)
        {
            result += getRetainedHeapSize(topAncestorId);
        }
        return result;
    }

    public int[] getMinRetainedSet(int[] objectIds, IProgressListener progressMonitor)
                    throws UnsupportedOperationException, SnapshotException
    {
        if (objectIds.length == 1) { return getSingleObjectRetainedSet(objectIds[0]); }

        SetInt retainedSet = new SetInt(2 * objectIds.length);
        for (int i : objectIds)
        {
            retainedSet.add(i);
        }

        /*
         * objects on the path from a top-ancestor to the <root> will be saved
         * here to avoid walking the same path many times
         */
        SetInt negativeCache = new SetInt(2 * objectIds.length);

        // used to temporarily keep the walked-through objects before we decide
        // in which cache to store them
        // inline the stack functionality for performance reasons
        // IntStack temp = new IntStack();
        int tempSize = 0;
        int tempCapacity = 10 * 1024;
        int[] temp = new int[tempCapacity];

        IIndexReader.IOne2OneIndex dominatorIdx = indexManager.dominator();
        IIndexReader.IOne2ManyIndex dominated = indexManager.dominated();

        int size = 0;
        int capacity = 10 * 1024;
        int[] stack = new int[capacity];

        int iterations = 0;
        for (int objectId : objectIds)
        {

            iterations++;
            if ((iterations & 0xffff) == 0)
            {
                if (progressMonitor.isCanceled()) { throw new IProgressListener.OperationCanceledException(); }
            }

            int dominatorId = dominatorIdx.get(objectId) - 2;
            boolean save = true;

            /*
             * For each object walk up the dominator tree until either the
             * <root> is reached or an object which is already in the retained
             * set
             */
            while (dominatorId > -1)
            {
                // temp.push(dominatorId); // save all objects on the path
                if (tempSize == tempCapacity)
                {
                    int newCapacity = tempCapacity << 1;
                    int[] newArr = new int[newCapacity];
                    System.arraycopy(temp, 0, newArr, 0, tempCapacity);
                    temp = newArr;
                    tempCapacity = newCapacity;

                }
                temp[tempSize++] = dominatorId;
                // end of push()

                // check if the dominator is in the retained set (i.e. there
                // is another object from the initial set dominating it)
                if (retainedSet.contains(dominatorId))
                {
                    save = false;
                    break;
                }

                // check if the dominator is in the negative cache - i.e. there
                // are no objects from the initial set on the way to the <root>
                if (negativeCache.contains(dominatorId))
                {
                    // save is true, so simply break and let the result be saved
                    break;
                }
                dominatorId = dominatorIdx.get(dominatorId) - 2;
            }
            if (save)
            {
                // add the path from the object up to the <root> to the negative
                // cache
                while (tempSize > 0)
                {
                    // negativeCache.add(temp.pop());
                    negativeCache.add(temp[--tempSize]); // pop
                }

                /*
                 * Add the the objects retained by objectId to the whole
                 * retained set. Always use one and the same stack, it is empty
                 * at the end of this block
                 */

                stack[size++] = objectId; // push

                int current;

                while (size > 0) // are there elements in the stack?
                {
                    current = stack[--size]; // pop
                    retainedSet.add(current);

                    int[] next = dominated.get(current + 1);
                    for (int i : next)
                    {
                        // push, check capacity first
                        if (size == capacity)
                        {
                            int newCapacity = capacity << 1;
                            int[] newArr = new int[newCapacity];
                            System.arraycopy(stack, 0, newArr, 0, capacity);
                            stack = newArr;
                            capacity = newCapacity;

                        }
                        stack[size++] = i;
                    }
                }
            }
        }

        return retainedSet.toArray();

    }

    public int[] getTopAncestorsInDominatorTree(int[] objectIds, IProgressListener listener) throws SnapshotException
    {
        if (!isDominatorTreeCalculated())
            throw new SnapshotException(Messages.SnapshotImpl_Error_DomTreeNotAvailable);

        if (listener == null)
            listener = new VoidProgressListener();

        /*
         * For big objects sets use a boolean[] instead of SetInt to mark
         * processed objects SetInt is too memory expensive and on huge sets may
         * lead to an OOMError Using the boolean[] is also faster on bigger
         * sets.
         */
        if (objectIds.length > 1000000)
            return getTopAncestorsWithBooleanCache(objectIds, listener);

        /*
         * objects on the path from a top-ancestor to the <root> will be saved
         * here to avoid walking the same path many times
         */
        SetInt negativeCache = new SetInt(objectIds.length);

        /*
         * objects on the path to a top-ancestor will be cached here, to avoid
         * walking the same path multiple times.
         */
        SetInt positiveCache = new SetInt(2 * objectIds.length);
        for (int i : objectIds)
        {
            positiveCache.add(i);
        }

        /*
         * an array where all top-ancestors will be saved
         */
        ArrayInt result = new ArrayInt();

        // used to temporarily keep the walked-through objects before we decide
        // in which cache to store them
        // inline the stack functionality for performance reasons
        // IntStack temp = new IntStack();
        int tempSize = 0;
        int tempCapacity = 10 * 1024;
        int[] temp = new int[tempCapacity];

        IIndexReader.IOne2OneIndex dominatorIdx = indexManager.dominator();

        int iterations = 0;
        for (int objectId : objectIds)
        {
            iterations++;
            if ((iterations & 0xffff) == 0)
            {
                if (listener.isCanceled()) { throw new IProgressListener.OperationCanceledException(); }
            }

            int dominatorId = dominatorIdx.get(objectId) - 2;
            boolean save = true;

            /*
             * For each object walk up the dominator tree until either the
             * <root> is reached or an object which is already in the retained
             * set
             */
            while (dominatorId > -1)
            {
                // temp.push(dominatorId); // save all objects on the path
                if (tempSize == tempCapacity)
                {
                    int newCapacity = tempCapacity << 1;
                    int[] newArr = new int[newCapacity];
                    System.arraycopy(temp, 0, newArr, 0, tempCapacity);
                    temp = newArr;
                    tempCapacity = newCapacity;

                }
                temp[tempSize++] = dominatorId;

                // check if the dominator is in the positive cache (i.e. there
                // is another object from the initial set dominating it)
                if (positiveCache.contains(dominatorId))
                {
                    save = false;
                    // add the marked objects to the positiveCache
                    while (tempSize > 0)
                    {
                        // positiveCahce.add(temp.pop());
                        positiveCache.add(temp[--tempSize]); // pop
                    }
                    break;
                }

                // check if the dominator is in the negative cache - i.e. there
                // are no objects from the initial set on the way to the <root>
                if (negativeCache.contains(dominatorId))
                {
                    // save is true, so simply break and let the result be saved
                    break;
                }
                dominatorId = dominatorIdx.get(dominatorId) - 2;
            }
            if (save)
            {
                result.add(objectId);
                while (tempSize > 0)
                {
                    // negativeCache.add(temp.pop());
                    negativeCache.add(temp[--tempSize]); // pop
                }
            }
        }

        return result.toArray();

    }

    private int[] getTopAncestorsWithBooleanCache(int[] objectIds, IProgressListener listener)
    {
        /*
         * objects on the path from a top-ancestor to the <root> will be saved
         * here to avoid walking the same path many times
         */
        boolean[] negativeCache = new boolean[snapshotInfo.getNumberOfObjects()];

        /*
         * objects on the path to a top-ancestor will be cached here, to avoid
         * walking the same path multiple times.
         */
        boolean[] positiveCache = new boolean[snapshotInfo.getNumberOfObjects()];
        for (int i : objectIds)
        {
            positiveCache[i] = true;
        }

        /*
         * an array where all top-ancestors will be saved
         */
        ArrayInt result = new ArrayInt();

        // used to temporarily keep the walked-through objects before we decide
        // in which cache to store them
        // inline the stack functionality for performance reasons
        // IntStack temp = new IntStack();
        int tempSize = 0;
        int tempCapacity = 10 * 1024;
        int[] temp = new int[tempCapacity];

        IIndexReader.IOne2OneIndex dominatorIdx = indexManager.dominator();

        int iterations = 0;
        for (int objectId : objectIds)
        {
            iterations++;
            if ((iterations & 0xffff) == 0)
            {
                if (listener.isCanceled()) { throw new IProgressListener.OperationCanceledException(); }
            }

            int dominatorId = dominatorIdx.get(objectId) - 2;
            boolean save = true;

            /*
             * For each object walk up the dominator tree until either the
             * <root> is reached or an object which is already in the retained
             * set
             */
            while (dominatorId > -1)
            {
                // temp.push(dominatorId); // save all objects on the path
                if (tempSize == tempCapacity)
                {
                    int newCapacity = tempCapacity << 1;
                    int[] newArr = new int[newCapacity];
                    System.arraycopy(temp, 0, newArr, 0, tempCapacity);
                    temp = newArr;
                    tempCapacity = newCapacity;

                }
                temp[tempSize++] = dominatorId;

                // check if the dominator is in the positive cache (i.e. there
                // is another object from the initial set dominating it)
                if (positiveCache[dominatorId])
                {
                    save = false;
                    // add the marked objects to the positiveCache
                    while (tempSize > 0)
                    {
                        // positiveCahce.add(temp.pop());
                        positiveCache[temp[--tempSize]] = true; // pop
                    }
                    break;
                }

                // check if the dominator is in the negative cache - i.e. there
                // are no objects from the initial set on the way to the <root>
                if (negativeCache[dominatorId])
                {
                    // save is true, so simply break and let the result be saved
                    break;
                }
                dominatorId = dominatorIdx.get(dominatorId) - 2;
            }
            if (save)
            {
                result.add(objectId);
                while (tempSize > 0)
                {
                    // negativeCache.add(temp.pop());
                    negativeCache[temp[--tempSize]] = true; // pop
                }
            }
        }

        return result.toArray();

    }

    private boolean isDominatorTreeCalculated()
    {
        return dominatorTreeCalculated;
    }

    public void calculateDominatorTree(IProgressListener listener) throws SnapshotException,
                    IProgressListener.OperationCanceledException
    {
        try
        {
            DominatorTree.calculate(this, listener);
            dominatorTreeCalculated = indexManager.dominated() != null && indexManager.o2retained() != null
                            && indexManager.dominator() != null;
        }
        catch (IOException e)
        {
            throw new SnapshotException(e);
        }
    }

    public int[] getImmediateDominatedIds(int objectId) throws SnapshotException
    {
        if (!isDominatorTreeCalculated())
            throw new SnapshotException(Messages.SnapshotImpl_Error_DomTreeNotAvailable);
        return indexManager.dominated().get(objectId + 1);
    }

    public int getImmediateDominatorId(int objectId) throws SnapshotException
    {
        if (!isDominatorTreeCalculated())
            throw new SnapshotException(Messages.SnapshotImpl_Error_DomTreeNotAvailable);
        return indexManager.dominator().get(objectId) - 2;
    }

    public DominatorsSummary getDominatorsOf(int[] objectIds, Pattern excludePattern, IProgressListener progressListener)
                    throws SnapshotException
    {
        if (!isDominatorTreeCalculated())
            throw new SnapshotException(Messages.SnapshotImpl_Error_DomTreeNotAvailable);

        if (progressListener == null)
            progressListener = new VoidProgressListener();

        IIndexReader.IOne2OneIndex dominatorIndex = indexManager.dominator();
        IIndexReader.IOne2OneIndex o2classIndex = indexManager.o2class();

        SetInt excludeSet = new SetInt();
        SetInt includeSet = new SetInt();

        progressListener.beginTask(Messages.SnapshotImpl_RetrievingDominators, objectIds.length / 10);

        Map<IClass, DominatorsSummary.ClassDominatorRecord> map = new HashMap<IClass, DominatorsSummary.ClassDominatorRecord>();
        for (int ii = 0; ii < objectIds.length; ii++)
        {
            int objectId = objectIds[ii];

            int domClassId;
            IClass clasz;
            String domClassName;

            // the values in the index are 2+the real value
            int dominatorId = dominatorIndex.get(objectId) - 2;
            if (dominatorId == -1)
            {
                clasz = null;
                domClassName = "<ROOT>";//$NON-NLS-1$
                domClassId = -1;
            }
            else
            {
                domClassId = o2classIndex.get(dominatorId);
                clasz = classCache.get(domClassId);
                domClassName = clasz.getName();
            }

            if (excludePattern != null && dominatorId >= 0)
            {
                boolean exclude = true;
                while (exclude)
                {
                    if (progressListener.isCanceled())
                        throw new IProgressListener.OperationCanceledException();

                    // check the negative cache first
                    if (excludeSet.contains(domClassId))
                    {
                        // the values in the index are 2+the real value
                        dominatorId = dominatorIndex.get(dominatorId) - 2;
                        if (dominatorId == -1)
                        {
                            clasz = null;
                            domClassName = "<ROOT>";//$NON-NLS-1$
                            domClassId = -1;
                        }
                        else
                        {
                            domClassId = o2classIndex.get(dominatorId);
                            clasz = classCache.get(domClassId);
                            domClassName = clasz.getName();
                        }
                    }
                    // then check the positive cache
                    else if (includeSet.contains(domClassId))
                    {
                        exclude = false;
                    }
                    // the class has not been processed so far
                    else
                    {
                        if (excludePattern.matcher(domClassName).matches() && dominatorId >= 0)
                        {
                            // just add the classId to the exclude cache
                            // the next iteration will get the next dominator
                            excludeSet.add(domClassId);
                        }
                        else
                        {
                            includeSet.add(domClassId);
                            exclude = false;
                        }
                    }
                }
            }

            DominatorsSummary.ClassDominatorRecord record = map.get(clasz);
            if (record == null)
            {
                record = new DominatorsSummary.ClassDominatorRecord();
                map.put(clasz, record);
                record.setClassName(domClassName);
                record.setClassId(domClassId);
                record.setClassloaderId(dominatorId == -1 || clasz == null ? -1 : clasz.getClassLoaderId());
            }

            if (record.addDominator(dominatorId) && dominatorId != -1)
                record.addDominatorNetSize(getHeapSize(dominatorId));
            if (record.addDominated(objectId))
                record.addDominatedNetSize(getHeapSize(objectId));

            if (ii % 10 == 0)
            {
                if (progressListener.isCanceled())
                    throw new IProgressListener.OperationCanceledException();
                progressListener.worked(1);
            }
        }

        ClassDominatorRecord[] records = map.values().toArray(new DominatorsSummary.ClassDominatorRecord[0]);

        progressListener.done();

        return new DominatorsSummary(records, this);
    }

    public IObject getObject(int objectId) throws SnapshotException
    {
        IObject answer = this.classCache.get(objectId);
        if (answer != null)
            return answer;

        return this.objectCache.get(objectId);
    }

    public GCRootInfo[] getGCRootInfo(int objectId) throws SnapshotException
    {
        return roots.get(objectId);
    }

    public IClass getClassOf(int objectId) throws SnapshotException
    {
        if (isClass(objectId))
            return getObject(objectId).getClazz();
        else
            return (IClass) getObject(indexManager.o2class().get(objectId));
    }

    public long mapIdToAddress(int objectId) throws SnapshotException
    {
        return indexManager.o2address().get(objectId);
    }

    public int getHeapSize(int objectId) throws SnapshotException
    {
        if (arrayObjects.get(objectId))
        {
            return indexManager.a2size().get(objectId);
        }
        else
        {
            IClass clazz = classCache.get(objectId);

            if (clazz != null)
            {
                // it is a class
                return clazz.getUsedHeapSize();
            }
            else
            {
                // it is an instance
                clazz = classCache.get(indexManager.o2class().get(objectId));
                return clazz.getHeapSizePerInstance();
            }

        }
    }

    public long getHeapSize(int[] objectIds) throws UnsupportedOperationException, SnapshotException
    {
        long total = 0;
        IOne2OneIndex o2class = indexManager.o2class();
        IOne2OneIndex a2size = indexManager.a2size();
        for (int objectId : objectIds)
        {
            if (arrayObjects.get(objectId)) // take array sizes from another
            // index
            {
                total += a2size.get(objectId);
            }
            else
            {
                IClass clazz = classCache.get(objectId);

                if (clazz != null)
                {
                    // it is a class
                    total += clazz.getUsedHeapSize();
                }
                else
                {
                    // it is an instance
                    clazz = classCache.get(o2class.get(objectId));
                    total += clazz.getHeapSizePerInstance();
                }
            }
        }
        return total;
    }

    public long getRetainedHeapSize(int objectId) throws SnapshotException
    {
        if (this.isDominatorTreeCalculated())
            return indexManager.o2retained().get(objectId);
        else
            return 0;
    }

    public boolean isArray(int objectId)
    {
        if (arrayObjects.get(objectId))
        {
            // Variable size, so see if actually an array
            IClass clazz = classCache.get(indexManager.o2class().get(objectId));
            if (clazz.isArrayType())
            {
                return true;
            } else {
                return false;
            }
        }
        return false;
    }

    public boolean isClass(int objectId)
    {
        return classCache.containsKey(objectId);
    }

    public boolean isGCRoot(int objectId)
    {
        return roots.containsKey(objectId);
    }

    public int mapAddressToId(long objectAddress) throws SnapshotException
    {
        int objectId = indexManager.o2address().reverse(objectAddress);
        if (objectId < 0)
            throw new SnapshotException(MessageUtil.format(Messages.SnapshotImpl_Error_ObjectNotFound,
                            new Object[] { "0x" //$NON-NLS-1$
                                            + Long.toHexString(objectAddress) }));
        return objectId;
    }

    public void dispose()
    {
        IOException error = null;

        try
        {
            heapObjectReader.close();
        }
        catch (IOException e1)
        {
            error = e1;
        }

        try
        {
            indexManager.close();
        }
        catch (IOException e1)
        {
            error = e1;
        }

        retainedSizeCache.close();

        if (error != null)
            throw new RuntimeException(error);
    }

    // //////////////////////////////////////////////////////////////
    // internal stuff
    // //////////////////////////////////////////////////////////////

    public List<IClass> resolveClassHierarchy(int classIndex)
    {
        IClass clazz = classCache.get(classIndex);
        if (clazz == null)
            return null;

        List<IClass> answer = new ArrayList<IClass>();
        answer.add(clazz);
        while (clazz.hasSuperClass())
        {
            clazz = classCache.get(clazz.getSuperClassId());
            if (clazz == null)
                return null;
            answer.add(clazz);
        }

        return answer;
    }

    /** performance improved check if the object is a class loader */
    public boolean isClassLoader(int objectId)
    {
        return loaderLabels.containsKey(objectId);
    }

    public String getClassLoaderLabel(int objectId)
    {
        return loaderLabels.get(objectId);
    }

    public void setClassLoaderLabel(int objectId, String label)
    {
        if (label == null)
            throw new NullPointerException(Messages.SnapshotImpl_Label);

        String old = loaderLabels.put(objectId, label);
        if (old == null)
            throw new RuntimeException(Messages.SnapshotImpl_Error_ReplacingNonExistentClassLoader);
    }

    private int dfs2(BitField bits, BitField exclude, String[] fieldNames) throws SnapshotException
    {
        int count = 0;

        HashSet<String> fieldNamesSet = new HashSet<String>(fieldNames.length);
        for (int i = 0; i < fieldNames.length; i++)
        {
            fieldNamesSet.add(fieldNames[i]);
        }

        IIndexReader.IOne2ManyIndex outbound = indexManager.outbound();

        IntStack stack = new IntStack();

        for (IteratorInt en = roots.keys(); en.hasNext();)
        {
            int i = en.next();
            stack.push(i);
            bits.set(i);
            count++;
        }

        int current;

        while (stack.size() > 0)
        {
            current = stack.pop();
            if (exclude.get(current))
            {
                // check for objects which are only referenced by the desired
                // fields
                for (int child : outbound.get(current))
                {
                    // well, we must load the obj
                    IObject obj = getObject(current);
                    long childAddress = mapIdToAddress(child);

                    List<NamedReference> refs = obj.getOutboundReferences();
                    for (NamedReference reference : refs)
                    {
                        // if there is a ref from a not-specified field - put
                        // the child
                        // on the list. This means it's not part of the desired
                        // retained set
                        if (!bits.get(child) && reference.getObjectAddress() == childAddress
                                        && !fieldNamesSet.contains(reference.getName()))
                        {
                            stack.push(child);
                            bits.set(child);
                            count++;
                        }
                    }
                }
            }
            else
            {
                for (int child : outbound.get(current))
                {
                    if (!bits.get(child))
                    {
                        stack.push(child);
                        bits.set(child);
                        count++;
                    }
                }
            }

        }

        return count;
    }

    private int[] getSingleObjectRetainedSet(int objectId) throws SnapshotException
    {
        ArrayIntBig result = new ArrayIntBig();
        IntStack stack = new IntStack();

        stack.push(objectId);

        int current;

        while (stack.size() > 0)
        {
            current = stack.pop();
            result.add(current);

            int[] next = getImmediateDominatedIds(current);
            for (int i : next)
            {
                stack.push(i);
            }
        }

        return result.toArray();
    }

    private static class Path
    {

        int index;
        Path next;

        public Path(int index, Path next)
        {
            this.index = index;
            this.next = next;
        }

        public Path getNext()
        {
            return next;
        }

        public int getIndex()
        {
            return index;
        }

        public boolean contains(long id)
        {
            Path p = this;
            while (p != null)
            {
                if (p.index == id)
                    return true;
                p = p.next;
            }
            return false;
        }

    }

    private class PathsFromGCRootsComputerImpl implements IPathsFromGCRootsComputer
    {
        /*
         * special state of the path computer 0 initial; 1 final; 2 processing a
         * GC root; 3 normal processing
         */
        private int state;

        private int nextState;

        int objectId;
        LinkedList<Path> fifo = new LinkedList<Path>();
        BitField visited = new BitField(indexManager.o2address().size());
        BitField excludeInstances;
        IIndexReader.IOne2ManyIndex inboundIndex; // to avoid method calls to

        int currentId;
        Path currentPath;
        int[] currentReferrers;
        int lastReadReferrer;

        int[] referringThreads;
        int currentReferringThread;
        int[] foundPath;

        Map<IClass, Set<String>> excludeMap;

        public PathsFromGCRootsComputerImpl(int objectId, Map<IClass, Set<String>> excludeMap) throws SnapshotException
        {
            this.objectId = objectId;
            this.excludeMap = excludeMap;
            inboundIndex = indexManager.inbound();

            if (excludeMap != null)
            {
                initExcludeInstances();
            }

            currentId = objectId;

            visited.set(objectId);
            if (roots.get(objectId) != null)
            {
                // leave the fifo empty
            }
            else
            {
                fifo.add(new Path(objectId, null));
            }
        }

        private void initExcludeInstances() throws SnapshotException
        {
            excludeInstances = new BitField(indexManager.o2address().size());
            for (IClass clazz : excludeMap.keySet())
            {
                int[] objects = clazz.getObjectIds();
                for (int objId : objects)
                {
                    excludeInstances.set(objId);
                }
            }
        }

        private boolean refersOnlyThroughExcluded(int referrerId, int referentId) throws SnapshotException
        {
            if (!excludeInstances.get(referrerId))
                return false;

            IObject referrerObject = getObject(referrerId);
            Set<String> excludeFields = excludeMap.get(referrerObject.getClazz());
            if (excludeFields == null)
                return true; // treat null as all fields

            long referentAddr = mapIdToAddress(referentId);

            List<NamedReference> refs = referrerObject.getOutboundReferences();
            for (NamedReference reference : refs)
            {
                if (referentAddr == reference.getObjectAddress() && !excludeFields.contains(reference.getName())) { return false; }
            }
            return true;
        }

        public int[] getNextShortestPath() throws SnapshotException
        {
            switch (state)
            {
                case 0: // INITIAL
                {
                    /*
                     * some special check if the initial object itself is a GC
                     * root usually the GC roots are found among the referrers
                     */
                    if (roots.containsKey(currentId))
                    {
                        referringThreads = null;
                        state = 2; // PROCESSING GC ROOT
                        nextState = 1; // FINAL
                        foundPath = new int[] { currentId };
                        return getNextShortestPath();
                    }
                    else
                    {
                        state = 3; // NORMAL
                        return getNextShortestPath();
                    }

                }
                case 1: // FINAL
                    return null;

                case 2: // PROCESSING GC ROOT
                {
                    if (referringThreads == null)
                    {
                        referringThreads = getReferringTreads(getGCRootInfo(foundPath[foundPath.length - 1]));
                        currentReferringThread = 0;
                        if (referringThreads.length == 0)
                        {
                            // there were no threads found to refer to this GC
                            // root
                            state = nextState;
                            return foundPath;
                        }
                    }
                    if (currentReferringThread < referringThreads.length)
                    {
                        int[] result = new int[foundPath.length + 1];
                        System.arraycopy(foundPath, 0, result, 0, foundPath.length);
                        result[result.length - 1] = referringThreads[currentReferringThread];

                        currentReferringThread++;
                        return result;
                    }
                    else
                    {
                        state = nextState;
                        return getNextShortestPath();
                    }

                }
                case 3: // NORMAL PROCESSING
                {
                    int[] res;

                    // finish processing the current entry
                    if (currentReferrers != null)
                    {
                        res = processCurrentReferrefs(lastReadReferrer + 1);
                        if (res != null)
                            return res;
                    }

                    // Continue with the FIFO
                    while (fifo.size() > 0)
                    {
                        currentPath = fifo.getFirst();
                        fifo.removeFirst();
                        currentId = currentPath.getIndex();
                        currentReferrers = inboundIndex.get(currentId);

                        if (currentReferrers != null)
                        {
                            res = processCurrentReferrefs(0);
                            if (res != null)
                                return res;
                        }
                    }
                    return null;
                }

                default:
                    throw new RuntimeException(Messages.SnapshotImpl_Error_UnrecognizedState + state);
            }

        }

        private int[] getReferringTreads(GCRootInfo[] rootInfos)
        {
            SetInt threads = new SetInt();
            for (GCRootInfo info : rootInfos)
            {
                // add only threads different from the current GC root
                if (info.getContextAddress() != 0 && info.getObjectAddress() != info.getContextAddress())
                {
                    threads.add(info.getContextId());
                }
            }
            return threads.toArray();
        }

        public PathsFromGCRootsTree getTree(Collection<int[]> paths)
        {
            PathsFromGCRootsTreeBuilder rootBuilder = new PathsFromGCRootsTreeBuilder(objectId);
            for (int[] path : paths)
            {
                PathsFromGCRootsTreeBuilder current = rootBuilder;

                /*
                 * now add the path as a branch start from 1, as path[0] is the
                 * starting object
                 */
                for (int k = 1; k < path.length; k++)
                {
                    int childId = path[k];
                    PathsFromGCRootsTreeBuilder child = current.getObjectReferers().get(childId);
                    if (child == null)
                    {
                        child = new PathsFromGCRootsTreeBuilder(childId);
                        current.addObjectReferer(child);
                    }
                    current = child;
                }
            }

            return rootBuilder.toPathsFromGCRootsTree();
        }

        private int[] path2Int(Path p)
        {
            IntStack s = new IntStack();
            while (p != null)
            {
                s.push(p.getIndex());
                p = p.getNext();
            }
            int res[] = new int[s.size()];
            for (int i = 0; i < res.length; i++)
            {
                res[i] = s.pop();
            }
            return res;
        }

        private int[] processCurrentReferrefs(int fromIndex) throws SnapshotException
        {
            GCRootInfo[] rootInfo = null;
            for (int i = fromIndex; i < currentReferrers.length; i++)
            {
                rootInfo = roots.get(currentReferrers[i]);
                if (rootInfo != null)
                {
                    if (excludeMap == null)
                    {
                        // save state
                        lastReadReferrer = i;
                        Path p = new Path(currentReferrers[i], currentPath);
                        referringThreads = null;
                        state = 2; // FOUND GC ROOT
                        nextState = 3; // NORMAL PROCESSING
                        foundPath = path2Int(p);
                        return getNextShortestPath();
                    }
                    else
                    {
                        if (!refersOnlyThroughExcluded(currentReferrers[i], currentId))
                        {
                            // save state
                            lastReadReferrer = i;
                            Path p = new Path(currentReferrers[i], currentPath);
                            referringThreads = null;
                            state = 2; // FOUND GC ROOT
                            nextState = 3; // NORMAL PROCESSING
                            foundPath = path2Int(p);
                            return getNextShortestPath();
                        }
                    }
                }
            }
            for (int referrer : currentReferrers)
            {
                if (referrer >= 0 && !visited.get(referrer) && !roots.containsKey(referrer))
                {
                    if (excludeMap == null)
                    {
                        fifo.add(new Path(referrer, currentPath));
                        visited.set(referrer);
                    }
                    else
                    {
                        if (!refersOnlyThroughExcluded(referrer, currentId))
                        {
                            fifo.add(new Path(referrer, currentPath));
                            visited.set(referrer);
                        }
                    }
                }
            }
            return null;
        }

    }

    public IndexManager getIndexManager()
    {
        return indexManager;
    }

    public IObjectReader getHeapObjectReader()
    {
        return heapObjectReader;
    }

    public RetainedSizeCache getRetainedSizeCache()
    {
        return retainedSizeCache;
    }

    public HashMapIntObject<HashMapIntObject<XGCRootInfo[]>> getRootsPerThread()
    {
        return rootsPerThread;
    }

    @SuppressWarnings("unchecked")
    public <A> A getSnapshotAddons(Class<A> addon) throws SnapshotException
    {
//        if (addon == UnreachableObjectsHistogram.class)
//        {
//            return (A) this.getSnapshotInfo().getProperty(UnreachableObjectsHistogram.class.getName());
//        }
//        else
//        {
            return heapObjectReader.getAddon(addon);
//        }
    }
    
    public IThreadStack getThreadStack(int objectId) throws SnapshotException
    {
    	if (!parsedThreads)
    	{
    		threadId2stack = ThreadStackHelper.loadThreadsData(this);
    		parsedThreads = true;
    	}
    	
    	if (threadId2stack != null)
    	{
    		return threadId2stack.get(objectId);
    	}
    	return null;
    }

    // //////////////////////////////////////////////////////////////
    // private classes
    // //////////////////////////////////////////////////////////////

    private static final class HeapObjectCache extends ObjectCache<IObject>
    {
        SnapshotImpl snapshot;

        private HeapObjectCache(SnapshotImpl snapshot, int maxSize)
        {
            super(maxSize);
            this.snapshot = snapshot;
        }

        @Override
        protected IObject load(int objectId)
        {
            try
            {
                IObject answer = null;
                // check if the object is an array (no index needed)
                if (snapshot.isArray(objectId))
                {
                    answer = snapshot.heapObjectReader.read(objectId, snapshot);
                }
                else
                {
                    ClassImpl classImpl = (ClassImpl) snapshot.getObject(snapshot.indexManager.o2class().get(objectId));
                    if (snapshot.isClassLoader(objectId))
                        answer = new ClassLoaderImpl(objectId, Long.MIN_VALUE, classImpl, null);
                    else
                        answer = new InstanceImpl(objectId, Long.MIN_VALUE, classImpl, null);

                }

                ((AbstractObjectImpl) answer).setSnapshot(snapshot);

                return answer;

            }
            catch (IOException e)
            {
                throw new RuntimeException(e);
            }
            catch (SnapshotException e)
            {
                throw new RuntimeException(e);
            }
        }
    }
}
