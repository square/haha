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
package org.eclipse.mat.parser.internal.snapshot;

import java.util.List;
import java.util.Set;

import org.eclipse.mat.SnapshotException;
import org.eclipse.mat.collect.BitField;
import org.eclipse.mat.parser.index.IIndexReader;
import org.eclipse.mat.parser.internal.Messages;
import org.eclipse.mat.parser.internal.util.IntStack;
import org.eclipse.mat.snapshot.ExcludedReferencesDescriptor;
import org.eclipse.mat.snapshot.ISnapshot;
import org.eclipse.mat.snapshot.model.IObject;
import org.eclipse.mat.snapshot.model.NamedReference;
import org.eclipse.mat.util.IProgressListener;

public class ObjectMarker
{
    int[] roots;
    boolean[] bits;
    IIndexReader.IOne2ManyIndex outbound;
    IProgressListener progressListener;

    public ObjectMarker(int[] roots, boolean[] bits, IIndexReader.IOne2ManyIndex outbound,
                    IProgressListener progressListener)
    {
        this.roots = roots;
        this.bits = bits;
        this.outbound = outbound;
        this.progressListener = progressListener;
    }

    public int markSingleThreaded() throws IProgressListener.OperationCanceledException
    {
        int count = 0;
        int size = 0;
        int[] data = new int[10 * 1024]; // start with 10k
        int rootsToProcess = 0;

        for (int rootId : roots)
        {
            if (!bits[rootId])
            {
                /* start stack.push() */
                if (size == data.length)
                {
                    int[] newArr = new int[data.length << 1];
                    System.arraycopy(data, 0, newArr, 0, data.length);
                    data = newArr;
                }
                data[size++] = rootId;
                /* end stack.push() */

                bits[rootId] = true;
                count++;

                rootsToProcess++;
            }
        }

        progressListener.beginTask(Messages.ObjectMarker_CalculateRetainedSize, rootsToProcess);

        int current;

        while (size > 0)
        {
            /* start stack.pop() */
            current = data[--size];

            if (size <= rootsToProcess)
            {
                rootsToProcess--;
                progressListener.worked(1);
                if (progressListener.isCanceled())
                    throw new IProgressListener.OperationCanceledException();
            }

            for (int child : outbound.get(current))
            {
                if (!bits[child])
                {
                    // stack.push(child);
                    /* start stack.push() */
                    if (size == data.length)
                    {
                        int[] newArr = new int[data.length << 1];
                        System.arraycopy(data, 0, newArr, 0, data.length);
                        data = newArr;
                    }
                    data[size++] = child;
                    /* end stack.push() */

                    bits[child] = true;
                    count++;
                }

            }
        }

        progressListener.done();

        return count;
    }

    public int markSingleThreaded(ExcludedReferencesDescriptor[] excludeSets, ISnapshot snapshot)
                    throws SnapshotException, IProgressListener.OperationCanceledException
    {
        /*
         * prepare the exclude stuff
         */
        BitField excludeObjectsBF = new BitField(snapshot.getSnapshotInfo().getNumberOfObjects());
        for (ExcludedReferencesDescriptor set : excludeSets)
        {
            for (int k : set.getObjectIds())
            {
                excludeObjectsBF.set(k);
            }
        }

        int count = 0; // # of processed objects in the stack
        int rootsToProcess = 0; // counter to report progress

        /* a stack of int structure */
        int size = 0; // # of elements in the stack
        int[] data = new int[10 * 1024]; // data for the stack - start with 10k

        /* first put all "roots" in the stack, and mark them as processed */
        for (int rootId : roots)
        {
            if (!bits[rootId])
            {
                /* start stack.push() */
                if (size == data.length)
                {
                    int[] newArr = new int[data.length << 1];
                    System.arraycopy(data, 0, newArr, 0, data.length);
                    data = newArr;
                }
                data[size++] = rootId;
                /* end stack.push() */

                bits[rootId] = true; // mark the object
                count++;

                rootsToProcess++;
            }
        }

        /* now do the real marking */
        progressListener.beginTask(Messages.ObjectMarker_CalculateRetainedSize, rootsToProcess);

        int current;

        while (size > 0) // loop until there are elements in the stack
        {
            /* do a stack.pop() */
            current = data[--size];

            /* report progress if one of the roots is processed */
            if (size <= rootsToProcess)
            {
                rootsToProcess--;
                progressListener.worked(1);
                if (progressListener.isCanceled())
                    throw new IProgressListener.OperationCanceledException();
            }

            for (int child : outbound.get(current))
            {
                if (!bits[child]) // already visited?
                {
                    if (!refersOnlyThroughExcluded(current, child, excludeSets, excludeObjectsBF, snapshot))
                    {
                        /* start stack.push() */
                        if (size == data.length)
                        {
                            int[] newArr = new int[data.length << 1];
                            System.arraycopy(data, 0, newArr, 0, data.length);
                            data = newArr;
                        }
                        data[size++] = child;
                        /* end stack.push() */

                        bits[child] = true; // mark the object
                        count++;
                    }
                }
            }
        }

        progressListener.done();

        return count;
    }

    public void markMultiThreaded(int numberOfThreads) throws InterruptedException
    {
        IntStack rootsStack = new IntStack(roots.length);

        for (int rootId : roots)
        {
            if (!bits[rootId])
            {
                rootsStack.push(rootId);
                bits[rootId] = true;
            }
        }

        progressListener.beginTask(Messages.ObjectMarker_CalculateRetainedSize, rootsStack.size());

        // create and start as much marker threads as specified
        DfsThread[] dfsthreads = new DfsThread[numberOfThreads];
        Thread[] threads = new Thread[numberOfThreads];

        for (int i = 0; i < numberOfThreads; i++)
        {
            DfsThread dfsthread = new DfsThread(rootsStack);
            dfsthreads[i] = dfsthread;
            Thread thread = new Thread(dfsthread, "ObjectMarkerThread-" + (i + 1));//$NON-NLS-1$
            thread.start();
            threads[i] = thread;
        }

        // wait for all the threads to finish
        for (int i = 0; i < numberOfThreads; i++)
        {
            threads[i].join();
        }

        if (progressListener.isCanceled())
            return;

        progressListener.done();
    }

    public class DfsThread implements Runnable
    {

        int size = 0;
        int[] data = new int[10 * 1024]; // start with 10k
        IntStack rootsStack;

        public DfsThread(IntStack roots)
        {
            this.rootsStack = roots;
        }

        public void run()
        {
            while (true)
            {
                synchronized (rootsStack)
                {
                    progressListener.worked(1);
                    if (progressListener.isCanceled())
                        return;

                    if (rootsStack.size() > 0) // still some roots are not
                    // processed
                    {
                        data[0] = rootsStack.pop();
                        size = 1;
                    }
                    else
                    // the work is done
                    {
                        break;
                    }
                }

                int current;

                while (size > 0)
                {
                    /* start stack.pop() */
                    current = data[--size];
                    /* end stack.pop */

                    for (int child : outbound.get(current))
                    {
                        /*
                         * No synchronization here. It costs a lot of
                         * performance It is possible that some bits are marked
                         * more than once, but this is not a problem
                         */
                        if (!bits[child])
                        {
                            bits[child] = true;
                            // stack.push(child);
                            /* start stack.push() */
                            if (size == data.length)
                            {
                                int[] newArr = new int[data.length << 1];
                                System.arraycopy(data, 0, newArr, 0, data.length);
                                data = newArr;
                            }
                            data[size++] = child;
                            /* end stack.push() */
                        }
                    }
                } // end of processing one GC root
            }
        }
    }

    private boolean refersOnlyThroughExcluded(int referrerId, int referentId,
                    ExcludedReferencesDescriptor[] excludeSets, BitField excludeObjectsBF, ISnapshot snapshot)
                    throws SnapshotException
    {
        if (!excludeObjectsBF.get(referrerId))
            return false;

        IObject referrerObject = snapshot.getObject(referrerId);
        Set<String> excludeFields = null;
        for (ExcludedReferencesDescriptor set : excludeSets)
        {
            if (set.contains(referrerId))
            {
                excludeFields = set.getFields();
                break;
            }
        }
        if (excludeFields == null)
            return true; // treat null as all fields

        long referentAddr = snapshot.mapIdToAddress(referentId);

        List<NamedReference> refs = referrerObject.getOutboundReferences();
        for (NamedReference reference : refs)
        {
            if (referentAddr == reference.getObjectAddress() && !excludeFields.contains(reference.getName())) { return false; }
        }
        return true;
    }

}
