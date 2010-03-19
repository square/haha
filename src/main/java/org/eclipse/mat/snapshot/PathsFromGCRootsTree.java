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
package org.eclipse.mat.snapshot;

import java.util.HashMap;

/**
 * This class holds the paths from one single object to its GC roots.
 * 
 * @noinstantiate
 */
public final class PathsFromGCRootsTree
{
    private int ownId;
    private int[] objectIds;
    private HashMap<Integer, PathsFromGCRootsTree> objectInboundReferers;

    public PathsFromGCRootsTree(int ownId, HashMap<Integer, PathsFromGCRootsTree> objectInboundReferers, int[] objectIds)
    {
        this.ownId = ownId;
        this.objectInboundReferers = objectInboundReferers;
        this.objectIds = objectIds;
    }

    /**
     * Get object being the root for this tree.
     * 
     * @return object being the root for this tree
     */
    public int getOwnId()
    {
        return ownId;
    }

    /**
     * Get referencing objects.
     * 
     * @return referencing objects
     */
    public int[] getObjectIds()
    {
        return objectIds;
    }

    /**
     * Get sub tree for a referencing object.
     * 
     * @param objId
     *            referencing object from which on the sub tree is requested
     * @return sub tree for a referencing object
     */
    public PathsFromGCRootsTree getBranch(int objId)
    {
        return objectInboundReferers.get(objId);
    }
}
