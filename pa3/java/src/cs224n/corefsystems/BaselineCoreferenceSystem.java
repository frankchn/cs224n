package cs224n.corefsystems;

import cs224n.coref.ClusteredMention;
import cs224n.coref.Document;
import cs224n.coref.Entity;
import cs224n.coref.Mention;
import cs224n.util.Pair;

import java.util.ArrayList;
import java.util.*;

/**
 * A simple exact-match coreference system.
 *
 * Note that the train() method doesn't actually do anything, since it is a
 * deterministic system; instead, it contains a skeleton for code which *does* do
 * useful training.
 *
 * @author Gabor Angeli (angeli at cs.stanford)
 */
public class BaselineCoreferenceSystem implements CoreferenceSystem{
  /**
   * Since this is a deterministic system, the train() method does nothing
   * @param trainingData The data to train off of
   */
  public void train(Collection<Pair<Document, List<Entity>>> trainingData) {
    for(Pair<Document, List<Entity>> pair : trainingData){
      //--Get Variables
      Document doc = pair.getFirst();
      List<Entity> clusters = pair.getSecond();
      List<Mention> mentions = doc.getMentions();
      //--Print the Document
//      System.out.println(doc.prettyPrint(clusters));
      //--Iterate over mentions
      for(Mention m : mentions){
//        System.out.println(m);
      }
      //--Iterate Over Coreferent Mention Pairs
      for(Entity e : clusters){
        for(Pair<Mention, Mention> mentionPair : e.orderedMentionPairs()){
//          System.out.println(""+mentionPair.getFirst() + " and " + mentionPair.getSecond() + " are coreferent");
        }
      }
    }
  }

  /**
   * Find mentions that are exact matches of each other, and mark them as coreferent.
   * @param doc The document to run coreference on
   * @return The list of clustered mentions to return
   */
  public List<ClusteredMention> runCoreference(Document doc) {
    //(variables)
    List<ClusteredMention> mentions = new ArrayList<ClusteredMention>();
    Map<String,Entity> clusters = new HashMap<String,Entity>();
    //(for each mention...)
    for(Mention m : doc.getMentions()){
      //(...get its text)
      String mentionString = m.gloss();
      //(...if we've seen this text before...)
      if(clusters.containsKey(mentionString)){
        //(...add it to the cluster)
        mentions.add(m.markCoreferent(clusters.get(mentionString)));
      } else {
        //(...else create a new singleton cluster)
        ClusteredMention newCluster = m.markSingleton();
        mentions.add(newCluster);
        clusters.put(mentionString,newCluster.entity);
      }
    }
    //(return the mentions)
    return mentions;
  }
}
