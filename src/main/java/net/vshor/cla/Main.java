package net.vshor.cla;

import java.io.File;
import java.util.HashMap;
import java.util.Iterator;
import java.util.regex.Pattern;

import org.eclipse.mat.collect.HashMapIntObject;
import org.eclipse.mat.collect.IteratorInt;
import org.eclipse.mat.collect.SetInt;
import org.eclipse.mat.parser.internal.SnapshotFactory;
import org.eclipse.mat.snapshot.DominatorsSummary;
import org.eclipse.mat.snapshot.ISnapshot;
import org.eclipse.mat.snapshot.DominatorsSummary.ClassDominatorRecord;
import org.eclipse.mat.util.ConsoleProgressListener;
import org.eclipse.mat.util.IProgressListener;

public class Main {

	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception {
		if (args.length < 1) {
			System.out.println("No arguments supplied");
		}

		IProgressListener listener = new ConsoleProgressListener(System.out);

		SnapshotFactory sf = new SnapshotFactory();
		ISnapshot snapshot = sf.openSnapshot(new File(args[0]),
				new HashMap<String, String>(), listener);
		int[] retainedSet = snapshot.getRetainedSet(snapshot.getGCRoots(),
				listener);

		System.out.println(retainedSet.length);
		
		HashMapIntObject<SetInt> clObjects = new HashMapIntObject<SetInt>(500);
		
		for (int obj : retainedSet) {
			int clId = snapshot.getClassOf(obj).getClassLoaderId();
			SetInt cntSet = clObjects.get(clId);
			if (cntSet == null) {
				cntSet = new SetInt();
				clObjects.put(clId, cntSet);
			}
			cntSet.add(obj);
		}
		
		DominatorsSummary dominators = snapshot.getDominatorsOf(retainedSet, Pattern.compile("java.lang.ref.WeakReference"), listener);
		ClassDominatorRecord[] cldr = dominators.getClassDominatorRecords();
		System.out.println();
		for (ClassDominatorRecord cdr : cldr) {
			for (int dominator : cdr.getDominators()) {
				if (snapshot.isClassLoader(dominator) && clObjects.containsKey(dominator)) {
					SetInt clObj = clObjects.get(dominator);
					String clName = snapshot.getObject(dominator).getTechnicalName();
					System.out.println(String.format("Classloader %s found in dominators. It loaded %d classes and dominates %d objects", clName,  clObj.size(), cdr.getDominated().length));
					boolean leaks = true;
					for (IteratorInt it = clObj.iterator(); it.hasNext(); ) {
						int loaded = it.next();
						boolean loadedAndDominated = false;
						for (int dominee : cdr.getDominated()) {
//							System.out.print(".");
							if (dominee == loaded) {
								loadedAndDominated = true;
								break;
							}
						}
						if (!loadedAndDominated) {
//							System.out.println("Object " + loaded + " is loaded but not dominated.");
							leaks = false;
						}
						else {
							System.out.println("Object " + loaded + " is loaded and dominated.");
						}
					}
					if (leaks) {
						System.out.println(clName + " IS LEAKING");
					}
				}
			}
		} 
	}

}
