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

import java.io.IOException;
import java.io.Serializable;

import org.eclipse.mat.SnapshotException;

public interface IIndexReader
{
    public interface IOne2OneIndex extends IIndexReader
    {
        int get(int index);

        int[] getAll(int index[]);

        int[] getNext(int index, int length);
    }

    public interface IOne2LongIndex extends IIndexReader
    {
        long get(int index);

        int reverse(long value);

        long[] getNext(int index, int length);
    }

    public interface IOne2ManyIndex extends IIndexReader
    {
        int[] get(int index);
    }

    public interface IOne2ManyObjectsIndex extends IOne2ManyIndex
    {
        int[] getObjectsOf(Serializable key) throws SnapshotException, IOException;
    }

    int size();

    void unload() throws IOException;

    void close() throws IOException;

    void delete();
}
