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

import java.io.IOException;
import org.eclipse.mat.SnapshotException;
import org.eclipse.mat.parser.model.ObjectArrayImpl;
import org.eclipse.mat.parser.model.PrimitiveArrayImpl;
import org.eclipse.mat.snapshot.ISnapshot;
import org.eclipse.mat.snapshot.model.IObject;

public interface IObjectReader {
  void open(ISnapshot snapshot) //
      throws SnapshotException, IOException;

  IObject read(int objectId, ISnapshot snapshot) //
      throws SnapshotException, IOException;

  Object readPrimitiveArrayContent(PrimitiveArrayImpl array, int offset, int length) //
      throws IOException, SnapshotException;

  long[] readObjectArrayContent(ObjectArrayImpl array, int offset, int length) //
      throws IOException, SnapshotException;

  <A> A getAddon(Class<A> addon) //
      throws SnapshotException;

  void close() throws IOException;
}
