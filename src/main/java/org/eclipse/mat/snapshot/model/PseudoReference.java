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
package org.eclipse.mat.snapshot.model;

import org.eclipse.mat.snapshot.ISnapshot;

/**
 * Value of a field if it is a pseudo references. Some references do not
 * actually exist in the heap dump but are automatically generated because they
 * are maintained by the JVM. Examples are the link from an instance to the
 * class and from the class to the class loader.
 */
public class PseudoReference extends NamedReference
{
    private static final long serialVersionUID = 1L;

    public PseudoReference(ISnapshot snapshot, long address, String name)
    {
        super(snapshot, address, name);
    }
}
