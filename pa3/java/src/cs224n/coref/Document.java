package cs224n.coref;

import cs224n.util.Decodable;

import java.io.Serializable;
import java.util.*;

/**
 * Encapuslates a document you are to run coreference on. It includes the sentences in the document,
 * as well as the annotated mentions (which you will cluster into
 * coreferent mentions).
 *
 * @author Gabor Angeli (angeli at cs.stanford)
 */
public class Document implements Serializable, Decodable {
  private static final long serialVersionUID = 1L;
  /**
   * A unique identifier for this document
   */
  public final String id;
  /**
   * The sentences in this document
   */
  public final List<Sentence> sentences;
  private List<Mention> mentions = null;
  private final HashMap<Sentence,Integer> sentenceToIndex = new HashMap<Sentence,Integer>();
  private final HashMap<Mention,Integer> mentionToIndex = new HashMap<Mention,Integer>();

  /**
   * Create a document from an id and a list of sentences.
   * You're not likely to have to use this method.
   *
   * @param id The unique id of the document
   * @param sentences The sentences in the document
   */
  public Document(String id, List<Sentence> sentences){
    this.id = id;
    this.sentences = sentences;
  }

  /**
   * Sets the mentions for this document.
   * You are not likely to call this method; it is used in the framework code
   * @param mentions The mentions to associate with this document
   * @return The document
   */
  public Document setMentions(List<Mention> mentions){
    for(Mention mention : mentions){
      if(mention.doc != this){ throw new IllegalArgumentException("Mention does not belong to this document: " + mention); }
    }
    this.mentions = mentions;
    return this;
  }

  /**
   * Returns the mentions associated with this document.
   * @return A list of mentions associated with the document.
   */
  public List<Mention> getMentions(){
    if(mentions == null){
      throw new IllegalStateException("No mentions extracted for document: " + id);
    }
    return mentions;
  }

  /**
   * Create a formatted string for the document, including marking the mentions that are
   * coreferent
   * @param entities The set of entities to print
   * @return A formatted string representing the document
   */
  public String prettyPrint(Collection<Entity> entities){
    //--Variables
    int sentenceIndex = 0;
    StringBuilder b = new StringBuilder();
    //--Print Sentences
    for(Sentence s : sentences){
      b.append("(").append(sentenceIndex).append(") ");
      s.prettyPrint(b,getMentions(),entities);
      if(sentenceIndex < sentences.size()-1){ b.append("\n"); }
      sentenceIndex += 1;
    }
    return b.toString();
  }

  /**
   * Prints the guessed versus gold clusters for the given document
   * @param guess The guessed [clustered] mentions
   * @param gold The gold mentions
   * @return A string debug output
   */
  public String debug(Collection<ClusteredMention> guess, Collection<Entity> gold) {
    //--Find Cannonical Mentions
    Map<Entity,Mention> cannonicalMention = new HashMap<Entity,Mention>();
    Map<Mention,Entity> goldEntities = new HashMap<Mention,Entity>();
    for(Entity e : gold){
      Mention argmax = null;
      for(Mention m : e.mentions){
        if(argmax == null){
          //(if null, this is more cannonical)
          argmax = m;
        } else if(!argmax.headToken().isNoun() && m.headToken().isNoun()){
          //(if we're a noun, we're more cannonical)
          argmax = m;
        } else if(!argmax.headToken().isProperNoun() && m.headToken().isProperNoun()){
          //(if we're a proper noun, we're more cannonical)
          argmax = m;
//        } else if(argmax.headToken().isProperNoun()){ //uncomment these two to give priority to longer utterances
//          if(m.headToken().isProperNoun() && m.length() > argmax.length()){
//            //(if we're both proper nouns, the longer is more cannonical)
//            argmax = m;
//          }
//        } else if(argmax.headToken().isNoun()){
//          if(m.headToken().isNoun() && m.length() > argmax.length()){
//            //(if we're both nouns, the longer is more cannonical)
//            argmax = m;
//          }
        } else {
          //(keep the old version)
        }
        goldEntities.put(m, e);
      }
      cannonicalMention.put(e,argmax);
    }
    //--List Mistakes
    StringBuilder mistakes = new StringBuilder();
    Set<Mention> mentionsPrinted = new HashSet<Mention>();
    for(Entity goldEntity : gold){
      //(get cannonical mention)
      Mention cannonical = cannonicalMention.get(goldEntity);
      //(get corresponding guess)
      Entity guessEntity = null;
      for(ClusteredMention m : guess){
        if(m.mention == cannonical){ guessEntity = m.entity; break; }
      }
      //(compare)
      goldEntity.format(mistakes, cannonical).append(" -->  ");
      for(Mention m : this.mentions){
        if(m == cannonical){ mentionsPrinted.add(m); continue; }
        if(guessEntity == null){
          throw new IllegalStateException("Cannot call Document.debug() when using predicted mentions");
        }
        if(guessEntity.mentions.contains(m)){
          if(goldEntities.get(m) != goldEntity){ mistakes.append("!"); }
          goldEntities.get(m).format(mistakes, m).append("; ");
          mentionsPrinted.add(m);
        } else if(goldEntity.mentions.contains(m)){
          mistakes.append("!{").append(m.gloss()).append("}; ");
          mentionsPrinted.add(m);
        }
      }
      mistakes.append("\n");
    }
    //--Junk Clusters
    for(Mention m : this.mentions){
      if(!mentionsPrinted.contains(m)){
        //(this mention is dangling)
        for(ClusteredMention cm : guess){
          if(cm.mention == m){
            mistakes.append("JUNK ENTITY: ");
            cm.entity.prettyPrint(mistakes).append("\n");
            for(Mention otherMention : cm.entity.mentions){ mentionsPrinted.add(otherMention); }
          }
        }
      }
    }
    //--Return
    return mistakes.toString();
  }

  /**
   * Check if every mention associated with this document has been clustered with
   * an entity
   * @return True if all mentions are clustered
   */
  public boolean areAllMentionsClustered() {
    for(Mention m : getMentions()){
      if(m.corefferentWith == null){ return false; }
    }
    return true;
  }

  public int indexOfSentence(Sentence s){
    //(try simple get)
    Integer cand = sentenceToIndex.get(s);
    if(cand == null){
      //(populate map)
      for(int i=0; i<sentences.size(); i++){
        if(sentenceToIndex.containsKey(sentences.get(i))){ throw new IllegalStateException("Sentence equals() collision (not your fault!): " + sentences.get(i)); }
        sentenceToIndex.put(sentences.get(i), i);
      }
      cand = sentenceToIndex.get(s);
    }
    //(error check)
    if(cand == null){ throw new IllegalArgumentException("Sentence is not in document: " + s); }
    //(return)
    return cand.intValue();
  }

  public int indexOfMention(Mention m){
    //(try simple get)
    Integer cand = mentionToIndex.get(m);
    if(cand == null){
      //(error check)
      if(mentions == null){ throw new IllegalStateException("Document has no mentions stored"); }
      //(populate map)
      for(int i=0; i<mentions.size(); i++){
        if(mentionToIndex.containsKey(mentions.get(i))){ throw new IllegalStateException("Mention equals() collision (not your fault!): " + mentions.get(i)); }
        mentionToIndex.put(mentions.get(i), i);
      }
      cand = mentionToIndex.get(m);
    }
    //(error check)
    if(cand == null){ throw new IllegalArgumentException("Mention is not in document: " + m); }
    //(return)
    return cand.intValue();
  }

  @Override
  public boolean equals(Object o){ return o instanceof Document && ((Document) o).id.equals(this.id); }
  @Override
  public int hashCode(){ return id.hashCode(); }
  @Override
  public String toString(){ return id; }


  //--------------
  // SERIALIZATION
  //--------------
  public String encode() {
    StringBuilder b = new StringBuilder();
    //(save id)
    b.append(this.id).append("\n");
    //(save sentences)
    b.append("sentences:\n");
    for(Sentence s : sentences){
      b.append(s.encode()).append("\n");
    }
    //(end)
    b.append("<end>");
    //(error check)
    if(!decode(b.toString()).equals(this)){
      throw new IllegalStateException("Did not encode Document properly");
    }
    return b.toString();
  }

  public static Document decode(String encoded){
    String[] lines = encoded.split("\n");
    //(get id)
    String id = lines[0];
    //(get sentences)
    if(!lines[1].equals("sentences:")){ throw new IllegalStateException("Could not decode document: " + id + " offending line: " + lines[1]); }
    int index = 2;
    List<Sentence> sentences = new ArrayList<Sentence>();
    while(!lines[index].equals("<end>")){
      sentences.add(Sentence.decode(lines[index]));
      index += 1;
    }
    return new Document(id, sentences);
  }
}
