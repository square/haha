package net.vshor.cla;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

import org.eclipse.mat.collect.HashMapIntObject;
import org.eclipse.mat.collect.IteratorInt;
import org.eclipse.mat.collect.SetInt;
import org.eclipse.mat.parser.internal.SnapshotFactory;
import org.eclipse.mat.snapshot.ISnapshot;
import org.eclipse.mat.util.ConsoleProgressListener;
import org.eclipse.mat.util.IProgressListener;

public class Reducer {
  private static class ClassLoaderInfo {
    String name;
    int size = 0;
    int incomingIndirect = 0;
    int incomingDirect = 0;

    public ClassLoaderInfo(String name) {        
      this.name = name;
    }

    @Override
    public String toString() {     
      return name + ", size = " + size + ", incoming direct = " + incomingDirect + ", incoming indirect = " + incomingIndirect;
    }
  }


  /**
   * @param args
   */
  public static void main(String[] args) throws Exception {
    System.out.println("XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX");
    
    if (args.length < 1) {
      System.out.println("No arguments supplied");
    }

    IProgressListener listener = new ConsoleProgressListener(System.out);

    SnapshotFactory sf = new SnapshotFactory();
    ISnapshot snapshot = sf.openSnapshot(new File(args[0]),
        new HashMap<String, String>(), listener);
    int[] retainedSetArr = snapshot.getRetainedSet(snapshot.getGCRoots(),
        listener);
    
    SetInt retainedSet = new SetInt();
    for (int i : retainedSetArr) {
      retainedSet.add(i);
    }

    System.out.println(retainedSetArr.length);		

    HashMapIntObject<ClassLoaderInfo> classloaders = new HashMapIntObject<ClassLoaderInfo>(500);

    for (int obj : retainedSetArr) {
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
        
        if (!retainedSet.contains(inObj))
          continue;
        
        do {
          if (snapshot.getClassOf(inObj).getClassLoaderId() == clId
              || inObj == clId) {
            counts = false;
            break;
          }
          
          inObj = snapshot.getImmediateDominatorId(inObj);
        } while (inObj != -1);
        
        if (counts)
          cli.incomingIndirect++;
      }
    }

    List<ClassLoaderInfo> cliList = new ArrayList<Reducer.ClassLoaderInfo>();
    
    for (IteratorInt i = classloaders.keys(); i.hasNext();) {
      int clId = i.next();
      ClassLoaderInfo cli = classloaders.get(clId);
      
      int[] inbound = snapshot.getInboundRefererIds(clId);
      for (int inObj : inbound) {
        boolean counts = true;
        
        if (!retainedSet.contains(inObj))
          continue;
        
        int tmpInObj = inObj;
        
        do {
          if (snapshot.getClassOf(tmpInObj).getClassLoaderId() == clId
              || tmpInObj == clId) {
            counts = false;
            break;
          }
          
          tmpInObj = snapshot.getImmediateDominatorId(tmpInObj);
        } while (tmpInObj != -1);
        
        if (counts) {
          if (snapshot.getObject(clId).getTechnicalName().endsWith("0x1065ef1e0")) {
            try {
              System.out.println(snapshot.getObject(inObj).getTechnicalName());
            } catch (Exception e) {
              // TODO Auto-generated catch block
              e.printStackTrace();
            }
          }
            
          cli.incomingDirect++;
        }
      }  
      
      cliList.add(cli);
    }    
    
    Collections.sort(cliList, new Comparator<ClassLoaderInfo>() {
      public int compare(ClassLoaderInfo o1, ClassLoaderInfo o2) {
        return -Integer.valueOf(o1.size).compareTo(Integer.valueOf(o2.size));
      }
    });
    
    for (ClassLoaderInfo cli : cliList) {
      System.out.println(cli);
    }
  }

}
