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
package org.eclipse.mat.parser;

import java.io.File;
import java.io.IOException;
import org.eclipse.mat.SnapshotException;
import org.eclipse.mat.util.IProgressListener;

public interface IIndexBuilder {
  /* initialize with file and prefix (needed for naming conventions) */
  void init(File file, String prefix) throws SnapshotException, IOException;

  /* hprof: pass1 and pass2 parsing */
  void fill(IPreliminaryIndex index, IProgressListener listener)
      throws SnapshotException, IOException;

  /* hprof: update object 2 file position index */
  void clean(final int[] purgedMapping, IProgressListener listener) throws IOException;

  /* called in case of error to delete any files / close any file handles */
  void cancel();
}
