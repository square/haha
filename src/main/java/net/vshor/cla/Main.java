package net.vshor.cla;

import java.io.File;
import java.util.HashMap;
import java.util.Iterator;
import java.util.regex.Pattern;

import org.eclipse.mat.collect.IteratorInt;
import org.eclipse.mat.collect.SetInt;
import org.eclipse.mat.parser.internal.SnapshotFactory;
import org.eclipse.mat.snapshot.DominatorsSummary;
import org.eclipse.mat.snapshot.ISnapshot;
import org.eclipse.mat.snapshot.DominatorsSummary.ClassDominatorRecord;
import org.eclipse.mat.snapshot.DominatorsSummary.ClassloaderDominatorRecord;
import org.eclipse.mat.util.ConsoleProgressListener;
import org.eclipse.mat.util.IProgressListener;

import sun.text.IntHashtable;

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
		
		SetInt classLoaders = new SetInt();
		for (int cl : retainedSet) {
			if (snapshot.isClassLoader(cl)) {
				String clName = snapshot.getClassOf(cl).getName();
				if (!clName.startsWith("sun.")) {
					classLoaders.add(cl);
					int[] clRetSet = snapshot.getRetainedSet(new int[] {cl}, listener);
					System.out.println(String.format("Found classloader: %s, it has loaded: %d ", clName, clRetSet.length));
				}
			}
		}
		System.out.println("Classloaders found: " + classLoaders.size());
		
		DominatorsSummary dominators = snapshot.getDominatorsOf(retainedSet, Pattern.compile("sun.*"), listener);
		ClassloaderDominatorRecord[] classloaderDominatorRecords = dominators.getClassloaderDominatorRecords();
		for (ClassloaderDominatorRecord cldr : classloaderDominatorRecords) {
				System.out.println("Processing dominator records for classloader: " + cldr.getName());
				int[] clRetSet = snapshot.getRetainedSet(new int[] {cldr.getId()}, listener);
				System.out.println("Classloader has loaded: " + clRetSet.length);
				for (ClassDominatorRecord cdr : cldr.getRecords()) {
//	//				if (cdr.getDominatorCount() == 1 && cdr.getDominators()[0] == cldr.getId()) 
					for (int dominator : cdr.getDominators()) {
						if (dominator == cldr.getId()) {
							System.out.println("Classloader is dominating");
						}
					}
				} 
		}
	}

}
