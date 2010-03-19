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

/**
 * May be subject of change
 * 
 * @noimplement
 * @since 0.8
 */
public interface IThreadStack {

	/**
	 * Get the stack frames (i.e. the different method calls) of the thread
	 * stack
	 * 
	 * @return {@link IStackFrame}[] an array containing all stack frames. The
	 *         first element of the array contains the top of the stack, and the
	 *         last element the bottom of the stack
	 */
	public IStackFrame[] getStackFrames();

	/**
	 * Get the ID of the thread to which this stack belongs
	 * 
	 * @return the object ID of the thread owning this stack
	 */
	public int getThreadId();

}