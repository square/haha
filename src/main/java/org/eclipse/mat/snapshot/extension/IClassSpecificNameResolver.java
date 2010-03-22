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
package org.eclipse.mat.snapshot.extension;

import org.eclipse.mat.SnapshotException;
import org.eclipse.mat.snapshot.model.IObject;

/**
 * Interface describing a name resolver for objects of specific classes (found
 * in an snapshot), e.g. String (where the char[] is evaluated) or as specific
 * class loader (where the appropriate field holding its name and thereby
 * deployment unit is evaluated). Objects of this interface need to be
 * registered with the <code>nameResolver</code> extension point.
 */
public interface IClassSpecificNameResolver
{
    /**
     * Resolve the name for snapshot object.
     * 
     * @param object
     *            object for which the name should be resolved
     * @return name for snapshot object
     * @throws SnapshotException
     */
    public String resolve(IObject object) throws SnapshotException;
}
