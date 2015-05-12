/**
 * ****************************************************************************
 * Copyright (c) 2008 SAP AG.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * SAP AG - initial API and implementation
 * *****************************************************************************
 */
package org.eclipse.mat.snapshot;

/**
 * @noinstantiate
 */
public class SnapshotFormat {
  private String name;
  private String[] fileExtensions;

  public SnapshotFormat(String name, String[] fileExtensions) {
    this.fileExtensions = fileExtensions;
    this.name = name;
  }

  public String getName() {
    return name;
  }

  public String[] getFileExtensions() {
    return fileExtensions;
  }
}
