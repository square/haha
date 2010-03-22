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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.mat.SnapshotException;
import org.eclipse.mat.collect.ArrayInt;
import org.eclipse.mat.collect.HashMapIntObject;
import org.eclipse.mat.snapshot.ISnapshot;
import org.eclipse.mat.snapshot.model.IThreadStack;

/* package */class ThreadStackHelper
{

    /* package */static HashMapIntObject<IThreadStack> loadThreadsData(ISnapshot snapshot) throws SnapshotException
    {
        String fileName = snapshot.getSnapshotInfo().getPrefix() + "threads"; //$NON-NLS-1$
        File f = new File(fileName);
        if (!f.exists())
            return null;

        HashMapIntObject<IThreadStack> threadId2stack = new HashMapIntObject<IThreadStack>();

        BufferedReader in = null;
        try
        {
            in = new BufferedReader(new FileReader(f));
            String line = in.readLine();

            while (line != null)
            {
                line = line.trim();
                if (line.startsWith("Thread")) //$NON-NLS-1$
                {
                    long threadAddress = readThreadAddres(line);
                    List<String> lines = new ArrayList<String>();
                    HashMapIntObject<ArrayInt> line2locals = new HashMapIntObject<ArrayInt>();

                    line = in.readLine();
                    while (line != null && !line.equals("")) //$NON-NLS-1$
                    {
                        lines.add(line.trim());
                        line = in.readLine();
                    }

                    line = in.readLine();
                    if (line != null && line.trim().startsWith("locals")) //$NON-NLS-1$
                    {
                        line = in.readLine();
                        while (line != null && !line.equals("")) //$NON-NLS-1$
                        {
                            int lineNr = readLineNumber(line);
                            if (lineNr >= 0)
                            {
                                int objectId = readLocalId(line, snapshot);
                                ArrayInt arr = line2locals.get(lineNr);
                                if (arr == null)
                                {
                                    arr = new ArrayInt();
                                    line2locals.put(lineNr, arr);
                                }
                                arr.add(objectId);
                            }
                            line = in.readLine();
                        }
                    }

                    if (threadAddress != -1)
                    {
                        int threadId = snapshot.mapAddressToId(threadAddress);
                        IThreadStack stack = new ThreadStackImpl(threadId, buildFrames(lines, line2locals));
                        threadId2stack.put(threadId, stack);
                    }
                }

                if (line != null)
                    line = in.readLine();
                else
                    break;
            }
        }
        catch (IOException e)
        {
            throw new SnapshotException(e);
        }
        finally
        {
            if (in != null)
            {
                try
                {
                    in.close();
                }
                catch (Exception ignore)
                {
                    // $JL-EXC$
                }
            }
        }

        return threadId2stack;

    }

    private static long readThreadAddres(String line)
    {
        int start = line.indexOf("0x"); //$NON-NLS-1$
        if (start < 0)
            return -1;
        return (new BigInteger(line.substring(start + 2), 16)).longValue();
    }

    private static int readLocalId(String line, ISnapshot snapshot) throws SnapshotException
    {
        int start = line.indexOf("0x"); //$NON-NLS-1$
        int end = line.indexOf(',', start);
        long address = (new BigInteger(line.substring(start + 2, end), 16)).longValue();
        return snapshot.mapAddressToId(address);
    }

    private static int readLineNumber(String line)
    {
        int start = line.indexOf("line="); //$NON-NLS-1$
        return Integer.valueOf(line.substring(start + 5));
    }

    private static StackFrameImpl[] buildFrames(List<String> lines, HashMapIntObject<ArrayInt> line2locals)
    {
        int sz = lines.size();

        StackFrameImpl[] frames = new StackFrameImpl[sz];
        for (int i = 0; i < sz; i++)
        {
            int[] localsIds = null;
            ArrayInt locals = line2locals.get(i);
            if (locals != null && locals.size() > 0)
            {
                localsIds = locals.toArray();
            }
            frames[i] = new StackFrameImpl(lines.get(i), localsIds);
        }

        return frames;

    }

}
