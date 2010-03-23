package org.eclipse.mat.tests.snapshot;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Collection;
import java.util.StringTokenizer;

import org.eclipse.mat.snapshot.ISnapshot;
import org.eclipse.mat.snapshot.model.IClass;
import org.eclipse.mat.tests.TestSnapshots;
import org.eclipse.mat.util.MessageUtil;
import org.junit.Test;

public class TestInstanceSizes
{

	@Test
	public void testSun5_32Bit() throws Exception
	{
		File histogramFile = TestSnapshots.getResourceFile(TestSnapshots.HISTOGRAM_SUN_JDK5_13_32BIT);
		ISnapshot snapshot = TestSnapshots.getSnapshot(TestSnapshots.SUN_JDK5_13_32BIT, false);
		doTest(histogramFile, snapshot);
	}
	
	@Test
	public void testSun6_32Bit() throws Exception
	{
		File histogramFile = TestSnapshots.getResourceFile(TestSnapshots.HISTOGRAM_SUN_JDK6_18_32BIT);
		ISnapshot snapshot = TestSnapshots.getSnapshot(TestSnapshots.SUN_JDK6_18_32BIT, false);
		doTest(histogramFile, snapshot);
	}
	
	@Test
	public void testSun6_64Bit() throws Exception
	{
		File histogramFile = TestSnapshots.getResourceFile(TestSnapshots.HISTOGRAM_SUN_JDK6_18_64BIT);
		ISnapshot snapshot = TestSnapshots.getSnapshot(TestSnapshots.SUN_JDK6_18_64BIT, false);
		doTest(histogramFile, snapshot);
	}
	
	private void doTest(File histogramFile, ISnapshot snapshot) throws Exception
	{
		BufferedReader in = null;
		try
		{
			in = new BufferedReader(new FileReader(histogramFile));
			
			String line;
			int errorCount = 0;
			StringBuilder errorMessage = new StringBuilder();
			
			while ((line = in.readLine()) != null)
			{
				StringTokenizer tokenizer = new StringTokenizer(line);
				int numObjects = Integer.parseInt(tokenizer.nextToken());
				long shallowSize = Long.parseLong(tokenizer.nextToken());
				long instanceSize = shallowSize / numObjects;
				String className = tokenizer.nextToken();
//				System.out.println(className + " " + instanceSize);
				
				if (className.startsWith("[") || className.startsWith("<") || className.equals("java.lang.Class"))
					continue;
				
				Collection<IClass> classes = snapshot.getClassesByName(className, false);
				if (classes == null || classes.size() == 0)
				{
					System.out.println(MessageUtil.format("Cannot find class [{0}] in heap dump", className));
					continue;
				}
				IClass clazz = classes.iterator().next();
				if (clazz.getHeapSizePerInstance() != instanceSize)
				{
					errorCount++;
					errorMessage.append(MessageUtil.format("Class [{0}] expected size {1} but got {2}\r\n", className, instanceSize, clazz.getHeapSizePerInstance()));
				}
			}
			
			assert errorCount == 0 : "For the following classes the instance size isn't correct\r\n" + errorMessage.toString();
		}
		finally
		{
			if (in != null)
			{
				try
				{
					in.close();
				}
				catch (IOException ignore)
				{
					// ignore
				}
			}

		}
	}

}
