package net.vshor.cla;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.mat.SnapshotException;
import org.eclipse.mat.collect.HashMapIntObject;
import org.eclipse.mat.collect.IteratorInt;
import org.eclipse.mat.parser.internal.SnapshotFactory;
import org.eclipse.mat.snapshot.ExcludedReferencesDescriptor;
import org.eclipse.mat.snapshot.IPathsFromGCRootsComputer;
import org.eclipse.mat.snapshot.ISnapshot;
import org.eclipse.mat.snapshot.model.IClass;
import org.eclipse.mat.util.ConsoleProgressListener;
import org.eclipse.mat.util.IProgressListener;

public class Reducer {
  private static class ClassLoaderInfo {
    String name;
    int size = 0;
    int incoming = 0;

    public ClassLoaderInfo(String name) {        
      this.name = name;
    }

    @Override
    public String toString() {     
      return name + "," + size + "," + incoming;
    }
  }

  private static Map<IClass, Set<String>> EXCLUDED_REFERENCES;

  private static Map<IClass, Set<String>> getWeakExcludeMap(ISnapshot snapshot) throws SnapshotException {
    if (EXCLUDED_REFERENCES == null) {
      EXCLUDED_REFERENCES = new HashMap<IClass, Set<String>>();

      EXCLUDED_REFERENCES.put(snapshot.getClassesByName("java.lang.ref.WeakReference", false).iterator().next(), null);
      EXCLUDED_REFERENCES.put(snapshot.getClassesByName("java.lang.ref.PhantomReference", false).iterator().next(), null);
      EXCLUDED_REFERENCES.put(snapshot.getClassesByName("java.lang.ref.SoftReference", false).iterator().next(), null);
    }

    return new HashMap<IClass, Set<String>>(EXCLUDED_REFERENCES);
  }


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
    
    List<ExcludedReferencesDescriptor> erds = new ArrayList<ExcludedReferencesDescriptor>();
    
    for (IClass klass : snapshot.getClassesByName("java.lang.ref.WeakReference", true)) {
      erds.add(new ExcludedReferencesDescriptor(klass.getObjectIds(), (Set<String>) null));
    }
    for (IClass klass : snapshot.getClassesByName("java.lang.ref.SoftReference", true)) {
      erds.add(new ExcludedReferencesDescriptor(klass.getObjectIds(), (Set<String>) null));
    }
    for (IClass klass : snapshot.getClassesByName("java.lang.ref.PhantomReference", true)) {
      erds.add(new ExcludedReferencesDescriptor(klass.getObjectIds(), (Set<String>) null));
    }

    int[] retainedSetArr = snapshot.getRetainedSet(
        snapshot.getGCRoots(), 
        erds.toArray(new ExcludedReferencesDescriptor[erds.size()]),
        listener);

    HashMapIntObject<ClassLoaderInfo> classloaders = new HashMapIntObject<ClassLoaderInfo>(500);

    for (int obj : retainedSetArr) {
      int clId = snapshot.getClassOf(obj).getClassLoaderId();

      ClassLoaderInfo cli = classloaders.get(clId);
      if (cli == null) {
        cli = new ClassLoaderInfo(snapshot.getObject(clId).getTechnicalName());
        classloaders.put(clId, cli);
      }
      cli.size++;
    }

    List<ClassLoaderInfo> cliList = new ArrayList<Reducer.ClassLoaderInfo>();

    for (IteratorInt i = classloaders.keys(); i.hasNext();) {
      int clId = i.next();
      ClassLoaderInfo cli = classloaders.get(clId);

      IPathsFromGCRootsComputer paths = snapshot.getPathsFromGCRoots(clId, getWeakExcludeMap(snapshot));        

      while (paths.getNextShortestPath() != null)
        cli.incoming++;

      cliList.add(cli);
    }    

    Collections.sort(cliList, new Comparator<ClassLoaderInfo>() {
      public int compare(ClassLoaderInfo o1, ClassLoaderInfo o2) {
        return -Integer.valueOf(o1.size).compareTo(Integer.valueOf(o2.size));
      }
    });

	PrintStream out = System.out;
    if (args.length > 1) {
    	out = new PrintStream(new File(args[1]));
    }

    out.println("Name,Size,Incoming refs");
    for (ClassLoaderInfo cli : cliList) {
      System.out.println(cli);
    }
	out.close();
  }

}
