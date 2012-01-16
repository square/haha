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
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.text.MessageFormat;

public class IndexManager
{
    public enum Index
    {
        INBOUND("inbound", IndexReader.InboundReader.class), //$NON-NLS-1$
        OUTBOUND("outbound", IndexReader.IntIndex1NSortedReader.class), //$NON-NLS-1$
        O2CLASS("o2c", IndexReader.IntIndexReader.class), //$NON-NLS-1$
        IDENTIFIER("idx", IndexReader.LongIndexReader.class), //$NON-NLS-1$
        A2SIZE("a2s", IndexReader.IntIndexReader.class), //$NON-NLS-1$
        DOMINATED("domOut", IndexReader.IntIndex1NReader.class), //$NON-NLS-1$
        O2RETAINED("o2ret", IndexReader.LongIndexReader.class), //$NON-NLS-1$
        DOMINATOR("domIn", IndexReader.IntIndexReader.class);//$NON-NLS-1$

        public String filename;
        Class<? extends IIndexReader> impl;

        private Index(String filename, Class<? extends IIndexReader> impl)
        {
            this.filename = filename;
            this.impl = impl;
        }

        public File getFile(String prefix)
        {
            return new File(new StringBuilder(prefix).append(filename).append(".index").toString());//$NON-NLS-1$
        }

    }

    public IIndexReader.IOne2ManyObjectsIndex inbound;
    public IIndexReader.IOne2ManyIndex outbound;
    public IIndexReader.IOne2OneIndex o2c;
    public IIndexReader.IOne2LongIndex idx;
    public IIndexReader.IOne2OneIndex a2s;
    public IIndexReader.IOne2ManyIndex domOut;
    public IIndexReader.IOne2LongIndex o2ret;
    public IIndexReader.IOne2OneIndex domIn;

    public void setReader(final Index index, final IIndexReader reader)
    {
        try
        {
            this.getClass().getField(index.filename).set(this, reader);
        }
        catch (RuntimeException e)
        {
            throw new RuntimeException(e);
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }

    public IIndexReader getReader(final Index index)
    {
        try
        {
            return (IIndexReader) this.getClass().getField(index.filename).get(this);
        }
        catch (RuntimeException e)
        {
            throw new RuntimeException(e);
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }

    public void init(final String prefix) throws IOException
    {
        new Visitor()
        {

            @Override
            void visit(Index index, IIndexReader reader) throws IOException
            {
                if (reader != null)
                    return;

                try
                {
                    File indexFile = index.getFile(prefix);
                    if (indexFile.exists())
                    {
                        Constructor<?> constructor = index.impl.getConstructor(new Class[] { File.class });
                        reader = (IIndexReader) constructor.newInstance(new Object[] { indexFile });
                        setReader(index, reader);
                    }
                }
                catch (NoSuchMethodException e)
                {
                    throw new RuntimeException(e);
                }
                catch (InstantiationException e)
                {
                    throw new RuntimeException(e);
                }
                catch (IllegalAccessException e)
                {
                    throw new RuntimeException(e);
                }
                catch (InvocationTargetException e)
                {
                    Throwable cause = e.getCause();
                    String msg = MessageFormat.format("{0}: {1}", //$NON-NLS-1$
                            cause.getClass().getName(),
                            cause.getMessage());
                    throw new IOException(msg, cause);
                }
                catch (RuntimeException e)
                {
                    // re-wrap runtime exceptions caught during index processing
                    // into IOExceptions -> trigger reparsing of hprof dump
                    throw new IOException(e);
                }
            }

        }.doIt();
    }

    public IIndexReader.IOne2ManyIndex inbound()
    {
        return inbound;
    }

    public IIndexReader.IOne2ManyIndex outbound()
    {
        return outbound;
    }

    public IIndexReader.IOne2OneIndex o2class()
    {
        return o2c;
    }

    public IIndexReader.IOne2ManyObjectsIndex c2objects()
    {
        return inbound;
    }

    public IIndexReader.IOne2LongIndex o2address()
    {
        return idx;
    }

    public IIndexReader.IOne2OneIndex a2size()
    {
        return a2s;
    }

    public IIndexReader.IOne2ManyIndex dominated()
    {
        return domOut;
    }

    public IIndexReader.IOne2LongIndex o2retained()
    {
        return o2ret;
    }

    public IIndexReader.IOne2OneIndex dominator()
    {
        return domIn;
    }

    public void close() throws IOException
    {
        new Visitor()
        {

            @Override
            void visit(Index index, IIndexReader reader) throws IOException
            {
                if (reader == null)
                    return;

                reader.close();
                setReader(index, null);
            }

        }.doIt();
    }

    public void delete() throws IOException
    {
        new Visitor()
        {

            @Override
            void visit(Index index, IIndexReader reader) throws IOException
            {
                if (reader == null)
                    return;

                reader.close();
                reader.delete();
                setReader(index, null);
            }

        }.doIt();
    }

    private abstract class Visitor
    {
        abstract void visit(Index index, IIndexReader reader) throws IOException;

        void doIt() throws IOException
        {
            try
            {
                for (Index index : Index.values())
                {
                    IIndexReader reader = getReader(index);
                    visit(index, reader);
                }
            }
            catch (IOException e)
            {
                throw e;
            }
            catch (Exception e)
            {
                throw new RuntimeException(e);
            }
        }
    }
}
