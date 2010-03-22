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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.mat.snapshot.PathsFromGCRootsTree;

public class PathsFromGCRootsTreeBuilder
{
    private int ownId;
    private ArrayList<Integer> objectIds = new ArrayList<Integer>();
    private HashMap<Integer, PathsFromGCRootsTreeBuilder> objectInboundReferers;

    public PathsFromGCRootsTreeBuilder(int ownId)
    {
        this.ownId = ownId;
        objectInboundReferers = new HashMap<Integer, PathsFromGCRootsTreeBuilder>();
    }

    public HashMap<Integer, PathsFromGCRootsTreeBuilder> getObjectReferers()
    {
        return objectInboundReferers;
    }

    public PathsFromGCRootsTree toPathsFromGCRootsTree()
    {
        HashMap<Integer, PathsFromGCRootsTree> data = new HashMap<Integer, PathsFromGCRootsTree>(objectInboundReferers
                        .size());
        for (Map.Entry<Integer, PathsFromGCRootsTreeBuilder> entry : objectInboundReferers.entrySet())
        {
            data.put(entry.getKey(), entry.getValue().toPathsFromGCRootsTree());
        }
        int[] children = new int[objectIds.size()];
        for (int i = 0; i < children.length; i++)
        {
            children[i] = objectIds.get(i);
        }
        return new PathsFromGCRootsTree(ownId, data, children);
    }

    public int getOwnId()
    {
        return ownId;
    }

    public void addObjectReferer(PathsFromGCRootsTreeBuilder referer)
    {
        if (objectInboundReferers.put(referer.getOwnId(), referer) == null)
        {
            objectIds.add(referer.getOwnId());
        }
    }
}
