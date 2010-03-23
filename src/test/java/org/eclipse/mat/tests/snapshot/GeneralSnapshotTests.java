package org.eclipse.mat.tests.snapshot;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collection;

import org.eclipse.mat.SnapshotException;
import org.eclipse.mat.snapshot.ISnapshot;
import org.eclipse.mat.snapshot.model.IClass;
import org.eclipse.mat.snapshot.model.IObject;
import org.eclipse.mat.snapshot.model.IStackFrame;
import org.eclipse.mat.snapshot.model.IThreadStack;
import org.eclipse.mat.tests.TestSnapshots;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

@RunWith(value = Parameterized.class)
public class GeneralSnapshotTests
{

    @Parameters
    public static Collection<Object[]> data()
    {
        return Arrays.asList(new Object[][] {
            {TestSnapshots.SUN_JDK6_32BIT},
            {TestSnapshots.SUN_JDK5_64BIT},
            {TestSnapshots.IBM_JDK6_32BIT_HEAP},
            {TestSnapshots.IBM_JDK6_32BIT_JAVA},
            {TestSnapshots.IBM_JDK6_32BIT_HEAP_AND_JAVA},
            {TestSnapshots.IBM_JDK6_32BIT_SYSTEM},
        });
    }

    public GeneralSnapshotTests(String snapshotname)
    {
        snapshot = TestSnapshots.getSnapshot(snapshotname, false);
    }

    final ISnapshot snapshot;

    @Test
    public void Stacks1() throws SnapshotException
    {
        int frames = 0;
        int foundTop = 0;
        int foundNotTop = 0;
        for (IClass thrdcls : snapshot.getClassesByName("java.lang.Thread", true))
        {
            for (int o : thrdcls.getObjectIds())
            {
                IThreadStack stk = snapshot.getThreadStack(o);
                if (stk != null)
                {
                    int i = 0;
                    for (IStackFrame frm : stk.getStackFrames())
                    {
                        int os[] = frm.getLocalObjectsIds();
                        if (os != null)
                        {
                            if (i == 0)
                                foundTop += os.length;
                            else
                                foundNotTop += os.length;
                        }
                        ++i;
                        ++frames;
                    }
                }
            }
        }
        // If there were some frames, and some frames had some objects
        // then a topmost frame should have some objects
        if (frames > 0 && foundNotTop > 0)
        {
            assertTrue("Expected some objects on top of stack", foundTop > 0);
        }
    }

    @Test
    public void TotalClasses() throws SnapshotException
    {
        int nc = snapshot.getClasses().size();
        int n = snapshot.getSnapshotInfo().getNumberOfClasses();
        assertEquals("Total classes", n, nc);
    }

    @Test
    public void TotalObjects() throws SnapshotException
    {
        int no = 0;
        for (IClass cls : snapshot.getClasses())
        {
            no += cls.getNumberOfObjects();
        }
        int n = snapshot.getSnapshotInfo().getNumberOfObjects();
        assertEquals("Total objects", n, no);
    }

    @Test
    public void TotalHeapSize() throws SnapshotException
    {
        long total = 0;
        for (IClass cls : snapshot.getClasses())
        {
            total += snapshot.getHeapSize(cls.getObjectIds());
        }
        long n = snapshot.getSnapshotInfo().getUsedHeapSize();
        assertEquals("Total heap size", n, total);
    }

    @Test
    public void ObjectSizes() throws SnapshotException
    {
        long total = 0;
        for (IClass cls : snapshot.getClasses())
        {
            for (int o : cls.getObjectIds())
            {
                IObject obj = snapshot.getObject(o);
                int n = obj.getUsedHeapSize();
                int n2 = snapshot.getHeapSize(o);
                if (n != n2)
                {
                    assertEquals("snapshot object heap size / object heap size "+obj, n, n2);
                }
                total += n;
            }
        }
        long n = snapshot.getSnapshotInfo().getUsedHeapSize();
        assertEquals("Total heap size", n, total);
    }

}
