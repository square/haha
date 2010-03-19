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
public interface IStackFrame {

	/**
	 * Returns the object IDs of all objects referenced from this stack frame -
	 * both Java and JNI local objects
	 * 
	 * @return int[] and array containing the object Ids. If there are no local
	 *         objects to the frame, and empty array will be returned
	 */
	public int[] getLocalObjectsIds();

	/**
	 * Get the text representation of the stack frame
	 * 
	 * @return java.lang.String the text representation of the stack fame - class and method
	 */
	public String getText();

}
