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
package org.eclipse.mat.parser.model;

import org.eclipse.mat.snapshot.model.GCRootInfo;

public final class XGCRootInfo extends GCRootInfo
{
    private static final long serialVersionUID = 1L;

    public XGCRootInfo(long objectAddress, long contextAddress, int type)
    {
        super(objectAddress, contextAddress, type);
    }

    public void setObjectId(int objectId)
    {
        this.objectId = objectId;
    }

    public void setContextId(int objectId)
    {
        this.contextId = objectId;
    }
}
