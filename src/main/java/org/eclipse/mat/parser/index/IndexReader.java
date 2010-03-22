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

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.io.Serializable;
import java.lang.ref.SoftReference;

import org.eclipse.mat.SnapshotException;
import org.eclipse.mat.collect.ArrayIntCompressed;
import org.eclipse.mat.collect.ArrayLongCompressed;
import org.eclipse.mat.collect.HashMapIntObject;
import org.eclipse.mat.parser.internal.Messages;
import org.eclipse.mat.parser.io.SimpleBufferedRandomAccessInputStream;

public abstract class IndexReader
{
    public static final boolean DEBUG = false;

    public static class IntIndexReader extends IndexWriter.IntIndex<SoftReference<ArrayIntCompressed>> implements
                    IIndexReader.IOne2OneIndex
    {
        public Object LOCK = new Object();

        File indexFile;
        public SimpleBufferedRandomAccessInputStream in;
        long[] pageStart;

        public IntIndexReader(File indexFile, IndexWriter.Pages<SoftReference<ArrayIntCompressed>> pages, int size,
                        int pageSize, long[] pageStart)
        {
            this.size = size;
            this.pageSize = pageSize;
            this.pages = pages;

            this.indexFile = indexFile;
            this.pageStart = pageStart;

            if (indexFile != null)
                open();
        }

        public IntIndexReader(File indexFile) throws IOException
        {
            this(new SimpleBufferedRandomAccessInputStream(new RandomAccessFile(indexFile, "r")), 0, indexFile.length());//$NON-NLS-1$
            this.indexFile = indexFile;
        }

        public IntIndexReader(SimpleBufferedRandomAccessInputStream in, long start, long length) throws IOException
        {
            this.in = in;
            this.in.seek(start + length - 8);

            int pageSize = this.in.readInt();
            int size = this.in.readInt();

            init(size, pageSize);

            int pages = (size / pageSize) + (size % pageSize > 0 ? 2 : 1);

            pageStart = new long[pages];

            this.in.seek(start + length - 8 - (pageStart.length * 8));
            this.in.readLongArray(pageStart);
        }

        private synchronized void open()
        {
            try
            {
                if (in != null)
                    return;

                if (indexFile == null)
                    throw new IOException(Messages.IndexReader_Error_IndexIsEmbedded);

                in = new SimpleBufferedRandomAccessInputStream(new RandomAccessFile(this.indexFile, "r"));//$NON-NLS-1$
            }
            catch (IOException e)
            {
                throw new RuntimeException(e);
            }
        }

        public synchronized void close()
        {
            unload();

            if (in != null)
            {
                try
                {
                    in.close();
                }
                catch (IOException ignore)
                {
                    // $JL-EXC$
                }
                finally
                {
                    in = null;
                }
            }
        }

        @Override
        protected ArrayIntCompressed getPage(int page)
        {
            SoftReference<ArrayIntCompressed> ref = pages.get(page);
            ArrayIntCompressed array = ref == null ? null : ref.get();
            if (array == null)
            {
                synchronized (LOCK)
                {
                    ref = pages.get(page);
                    array = ref == null ? null : ref.get();

                    if (array == null)
                    {
                        try
                        {
                            byte[] buffer = null;

                            this.in.seek(pageStart[page]);

                            buffer = new byte[(int) (pageStart[page + 1] - pageStart[page])];
                            if (this.in.read(buffer) != buffer.length)
                                throw new IOException();

                            array = new ArrayIntCompressed(buffer);

                            synchronized (pages)
                            {
                                pages.put(page, new SoftReference<ArrayIntCompressed>(array));
                            }
                        }
                        catch (IOException e)
                        {
                            throw new RuntimeException(e);
                        }
                    }
                }
            }
            return array;
        }

        public void delete()
        {
            close();

            if (indexFile != null)
                indexFile.delete();
        }

    }

    /* package */static class IntIndex1NReader implements IIndexReader.IOne2ManyIndex
    {
        File indexFile;
        SimpleBufferedRandomAccessInputStream in;
        IntIndexReader header;
        IntIndexReader body;

        public IntIndex1NReader(File indexFile) throws IOException
        {
            try
            {
                this.indexFile = indexFile;

                open();

                long indexLength = indexFile.length();
                in.seek(indexLength - 8);
                long divider = in.readLong();

                this.header = new IntIndexReader(in, divider, indexLength - divider - 8);
                this.body = new IntIndexReader(in, 0, divider);

                this.body.LOCK = this.header.LOCK;

            }
            catch (RuntimeException e)
            {
                close();
                throw e;
            }
        }

        public IntIndex1NReader(File indexFile, IIndexReader.IOne2OneIndex header, IIndexReader.IOne2OneIndex body)
        {
            this.indexFile = indexFile;
            this.header = ((IntIndexReader) header);
            this.body = ((IntIndexReader) body);

            this.body.LOCK = this.header.LOCK;

            open();
        }

        public int[] get(int index)
        {
            int p = header.get(index);

            int length = body.get(p);

            return body.getNext(p + 1, length);
        }

        protected synchronized void open()
        {
            try
            {
                if (in == null)
                {

                    in = new SimpleBufferedRandomAccessInputStream(new RandomAccessFile(this.indexFile, "r"));//$NON-NLS-1$

                    if (this.header != null)
                        this.header.in = in;

                    if (this.body != null)
                        this.body.in = in;
                }
            }
            catch (IOException e)
            {
                throw new RuntimeException(e);
            }
        }

        public synchronized void close()
        {
            header.unload();
            body.unload();

            if (in != null)
            {
                try
                {
                    in.close();
                }
                catch (IOException ignore)
                {
                    // $JL-EXC$
                }
                finally
                {
                    in = null;
                    if (this.header != null)
                        this.header.in = null;
                    if (this.body != null)
                        this.body.in = null;
                }
            }
        }

        public void unload() throws IOException
        {
            header.unload();
            body.unload();
        }

        public int size()
        {
            return header.size();
        }

        public void delete()
        {
            close();

            if (indexFile != null)
                indexFile.delete();
        }
    }

    public static class IntIndex1NSortedReader extends IntIndex1NReader
    {
        public IntIndex1NSortedReader(File indexFile) throws IOException
        {
            super(indexFile);
        }

        /**
         * @throws IOException
         */
        public IntIndex1NSortedReader(File indexFile, IOne2OneIndex header, IOne2OneIndex body) throws IOException
        {
            super(indexFile, header, body);
        }

        public int[] get(int index)
        {
            int p[] = null;

            if (index + 1 < header.size())
            {
                p = header.getNext(index++, 2);
                if (p[0] == 0)
                    return new int[0];

                for (index++; p[1] < p[0] && index < header.size(); index++)
                    p[1] = header.get(index);

                if (p[1] < p[0])
                    p[1] = body.size() + 1;
            }
            else
            {
                p = new int[] { header.get(index), 0 };
                if (p[0] == 0)
                    return new int[0];
                p[1] = body.size() + 1;
            }

            return body.getNext(p[0] - 1, p[1] - p[0]);
        }

    }

    static class InboundReader extends IntIndex1NSortedReader implements IIndexReader.IOne2ManyObjectsIndex
    {
        public InboundReader(File indexFile) throws IOException
        {
            super(indexFile);
        }

        public InboundReader(File indexFile, IOne2OneIndex header, IOne2OneIndex body) throws IOException
        {
            super(indexFile, header, body);
        }

        public int[] getObjectsOf(Serializable key) throws SnapshotException, IOException
        {
            if (key == null)
                return new int[0];

            int[] pos = (int[]) key;

            synchronized (this)
            {
                return body.getNext(pos[0], pos[1]);
            }
        }

    }

    public static class LongIndexReader extends IndexWriter.LongIndex implements IIndexReader.IOne2LongIndex
    {
        Object LOCK = new Object();

        File indexFile;
        SimpleBufferedRandomAccessInputStream in;
        long[] pageStart;

        public LongIndexReader(File indexFile, HashMapIntObject<Object> pages, int size, int pageSize, long[] pageStart)
                        throws IOException
        {
            this.size = size;
            this.pageSize = pageSize;
            this.pages = pages;

            this.indexFile = indexFile;
            this.pageStart = pageStart;

            open();
        }

        public LongIndexReader(File indexFile) throws IOException
        {
            this(new SimpleBufferedRandomAccessInputStream(new RandomAccessFile(indexFile, "r")), 0, indexFile.length());//$NON-NLS-1$
            this.indexFile = indexFile;

            open();
        }

        protected LongIndexReader(SimpleBufferedRandomAccessInputStream in, long start, long length) throws IOException
        {
            this.in = in;
            this.in.seek(start + length - 8);

            int pageSize = this.in.readInt();
            int size = this.in.readInt();

            init(size, pageSize);

            int pages = (size / pageSize) + (size % pageSize > 0 ? 2 : 1);

            pageStart = new long[pages];

            this.in.seek(start + length - 8 - (pageStart.length * 8));
            this.in.readLongArray(pageStart);
        }

        private synchronized void open() throws IOException
        {
            if (in != null)
                return;

            if (indexFile == null)
                throw new IOException(Messages.IndexReader_Error_IndexIsEmbedded);

            in = new SimpleBufferedRandomAccessInputStream(new RandomAccessFile(this.indexFile, "r"));//$NON-NLS-1$
        }

        public synchronized void close()
        {
            unload();

            if (in != null)
            {
                try
                {
                    in.close();
                }
                catch (IOException ignore)
                {
                    // $JL-EXC$
                }
                finally
                {
                    in = null;
                }
            }
        }

        @SuppressWarnings("unchecked")
        @Override
        protected ArrayLongCompressed getPage(int page)
        {
            SoftReference<ArrayLongCompressed> ref = (SoftReference<ArrayLongCompressed>) pages.get(page);
            ArrayLongCompressed array = ref == null ? null : ref.get();
            if (array == null)
            {
                synchronized (LOCK)
                {
                    ref = (SoftReference<ArrayLongCompressed>) pages.get(page);
                    array = ref == null ? null : ref.get();

                    if (array == null)
                    {
                        try
                        {
                            byte[] buffer = null;

                            this.in.seek(pageStart[page]);

                            buffer = new byte[(int) (pageStart[page + 1] - pageStart[page])];
                            if (this.in.read(buffer) != buffer.length)
                                throw new IOException();

                            array = new ArrayLongCompressed(buffer);

                            synchronized (pages)
                            {
                                pages.put(page, new SoftReference<ArrayLongCompressed>(array));
                            }
                        }
                        catch (IOException e)
                        {
                            throw new RuntimeException(e);
                        }
                    }
                }
            }
            return array;
        }

        public void delete()
        {
            close();

            if (indexFile != null)
                indexFile.delete();
        }

    }

    public static class LongIndex1NReader implements IIndexReader
    {
        File indexFile;
        SimpleBufferedRandomAccessInputStream in;
        IntIndexReader header;
        LongIndexReader body;

        public LongIndex1NReader(File indexFile) throws IOException
        {
            this.indexFile = indexFile;

            open();

            long indexLength = indexFile.length();
            in.seek(indexLength - 8);
            long divider = in.readLong();

            this.header = new IntIndexReader(in, divider, indexLength - divider - 8);
            this.body = new LongIndexReader(in, 0, divider);

            this.body.LOCK = this.header.LOCK;
        }

        public long[] get(int index)
        {
            int p = header.get(index);

            if (p == 0)
                return new long[0];

            int length = (int) body.get(p - 1);

            return body.getNext(p, length);
        }

        protected synchronized void open()
        {
            try
            {
                if (in == null)
                {

                    in = new SimpleBufferedRandomAccessInputStream(new RandomAccessFile(this.indexFile, "r"));//$NON-NLS-1$

                    if (this.header != null)
                        this.header.in = in;

                    if (this.body != null)
                        this.body.in = in;
                }
            }
            catch (IOException e)
            {
                throw new RuntimeException(e);
            }
        }

        public synchronized void close()
        {
            unload();

            if (in != null)
            {
                try
                {
                    in.close();
                }
                catch (IOException ignore)
                {
                    // $JL-EXC$
                }
                finally
                {
                    in = this.header.in = this.body.in = null;
                }
            }
        }

        public void unload()
        {
            header.unload();
            body.unload();
        }

        public int size()
        {
            return header.size();
        }

        public void delete()
        {
            close();

            if (indexFile != null)
                indexFile.delete();
        }
    }
}
