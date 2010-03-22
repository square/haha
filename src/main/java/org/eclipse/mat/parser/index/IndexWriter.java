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
package org.eclipse.mat.parser.index;

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.lang.ref.SoftReference;
import java.util.Arrays;
import java.util.NoSuchElementException;

import org.eclipse.mat.collect.ArrayInt;
import org.eclipse.mat.collect.ArrayIntCompressed;
import org.eclipse.mat.collect.ArrayLong;
import org.eclipse.mat.collect.ArrayLongCompressed;
import org.eclipse.mat.collect.ArrayUtils;
import org.eclipse.mat.collect.BitField;
import org.eclipse.mat.collect.HashMapIntLong;
import org.eclipse.mat.collect.HashMapIntObject;
import org.eclipse.mat.collect.IteratorInt;
import org.eclipse.mat.collect.IteratorLong;
import org.eclipse.mat.collect.SetInt;
import org.eclipse.mat.parser.index.IIndexReader.IOne2LongIndex;
import org.eclipse.mat.parser.index.IIndexReader.IOne2OneIndex;
import org.eclipse.mat.parser.io.BitInputStream;
import org.eclipse.mat.parser.io.BitOutputStream;
import org.eclipse.mat.util.IProgressListener;

public abstract class IndexWriter
{
    public static final int PAGE_SIZE_INT = 1000000;
    public static final int PAGE_SIZE_LONG = 500000;

    public interface KeyWriter
    {
        public void storeKey(int index, Serializable key);
    }

    // //////////////////////////////////////////////////////////////
    // integer based indices
    // //////////////////////////////////////////////////////////////

    public static class Identifier implements IIndexReader.IOne2LongIndex
    {
        long[] identifiers;
        int size;

        public void add(long id)
        {
            if (identifiers == null)
            {
                identifiers = new long[10000];
                size = 0;
            }

            if (size + 1 > identifiers.length)
            {
                int newCapacity = (identifiers.length * 3) / 2 + 1;
                if (newCapacity < size + 1)
                    newCapacity = size + 1;
                identifiers = copyOf(identifiers, newCapacity);
            }

            identifiers[size++] = id;
        }

        public void sort()
        {
            Arrays.sort(identifiers, 0, size);
        }

        public int size()
        {
            return size;
        }

        public long get(int index)
        {
            if (index < 0 || index >= size)
                throw new IndexOutOfBoundsException();

            return identifiers[index];
        }

        public int reverse(long val)
        {
            int a, c;
            for (a = 0, c = size; a < c;)
            {
                // Avoid overflow problems by using unsigned divide by 2
                int b = (a + c) >>> 1;
                long probeVal = get(b);
                if (val < probeVal)
                {
                    c = b;
                }
                else if (probeVal < val)
                {
                    a = b + 1;
                }
                else
                {
                    return b;
                }
            }
            // Negative index indicates not found (and where to insert)
            return -1 - a;
        }

        public IteratorLong iterator()
        {
            return new IteratorLong()
            {

                int index = 0;

                public boolean hasNext()
                {
                    return index < size;
                }

                public long next()
                {
                    return identifiers[index++];
                }

            };
        }

        public long[] getNext(int index, int length)
        {
            long answer[] = new long[length];
            for (int ii = 0; ii < length; ii++)
                answer[ii] = identifiers[index + ii];
            return answer;
        }

        public void close() throws IOException
        {}

        public void delete()
        {
            identifiers = null;
        }

        public void unload() throws IOException
        {
            throw new UnsupportedOperationException();
        }
    }

    public static class IntIndexCollectorUncompressed
    {
        int[] dataElements;

        public IntIndexCollectorUncompressed(int size)
        {
            dataElements = new int[size];
        }

        public void set(int index, int value)
        {
            dataElements[index] = value;
        }

        public int get(int index)
        {
            return dataElements[index];
        }

        public IIndexReader.IOne2OneIndex writeTo(File indexFile) throws IOException
        {
            return new IntIndexStreamer().writeTo(indexFile, dataElements);
        }
    }

    static class Pages<V>
    {
        int size;
        Object[] elements;

        public Pages(int initialSize)
        {
            elements = new Object[initialSize];
            size = 0;
        }

        private void ensureCapacity(int minCapacity)
        {
            int oldCapacity = elements.length;
            if (minCapacity > oldCapacity)
            {
                int newCapacity = (oldCapacity * 3) / 2 + 1;
                if (newCapacity < minCapacity)
                    newCapacity = minCapacity;

                Object[] copy = new Object[newCapacity];
                System.arraycopy(elements, 0, copy, 0, Math.min(elements.length, newCapacity));
                elements = copy;
            }
        }

        @SuppressWarnings("unchecked")
        public V get(int key)
        {
            return (key >= elements.length) ? null : (V) elements[key];
        }

        public void put(int key, V value)
        {
            ensureCapacity(key + 1);
            elements[key] = value;
            size = Math.max(size, key + 1);
        }

        public int size()
        {
            return size;
        }
    }

    abstract static class IntIndex<V>
    {
        int pageSize;
        int size;
        Pages<V> pages;

        protected IntIndex()
        {}

        protected IntIndex(int size)
        {
            init(size, PAGE_SIZE_INT);
        }

        protected void init(int size, int pageSize)
        {
            this.size = size;
            this.pageSize = pageSize;
            this.pages = new Pages<V>(size / pageSize + 1);
        }

        public int get(int index)
        {
            ArrayIntCompressed array = getPage(index / pageSize);
            return array.get(index % pageSize);
        }

        public int[] getNext(int index, int length)
        {
            int answer[] = new int[length];
            int page = index / pageSize;
            int pageIndex = index % pageSize;

            ArrayIntCompressed array = getPage(page);
            for (int ii = 0; ii < length; ii++)
            {
                answer[ii] = array.get(pageIndex++);
                if (pageIndex >= pageSize)
                {
                    array = getPage(++page);
                    pageIndex = 0;
                }
            }

            return answer;
        }

        @SuppressWarnings("null")
        public int[] getAll(int index[])
        {
            int[] answer = new int[index.length];

            int page = -1;
            ArrayIntCompressed array = null;

            for (int ii = 0; ii < answer.length; ii++)
            {
                int p = index[ii] / pageSize;
                if (p != page)
                    array = getPage(page = p);

                answer[ii] = array.get(index[ii] % pageSize);
            }

            return answer;
        }

        public void set(int index, int value)
        {
            ArrayIntCompressed array = getPage(index / pageSize);
            array.set(index % pageSize, value);
        }

        protected abstract ArrayIntCompressed getPage(int page);

        public synchronized void unload()
        {
            this.pages = new Pages<V>(size / pageSize + 1);
        }

        public int size()
        {
            return size;
        }

        public IteratorInt iterator()
        {
            return new IntIndexIterator(this);
        }
    }

    static class IntIndexIterator implements IteratorInt
    {
        IntIndex<?> intArray;
        int nextIndex = 0;

        public IntIndexIterator(IntIndex<?> intArray)
        {
            this.intArray = intArray;
        }

        public int next()
        {
            return intArray.get(nextIndex++);
        }

        public boolean hasNext()
        {
            return nextIndex < intArray.size();
        }
    }

    public static class IntIndexCollector extends IntIndex<ArrayIntCompressed> implements IOne2OneIndex
    {
        int mostSignificantBit;

        public IntIndexCollector(int size, int mostSignificantBit)
        {
            super(size);
            this.mostSignificantBit = mostSignificantBit;
        }

        @Override
        protected ArrayIntCompressed getPage(int page)
        {
            ArrayIntCompressed array = pages.get(page);
            if (array == null)
            {
                int ps = page < (size / pageSize) ? pageSize : size % pageSize;
                array = new ArrayIntCompressed(ps, 31 - mostSignificantBit, 0);
                pages.put(page, array);
            }
            return array;
        }

        public IIndexReader.IOne2OneIndex writeTo(File indexFile) throws IOException
        {
            // needed to re-compress
            return new IntIndexStreamer().writeTo(indexFile, this.iterator());
        }

        public IIndexReader.IOne2OneIndex writeTo(DataOutputStream out, long position) throws IOException
        {
            return new IntIndexStreamer().writeTo(out, position, this.iterator());
        }

        public void close() throws IOException
        {}

        public void delete()
        {
            pages = null;
        }
    }

    public static class IntIndexStreamer extends IntIndex<SoftReference<ArrayIntCompressed>>
    {
        DataOutputStream out;
        ArrayLong pageStart;
        int[] page;
        int left;

        public IIndexReader.IOne2OneIndex writeTo(File indexFile, IteratorInt iterator) throws IOException
        {
            DataOutputStream out = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(indexFile)));

            openStream(out, 0);
            addAll(iterator);
            closeStream();

            out.close();

            return getReader(indexFile);
        }

        public IIndexReader.IOne2OneIndex writeTo(File indexFile, int[] array) throws IOException
        {
            DataOutputStream out = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(indexFile)));

            openStream(out, 0);
            addAll(array);
            closeStream();

            out.close();

            return getReader(indexFile);
        }

        public IIndexReader.IOne2OneIndex writeTo(DataOutputStream out, long position, IteratorInt iterator)
                        throws IOException
        {
            openStream(out, position);
            addAll(iterator);
            closeStream();

            return getReader(null);
        }

        public IIndexReader.IOne2OneIndex writeTo(DataOutputStream out, long position, int[] array) throws IOException
        {
            openStream(out, position);
            addAll(array);
            closeStream();

            return getReader(null);
        }

        void openStream(DataOutputStream out, long position)
        {
            this.out = out;

            init(0, PAGE_SIZE_INT);

            this.page = new int[pageSize];
            this.pageStart = new ArrayLong();
            this.pageStart.add(position);
            this.left = page.length;
        }

        /**
         * @return total bytes written to index file
         */
        long closeStream() throws IOException
        {
            if (left < page.length)
                addPage();

            // write header information
            for (int jj = 0; jj < pageStart.size(); jj++)
                out.writeLong(pageStart.get(jj));

            out.writeInt(pageSize);
            out.writeInt(size);

            this.page = null;

            this.out = null;

            return this.pageStart.lastElement() + (8 * pageStart.size()) + 8 - this.pageStart.firstElement();
        }

        IndexReader.IntIndexReader getReader(File indexFile)
        {
            return new IndexReader.IntIndexReader(indexFile, pages, size, pageSize, pageStart.toArray());
        }

        void addAll(IteratorInt iterator) throws IOException
        {
            while (iterator.hasNext())
                add(iterator.next());
        }

        void add(int value) throws IOException
        {
            if (left == 0)
                addPage();

            page[page.length - left--] = value;
            size++;

        }

        void addAll(int[] values) throws IOException
        {
            addAll(values, 0, values.length);
        }

        void addAll(int[] values, int offset, int length) throws IOException
        {
            while (length > 0)
            {
                if (left == 0)
                    addPage();

                int chunk = Math.min(left, length);

                System.arraycopy(values, offset, page, page.length - left, chunk);
                left -= chunk;
                size += chunk;

                length -= chunk;
                offset += chunk;
            }
        }

        private void addPage() throws IOException
        {
            ArrayIntCompressed array = new ArrayIntCompressed(page, 0, page.length - left);

            byte[] buffer = array.toByteArray();
            out.write(buffer);
            int written = buffer.length;

            pages.put(pages.size(), new SoftReference<ArrayIntCompressed>(array));
            pageStart.add(pageStart.lastElement() + written);

            left = page.length;
        }

        @Override
        protected ArrayIntCompressed getPage(int page)
        {
            throw new UnsupportedOperationException();
        }

    }

    public static class IntArray1NWriter
    {
        int[] header;
        File indexFile;

        DataOutputStream out;
        IntIndexStreamer body;

        public IntArray1NWriter(int size, File indexFile) throws IOException
        {
            this.header = new int[size];
            this.indexFile = indexFile;

            this.out = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(indexFile)));
            this.body = new IntIndexStreamer();
            this.body.openStream(this.out, 0);
        }

        public void log(Identifier identifer, int index, ArrayLong references) throws IOException
        {
            // remove duplicates and convert to identifiers
            // keep pseudo reference as first one

            long pseudo = references.firstElement();

            references.sort();

            int[] objectIds = new int[references.size()];
            int length = 1;

            long current = 0, last = references.firstElement() - 1;
            for (int ii = 0; ii < objectIds.length; ii++)
            {
                current = references.get(ii);
                if (last != current)
                {
                    int objectId = identifer.reverse(current);

                    if (objectId >= 0)
                    {
                        int jj = (current == pseudo) ? 0 : length++;
                        objectIds[jj] = objectId;
                    }

                }

                last = current;
            }

            this.set(index, objectIds, 0, length);
        }

        /**
         * must not contain duplicates!
         */
        public void log(int index, ArrayInt references) throws IOException
        {
            this.set(index, references.toArray(), 0, references.size());
        }

        public void log(int index, int[] values) throws IOException
        {
            this.set(index, values, 0, values.length);
        }

        protected void set(int index, int[] values, int offset, int length) throws IOException
        {
            header[index] = body.size();

            body.add(length);

            body.addAll(values, offset, length);
        }

        public IIndexReader.IOne2ManyIndex flush() throws IOException
        {
            long divider = body.closeStream();

            IIndexReader.IOne2OneIndex headerIndex = new IntIndexStreamer().writeTo(out, divider, header);

            out.writeLong(divider);

            out.close();
            out = null;

            return createReader(headerIndex, body.getReader(null));
        }

        /**
         * @throws IOException
         */
        protected IIndexReader.IOne2ManyIndex createReader(IIndexReader.IOne2OneIndex headerIndex,
                        IIndexReader.IOne2OneIndex bodyIndex) throws IOException
        {
            return new IndexReader.IntIndex1NReader(this.indexFile, headerIndex, bodyIndex);
        }

        public void cancel()
        {
            try
            {
                if (out != null)
                {
                    out.close();
                    body = null;
                    out = null;
                }
            }
            catch (IOException ignore)
            {}
            finally
            {
                if (indexFile.exists())
                    indexFile.delete();
            }
        }

        public File getIndexFile()
        {
            return indexFile;
        }
    }

    public static class IntArray1NSortedWriter extends IntArray1NWriter
    {
        public IntArray1NSortedWriter(int size, File indexFile) throws IOException
        {
            super(size, indexFile);
        }

        protected void set(int index, int[] values, int offset, int length) throws IOException
        {
            header[index] = body.size() + 1;

            body.addAll(values, offset, length);
        }

        protected IIndexReader.IOne2ManyIndex createReader(IIndexReader.IOne2OneIndex headerIndex,
                        IIndexReader.IOne2OneIndex bodyIndex) throws IOException
        {
            return new IndexReader.IntIndex1NSortedReader(this.indexFile, headerIndex, bodyIndex);
        }

    }

    public static class InboundWriter
    {
        int size;
        File indexFile;

        int bitLength;
        int pageSize;
        BitOutputStream[] segments;
        int[] segmentSizes;

        /**
         * @throws IOException
         */
        public InboundWriter(int size, File indexFile) throws IOException
        {
            this.size = size;
            this.indexFile = indexFile;

            int requiredSegments = (size / 500000) + 1;

            int segments = 1;
            while (segments < requiredSegments)
                segments <<= 1;

            this.bitLength = mostSignificantBit(size) + 1;
            this.pageSize = (size / segments) + 1;
            this.segments = new BitOutputStream[segments];
            this.segmentSizes = new int[segments];
        }

        public void log(int objectIndex, int refIndex, boolean isPseudo) throws IOException
        {
            int segment = objectIndex / pageSize;
            if (segments[segment] == null)
            {
                File segmentFile = new File(this.indexFile.getAbsolutePath() + segment + ".log");//$NON-NLS-1$
                segments[segment] = new BitOutputStream(new FileOutputStream(segmentFile));
            }

            segments[segment].writeBit(isPseudo ? 1 : 0);
            segments[segment].writeInt(objectIndex, bitLength);
            segments[segment].writeInt(refIndex, bitLength);

            segmentSizes[segment]++;
        }

        public IIndexReader.IOne2ManyObjectsIndex flush(IProgressListener monitor, KeyWriter keyWriter)
                        throws IOException
        {
            close();

            int[] header = new int[size];

            DataOutputStream index = new DataOutputStream(new BufferedOutputStream(
                            new FileOutputStream(this.indexFile), 1024 * 256));

            BitInputStream segmentIn = null;

            try
            {
                IntIndexStreamer body = new IntIndexStreamer();
                body.openStream(index, 0);

                for (int segment = 0; segment < segments.length; segment++)
                {
                    if (monitor.isCanceled())
                        throw new IProgressListener.OperationCanceledException();

                    File segmentFile = new File(this.indexFile.getAbsolutePath() + segment + ".log");//$NON-NLS-1$
                    if (!segmentFile.exists())
                        continue;

                    // read & sort payload
                    segmentIn = new BitInputStream(new FileInputStream(segmentFile));

                    int objIndex[] = new int[segmentSizes[segment]];
                    int refIndex[] = new int[segmentSizes[segment]];

                    for (int ii = 0; ii < segmentSizes[segment]; ii++)
                    {

                        boolean isPseudo = segmentIn.readBit() == 1;

                        objIndex[ii] = segmentIn.readInt(bitLength);
                        refIndex[ii] = segmentIn.readInt(bitLength);

                        if (isPseudo)
                            refIndex[ii] = -1 - refIndex[ii]; // 0 is a valid!
                    }

                    segmentIn.close();
                    segmentIn = null;

                    if (monitor.isCanceled())
                        throw new IProgressListener.OperationCanceledException();

                    // delete segment log
                    segmentFile.delete();
                    segmentFile = null;

                    processSegment(monitor, keyWriter, header, body, objIndex, refIndex);
                }

                // write header
                long divider = body.closeStream();
                IIndexReader.IOne2OneIndex headerIndex = new IntIndexStreamer().writeTo(index, divider, header);

                index.writeLong(divider);

                index.flush();
                index.close();

                index = null;

                // return index reader
                return new IndexReader.InboundReader(this.indexFile, headerIndex, body.getReader(null));
            }
            finally
            {
                try
                {
                    if (index != null)
                        index.close();
                }
                catch (IOException ignore)
                {}

                try
                {
                    if (segmentIn != null)
                        segmentIn.close();
                }
                catch (IOException ignore)
                {}

                if (monitor.isCanceled())
                    cancel();
            }
        }

        private void processSegment(IProgressListener monitor, KeyWriter keyWriter, int[] header,
                        IntIndexStreamer body, int[] objIndex, int[] refIndex) throws IOException
        {
            // sort (only by objIndex though)
            ArrayUtils.sort(objIndex, refIndex);

            // write index body
            int start = 0;
            int previous = -1;

            for (int ii = 0; ii <= objIndex.length; ii++)
            {
                if (ii == 0)
                {
                    start = ii;
                    previous = objIndex[ii];
                }
                else if (ii == objIndex.length || previous != objIndex[ii])
                {
                    if (monitor.isCanceled())
                        throw new IProgressListener.OperationCanceledException();

                    header[previous] = body.size() + 1;

                    processObject(keyWriter, header, body, previous, refIndex, start, ii);

                    if (ii < objIndex.length)
                    {
                        previous = objIndex[ii];
                        start = ii;
                    }
                }
            }
        }

        private void processObject(KeyWriter keyWriter, int[] header, IntIndexStreamer body, int objectId,
                        int[] refIndex, int fromIndex, int toIndex) throws IOException
        {
            Arrays.sort(refIndex, fromIndex, toIndex);

            int endPseudo = fromIndex;

            if ((toIndex - fromIndex) > 100000)
            {
                BitField duplicates = new BitField(size);

                int jj = fromIndex;

                for (; jj < toIndex; jj++) // pseudo references
                {
                    if (refIndex[jj] >= 0)
                        break;

                    endPseudo++;
                    refIndex[jj] = -refIndex[jj] - 1;

                    if (!duplicates.get(refIndex[jj]))
                    {
                        body.add(refIndex[jj]);
                        duplicates.set(refIndex[jj]);
                    }
                }

                for (; jj < toIndex; jj++) // other references
                {
                    if ((jj == fromIndex || refIndex[jj - 1] != refIndex[jj]) && !duplicates.get(refIndex[jj]))
                    {
                        body.add(refIndex[jj]);
                    }
                }
            }
            else
            {
                SetInt duplicates = new SetInt(toIndex - fromIndex);

                int jj = fromIndex;

                for (; jj < toIndex; jj++) // pseudo references
                {
                    if (refIndex[jj] >= 0)
                        break;

                    endPseudo++;
                    refIndex[jj] = -refIndex[jj] - 1;

                    if (duplicates.add(refIndex[jj]))
                        body.add(refIndex[jj]);
                }

                for (; jj < toIndex; jj++) // other references
                {
                    if ((jj == fromIndex || refIndex[jj - 1] != refIndex[jj]) && !duplicates.contains(refIndex[jj]))
                    {
                        body.add(refIndex[jj]);
                    }
                }
            }

            if (endPseudo > fromIndex)
            {
                keyWriter.storeKey(objectId, new int[] { header[objectId] - 1, endPseudo - fromIndex });
            }
        }

        public synchronized void cancel()
        {
            try
            {
                close();

                if (segments != null)
                {
                    for (int ii = 0; ii < segments.length; ii++)
                    {
                        new File(this.indexFile.getAbsolutePath() + ii + ".log").delete();//$NON-NLS-1$
                    }
                }
            }
            catch (IOException ignore)
            {}
            finally
            {
                indexFile.delete();
            }
        }

        public synchronized void close() throws IOException
        {
            if (segments != null)
            {
                for (int ii = 0; ii < segments.length; ii++)
                {
                    if (segments[ii] != null)
                    {
                        segments[ii].flush();
                        segments[ii].close();
                        segments[ii] = null;
                    }
                }
            }
        }

        public File getIndexFile()
        {
            return indexFile;
        }

    }

    public static class IntArray1NUncompressedCollector
    {
        int[][] elements;
        File indexFile;

        /**
         * @throws IOException
         */
        public IntArray1NUncompressedCollector(int size, File indexFile) throws IOException
        {
            this.elements = new int[size][];
            this.indexFile = indexFile;
        }

        public void log(int classId, int methodId)
        {
            if (elements[classId] == null)
            {
                elements[classId] = new int[] { methodId };
            }
            else
            {
                int[] newChildren = new int[elements[classId].length + 1];
                System.arraycopy(elements[classId], 0, newChildren, 0, elements[classId].length);
                newChildren[elements[classId].length] = methodId;
                elements[classId] = newChildren;
            }
        }

        public File getIndexFile()
        {
            return indexFile;
        }

        public IIndexReader.IOne2ManyIndex flush() throws IOException
        {
            IntArray1NSortedWriter writer = new IntArray1NSortedWriter(elements.length, indexFile);
            for (int ii = 0; ii < elements.length; ii++)
            {
                if (elements[ii] != null)
                    writer.log(ii, elements[ii]);
            }
            return writer.flush();
        }

    }

    // //////////////////////////////////////////////////////////////
    // long based indices
    // //////////////////////////////////////////////////////////////

    public static class LongIndexCollectorUncompressed
    {
        long[] dataElements;

        public LongIndexCollectorUncompressed(int size)
        {
            dataElements = new long[size];
        }

        public void set(int index, long value)
        {
            dataElements[index] = value;
        }

        public long get(int index)
        {
            return dataElements[index];
        }

        public IIndexReader.IOne2LongIndex writeTo(File indexFile) throws IOException
        {
            return new LongIndexStreamer().writeTo(indexFile, dataElements);
        }
    }

    abstract static class LongIndex
    {
        private static final int DEPTH = 10;

        int pageSize;
        int size;
        // pages are either IntArrayCompressed or
        // SoftReference<IntArrayCompressed>
        HashMapIntObject<Object> pages;
        HashMapIntLong binarySearchCache = new HashMapIntLong(1 << DEPTH);

        protected LongIndex()
        {}

        protected LongIndex(int size)
        {
            init(size, PAGE_SIZE_LONG);
        }

        protected void init(int size, int pageSize)
        {
            this.size = size;
            this.pageSize = pageSize;
            this.pages = new HashMapIntObject<Object>(size / pageSize + 1);
        }

        public long get(int index)
        {
            ArrayLongCompressed array = getPage(index / pageSize);
            return array.get(index % pageSize);
        }

        public long[] getNext(int index, int length)
        {
            long answer[] = new long[length];
            int page = index / pageSize;
            int pageIndex = index % pageSize;

            ArrayLongCompressed array = getPage(page);
            for (int ii = 0; ii < length; ii++)
            {
                answer[ii] = array.get(pageIndex++);
                if (pageIndex >= pageSize)
                {
                    array = getPage(++page);
                    pageIndex = 0;
                }
            }

            return answer;
        }

        @SuppressWarnings("null")
        public int reverse(long value)
        {
            int low = 0;
            int high = size - 1;

            int depth = 0;
            int page = -1;
            ArrayLongCompressed array = null;

            while (low <= high)
            {
                int mid = (low + high) >> 1;

                long midVal;

                if (depth++ < DEPTH)
                {
                    try
                    {
                        midVal = binarySearchCache.get(mid);
                    }
                    catch (NoSuchElementException e)
                    {
                        int p = mid / pageSize;
                        if (p != page)
                            array = getPage(page = p);

                        midVal = array.get(mid % pageSize);

                        binarySearchCache.put(mid, midVal);
                    }
                }
                else
                {
                    int p = mid / pageSize;
                    if (p != page)
                        array = getPage(page = p);

                    midVal = array.get(mid % pageSize);
                }

                if (midVal < value)
                    low = mid + 1;
                else if (midVal > value)
                    high = mid - 1;
                else
                    return mid; // key found
            }
            return -(low + 1); // key not found.
        }

        public void set(int index, long value)
        {
            ArrayLongCompressed array = getPage(index / pageSize);
            array.set(index % pageSize, value);
        }

        protected abstract ArrayLongCompressed getPage(int page);

        public synchronized void unload()
        {
            pages = new HashMapIntObject<Object>(size / pageSize + 1);
            binarySearchCache = new HashMapIntLong(1 << DEPTH);
        }

        public int size()
        {
            return size;
        }

        public IteratorLong iterator()
        {
            return new LongIndexIterator(this);
        }
    }

    static class LongIndexIterator implements IteratorLong
    {
        LongIndex longArray;
        int nextIndex = 0;

        public LongIndexIterator(LongIndex longArray)
        {
            this.longArray = longArray;
        }

        public long next()
        {
            return longArray.get(nextIndex++);
        }

        public boolean hasNext()
        {
            return nextIndex < longArray.size();
        }
    }

    public static class LongIndexCollector extends LongIndex
    {
        int mostSignificantBit;

        public LongIndexCollector(int size, int mostSignificantBit)
        {
            super(size);
            this.mostSignificantBit = mostSignificantBit;
        }

        @Override
        protected ArrayLongCompressed getPage(int page)
        {
            ArrayLongCompressed array = (ArrayLongCompressed) pages.get(page);
            if (array == null)
            {
                int ps = page < (size / pageSize) ? pageSize : size % pageSize;
                array = new ArrayLongCompressed(ps, 63 - mostSignificantBit, 0);
                pages.put(page, array);
            }
            return array;
        }

        public IIndexReader.IOne2LongIndex writeTo(File indexFile) throws IOException
        {
            // needed to re-compress
            return new LongIndexStreamer().writeTo(indexFile, this.size, this.pages, this.pageSize);
        }
    }

    public static class LongIndexStreamer extends LongIndex
    {
        DataOutputStream out;
        ArrayLong pageStart;
        long[] page;
        int left;

        public LongIndexStreamer()
        {}

        public LongIndexStreamer(File indexFile) throws IOException
        {
            DataOutputStream out = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(indexFile)));
            openStream(out, 0);
        }

        public void close() throws IOException
        {
            DataOutputStream out = this.out;
            closeStream();
            out.close();
        }

        public IOne2LongIndex writeTo(File indexFile, int size, HashMapIntObject<Object> pages, int pageSize)
                        throws IOException
        {
            DataOutputStream out = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(indexFile)));

            openStream(out, 0);

            int noOfPages = size / pageSize + (size % pageSize > 0 ? 1 : 0);
            for (int ii = 0; ii < noOfPages; ii++)
            {
                ArrayLongCompressed a = (ArrayLongCompressed) pages.get(ii);
                int len = (ii + 1) < noOfPages ? pageSize : (size % pageSize);

                if (a == null)
                    addAll(new long[len]);
                else
                    for (int jj = 0; jj < len; jj++)
                    {
                        add(a.get(jj));
                    }
            }

            closeStream();
            out.close();

            return getReader(indexFile);
        }

        public IOne2LongIndex writeTo(File indexFile, long[] array) throws IOException
        {
            DataOutputStream out = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(indexFile)));

            openStream(out, 0);
            addAll(array);
            closeStream();

            out.close();

            return getReader(indexFile);
        }

        public IOne2LongIndex writeTo(File indexFile, IteratorLong iterator) throws IOException
        {
            FileOutputStream fos = new FileOutputStream(indexFile);
            try
            {
                DataOutputStream out = new DataOutputStream(new BufferedOutputStream(fos));

                openStream(out, 0);
                addAll(iterator);
                closeStream();

                out.flush();

                return getReader(indexFile);
            }
            finally
            {
                try
                {
                    fos.close();
                }
                catch (IOException ignore)
                {}
            }
        }

        public IOne2LongIndex writeTo(File indexFile, ArrayLong array) throws IOException
        {
            DataOutputStream out = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(indexFile)));

            openStream(out, 0);
            addAll(array);
            closeStream();

            out.close();

            return getReader(indexFile);
        }

        void openStream(DataOutputStream out, long position)
        {
            this.out = out;

            init(0, PAGE_SIZE_LONG);

            this.page = new long[pageSize];
            this.pageStart = new ArrayLong();
            this.pageStart.add(position);
            this.left = page.length;
        }

        /**
         * @return total bytes written to index file
         */
        long closeStream() throws IOException
        {
            if (left < page.length)
                addPage();

            // write header information
            for (int jj = 0; jj < pageStart.size(); jj++)
                out.writeLong(pageStart.get(jj));

            out.writeInt(pageSize);
            out.writeInt(size);

            this.page = null;

            this.out = null;

            return this.pageStart.lastElement() + (8 * pageStart.size()) + 8 - this.pageStart.firstElement();
        }

        IndexReader.LongIndexReader getReader(File indexFile) throws IOException
        {
            return new IndexReader.LongIndexReader(indexFile, pages, size, pageSize, pageStart.toArray());
        }

        public void addAll(IteratorLong iterator) throws IOException
        {
            while (iterator.hasNext())
                add(iterator.next());
        }

        public void addAll(ArrayLong array) throws IOException
        {
            for (IteratorLong e = array.iterator(); e.hasNext();)
                add(e.next());
        }

        public void add(long value) throws IOException
        {
            if (left == 0)
                addPage();

            page[page.length - left--] = value;
            size++;

        }

        public void addAll(long[] values) throws IOException
        {
            addAll(values, 0, values.length);
        }

        public void addAll(long[] values, int offset, int length) throws IOException
        {
            while (length > 0)
            {
                if (left == 0)
                    addPage();

                int chunk = Math.min(left, length);

                System.arraycopy(values, offset, page, page.length - left, chunk);
                left -= chunk;
                size += chunk;

                length -= chunk;
                offset += chunk;
            }
        }

        private void addPage() throws IOException
        {
            ArrayLongCompressed array = new ArrayLongCompressed(page, 0, page.length - left);

            byte[] buffer = array.toByteArray();
            out.write(buffer);
            int written = buffer.length;

            pages.put(pages.size(), new SoftReference<ArrayLongCompressed>(array));
            pageStart.add(pageStart.lastElement() + written);

            left = page.length;
        }

        @Override
        protected ArrayLongCompressed getPage(int page)
        {
            throw new UnsupportedOperationException();
        }

    }

    public static class LongArray1NWriter
    {
        int[] header;
        File indexFile;

        DataOutputStream out;
        LongIndexStreamer body;

        public LongArray1NWriter(int size, File indexFile) throws IOException
        {
            this.header = new int[size];
            this.indexFile = indexFile;

            this.out = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(indexFile)));
            this.body = new LongIndexStreamer();
            this.body.openStream(this.out, 0);
        }

        public void log(int index, long[] values) throws IOException
        {
            this.set(index, values, 0, values.length);
        }

        protected void set(int index, long[] values, int offset, int length) throws IOException
        {
            header[index] = body.size() + 1;

            body.add(length);

            body.addAll(values, offset, length);
        }

        public void flush() throws IOException
        {
            long divider = body.closeStream();

            new IntIndexStreamer().writeTo(out, divider, header).close();

            out.writeLong(divider);

            out.close();
            out = null;
        }

        public void cancel()
        {
            try
            {
                if (out != null)
                {
                    out.close();
                    body = null;
                    out = null;
                }
            }
            catch (IOException ignore)
            {}
            finally
            {
                if (indexFile.exists())
                    indexFile.delete();
            }
        }

        public File getIndexFile()
        {
            return indexFile;
        }
    }

    // //////////////////////////////////////////////////////////////
    // mama's little helpers
    // //////////////////////////////////////////////////////////////

    public static long[] copyOf(long[] original, int newLength)
    {
        long[] copy = new long[newLength];
        System.arraycopy(original, 0, copy, 0, Math.min(original.length, newLength));
        return copy;
    }

    public static int mostSignificantBit(int x)
    {
        int length = 0;
        if ((x & 0xffff0000) != 0)
        {
            length += 16;
            x >>= 16;
        }
        if ((x & 0xff00) != 0)
        {
            length += 8;
            x >>= 8;
        }
        if ((x & 0xf0) != 0)
        {
            length += 4;
            x >>= 4;
        }
        if ((x & 0xc) != 0)
        {
            length += 2;
            x >>= 2;
        }
        if ((x & 0x2) != 0)
        {
            length += 1;
            x >>= 1;
        }
        if ((x & 0x1) != 0)
        {
            length += 1;
            // x >>= 1;
        }

        return length - 1;
    }

    public static int mostSignificantBit(long x)
    {
        long lead = x >>> 32;
        return lead == 0x0 ? mostSignificantBit((int) x) : 32 + mostSignificantBit((int) lead);
    }

}
