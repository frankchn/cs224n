package cs224n.coref;

import cs224n.ling.Tree;
import edu.stanford.nlp.dcoref.CorefCluster;
import edu.stanford.nlp.dcoref.Document;
import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.trees.TreeCoreAnnotations;
import edu.stanford.nlp.util.CollectionValuedMap;
import edu.stanford.nlp.util.CoreMap;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * @author Gabor Angeli (angeli at cs.stanford)
 */
public class CoreNLPDocument implements Serializable {
  public final Document doc;

  public final String documentIdPart;
  public final List<CoreMap> nerChunks;
  public final String documentID;
  public final String partNo;
  public final List<List<String[]>> sentenceWordLists;

  public final Annotation annotation;
  public final CollectionValuedMap<String,CoreMap> corefChainMap;

  private <E> E getByReflection(String fieldName){
    try {
      Field f = this.doc.conllDoc.getClass().getDeclaredField(fieldName);
      f.setAccessible(true);
      //noinspection unchecked
      return (E) f.get(this.doc.conllDoc);
    } catch (IllegalAccessException e) {
      throw new RuntimeException(e);
    } catch (NoSuchFieldException e) {
      throw new RuntimeException(e);
    }
  }

  public CoreNLPDocument(edu.stanford.nlp.dcoref.Document doc){
    this.doc = doc;
    //(copy CoNLL document)
    this.documentID = doc.conllDoc.getDocumentID();
    this.partNo = doc.conllDoc.getPartNo();
    this.sentenceWordLists = doc.conllDoc.getSentenceWordLists();
    this.annotation = doc.conllDoc.getAnnotation();
    this.corefChainMap = doc.conllDoc.getCorefChainMap();
    this.documentIdPart = getByReflection("documentIdPart"); //don't exist yet maybe?
    this.nerChunks = getByReflection("nerChunks");
    //(clear CoNLL document)
    doc.conllDoc = null;  //so we can serialize
  }

  private static Tree<String> convertTree(edu.stanford.nlp.trees.Tree coreTree){
    Tree<String> tree = new Tree<String>(coreTree.label().value());
    if(coreTree.isLeaf()){
      //(case: leaf node)
      return tree;
    } else {
      //(case: non-terminal)
      //((convert children))
      List<Tree<String>> children = new ArrayList<Tree<String>>();
      for(edu.stanford.nlp.trees.Tree coreChild : coreTree.children()){
        children.add(convertTree(coreChild));
      }
      //((set children))
      tree.setChildren(children);
      //((return))
      return tree;
    }
  }

  private static Tree<String> getTreeInSpan(Tree<String> tree, int beginInclusive, int endExclusive, int posSoFar){
    if(posSoFar < beginInclusive){
      //--Case: Span to the Right
      int increment = 0;
      for(Tree<String> child : tree.getChildren()){
        int delta = child.getYield().size();
        if(posSoFar+increment+delta <= beginInclusive){
          //(case: still to the right)
          increment += delta;
        } else {
          //(case: span somewhere in this tree)
          Tree<String> cand = getTreeInSpan(child, beginInclusive, endExclusive, posSoFar+increment);
          return cand == null ? tree : cand;
        }
      }
      throw new IllegalArgumentException("Should not reach here");
    } else if(posSoFar == beginInclusive){
      int length = tree.getYield().size();
      if(beginInclusive+length == endExclusive){
        //(case: exact match)
        return tree;
      } else if(beginInclusive+length < endExclusive){
        //(case: no match found)
        throw new IllegalArgumentException("Phrase doesn't align to parse boundary: " + tree); //TODO this could happen
      } else {
        //(case: begin ok; end too far to the right)
        Tree<String> cand = getTreeInSpan(tree.getChildren().get(0), beginInclusive, endExclusive, posSoFar);
        return cand == null ? tree : cand;
      }
    } else {
      throw new IllegalArgumentException("Should not reach here either");
    }
  }
  private static Tree<String> getTreeInSpan(Tree<String> tree, int beginInclusive, int endExclusive){
    return getTreeInSpan(tree, beginInclusive, endExclusive,0);
  }

  private static Mention convertMention(edu.stanford.nlp.dcoref.Mention coreMention, cs224n.coref.Document doc, Sentence sent){
    //--Convert Mention
    //(variables)
    int beginInclusive = coreMention.startIndex;
    int endExclusive = coreMention.endIndex;
    //(parse)
    Tree<String> parse;
    if(coreMention.mentionSubTree != null){
      parse = convertTree(coreMention.mentionSubTree);
    } else {
      parse = getTreeInSpan(sent.parse, beginInclusive, endExclusive);
    }
    //(head word)
    int headWordIndex = coreMention.headIndex;
    //--Create Mention
    return new Mention(
        doc,
        sent,
        beginInclusive,
        endExclusive,
        parse,
        headWordIndex
      );
  }

  private Entity convertCluster(CorefCluster coreCluster, List<Mention> goldMentions, cs224n.coref.Document doc){
    //--Variables
    Set<edu.stanford.nlp.dcoref.Mention> coreMentions = coreCluster.getCorefMentions();
    Entity cluster = new Entity(goldMentions);
    int coreSize = coreMentions.size();
    if(coreSize == 0){ throw new IllegalStateException("Cluster has no elements!"); }
    //--Convert Mentions
    for(edu.stanford.nlp.dcoref.Mention coreMention : coreMentions){
      //(find mention) //efficiency? never heard of 'er.
      //((find sentence))
      int sentIndex = -1;
      for(int i=0; i<this.doc.goldOrderedMentionsBySentence.size(); i++){
        for(edu.stanford.nlp.dcoref.Mention coreCandMention : this.doc.goldOrderedMentionsBySentence.get(i)){
          if(coreCandMention == coreMention){
            sentIndex = i;
          }
        }
      }
      //((match))
      Mention matchedMention = null;
      for(Mention goldMention : goldMentions){
        if(goldMention.sentence == doc.sentences.get(sentIndex) && goldMention.beginIndexInclusive == coreMention.startIndex && goldMention.endIndexExclusive == coreMention.endIndex){
          if(matchedMention != null){ throw new IllegalArgumentException("Duplicate mentions"); }
          matchedMention = goldMention;
        }
      }
      //(add mention)
      if(matchedMention == null){
        throw new IllegalArgumentException("Could not match mention: " + coreMention);
      }
      cluster.add(matchedMention);
    }
    //--Return
    if(cluster.size() != coreSize){ throw new IllegalStateException("Cluster size changed from " + coreSize + " to " + cluster.size()); }
    return cluster;
  }

  public cs224n.coref.Document mkDocument(){
    //--Create Sentences
    List<Sentence> sentences = new ArrayList<Sentence>();
    //(for each sentence...)
    for(CoreMap coreSent : this.annotation.get(CoreAnnotations.SentencesAnnotation.class)){
      //(variables of interest)
      List<String> words = new ArrayList<String>();
      List<String> lemmas = new ArrayList<String>();
      List<String> posTags = new ArrayList<String>();
      List<String> nerTags = new ArrayList<String>();
      List<String> speakers = new ArrayList<String>();
      //(for each word...)
      for(CoreLabel token : coreSent.get(CoreAnnotations.TokensAnnotation.class)){
        words.add(token.word());
        posTags.add(token.tag());
        lemmas.add(token.lemma());
        nerTags.add(token.ner());
        speakers.add(token.get(CoreAnnotations.SpeakerAnnotation.class));
      }
      //(get tree)
      edu.stanford.nlp.trees.Tree coreTree = coreSent.get(TreeCoreAnnotations.TreeAnnotation.class);
      //(convert tree)
      Tree<String> parse = convertTree(coreTree);
      //(create sentence)
      Sentence sent = new Sentence(
          words,
          lemmas,
          posTags,
          nerTags,
          speakers,
          parse
        );
      //(add sentence)
      sentences.add(sent);
    }
    //--Return
    return new cs224n.coref.Document(this.annotation.get(CoreAnnotations.DocIDAnnotation.class),sentences);
  }

  public List<Mention> mkGoldMentions(cs224n.coref.Document doc){
    List<Mention> goldMentions = new ArrayList<Mention>();
    for(int sent=0; sent<this.doc.goldOrderedMentionsBySentence.size(); sent++){
      List<edu.stanford.nlp.dcoref.Mention> coreMentionsInSentence = this.doc.goldOrderedMentionsBySentence.get(sent);
      for(edu.stanford.nlp.dcoref.Mention coreMention : coreMentionsInSentence){
        goldMentions.add(convertMention(coreMention, doc, doc.sentences.get(sent)));
      }
    }
    return goldMentions;
  }

  public List<Mention> mkPredictedMentions(cs224n.coref.Document doc){
    List<Mention> predictedMentions = new ArrayList<Mention>();
    for(int sent=0; sent<this.doc.predictedOrderedMentionsBySentence.size(); sent++){
      List<edu.stanford.nlp.dcoref.Mention> coreMentionsInSentence = this.doc.predictedOrderedMentionsBySentence.get(sent);
      for(edu.stanford.nlp.dcoref.Mention coreMention : coreMentionsInSentence){
        predictedMentions.add(convertMention(coreMention, doc, doc.sentences.get(sent)));
      }
    }
    return predictedMentions;
  }

  public List<Entity> mkGold(cs224n.coref.Document doc, List<Mention> goldMentions){
    List<Entity> gold = new ArrayList<Entity>();
    for(int key : this.doc.goldCorefClusters.keySet()){
      gold.add(convertCluster(this.doc.goldCorefClusters.get(key), goldMentions, doc));
    }
    return gold;
  }

}
