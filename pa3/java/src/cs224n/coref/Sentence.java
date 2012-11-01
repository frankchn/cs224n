package cs224n.coref;

import cs224n.ling.Tree;
import cs224n.util.Decodable;
import cs224n.util.Pair;

import java.io.Serializable;
import java.util.*;

/**
 * Denotes a sentence. A document is a collection of sentences; each mention is
 * marked as part of both a document and a sentence.
 *
 * @author Gabor Angeli (angeli at cs.stanford)
 */
public class Sentence implements Serializable, Decodable {
  private static final long serialVersionUID = 1L;

  /**
   * A token of the sentence, encapsulating useful information
   * about a particular index in the sentence.
   */
  public class Token implements Serializable{
    private int index;
    private Token(int index){
      this.index = index;
    }
    // -- Getters --
    /**
     * The word associated with this token
     * @return A String word
     */
    public String word(){ return words.get(index); }

    /**
     * The lemma ( http://en.wikipedia.org/wiki/Lemma_(morphology) ) of a word
     * @return The String lemma
     */
    public String lemma(){ return lemmas.get(index); }

    /**
     * The part of speech tag of the token
     * @return The POS tag, as a String
     */
    public String posTag(){ return posTags.get(index); }

    /**
     * The Named Entity of the token
     * @return The NER tag, as a String
     */
    public String nerTag(){ return nerTags.get(index); }

    /**
     * The speaker of this token (for example, whether it is in quotes)
     * @return The speaker, as a String
     */
    public String speaker(){ return speakersOfWord.get(index); }

    // -- Utility Methods --
    /**
     * Whether this token is inside quotes
     * @return True if this token is inside quotes
     */
    public boolean isQuoted(){
      return !speaker().equals("") && !speaker().equals("PER0");
    }

    /**
     * Returns whether this token is a plural noun.
     * @return true if the token is a noun, and it is a plural noun
     */
    public boolean isPluralNoun(){
      return isNoun() && (posTag().equals("NNS") || posTag().equals("NNPS"));
    }

    /**
     * Returns whether this token is a proper noun.
     * @return true if the token is a noun, and it is a proper noun
     */
    public boolean isProperNoun(){
      return isNoun() && (posTag().equals("NNP") || posTag().equals("NNPS"));
    }

    /**
     * Returns whether this token is a noun.
     * @return true if the token is a noun
     */
    public boolean isNoun(){
      String tag = posTag();
      return tag.equals("NN") || tag.equals("NNS") || tag.equals("NNP") || tag.equals("NNPS");
    }
  }

  /**
   * The words in this sentence
   */
  public final List<String> words;
  /**
   * The lemmas ( http://en.wikipedia.org/wiki/Lemma_(morphology) ) in this sentence
   */
  public final List<String> lemmas;
  /**
   * The Part of speech tags of this sentence
   */
  public final List<String> posTags;
  /**
   * The Named Entity tags of this sentence
   */
  public final List<String> nerTags;
  /**
   * The speaker tags of this sentence (e.g. for quoted text)
   */
  public final List<String> speakersOfWord;
  /**
   * The syntactic constituency parse of the sentence
   */
  public final Tree<String> parse;

  /**
   * The tokens of the sentence
   */
  public final List<Token> tokens;

  public Sentence(List<String> words,
                  List<String> lemmas,
                  List<String> posTags,
                  List<String> namedEntities,
                  List<String> speakers,
                  Tree<String> parse){
    //--Error Checks
    int length = words.size();
    if(lemmas.size() != length){ throw new IllegalArgumentException("Lemma size doesn't agree: " + lemmas.size()); }
    if(posTags.size() != length){ throw new IllegalArgumentException("POS tags size doesn't agree: " + posTags.size()); }
    if(namedEntities.size() != length){ throw new IllegalArgumentException("NER size doesn't agree: " + namedEntities.size()); }
    if(speakers.size() != length){ throw new IllegalArgumentException("Speakers size doesn't agree: " + speakers.size()); }
    //--Copy Variables
    this.words = words;
    this.lemmas = lemmas;
    this.posTags = posTags;
    this.nerTags = namedEntities;
    this.speakersOfWord = speakers;
    this.parse = parse;
    //--Create Tokens
    this.tokens = new ArrayList<Token>();
    for(int i=0; i<words.size(); i++){
      tokens.add(new Token(i));
    }
  }

  /**
   * The length of the sentence
   * @return The length of the sentence
   */
  public int length(){ return words.size(); }
  public String gloss(){
    StringBuilder b = new StringBuilder();
    for(int i=0; i<words.size()-1; i++){
      b.append(words.get(i)).append(" ");
    }
    if(words.size() > 0){ b.append(words.get(words.size()-1)); }
    return b.toString();
  }

  /**
   * Print this sentence nicely
   * @param b The builder to append to
   * @param mentions The mentions that might occur in the sentence
   * @param entities The entities that should be printed
   * @return The string builder passed as an argument with the sentence appended
   */
  public StringBuilder prettyPrint(StringBuilder b, List<Mention> mentions, Collection<Entity> entities) {
    //--Relevant Mentions
    Map<Integer,Pair<Integer,Pair<Mention,Entity>>> mentionLocations = new HashMap<Integer,Pair<Integer,Pair<Mention,Entity>>>();
    for(Mention m : mentions){
      //(if it's relevant to this sentence)
      if(m.sentence == this){
        //(find corresponding entity)
        Entity correspondingEntity = null;
        for(Entity e : entities){
          if(e.mentions.contains(m)){
            correspondingEntity = e;
            break;
          }
        }
        //(store in structure)
        mentionLocations.put(m.beginIndexInclusive,Pair.make(m.endIndexExclusive,Pair.make(m,correspondingEntity)));
      }
    }
    //--Format
    for(int i=0; i<words.size(); i++){
      if(mentionLocations.containsKey(i)){
        //(get info)
        Pair<Integer, Pair<Mention, Entity>> info = mentionLocations.get(i);
        int endLocation = info.getFirst();
        Mention mention = info.getSecond().getFirst();
        Entity entity = info.getSecond().getSecond();
        //(print info)
        if(entity == null){
          b.append(words.get(i)).append(" ");
        } else {
          entity.format(b,mention);
          b.append(" ");
          i = endLocation-1;
        }
      } else {
        b.append(words.get(i)).append(" ");
      }
    }
    //--Return
    return b;
  }

  //no equals() and hashcode() on purpose
  @Override
  public String toString(){ return gloss(); }

  //--------------
  // SERIALIZATION
  //--------------
  private static final String DIV = "" + (char) 0x04;

  public String encode() {
    //--Variables
    StringBuilder b = new StringBuilder();
    //--Save Words
    for(int i=0; i<length(); i++){
      //(error checks)
      if(words.get(i).contains(DIV)){ throw new IllegalStateException("Token contains term divisor: " + words.get(i)); }
      if(lemmas.get(i).contains(DIV)){ throw new IllegalStateException("Token contains term divisor: " + lemmas.get(i)); }
      if(posTags.get(i).contains(DIV)){ throw new IllegalStateException("Token contains term divisor: " + posTags.get(i)); }
      if(nerTags.get(i).contains(DIV)){ throw new IllegalStateException("Token contains term divisor: " + nerTags.get(i)); }
      if(speakersOfWord.get(i).contains(DIV)){ throw new IllegalStateException("Token contains term divisor: " + speakersOfWord.get(i)); }
      //(record word)
      b.append(words.get(i)).append(DIV)
          .append(lemmas.get(i)).append(DIV)
          .append(posTags.get(i)).append(DIV)
          .append(nerTags.get(i)).append(DIV)
          .append(speakersOfWord.get(i)).append(DIV);
    }
    //--Save Parse
    if(parse.toString().contains(DIV)){ throw new IllegalStateException("Parse contains term divisor: " + parse); }
    b.append(parse.encode());
    return b.toString();
  }

  public static Sentence decode(String encoded) {
    //--Get Terms
    String[] terms = encoded.trim().split(DIV);
    if(terms.length % 5 != 1 || terms.length == 0){
      throw new IllegalStateException("Cannot decode sentence ("+terms.length+"): " + encoded);
    }
    //--Parse Terms
    //(variables)
    List<String> words = new ArrayList<String>(terms.length/5);
    List<String> lemmas = new ArrayList<String>(terms.length/5);
    List<String> posTags = new ArrayList<String>(terms.length/5);
    List<String> nerTags = new ArrayList<String>(terms.length/5);
    List<String> speakersOfWord = new ArrayList<String>(terms.length/5);
    //(words)
    for(int i=0; i<terms.length/5; i++){
      words.add(terms[5*i+0].trim());
      lemmas.add(terms[5*i+1].trim());
      posTags.add(terms[5*i+2].trim());
      nerTags.add(terms[5 * i + 3].trim());
      speakersOfWord.add(terms[5 * i + 4].trim());

    }
    //(parse)
    Tree<String> parse = Tree.decode(terms[terms.length-1]);
    //--Return
    return new Sentence(words,lemmas,posTags,nerTags,speakersOfWord,parse);
  }
}
