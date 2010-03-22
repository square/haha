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

import org.eclipse.mat.snapshot.model.IStackFrame;

/**
 * 
 * @noextend This class is not intended to be subclassed by clients. May still
 *           be subject of change
 * 
 */
class StackFrameImpl implements IStackFrame
{
	private String text;

	private int[] localObjectIds;

	public StackFrameImpl(String text, int[] localObjectIds)
	{
		this.text = text;
		this.localObjectIds = localObjectIds;
	}

	public int[] getLocalObjectsIds()
	{
		return localObjectIds == null ? new int[0] : localObjectIds;
	}

	public String getText()
	{
		return text;
	}

}
