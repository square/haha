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

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.eclipse.mat.collect.BitField;
import org.eclipse.mat.collect.HashMapIntObject;
import org.eclipse.mat.collect.IteratorInt;
import org.eclipse.mat.parser.index.IndexManager;
import org.eclipse.mat.parser.index.IndexWriter;
import org.eclipse.mat.parser.index.IIndexReader.IOne2LongIndex;
import org.eclipse.mat.parser.index.IIndexReader.IOne2ManyIndex;
import org.eclipse.mat.parser.index.IIndexReader.IOne2OneIndex;
import org.eclipse.mat.parser.index.IndexManager.Index;
import org.eclipse.mat.parser.internal.snapshot.ObjectMarker;
import org.eclipse.mat.parser.model.ClassImpl;
import org.eclipse.mat.parser.model.XGCRootInfo;
import org.eclipse.mat.util.IProgressListener;
import org.eclipse.mat.util.MessageUtil;
import org.eclipse.mat.util.SilentProgressListener;
import org.eclipse.mat.util.IProgressListener.OperationCanceledException;

/* package */class GarbageCleaner
{

    public static int[] clean(final PreliminaryIndexImpl idx, final SnapshotImplBuilder builder,
                    Map<String, String> arguments, IProgressListener listener) throws IOException
    {
        IndexManager idxManager = new IndexManager();

        try
        {
            listener.beginTask(Messages.GarbageCleaner_RemovingUnreachableObjects, 11);
            listener.subTask(Messages.GarbageCleaner_SearchingForUnreachableObjects);

            final int oldNoOfObjects = idx.identifiers.size();

            // determine reachable objects
            boolean[] reachable = new boolean[oldNoOfObjects];
            int newNoOfObjects = 0;
            int[] newRoots = idx.gcRoots.getAllKeys();

            IOne2LongIndex identifiers = idx.identifiers;
            IOne2ManyIndex preOutbound = idx.outbound;
            IOne2OneIndex object2classId = idx.object2classId;
            HashMapIntObject<ClassImpl> classesById = idx.classesById;

            /*
             * START - marking objects use ObjectMarker to mark the reachable
             * objects if more than 1 CPUs are available - use multithreading
             */
            int numProcessors = Runtime.getRuntime().availableProcessors();
            ObjectMarker marker = new ObjectMarker(newRoots, reachable, preOutbound, new SilentProgressListener(
                            listener));
            if (numProcessors > 1)
            {
                try
                {
                    marker.markMultiThreaded(numProcessors);
                }
                catch (InterruptedException e)
                {
                    IOException ioe = new IOException(e.getMessage());
                    ioe.initCause(e);
                    throw ioe;
                }

                // find the number of new objects. It's not returned by marker
                for (boolean b : reachable)
                    if (b)
                        newNoOfObjects++;

            }
            else
            {
                try
                {
                    newNoOfObjects = marker.markSingleThreaded();
                }
                catch (OperationCanceledException e)
                {
                    // $JL-EXC$
                    return null;
                }
            }
            marker = null;
            /* END - marking objects */

            // check if unreachable objects exist, then either mark as GC root
            // unreachable (keep objects) or store a histogram of unreachable
            // objects
            if (newNoOfObjects < oldNoOfObjects)
            {
                Object un = idx.getSnapshotInfo().getProperty("keep_unreachable_objects"); //$NON-NLS-1$
                if (un instanceof Integer)
                {
                    int newRoot;
                    newRoot = (Integer)un;
                    markUnreachbleAsGCRoots(idx, reachable, newNoOfObjects, newRoot, listener);
                    newNoOfObjects = oldNoOfObjects;
                }
//                else
//                {
//                    createHistogramOfUnreachableObjects(idx, reachable);
//                }
            }

            if (listener.isCanceled())
                throw new IProgressListener.OperationCanceledException();
            listener.worked(1); // 3
            listener.subTask(Messages.GarbageCleaner_ReIndexingObjects);

            // create re-index map
            final int[] map = new int[oldNoOfObjects];
            final long[] id2a = new long[newNoOfObjects];

            List<ClassImpl> classes2remove = new ArrayList<ClassImpl>();

            final IOne2OneIndex preA2size = idx.array2size;

            for (int ii = 0, jj = 0; ii < oldNoOfObjects; ii++)
            {
                if (reachable[ii])
                {
                    map[ii] = jj;
                    id2a[jj++] = identifiers.get(ii);
                }
                else
                {
                    map[ii] = -1;

                    int classId = object2classId.get(ii);
                    ClassImpl clazz = classesById.get(classId);

                    int arraySize = preA2size.get(ii);
                    if (arraySize > 0)
                    {
                        clazz.removeInstance(arraySize);
                    }
                    else
                    {
                        // [INFO] some instances of java.lang.Class are not
                        // reported as HPROF_GC_CLASS_DUMP but as
                        // HPROF_GC_INSTANCE_DUMP
                        ClassImpl c = classesById.get(ii);

                        if (c == null)
                        {
                            clazz.removeInstance(clazz.getHeapSizePerInstance());
                        }
                        else
                        {
                            clazz.removeInstance(c.getUsedHeapSize());
                            classes2remove.add(c);
                        }
                    }
                }
            }

            // classes cannot be removed right away
            // as they are needed to remove instances of this class
            for (ClassImpl c : classes2remove)
            {
                classesById.remove(c.getObjectId());

                ClassImpl superclass = classesById.get(c.getSuperClassId());
                if (superclass != null)
                    superclass.removeSubClass(c);
            }

            reachable = null; // early gc...

            identifiers.close();
            identifiers.delete();
            identifiers = null;

            if (listener.isCanceled())
                throw new IProgressListener.OperationCanceledException();
            listener.worked(1); // 4
            listener.subTask(Messages.GarbageCleaner_ReIndexingClasses);

            // fix classes
            HashMapIntObject<ClassImpl> classesByNewId = new HashMapIntObject<ClassImpl>(classesById.size());
            for (Iterator<ClassImpl> iter = classesById.values(); iter.hasNext();)
            {
                ClassImpl clazz = iter.next();
                int index = map[clazz.getObjectId()];
                clazz.setObjectId(index);

                if (clazz.getSuperClassId() >= 0) // java.lang.Object
                    clazz.setSuperClassIndex(map[clazz.getSuperClassId()]);
                clazz.setClassLoaderIndex(map[clazz.getClassLoaderId()]);

                classesByNewId.put(index, clazz);
            }

            idx.getSnapshotInfo().setNumberOfClasses(classesByNewId.size());

            if (listener.isCanceled())
                throw new IProgressListener.OperationCanceledException();
            listener.worked(1); // 5

            // //////////////////////////////////////////////////////////////
            // identifiers
            // //////////////////////////////////////////////////////////////

            File indexFile = Index.IDENTIFIER.getFile(idx.snapshotInfo.getPrefix());
            listener.subTask(MessageUtil.format(Messages.GarbageCleaner_Writing, indexFile.getAbsolutePath()));
            idxManager.setReader(Index.IDENTIFIER, new IndexWriter.LongIndexStreamer().writeTo(indexFile, id2a));

            if (listener.isCanceled())
                throw new IProgressListener.OperationCanceledException();
            listener.worked(1); // 6

            // //////////////////////////////////////////////////////////////
            // object 2 class Id
            // //////////////////////////////////////////////////////////////

            indexFile = Index.O2CLASS.getFile(idx.snapshotInfo.getPrefix());
            listener.subTask(MessageUtil.format(Messages.GarbageCleaner_Writing, indexFile.getAbsolutePath()));
            idxManager.setReader(Index.O2CLASS, new IndexWriter.IntIndexStreamer().writeTo(indexFile,
                            new NewObjectIntIterator()
                            {
                                @Override
                                int doGetNextInt(int index)
                                {
                                    return map[idx.object2classId.get(nextIndex)];
                                    // return
                                    // map[object2classId.get(nextIndex)];
                                }

                                @Override
                                int[] getMap()
                                {
                                    return map;
                                }
                            }));

            object2classId.close();
            object2classId.delete();
            object2classId = null;

            if (listener.isCanceled())
                throw new IProgressListener.OperationCanceledException();
            listener.worked(1); // 7

            // //////////////////////////////////////////////////////////////
            // array size
            // //////////////////////////////////////////////////////////////

            indexFile = Index.A2SIZE.getFile(idx.snapshotInfo.getPrefix());
            listener.subTask(MessageUtil.format(Messages.GarbageCleaner_Writing, new Object[] { indexFile
                            .getAbsolutePath() }));
            final BitField arrayObjects = new BitField(newNoOfObjects);
            // arrayObjects
            idxManager.setReader(Index.A2SIZE, new IndexWriter.IntIndexStreamer().writeTo(indexFile,
                            new NewObjectIntIterator()
                            {
                                IOne2OneIndex a2size = preA2size;
                                int newIndex = 0;

                                @Override
                                int doGetNextInt(int index)
                                {
                                    int size = a2size.get(nextIndex);
                                    if (size > 0)
                                        arrayObjects.set(newIndex);
                                    newIndex++;
                                    return size;
                                }

                                @Override
                                int[] getMap()
                                {
                                    return map;
                                }
                            }));

            preA2size.close();
            preA2size.delete();

            if (listener.isCanceled())
                throw new IProgressListener.OperationCanceledException();
            listener.worked(1); // 9

            // //////////////////////////////////////////////////////////////
            // inbound, outbound
            // //////////////////////////////////////////////////////////////

            listener.subTask(Messages.GarbageCleaner_ReIndexingOutboundIndex);

            IndexWriter.IntArray1NSortedWriter w_out = new IndexWriter.IntArray1NSortedWriter(newNoOfObjects,
                            IndexManager.Index.OUTBOUND.getFile(idx.snapshotInfo.getPrefix()));
            IndexWriter.InboundWriter w_in = new IndexWriter.InboundWriter(newNoOfObjects, IndexManager.Index.INBOUND
                            .getFile(idx.snapshotInfo.getPrefix()));

            for (int ii = 0; ii < oldNoOfObjects; ii++)
            {
                int k = map[ii];
				if (k < 0) continue;

				int[] a = preOutbound.get(ii);
				int[] tl = new int[a.length];
				for (int jj = 0; jj < a.length; jj++)
				{
					int t = map[a[jj]];

					/* No check if the referenced objects are alive */
					/* The garbage can't be reached from a live object */
					// removed if (t >= 0) ...
					tl[jj] = t;
					w_in.log(t, k, jj == 0);
				}

				w_out.log(k, tl);
            }

            preOutbound.close();
            preOutbound.delete();
            preOutbound = null;

            if (listener.isCanceled())
            {
                w_in.cancel();
                w_out.cancel();
                throw new IProgressListener.OperationCanceledException();
            }
            listener.worked(1); // 10

            listener
                            .subTask(MessageUtil.format(Messages.GarbageCleaner_Writing, w_in.getIndexFile()
                                            .getAbsolutePath()));

            idxManager.setReader(Index.INBOUND, w_in.flush(listener, new KeyWriterImpl(classesByNewId)));
            w_in = null;
            if (listener.isCanceled())
            {
                w_out.cancel();
                throw new IProgressListener.OperationCanceledException();
            }

            listener.worked(1); // 11

            listener.subTask(MessageUtil.format(Messages.GarbageCleaner_Writing, new Object[] { w_out.getIndexFile()
                            .getAbsolutePath() }));
            idxManager.setReader(Index.OUTBOUND, w_out.flush());
            w_out = null;
            if (listener.isCanceled())
                throw new IProgressListener.OperationCanceledException();
            listener.worked(1); // 12

            // fix roots
            HashMapIntObject<XGCRootInfo[]> roots = fix(idx.gcRoots, map);
            idx.getSnapshotInfo().setNumberOfGCRoots(roots.size());

            // fix threads 2 objects 2 roots
            HashMapIntObject<HashMapIntObject<XGCRootInfo[]>> rootsPerThread = new HashMapIntObject<HashMapIntObject<XGCRootInfo[]>>();
            for (IteratorInt iter = idx.thread2objects2roots.keys(); iter.hasNext();)
            {
                int threadId = iter.next();
                int fixedThreadId = map[threadId];
                if (fixedThreadId < 0)
                    continue;

                rootsPerThread.put(fixedThreadId, fix(idx.thread2objects2roots.get(threadId), map));
            }

            // fill stuff into builder
            builder.setIndexManager(idxManager);
            builder.setClassCache(classesByNewId);
            builder.setArrayObjects(arrayObjects);
            builder.setRoots(roots);
            builder.setRootsPerThread(rootsPerThread);

            return map;
        }
        finally
        {
            // delete all temporary indices
            idx.delete();

            if (idxManager != null && listener.isCanceled())
                idxManager.delete();

        }
    }

    private static HashMapIntObject<XGCRootInfo[]> fix(HashMapIntObject<List<XGCRootInfo>> roots, final int[] map)
    {
        HashMapIntObject<XGCRootInfo[]> answer = new HashMapIntObject<XGCRootInfo[]>(roots.size());
        for (Iterator<List<XGCRootInfo>> iter = roots.values(); iter.hasNext();)
        {
            List<XGCRootInfo> r = iter.next();
            XGCRootInfo[] a = new XGCRootInfo[r.size()];
            for (int ii = 0; ii < a.length; ii++)
            {
                a[ii] = r.get(ii);
                a[ii].setObjectId(map[a[ii].getObjectId()]);
                if (a[ii].getContextAddress() != 0)
                    a[ii].setContextId(map[a[ii].getContextId()]);
            }

            answer.put(a[0].getObjectId(), a);
        }
        return answer;
    }

    private static abstract class NewObjectIterator
    {
        int nextIndex = -1;
        int[] $map;

        public NewObjectIterator()
        {
            $map = getMap();
            findNext();
        }

        protected void findNext()
        {
            nextIndex++;
            while (nextIndex < $map.length && $map[nextIndex] < 0)
                nextIndex++;
        }

        public boolean hasNext()
        {
            return nextIndex < $map.length;
        }

        abstract int[] getMap();
    }

    private static abstract class NewObjectIntIterator extends NewObjectIterator implements IteratorInt
    {
        public int next()
        {
            int answer = doGetNextInt(nextIndex);
            findNext();
            return answer;
        }

        abstract int doGetNextInt(int nextIndex);

    }

    private static class KeyWriterImpl implements IndexWriter.KeyWriter
    {
        HashMapIntObject<ClassImpl> classesByNewId;

        KeyWriterImpl(HashMapIntObject<ClassImpl> classesByNewId)
        {
            this.classesByNewId = classesByNewId;
        }

        public void storeKey(int index, Serializable key)
        {
            ClassImpl impl = classesByNewId.get(index);
            impl.setCacheEntry(key);
        }
    }

    // //////////////////////////////////////////////////////////////
    // create histogram of unreachable objects
    // //////////////////////////////////////////////////////////////

//    private static final class Record
//    {
//        ClassImpl clazz;
//        int objectCount;
//        long size;
//
//        public Record(ClassImpl clazz)
//        {
//            this.clazz = clazz;
//        }
//    }

//    private static void createHistogramOfUnreachableObjects(PreliminaryIndexImpl idx, boolean[] reachable)
//    {
//        IOne2OneIndex array2size = idx.array2size;
//
//        HashMapIntObject<Record> histogram = new HashMapIntObject<Record>();
//
//        int totalObjectCount = 0;
//        long totalSize = 0;
//
//        for (int ii = 0; ii < reachable.length; ii++)
//        {
//            if (!reachable[ii])
//            {
//                int classId = idx.object2classId.get(ii);
//
//                Record r = histogram.get(classId);
//                if (r == null)
//                {
//                    ClassImpl clazz = idx.classesById.get(classId);
//                    r = new Record(clazz);
//                    histogram.put(classId, r);
//                }
//
//                r.objectCount++;
//                totalObjectCount++;
//                int s = 0;
//
//                s = array2size.get(ii);
//                if (s > 0)
//                {
//                    // Already got the size
//                }
//                else if (IClass.JAVA_LANG_CLASS.equals(r.clazz.getName()))
//                {
//                    ClassImpl classImpl = idx.classesById.get(ii);
//                    if (classImpl == null)
//                    {
//                        s = r.clazz.getHeapSizePerInstance();
//                    }
//                    else
//                    {
//                        s = classImpl.getUsedHeapSize();
//                    }
//                }
//                else
//                {
//                    s = r.clazz.getHeapSizePerInstance();
//                }
//                r.size += s;
//                totalSize += s;
//            }
//        }
//
//        List<UnreachableObjectsHistogram.Record> records = new ArrayList<UnreachableObjectsHistogram.Record>();
//        for (Iterator<Record> iter = histogram.values(); iter.hasNext();)
//        {
//            Record r = iter.next();
//            records.add(new UnreachableObjectsHistogram.Record(r.clazz.getName(), r.clazz.getObjectAddress(), r.objectCount, r.size));
//        }
//
//        UnreachableObjectsHistogram deadObjectHistogram = new UnreachableObjectsHistogram(records);
//        idx.getSnapshotInfo().setProperty(UnreachableObjectsHistogram.class.getName(), deadObjectHistogram);
//    }

    // //////////////////////////////////////////////////////////////
    // mark unreachable objects as GC unknown
    // //////////////////////////////////////////////////////////////

    private static void markUnreachbleAsGCRoots(final PreliminaryIndexImpl idx, //
                    boolean[] reachable, //
                    int noReachableObjects, //
                    int extraRootType, IProgressListener listener)
    {
        final int noOfObjects = reachable.length;
        final IOne2LongIndex identifiers = idx.identifiers;
        final IOne2ManyIndex preOutbound = idx.outbound;

        int root[] = new int[1];
        ObjectMarker marker = new ObjectMarker(root, reachable, preOutbound, new SilentProgressListener(listener));

        // find objects not referenced by any other object
        boolean inbounds[] = new boolean[noOfObjects];
        for (int ii = 0; ii < noOfObjects; ++ii)
        {
            if (!reachable[ii])
            {
                // We only need search the unreachable objects as
                // the reachable ones will have already marked
                // its outbound refs.
                for (int out : preOutbound.get(ii))
                    inbounds[out] = true;
            }
        }

        for (int pass = 0; pass < 2; ++pass)
        {
            // First pass mark only the unreferenced objects, second
            // pass do everything else
            for (int ii = 0; ii < noOfObjects && noReachableObjects < noOfObjects; ++ii)
            {
                // Do the objects with no inbounds first
                if (!reachable[ii] && (pass == 1 || !inbounds[ii]))
                {
                    // Identify this unreachable object as a root,
                    // and see what else is now reachable
                    // No need to mark it as the marker will do that
                    root[0] = ii;

                    XGCRootInfo xgc = new XGCRootInfo(identifiers.get(ii), 0, extraRootType);
                    xgc.setObjectId(ii);

                    ArrayList<XGCRootInfo> xgcs = new ArrayList<XGCRootInfo>(1);
                    xgcs.add(xgc);
                    idx.gcRoots.put(ii, xgcs);

                    int marked = marker.markSingleThreaded();
                    noReachableObjects += marked;
                }
            }
        }

        // update GC root information
        idx.setGcRoots(idx.gcRoots);
        idx.getSnapshotInfo().setNumberOfGCRoots(idx.gcRoots.size());
    }
}
