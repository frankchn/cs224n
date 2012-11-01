package cs224n.coref;

import cs224n.ling.Tree;
import cs224n.util.Decodable;

import java.io.Serializable;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *  Denotes a mention. This class stores much of the useful information from which
 *  you would extract rules and features. It includes the text for the mention, along with its
 *  location in the sentence, its parse tree, Named Entity type, etc.
 *
 * @author Gabor Angeli (angeli at cs.stanford)
 */
public class Mention implements Serializable, Decodable {
  private static final long serialVersionUID = 1L;

  /**
   * The document this mention is found in
   */
  public final Document doc;
  /**
   * The sentence this mention is found in
   */
  public final Sentence sentence;
  /**
   * The index of the first word of the mention, in the sentence
   */
  public final int beginIndexInclusive;
  /**
   * The index after the last word of the mention, in the sentence
   */
  public final int endIndexExclusive;
  /**
   * The parse fragment for this mention
   */
  public final Tree<String> parse;
  /**
   * The index of the word at the head of the parse for this mention.
   */
  public final int headWordIndex;

  /**
   * Marks the entity this mention is coreferent with
   */
  protected Entity corefferentWith = null;

  /**
   * Create a new mention from the necessary parameters.
   * You should not have to use this method.
   * @param doc The document
   * @param sentence The sentence
   * @param beginInclusive The start index
   * @param endExclusive The end index
   * @param parse The parse fragment
   * @param headWordIndex The head word index
   */
  public Mention(Document doc, Sentence sentence, int beginInclusive, int endExclusive, Tree<String> parse, int headWordIndex){
    this.doc = doc;
    this.sentence = sentence;
    this.beginIndexInclusive = beginInclusive;
    this.endIndexExclusive = endExclusive;
    this.parse = parse;
    this.headWordIndex = headWordIndex;
  }

  /**
   * The text of this mention, as a list of words
   * @return A list of words corresponding to this mention
   */
  public List<String> text(){ return sentence.words.subList(beginIndexInclusive, endIndexExclusive); }

  /**
   * The head word of this mention
   * @return The head word
   */
  public String headWord(){ return sentence.words.get(headWordIndex); }

  /**
   * The head word of this mention, as a token
   * @return The head word's token
   */
  public Sentence.Token headToken(){ return sentence.tokens.get(headWordIndex); }

  /**
   * A String reproduction of this mention
   * @return The gloss for this mention
   */
  public String gloss(){
    StringBuilder b = new StringBuilder();
    List<String> words = text();
    for(int i=0; i<words.size()-1; i++){
      b.append(words.get(i)).append(" ");
    }
    if(words.size() > 0){ b.append(words.get(words.size()-1)); }
    return b.toString();
  }

  /**
   * The length, in tokens, of this mention
   * @return the length of the mention
   */
  public int length(){
    return endIndexExclusive - beginIndexInclusive;
  }

  /**
   * Mark this mention as referring to an entity.
   * @param cluster The entity to refer to
   * @return The clustered mention associated with this assignment
   */
  public ClusteredMention markCoreferent(Entity cluster){
    if(corefferentWith != null && corefferentWith != cluster){ throw new IllegalArgumentException("Marking an entity as corefferent multiple times!"); }
    cluster.add(this);
    corefferentWith = cluster;
    return new ClusteredMention(this,cluster);
  }

  /**
   * Mark this mention as referring to an entity.
   * @param otherMention The other mention this is coreferent with
   * @return The clustered mention associated with this assignment
   */
  public ClusteredMention markCoreferent(ClusteredMention otherMention){
    return markCoreferent(otherMention.entity);
  }

  /**
   * Mark this mention as a singleton (for now, at least)
   * @return The clustered mention associated with this assignment
   */
  public ClusteredMention markSingleton(){
    if(doc.getMentions() == null){ throw new IllegalStateException("Cannot create cluster in a document without mentions stored"); }
    Entity cluster = new Entity(doc.getMentions());
    return markCoreferent(cluster);
  }

  @Override
  public boolean equals(Object o){
    if(o instanceof Mention){
      Mention other = (Mention) o;
      return other.sentence == this.sentence && other.beginIndexInclusive == this.beginIndexInclusive && other.endIndexExclusive == this.endIndexExclusive;
    } else {
      return false;
    }
  }
  @Override
  public int hashCode(){
    return this.sentence.hashCode() ^ beginIndexInclusive ^ (endIndexExclusive << 15);
  }
  @Override
  public String toString(){
    return gloss();
  }

  //--------------
  // SERIALIZATION
  //--------------
  private static final Pattern encodePattern = Pattern.compile(" *sid=([0-9]+) spanning ([0-9]+) until ([0-9]+) headed by ([0-9]+) parsed as (.*)$");
  public String encode() {
    //(find sentence)
    int sentenceIndex = 0;
    for(Sentence s : doc.sentences){
      if(s == sentence){
        break;
      } else {
        sentenceIndex += 1;
      }
    }
    //(build structure)
    StringBuilder b = new StringBuilder();
    b.append("sid=").append(sentenceIndex).append(" spanning ")
        .append(beginIndexInclusive).append(" until " ).append(endIndexExclusive)
        .append(" headed by ").append(headWordIndex)
        .append(" parsed as ").append(parse.encode());
    //(error check)
    if(!decode(b.toString(),doc).equals(this)){
      throw new IllegalStateException("Did not encode Mention properly: " + b.toString());
    }
    //(return)
     return b.toString();
  }

  public static Mention decode(String encoded, Document doc){
    Matcher m = encodePattern.matcher(encoded);
    if(!m.find()){ throw new IllegalStateException("Could not decode Mention: " + encoded); }
    int sentenceIndex = Integer.parseInt(m.group(1));
    int beginIndex = Integer.parseInt(m.group(2));
    int endIndex = Integer.parseInt(m.group(3));
    int head = Integer.parseInt(m.group(4));
    Tree<String> subParse = Tree.decode(m.group(5));
    return new Mention(doc, doc.sentences.get(sentenceIndex), beginIndex, endIndex, subParse, head);
  }
}
