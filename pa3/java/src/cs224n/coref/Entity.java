package cs224n.coref;

import cs224n.util.Ansi;
import cs224n.util.Decodable;
import cs224n.util.Pair;

import java.io.Serializable;
import java.util.*;

/**
 * Denotes a real-world entity, as defined by a set of mentions.
 *
 * @author Gabor Angeli (angeli at cs.stanford)
 */
public class Entity implements Serializable, Decodable, Iterable<Mention> {
  private static final long serialVersionUID = 1L;
  private static int nextUniqueID = 0;

  private final List<Mention> mentionList;
  /**
   * A collection of mentions which are deemed coreferent; that is,
   * which all refer to this entity.
   */
  public final Set<Mention> mentions = new HashSet<Mention>();
  /**
   * A unique ID for this entity
   */
  public final int uniqueID = nextUniqueID++; //set, then increment

  /**
   * Create an empty entity.
   * Do not call this method directly.
   * @param mentionList The list of all possible mentions
   */
  public Entity(List<Mention> mentionList){
    this.mentionList = mentionList;
  }

  /**
   * Create a singleton Entity.
   * @param mentionList The list of all possible mentions
   * @param singleton The singleton mention to add to the entity
   */
  public Entity(List<Mention> mentionList, Mention singleton){
    this(mentionList);
    mentions.add(singleton);
  }

  /**
   * Create an Entity from a collection of coreferent mentions.
   * @param mentionList The list of all possible mentions
   * @param mentions The mentions to associate with the new entity.
   */
  public Entity(List<Mention> mentionList, Collection<Mention> mentions){
    this(mentionList);
    this.mentions.addAll(mentions);
  }

  /**
   * Copy an entity
   * @param mentions The entity to copy
   */
  public Entity(Entity mentions){
    this(mentions.mentionList, mentions.mentions);
  }

  /**
   * Add a number of mentions to this entity.
   * @param mention The mention(s) to add
   * @return This entity
   */
  public Entity add(Mention... mention){
    for(Mention m : mention){
      if(m.corefferentWith != null && m.corefferentWith != this){ throw new IllegalArgumentException("Marking an entity as corefferent multiple times!"); }
      m.corefferentWith = this;
      mentions.add(m);
    }
    return this;
  }

  /**
   * Add all mentions in a collection
   * @param mentions The mentions to mark as coreferent
   * @return This entity
   */
  public Entity addAll(Collection<Mention> mentions){
    for(Mention m : mentions){
      if(m.corefferentWith != null && m.corefferentWith != this){ throw new IllegalArgumentException("Marking an entity as corefferent multiple times!"); }
      m.corefferentWith = this;
      this.mentions.add(m);
    }
    return this;
  }

  /**
   * The size of the entity (the number of mentions referring to it)
   * @return The number of clusters referring to this entity
   */
  public int size(){
    return mentions.size();
  }

  /**
   * An iterator over all the mentions in the entity
   * @return An iterator of the mentions referring to this entity
   */
  public Iterator<Mention> iterator() {
    return mentions.iterator();
  }

  /**
   * An Iterable representing each pair of mentions referring to this entity.
   * This is an order-sensitive iterator; that is, the mentions A and B will
   * show up both as (A,B) and (B,A).
   * @return An Iterable representing each pair of mentions referring to this entity.
   */
  public Iterable<Pair<Mention,Mention>> orderedMentionPairs(){
    return new Iterable<Pair<Mention, Mention>>() {
      public Iterator<Pair<Mention, Mention>> iterator() {
        return new Iterator<Pair<Mention, Mention>>() {
          // -- Variables --
          private Iterator<Mention> iterLeft = mentions.iterator();
          private Iterator<Mention> iterRight = mentions.iterator();
          private Mention leftCand = iterLeft.hasNext() ? iterLeft.next() : null;
          private Pair<Mention,Mention> retVal = getNext();
          // -- Helper Methods --
          private Pair<Mention,Mention> getNext(){
            boolean foundPair = false;
            Mention rightCand = null;
            //(while we have a left candidate...)
            while(leftCand != null && !foundPair){
              //(try some right candidates...)
              while(!foundPair && iterRight.hasNext()){
                rightCand = iterRight.next();
                if(rightCand != leftCand){
                  foundPair = true;
                }
              }
              //(if nothing worked, increment left)
              if(!foundPair){
                if(iterRight.hasNext()){ throw new IllegalStateException("Should not happen"); }
                leftCand = iterLeft.hasNext() ? iterLeft.next() : null;
                iterRight = mentions.iterator();
              }
            }
            return foundPair ? Pair.make(leftCand,rightCand) : null;
          }
          // -- Interface Methods --
          public boolean hasNext() {
            return retVal != null;
          }
          public Pair<Mention, Mention> next() {
            if(!hasNext()){ throw new NoSuchElementException("Iterator is empty"); }
            Pair<Mention,Mention> rtn = retVal;
            retVal = getNext();
            return rtn;
          }
          public void remove() {
            throw new IllegalArgumentException("Cannot remove from this iterator");
          }
        };
      }
    };
  }

  public StringBuilder prettyPrint(StringBuilder b){
    for(Mention m : mentions){
      format(b,m).append(" ");
    }
    return b;
  }

  /**
   * Format a mention, given that it refers to this entity
   * @param b The string builder to append to
   * @param mention The mention to format
   * @return The same string builder passed as an argument
   */
  protected StringBuilder format(StringBuilder b, Mention mention){
    if(Ansi.supportsAnsi()){
      return b.append(Ansi.prefix(uniqueID)).append("{").append(mention.toString()).append("}").append(Ansi.endFormatting);
    } else {
      return b.append("[").append(uniqueID).append("]{").append(mention.toString()).append("}");
    }
  }

  /**
   * Create a set of Entity objects from a collection of clustered mentions.
   * That is, given each mention is tagged with an entity, it creates a set of
   * the entities they refer to.
   * @param mentions The mentions that are clustered into entities
   * @return A set of entities representing the coreferent clusters of the mentions.
   */
  public static Set<Entity> fromMentions(Collection<ClusteredMention> mentions){
    Set<Entity> entities = new HashSet<Entity>();
    for(ClusteredMention mention : mentions){
      entities.add(mention.entity);
    }
    return entities;
  }

  public static Map<Mention,Entity> mentionToEntityMap(Iterable<Entity> entities){
    Map<Mention,Entity> map = new HashMap<Mention,Entity>();
    for(Entity targetEntity : entities){
      for(Mention sourceMention : targetEntity.mentions){
        if(map.containsKey(sourceMention)){ throw new IllegalArgumentException("Mention maps to multiple entities: " + sourceMention); }
        map.put(sourceMention,targetEntity);
      }
    }
    return map;
  }

  @Override
  public boolean equals(Object o){ return o instanceof Entity && ((Entity) o).mentions.equals(this.mentions); }
  @Override
  public int hashCode(){ return mentions.hashCode(); }
  @Override
  public String toString(){
    StringBuilder b = new StringBuilder();
    b.append("{ ");
    for(Mention m : mentions){
      b.append("'").append(m).append("'").append(" ");
    }
    b.append("}");
    return b.toString();
  }

  //--------------
  // SERIALIZATION
  //--------------
  public String encode() {
    StringBuilder b = new StringBuilder();
    //(create index map)
    HashMap<Mention,Integer> indices = new HashMap<Mention,Integer>();
    for(int i=0; i<mentionList.size(); i++) {
      indices.put(mentionList.get(i), i);
    }
    //(create string)
    for(Mention m : mentions){
      b.append(indices.get(m)).append(" ");
    }
    String encoded = b.substring(0,b.length()-1);
    //(error check)
    if(!decode(encoded,this.mentionList).equals(this)){
      System.out.println();
      System.out.println("encoded: " + encoded);
      System.out.println("this:    " + this);
      System.out.println("decoded: " + decode(encoded,this.mentionList));
      throw new IllegalStateException("Did not encode Entity properly: " + encoded);
    }
    return encoded;
  }

  public static Entity decode(String encoded,List<Mention> mentionList){
    //(create index map)
    HashMap<Mention,Integer> indices = new HashMap<Mention,Integer>();
    for(int i=0; i<mentionList.size(); i++) {
      indices.put(mentionList.get(i), i);
    }
    //(save indices)
    String[] mentionStrings = encoded.split(" +");
    List<Mention> mentions = new ArrayList<Mention>(mentionStrings.length);
    for(int i=0; i<mentionStrings.length; i++){
      if(!mentionStrings[i].trim().equals("")){
        int index = Integer.parseInt(mentionStrings[i].trim());
        mentions.add(mentionList.get(index));
      }
    }
    return new Entity(mentionList,mentions);
  }
}
