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

import java.io.IOException;
import java.util.Arrays;
import java.util.NoSuchElementException;

import org.eclipse.mat.SnapshotException;
import org.eclipse.mat.collect.ArrayUtils;
import org.eclipse.mat.collect.BitField;
import org.eclipse.mat.collect.IteratorInt;
import org.eclipse.mat.parser.index.IIndexReader;
import org.eclipse.mat.parser.index.IndexManager;
import org.eclipse.mat.parser.index.IndexWriter;
import org.eclipse.mat.parser.index.IndexManager.Index;
import org.eclipse.mat.parser.internal.util.IntStack;
import org.eclipse.mat.util.IProgressListener;
import org.eclipse.mat.util.SimpleMonitor;

public class DominatorTree
{

    public static void calculate(SnapshotImpl snapshot, IProgressListener listener) throws SnapshotException,
                    IOException
    {
        new Calculator(snapshot, listener).compute();
    }

    static class Calculator
    {
        SnapshotImpl snapshot;
        SimpleMonitor monitor;
        IIndexReader.IOne2ManyIndex inboundIndex;
        IIndexReader.IOne2ManyIndex outboundIndex;

        int[] gcRootsArray;
        private BitField gcRootsSet;

        int[] bucket;
        private int r, n;
        private int[] dom;
        private int[] parent;
        private int[] anchestor;
        private int[] vertex;
        private int[] label;
        private int[] semi;

        private static int ROOT_VALUE = -1;
        private static int[] ROOT_VALUE_ARR = new int[] { ROOT_VALUE };

        public Calculator(SnapshotImpl snapshot, IProgressListener listener) throws SnapshotException
        {

            this.snapshot = snapshot;
            inboundIndex = snapshot.getIndexManager().inbound();
            outboundIndex = snapshot.getIndexManager().outbound();
            this.monitor = new SimpleMonitor(Messages.DominatorTree_CalculatingDominatorTree, listener, new int[] {
                            300, 300, 200, 200, 200 });
            gcRootsArray = snapshot.getGCRoots();
            gcRootsSet = new BitField(snapshot.getSnapshotInfo().getNumberOfObjects());
            for (int id : gcRootsArray)
            {
                gcRootsSet.set(id);
            }

            IndexManager manager = this.snapshot.getIndexManager();
            try
            {
                manager.a2size().unload();
                manager.o2address().unload();
                manager.o2class().unload();
            }
            catch (IOException e)
            {
                throw new SnapshotException(e);
            }

            n = snapshot.getSnapshotInfo().getNumberOfObjects() + 1;
            r = 1;

            dom = new int[n + 1];
            parent = new int[n + 1];
            anchestor = new int[n + 1];
            vertex = new int[n + 1];
            label = new int[n + 1];
            semi = new int[n + 1];
            bucket = new int[n + 1];

            Arrays.fill(bucket, -1);

        }

        public void compute() throws IOException, SnapshotException, IProgressListener.OperationCanceledException
        {
            IProgressListener progressListener0 = this.monitor.nextMonitor();
            progressListener0.beginTask(Messages.DominatorTree_DominatorTreeCalculation, 3);

            n = 0;
            dfs(r);

            snapshot.getIndexManager().outbound().unload();

            IProgressListener progressListener = this.monitor.nextMonitor();
            progressListener.beginTask(Messages.DominatorTree_ComputingDominators, n / 1000);

            for (int i = n; i >= 2; i--)
            {
                int w = vertex[i];
                for (int v : getPredecessors(w))
                {
                    v += 2;
                    if (v < 0)
                        continue;
                    int u = eval(v);
                    if (semi[u] < semi[w])
                    {
                        semi[w] = semi[u];
                    }
                }
                // add w to bucket(vertex(semi(w)))
                // create the bucket if needed
                bucket[w] = bucket[vertex[semi[w]]]; // serves as next(w)
                bucket[vertex[semi[w]]] = w; // serves as
                // first(vertex[semi[w]])
                link(parent[w], w);

                int v = bucket[parent[w]];
                while (v != -1)
                {
                    int u = eval(v);
                    if (semi[u] < semi[v])
                    {
                        dom[v] = u;
                    }
                    else
                    {
                        dom[v] = parent[w];
                    }
                    v = bucket[v]; // here bucket serves as next[]
                }
                bucket[parent[w]] = -1;
                // }
                if (i % 1000 == 0)
                {
                    if (progressListener.isCanceled())
                        throw new IProgressListener.OperationCanceledException();
                    progressListener.worked(1);
                }
            }

            for (int i = 2; i <= n; i++)
            {
                int w = vertex[i];
                if (dom[w] != vertex[semi[w]])
                {
                    dom[w] = dom[dom[w]];
                }
            }
            dom[r] = 0;

            progressListener.done();

            parent = anchestor = vertex = label = semi = bucket = null;
            snapshot.getIndexManager().inbound().unload();

            if (progressListener0.isCanceled())
                throw new IProgressListener.OperationCanceledException();

            // pre-condition for index writing:
            // retainedSetIdx is still sorted by object id
            snapshot.getIndexManager().setReader(
                            IndexManager.Index.DOMINATOR,
                            new IndexWriter.IntIndexStreamer().writeTo(IndexManager.Index.DOMINATOR.getFile(snapshot
                                            .getSnapshotInfo().getPrefix()), new IteratorInt()
                            {
                                int nextIndex = 2;

                                public boolean hasNext()
                                {
                                    return nextIndex < dom.length;
                                }

                                public int next()
                                {
                                    return dom[nextIndex++];
                                }

                            }));

            int[] objectIds = new int[snapshot.getSnapshotInfo().getNumberOfObjects() + 2];
            for (int i = 0; i < objectIds.length; i++)
                objectIds[i] = i - 2;

            objectIds[0] = -2;
            objectIds[1] = ROOT_VALUE;
            progressListener0.worked(1);

            ArrayUtils.sort(dom, objectIds, 2, dom.length - 2);
            progressListener0.worked(1);

            FlatDominatorTree tree = new FlatDominatorTree(snapshot, dom, objectIds, ROOT_VALUE);

            if (progressListener0.isCanceled())
                throw new IProgressListener.OperationCanceledException();

            writeIndexFiles(tree);
            progressListener0.done();

        }

        private void dfs(int root) throws UnsupportedOperationException
        {
            IProgressListener progressListener = this.monitor.nextMonitor();
            progressListener.beginTask(Messages.DominatorTree_DepthFirstSearch, snapshot.getSnapshotInfo()
                            .getNumberOfObjects() >> 16);

            // a stack for each parameter - stack code is inlined for
            // performance
            // currentElementStack - for v, successorsStack - for the successors
            // array,
            // currentSuccessorStack - for the index in the array
            int capacity = 2048; // capacity for the arrays
            int size = 0; // one size for all arrays
            int[] currentElementStack = new int[capacity];
            int[] currentSuccessorStack = new int[capacity];
            Object[] successorsStack = new Object[capacity];

            int v = root;
            int[] successors = gcRootsArray;
            int currentSuccessor = 0;

            // push the initial values
            currentElementStack[size] = root;
            successorsStack[size] = successors;
            currentSuccessorStack[size] = currentSuccessor;
            size++;

            while (size > 0)
            {
                v = currentElementStack[size - 1];
                successors = (int[]) successorsStack[size - 1];
                currentSuccessor = currentSuccessorStack[size - 1];

                if (semi[v] == 0)
                {
                    n = n + 1;
                    semi[v] = n;
                    vertex[n] = v;
                    label[v] = v;
                    anchestor[v] = 0;
                }

                if (currentSuccessor < successors.length)
                {
                    int w = successors[currentSuccessor++] + 2;
                    currentSuccessorStack[size - 1] = currentSuccessor; // update
                    // the top
                    // value

                    // push the next unvisited successor
                    if (semi[w] == 0)
                    {
                        parent[w] = v;
                        successors = outboundIndex.get(w - 2); // get the
                        // successors of w

                        /* start push() */
                        // is expanding needed?
                        if (size == capacity)
                        {
                            int newCapacity = capacity << 1;
                            // resize currentElementStack
                            int[] newArr = new int[newCapacity];
                            System.arraycopy(currentElementStack, 0, newArr, 0, capacity);
                            currentElementStack = newArr;

                            // resize currentSuccessorStack
                            newArr = new int[newCapacity];
                            System.arraycopy(currentSuccessorStack, 0, newArr, 0, capacity);
                            currentSuccessorStack = newArr;

                            // resize successorsStack
                            Object[] newSuccessorsArr = new Object[newCapacity];
                            System.arraycopy(successorsStack, 0, newSuccessorsArr, 0, capacity);
                            successorsStack = newSuccessorsArr;

                            capacity = newCapacity;

                        }
                        currentElementStack[size] = w;
                        successorsStack[size] = successors;
                        currentSuccessorStack[size] = 0;
                        size++;
                        /* end push() */

                        // report progress
                        if ((n & 0xffff) == 0)
                        {
                            if (progressListener.isCanceled())
                                throw new IProgressListener.OperationCanceledException();
                            progressListener.worked(1);
                        }
                    }
                }
                else
                {
                    // this one acts as a pop() for all tree stacks
                    size--;
                }
            }

            progressListener.done();

        }

        // gets retained set idx and returns the real indexes
        private int[] getPredecessors(int v)
        {
            v -= 2;
            // for the GC roots return the artificial root
            if (gcRootsSet.get(v))
            {
                return ROOT_VALUE_ARR;
            }
            else
            {
                return inboundIndex.get(v);
            }
        }

        private void compress(int v)
        {
            IntStack stack = new IntStack();
            while (anchestor[anchestor[v]] != 0) // is ancestor[v] a root in
            // the
            // forest?
            {
                stack.push(v);
                v = anchestor[v];
            }
            while (stack.size() > 0)
            {
                v = stack.pop();
                if (semi[label[anchestor[v]]] < semi[label[v]])
                {
                    label[v] = label[anchestor[v]];
                }
                anchestor[v] = anchestor[anchestor[v]];
            }
        }

        private int eval(int v)
        {
            if (anchestor[v] == 0)
            {
                return v;
            }
            else
            {
                compress(v);
                return label[v];
            }
        }

        private void link(int v, int w)
        {
            anchestor[w] = v;
        }

        private void writeIndexFiles(FlatDominatorTree tree) throws IOException
        {

            IndexWriter.IntArray1NWriter writer = new IndexWriter.IntArray1NWriter(dom.length - 1,
                            IndexManager.Index.DOMINATED.getFile(snapshot.getSnapshotInfo().getPrefix()));

            int numberOfObjects = snapshot.getSnapshotInfo().getNumberOfObjects();

            IProgressListener progressListener = this.monitor.nextMonitor();
            progressListener.beginTask(Messages.DominatorTree_CreateDominatorsIndexFile, numberOfObjects / 1000);

            for (int i = -1; i < numberOfObjects; i++)
            {
                int[] successors = tree.getSuccessorsArr(i);
                tree.sortByTotalSize(successors);
                writer.log(i + 1, successors);

                if (i % 1000 == 0)
                {
                    if (progressListener.isCanceled())
                        throw new IProgressListener.OperationCanceledException();
                    progressListener.worked(1);
                }
            }

            snapshot.getIndexManager().setReader(IndexManager.Index.DOMINATED, writer.flush());

            progressListener.done();

        }

        public class FlatDominatorTree
        {
            private static final int TEMP_ARR_LENGTH = 1000000;

            int[] dom;
            int[] elements;
            long[] ts;
            SnapshotImpl dump;

            // temp arrays to pass for the radix sort
            long[] tempLongArray = new long[TEMP_ARR_LENGTH];
            int[] tempIntArray = new int[TEMP_ARR_LENGTH];

            FlatDominatorTree(SnapshotImpl dump, int[] dom, int[] elements, int root) throws SnapshotException,
                            IOException
            {
                this.dump = dump;
                this.dom = dom;
                this.elements = elements;
                this.ts = new long[dom.length];
                calculateTotalSizesIterative(root);

            }

            public SuccessorsEnum getSuccessorsEnum(int i)
            {
                return new SuccessorsEnum(i);
            }

            public int[] getSuccessorsArr(int parentId)
            {
                parentId += 2;

                // find the first child
                int j = Arrays.binarySearch(dom, parentId);
                if (j < 0)
                    return new int[0];

                int i = j;
                while ((i > 1) && (dom[i - 1] == parentId))
                    i--;

                // find length
                while (j < dom.length && dom[j] == parentId)
                    j++;

                int length = j - i;
                int[] result = new int[length];
                System.arraycopy(elements, i, result, 0, length);

                return result;
            }

            public void sortByTotalSize(int[] objectIds)
            {
                int length = objectIds.length;

                // collect the total sizes of the objects
                long[] totalSizes = new long[length];
                for (int i = 0; i < length; i++)
                {
                    totalSizes[i] = ts[objectIds[i] + 2];
                }

                // sort both arrays according to the total sizes
                if (totalSizes.length > 1)
                    if (totalSizes.length > TEMP_ARR_LENGTH)
                    {
                        ArrayUtils.sortDesc(totalSizes, objectIds);
                    }
                    else
                    {
                        ArrayUtils.sortDesc(totalSizes, objectIds, tempLongArray, tempIntArray);
                    }
            }

            class SuccessorsEnum
            {
                int parent;
                int nextIndex;

                SuccessorsEnum(int parent)
                {
                    this.parent = parent;
                    nextIndex = findFirstChildIndex(parent + 2);
                }

                public boolean hasMoreElements()
                {
                    return nextIndex > 0;
                }

                public int nextElement()
                {
                    if (nextIndex < 0)
                        throw new NoSuchElementException();
                    int res = elements[nextIndex++];

                    if (nextIndex >= dom.length || dom[nextIndex] != parent + 2)
                        nextIndex = -1;

                    return res;
                }

                int findFirstChildIndex(int el)
                {
                    int i = Arrays.binarySearch(dom, el);
                    while ((i > 1) && (dom[i - 1] == el))
                        i--;
                    return i;
                }
            }

            public void calculateTotalSizesIterative(int e) throws SnapshotException, IOException
            {
                IndexWriter.LongIndexCollector retained = new IndexWriter.LongIndexCollector(dump.getSnapshotInfo()
                                .getNumberOfObjects(), IndexWriter.mostSignificantBit(dump.getSnapshotInfo()
                                .getUsedHeapSize()));

                int capacity = 2048;
                int size = 0;
                int[] stack = new int[capacity];
                SuccessorsEnum[] succStack = new SuccessorsEnum[capacity];

                int currentEntry = e;
                SuccessorsEnum currentSucc = getSuccessorsEnum(currentEntry);
                stack[size] = currentEntry;
                succStack[size] = currentSucc;
                size++;

                IProgressListener progressListener = Calculator.this.monitor.nextMonitor();
                progressListener.beginTask(Messages.DominatorTree_CalculateRetainedSizes, dump.getSnapshotInfo()
                                .getNumberOfObjects() / 1000);
                int counter = 0;

                while (size > 0)
                {
                    currentEntry = stack[size - 1];
                    currentSucc = succStack[size - 1];

                    if (currentSucc.hasMoreElements())
                    {
                        int nextChild = currentSucc.nextElement();
                        currentSucc = getSuccessorsEnum(nextChild);

                        ts[nextChild + 2] = nextChild < 0 ? 0 : snapshot.getHeapSize(nextChild);

                        if (size == capacity)
                        {
                            int newCapacity = capacity << 1;
                            int[] newArr = new int[newCapacity];
                            System.arraycopy(stack, 0, newArr, 0, capacity);
                            stack = newArr;

                            // resize successorsStack
                            SuccessorsEnum[] newSuccessorsArr = new SuccessorsEnum[newCapacity];
                            System.arraycopy(succStack, 0, newSuccessorsArr, 0, capacity);
                            succStack = newSuccessorsArr;
                            capacity = newCapacity;
                        }
                        stack[size] = nextChild;
                        succStack[size] = currentSucc;
                        size++;
                    }
                    else
                    {
                        size--;

                        if (size > 0)
                            ts[stack[size - 1] + 2] += ts[currentEntry + 2];

                        if (currentEntry >= 0)
                        {
                            retained.set(currentEntry, ts[currentEntry + 2]);
                            if (++counter % 1000 == 0)
                            {
                                if (progressListener.isCanceled())
                                    throw new IProgressListener.OperationCanceledException();
                                progressListener.worked(1);
                            }
                        }

                    }
                }

                dump.getIndexManager().setReader(
                                Index.O2RETAINED,
                                retained.writeTo(IndexManager.Index.O2RETAINED.getFile(dump.getSnapshotInfo()
                                                .getPrefix())));
                retained = null;

                progressListener.done();
            }
        }
    }
}
