package net.vshor.cla;

import java.io.File;
import java.io.PrintStream;
import java.util.HashMap;

import org.eclipse.mat.collect.HashMapIntObject;
import org.eclipse.mat.collect.IteratorInt;
import org.eclipse.mat.parser.internal.SnapshotFactory;
import org.eclipse.mat.snapshot.ISnapshot;
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

    HashMapIntObject<ClassLoaderInfo> classloaders = new HashMapIntObject<ClassLoaderInfo>(500);

    for (int obj : retainedSet) {
      int clId = snapshot.getClassOf(obj).getClassLoaderId();
      ClassLoaderInfo cli = classloaders.get(clId);
      if (cli == null) {
        cli = new ClassLoaderInfo(snapshot.getObject(clId).getTechnicalName());
        classloaders.put(clId, cli);
      }
      cli.size++;

      int[] inbound = snapshot.getInboundRefererIds(obj);
      for (int inObj : inbound) {
        boolean counts = true;
        
        do {
          if (snapshot.getClassOf(inObj).getClassLoaderId() == clId
              || inObj == clId) {
            counts = false;
            break;
          }
          
          inObj = snapshot.getImmediateDominatorId(inObj);
        } while (inObj != -1);
        
        if (counts)
          cli.incoming++;
      }
    }

    PrintStream out = System.out;
    if (args.length > 1) {
    	out = new PrintStream(new File(args[1]));
    }

    out.println("Name,Size,Incoming refs");
    for (IteratorInt i = classloaders.keys(); i.hasNext();) {
      int clId = i.next();
      ClassLoaderInfo cli = classloaders.get(clId);
      
      int[] inbound = snapshot.getInboundRefererIds(clId);
      for (int inObj : inbound) {
        boolean counts = true;
        
        do {
          if (snapshot.getClassOf(inObj).getClassLoaderId() == clId
              || inObj == clId) {
            counts = false;
            break;
          }
          
          inObj = snapshot.getImmediateDominatorId(inObj);
        } while (inObj != -1);
        
        if (counts)
          cli.incoming++;
      }
      
      out.println(cli.toString());
    }
    
    out.close();
  }

}
