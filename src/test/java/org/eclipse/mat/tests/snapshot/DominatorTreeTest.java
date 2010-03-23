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
package org.eclipse.mat.tests.snapshot;

import static org.junit.Assert.assertEquals;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.eclipse.mat.SnapshotException;
import org.eclipse.mat.snapshot.ISnapshot;
import org.eclipse.mat.snapshot.model.IClass;
import org.eclipse.mat.tests.TestSnapshots;
import org.eclipse.mat.util.VoidProgressListener;
import org.junit.Test;

public class DominatorTreeTest
{
    @Test
    public void testDomTreeSunJdk6_32() throws SnapshotException
    {
        testWith(TestSnapshots.getSnapshot(TestSnapshots.SUN_JDK6_32BIT, false), 224);
    }

    @Test
    public void testDomTreeSunJdk5_64() throws SnapshotException
    {
        testWith(TestSnapshots.getSnapshot(TestSnapshots.SUN_JDK5_64BIT, false), 376);
    }

//    @Test
//    public void testDomTreeIBMJdk6_32_System() throws SnapshotException
//    {
//        testWith(TestSnapshots.getSnapshot(TestSnapshots.IBM_JDK6_32BIT_SYSTEM, false), 256);
//    }
//
//    @Test
//    public void testDomTreeIBMJdk6_32_Heap() throws SnapshotException
//    {
//        testWith(TestSnapshots.getSnapshot(TestSnapshots.IBM_JDK6_32BIT_HEAP, false), 256);
//    }

    private void testWith(ISnapshot snapshot, long size) throws SnapshotException
    {
        Collection<IClass> rClasses = snapshot.getClassesByName(
                        "org.eclipse.mat.tests.CreateSampleDump$DominatorTestData$R", false);
        IClass rClass = rClasses.iterator().next();
        int rId = rClass.getObjectIds()[0];

        int[] dominated = snapshot.getImmediateDominatedIds(rId);
        int[] retainedSetR = snapshot.getRetainedSet(new int[] { rId }, new VoidProgressListener());

        assert dominated.length == 8 : "R should be immediate dominator of 8 objects";
        assertEquals("R has an unexpected retained size", size, snapshot.getRetainedHeapSize(rId));
        assert retainedSetR.length == 13 : "R should retain 13 objects";

        Map<String, Set<String>> children = new HashMap<String, Set<String>>();

        Set<String> set = new HashSet<String>();
        set.add("$A");
        set.add("$B");
        set.add("$C");
        set.add("$D");
        set.add("$E");
        set.add("$H");
        set.add("$I");
        set.add("$K");
        children.put("$R", set);

        set = new HashSet<String>();
        set.add("$F");
        set.add("$G");
        children.put("$C", set);

        set = new HashSet<String>();
        set.add("$L");
        children.put("$D", set);

        set = new HashSet<String>();
        set.add("$J");
        children.put("$G", set);

        Map<String, String> parent = new HashMap<String, String>();
        parent.put("$A", "$R");
        parent.put("$B", "$R");
        parent.put("$C", "$R");
        parent.put("$D", "$R");
        parent.put("$E", "$R");
        parent.put("$H", "$R");
        parent.put("$I", "$R");
        parent.put("$K", "$R");

        parent.put("$F", "$C");
        parent.put("$G", "$C");

        parent.put("$L", "$D");

        parent.put("$J", "$G");

        // check R
        for (int id : retainedSetR)
        {
            String name = name(id, snapshot);
            int dominatorId = snapshot.getImmediateDominatorId(id);
            if (!"$R".equals(name))
            {
                assert parent.get(name).equals(name(dominatorId, snapshot)) : "Wrong parent of " + name;
            }

            int[] dominatedIds = snapshot.getImmediateDominatedIds(id);
            Set<String> childrenSet = children.get(name);
            for (int child : dominatedIds)
            {
                assert childrenSet.contains(name(child, snapshot)) : "Unknown child of " + name + " -> "
                                + name(child, snapshot);
            }
        }
    }

    private String name(int id, ISnapshot snapshot) throws UnsupportedOperationException, SnapshotException
    {
        String nodeClass = snapshot.getClassOf(id).getName();
        return nodeClass.substring(nodeClass.length() - 2);
    }

}
