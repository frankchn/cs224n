package cs224n.corefsystems;

import cs224n.coref.ClusteredMention;
import cs224n.coref.Document;
import cs224n.coref.Entity;
import cs224n.coref.Mention;
import cs224n.coref.Pronoun;
import cs224n.coref.Name;
import cs224n.util.Pair;

import java.util.ArrayList;
import java.util.*;

/*
java -Xmx500m -cp "extlib/*:classes" cs224n.assignments.CoreferenceTester \
     -path /afs/ir/class/cs224n/pa3/data/ \
     -model BetterBaseline -data dev -documents 100
*/

public class BetterBaseline implements CoreferenceSystem{

  HashMap<String, HashSet<String>> commonCo = new HashMap<String, HashSet<String>>();

  public void train(Collection<Pair<Document, List<Entity>>> trainingData) {
    for(Pair<Document, List<Entity>> pair : trainingData){
      //--Get Variables
      Document doc = pair.getFirst();
      List<Entity> clusters = pair.getSecond();
      List<Mention> mentions = doc.getMentions();

      //--Iterate Over Coreferent Mention Pairs
      for(Entity e : clusters){
        for(Pair<Mention, Mention> mentionPair : e.orderedMentionPairs()){
          String first = mentionPair.getFirst().headWord();
          String second = mentionPair.getSecond().headWord();

          if(!commonCo.containsKey(first))
            commonCo.put(first, new HashSet<String>());
          if(!commonCo.containsKey(second))
            commonCo.put(second, new HashSet<String>());

          commonCo.get(first).add(second);
          commonCo.get(second).add(first);
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

    for(Mention m : doc.getMentions()){

      String mentionString = m.gloss();
      boolean skiprest = false;

      if(clusters.containsKey(mentionString)){
        mentions.add(m.markCoreferent(clusters.get(mentionString)));
        skiprest = true;
      } 

      // We check for Pronouns first
      Pronoun p = Pronoun.valueOrNull(mentionString);
      if(!skiprest && p != null) {
        for(String k: clusters.keySet()) {
          if(Name.gender(k) == p.gender) {
            mentions.add(m.markCoreferent(clusters.get(k)));
            skiprest = true;
            break;
          }
        }
      }

      // We next check for head words coreference
      if(!skiprest && commonCo.containsKey(mentionString)) {
        for(String coref: commonCo.get(mentionString)) {
          if(clusters.containsKey(coref)) {
            mentions.add(m.markCoreferent(clusters.get(coref)));
            skiprest = true;
            break;    
          }
        }
      }

      // We give up let's create something else
      if(!skiprest) {
        ClusteredMention newCluster = m.markSingleton();
        mentions.add(newCluster);
        clusters.put(mentionString,newCluster.entity);
      }

    }

    //(return the mentions)
    return mentions;
  }
}
