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

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.NoSuchElementException;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.mat.collect.HashMapIntLong;
import org.eclipse.mat.parser.internal.Messages;
import org.eclipse.mat.parser.model.XSnapshotInfo;

public class RetainedSizeCache
{
    private String filename;
    private HashMapIntLong id2size;
    private boolean isDirty = false;

    public RetainedSizeCache(XSnapshotInfo snapshotInfo)
    {
        this.filename = snapshotInfo.getPrefix() + "i2sv2.index"; //$NON-NLS-1$
        readId2Size(snapshotInfo.getPrefix());
    }

    public long get(int key)
    {
        try
        {
            return id2size.get(key);
        }
        catch (NoSuchElementException e)
        {
            // $JL-EXC$
            return 0;
        }
    }

    public void put(int key, long value)
    {
        id2size.put(key, value);
        isDirty = true;
    }

    public void close()
    {
        if (!isDirty)
            return;

        try
        {
            File file = new File(filename);

            DataOutputStream out = new DataOutputStream(new FileOutputStream(file));

            for (int key : id2size.getAllKeys())
            {
                out.writeInt(key);
                out.writeLong(id2size.get(key));
            }

            out.close();

            isDirty = false;
        }
        catch (IOException e)
        {
            Logger.getLogger(RetainedSizeCache.class.getName()).log(Level.WARNING,
                            Messages.RetainedSizeCache_Warning_IgnoreError, e);
        }
    }

    private void doRead(File file, boolean readOldFormat)
    {
        DataInputStream in = null;
        boolean delete = false;

        try
        {
            id2size = new HashMapIntLong((int) file.length() / 8);

            in = new DataInputStream(new BufferedInputStream(new FileInputStream(file)));

            while (in.available() > 0)
            {
                int key = in.readInt();
                long value = in.readLong();
                if (value < 0 && readOldFormat)
                    value = -(value - (Long.MIN_VALUE + 1));
                id2size.put(key, value);
            }
        }
        catch (IOException e)
        {
            Logger.getLogger(RetainedSizeCache.class.getName()).log(Level.WARNING,
                            Messages.RetainedSizeCache_ErrorReadingRetainedSizes, e);

            // might have read corrupt data
            id2size.clear();
            delete = true;
        }
        finally
        {
            try
            {
                if (in != null)
                {
                    in.close();
                }
            }
            catch (IOException ignore)
            {
                // $JL-EXC$
            }
            try
            {
                if (delete)
                {
                    file.delete();
                }
            }
            catch (RuntimeException ignore)
            {
                // $JL-EXC$
            }
        }
    }

    private void readId2Size(String prefix)
    {
        File file = new File(filename);
        if (file.exists())
        {
            doRead(file, false);
        }
        else
        {
            File legacyFile = new File(prefix + "i2s.index");//$NON-NLS-1$
            if (legacyFile.exists())
            {
                doRead(legacyFile, true);
            }
            else
            {
                id2size = new HashMapIntLong();
            }
        }
    }

}
