package net.ekabanov.cla;
/*
 * Copyright 2005-2008 Sun Microsystems, Inc.  All Rights Reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Sun designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Sun in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Sun Microsystems, Inc., 4150 Network Circle, Santa Clara,
 * CA 95054 USA or visit www.sun.com if you need additional information or
 * have any questions.
 */

/*
 * The Original Code is HAT. The Initial Developer of the
 * Original Code is Bill Foote, with contributions from others
 * at JavaSoft/Sun.
 */

import java.io.File;
import java.util.HashMap;
import java.util.Iterator;

import org.eclipse.mat.collect.BitField;
import org.eclipse.mat.collect.HashMapIntObject;
import org.eclipse.mat.collect.SetInt;
import org.eclipse.mat.parser.internal.SnapshotFactory;
import org.eclipse.mat.snapshot.ISnapshot;
import org.eclipse.mat.snapshot.model.IObject;
import org.eclipse.mat.util.ConsoleProgressListener;
import org.eclipse.mat.util.IProgressListener;

public class Main {
  public static void main(String[] args) throws Exception {
    if (args.length < 1) {
      System.out.println("No arguments supplied");
    }

    IProgressListener listener = new ConsoleProgressListener(System.out);
    
    SnapshotFactory sf = new SnapshotFactory();
    ISnapshot snapshot = sf.openSnapshot(new File(args[0]), new HashMap<String, String>(), listener);
    int[] retainedSet = snapshot.getRetainedSet(snapshot.getGCRoots(), listener);

    HashMapIntObject<BitField> clLoaded = new HashMapIntObject<BitField>();

    for (int objectId : retainedSet) {
      IObject o = snapshot.getObject(objectId);
      int classLoaderId = o.getClazz().getClassLoaderId();
      BitField loaded = clLoaded.get(classLoaderId);
      if (loaded == null) {
        loaded = new BitField(retainedSet.length);
        clLoaded.put(classLoaderId, loaded);
      }
      loaded.set(objectId);
    }

    System.out.println("clLoaded.size() = " + clLoaded.size());

    SetInt leakedCls = new SetInt();

    for (Iterator<HashMapIntObject.Entry<BitField>> iter = clLoaded.entries(); iter.hasNext();) {
      HashMapIntObject.Entry<BitField> entry = iter.next();
      boolean leaked = false;
      for (int idx = 0; idx < retainedSet.length; idx++) {
        if (entry.getValue().get(idx)) {
          leaked = true;
          boolean clDominates = false;
          int dom = retainedSet[idx];
          do {
            dom = snapshot.getImmediateDominatorId(dom); 
            if (entry.getKey() == dom) {
              clDominates = true;
              break;
            }
          }
          while (dom != -1);

          if (!clDominates) {
            leaked = false;
            break;
          }            
        }
      }  
      
      if (leaked)
        leakedCls.add(entry.getKey());
    }

    for (int i : leakedCls.toArray()) {      
      int[] shortestPath = snapshot.getPathsFromGCRoots(i, null).getNextShortestPath();
      for (int j : shortestPath) {
        System.out.print(snapshot.getObject(j).getDisplayName() + " <- ");
      }
      System.out.println();
    }
  }

}
