package cs224n.ling;

import cs224n.util.Decodable;
import cs224n.util.Pair;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * Represent linguistic trees, with each node consisting of a label
 * and a list of children.
 * @author Dan Klein
 * @author Gabor Angeli (custom serialization; equals() and hashCode())
 */
public class Tree<L> implements Serializable, Decodable {
  private static int nextUniqueIndex = 0;

  private L label;
  private List<Tree<L>> children;
  private int uniqueIndex = nextUniqueIndex++;


  public List<Tree<L>> getChildren() {
    return children;
  }
  public void setChildren(List<Tree<L>> children) {
    this.children = children;
  }
  public L getLabel() {
    return label;
  }
  public void setLabel(L label) {
    this.label = label;
  }

  /* Returns true at the word(leaf) level of a tree */
  public boolean isLeaf() {
    return getChildren().isEmpty();
  }

  /* Returns true level of non-terminals which are directly above
   * single words(leafs) */
  public boolean isPreTerminal() {
    return getChildren().size() == 1 && getChildren().get(0).isLeaf();
  }

  public boolean isPhrasal() {
    return ! (isLeaf() || isPreTerminal());
  }

  /* Returns a list of words at the leafs of this tree gotten by
   * traversing from left to right */
  public List<L> getYield() {
    List<L> yield = new ArrayList<L>();
    appendYield(this, yield);
    return yield;
  }

  private static <L> void appendYield(Tree<L> tree, List<L> yield) {
    if (tree.isLeaf()) {
      yield.add(tree.getLabel());
      return;
    }
    for (Tree<L> child : tree.getChildren()) {
      appendYield(child, yield);
    }
  }

  /* Returns a list of the preterminals gotten by traversing from left
   * to right.  This is effectively an POS tagging for the words that
   * tree represents. */
  public List<L> getPreTerminalYield() {
    List<L> yield = new ArrayList<L>();
    appendPreTerminalYield(this, yield);
    return yield;
  }

  private static <L> void appendPreTerminalYield(Tree<L> tree, 
                                                 List<L> yield) {
    if (tree.isPreTerminal()) {
      yield.add(tree.getLabel());
      return;
    }
    for (Tree<L> child : tree.getChildren()) {
      appendPreTerminalYield(child, yield);
    }
  }

  /* Returns a list of the node values gotten by traversing in this
   * order: root, left subtree, right subtree */
  public List<Tree<L>> getPreOrderTraversal() { 
    ArrayList<Tree<L>> traversal = new ArrayList <Tree<L>>();
    traversalHelper(this, traversal, true); 
    return traversal; 
  } 

  /* Returns a list of the node values gotten by traversing in this
   * order: left subtree, right subtree, root */
  public List<Tree<L>> getPostOrderTraversal() {
    ArrayList<Tree<L>> traversal = new ArrayList<Tree<L>>();
    traversalHelper(this, traversal, false);
    return traversal;
  }

  private static <L> void traversalHelper(Tree<L> tree, List<Tree<L>> traversal, 
                                          boolean preOrder) {
    if (preOrder)
      traversal.add(tree);
    for (Tree<L> child : tree.getChildren()) {
      traversalHelper(child, traversal, preOrder);
    }
    if (! preOrder)
      traversal.add(tree);
  }

  /* Set the words at the leaves of a tree to the words from the
   * list */
  public void setWords(List<L> words) {
    setWordsHelper(words, 0);
  }

  private int setWordsHelper(List<L> words, int wordNum) {
    if (isLeaf()) {
      label = words.get(wordNum);
      return wordNum + 1;
    }
    else {
      for (Tree<L> child : getChildren())
        wordNum = child.setWordsHelper(words, wordNum);
      return wordNum;
    }
  }

    
  public List<Tree<L>> toSubTreeList() {
    return getPreOrderTraversal();
  }

  /* Creates a list of all constituents in this tree.  A constituent
   * is just a non-terminal label and that non-terminal covers in the
   * tree. */
  public List<Constituent<L>> toConstituentList() {
    List<Constituent<L>> constituentList = new ArrayList<Constituent<L>>();
    toConstituentCollectionHelper(this, 0, constituentList);
    return constituentList;
  }

  private static <L> int toConstituentCollectionHelper(Tree<L> tree, int start, 
                                                       List<Constituent<L>> constituents) {
    if (tree.isLeaf() || tree.isPreTerminal())
      return 1;
    int span = 0;
    for (Tree<L> child : tree.getChildren()) {
      span += toConstituentCollectionHelper(child, start+span, constituents);
    }
    constituents.add(new Constituent<L>(tree.getLabel(), start, start + span));
    return span;
  }

  /**
   * Returns a traversal of the tree between a start index and an end index.
   * @param startIndex The index of the word to start at
   * @param stopIndex The index of the word to end at
   * @return A list of pairs, where the first element denotes the nonterminal being
   * traversed, and the second element is one of [-1,0,1} denoting whether we are moving
   * up, at the relative root, or moving down in the tree
   */
  public Iterable<Pair<L,Integer>> getTraversalBetween(int startIndex, int stopIndex) {
    //--Get Paths
    LinkedList<Pair<L,Integer>> leftPath = pathToIndex(startIndex);
    LinkedList<Pair<L,Integer>> rightPath = pathToIndex(stopIndex);
    //--Remove Common Nodes
    L lastRemoved = null;
    while(leftPath.getFirst().getSecond().equals(rightPath.getFirst().getSecond())){
      lastRemoved = leftPath.getFirst().getFirst();
      leftPath.removeFirst();
      rightPath.removeFirst();
    }
    //--Construct Path
    LinkedList<Pair<L,Integer>> path = new LinkedList<Pair<L,Integer>>();
    //(add left path to the front)
    for(Pair<L, Integer> left : leftPath){
      path.addFirst(Pair.make(left.getFirst(), -1));
    }
    //(add common root)
    path.addLast(Pair.make(lastRemoved, 0));
    //(add right path to the back)
    for(Pair<L, Integer> right : rightPath){
      path.addLast(Pair.make(right.getFirst(), 1));
    }
    //--Return
    return path;
  }

  /**
   * Returns a path from the ROOT node of the tree to the leaf at the given index
   * @param index The index of the leaf to search for
   * @return A list of pairs, where the first element of the pair is the nonterminal,
   * and the second element is the unique index of the tree node
   */
  public LinkedList<Pair<L,Integer>> pathToIndex(int index){
    //--Base Case
    if(this.isLeaf()){
      return new LinkedList<Pair<L,Integer>>();
    }
    //--Recursive Case
    //(get children)
    List<Tree<L>> children = this.getChildren();
    //(get child with relevant span)
    int yieldSoFar = 0;
    int childIndex = 0;
    int lastYield = 0;
    while(yieldSoFar <= index){
      lastYield = children.get(childIndex).getYield().size();
      yieldSoFar += lastYield;
      childIndex += 1;
    }
    //(move back one step)
    childIndex -= 1;
    yieldSoFar -= lastYield;
    //(get rest of path)
    LinkedList<Pair<L,Integer>> restOfPath = children.get(childIndex).pathToIndex(index - yieldSoFar);
    //(add this node)
    restOfPath.addFirst(Pair.make(this.label, this.uniqueIndex));
    //(return)
    return restOfPath;
  }

  /* Returns a string representation of this tree using bracket
   * notation. */
  public String toString() {
    StringBuilder sb = new StringBuilder();
    toStringBuilder(sb);
    return sb.toString();
  }

  public void toStringBuilder(StringBuilder sb) {
    if (! isLeaf()) sb.append('(');
    if (getLabel() != null) {
      sb.append(getLabel());
    }
    if (! isLeaf()) {
      for (Tree<L> child : getChildren()) {
        sb.append(' ');
        child.toStringBuilder(sb);
      }
      sb.append(')');
    }
  }

  public Tree<L> deepCopy() {
    return deepCopy(this);
  }

  private static <L> Tree<L> deepCopy(Tree<L> tree) {
    List<Tree<L>> childrenCopies = new ArrayList<Tree<L>>();
    for (Tree<L> child : tree.getChildren()) {
      childrenCopies.add(deepCopy(child));
    }
    return new Tree<L>(tree.getLabel(), childrenCopies);
  }

  @SuppressWarnings({"unchecked"})
  public boolean equals(Object o){
    if(o instanceof Tree) {
      Tree<L> other = (Tree<L>) o;
      if(!other.getLabel().equals(this.getLabel())){ return false; }
      if(other.children.size() != this.children.size()){ return false; }
      for(int i=0; i<children.size(); i++){
        if(!other.children.get(i).equals(this.children.get(i))){ return false; }
      }
      return true;
    } else {
      return false;
    }
  }

  public int hashCode(){
    return label.hashCode() ^ (children.size()<<16);
  }

  /* The leaf constructor. */
  public Tree(L label, List<Tree<L>> children) {
    this.label = label;
    this.children = children;
  }

  public Tree(L label) {
    this.label = label;
    this.children = Collections.emptyList();
  }

  //--------------
  // SERIALIZATION
  //--------------
  public String encode() {
    //(error check)
    Tree<String> decoded = decode(toString());
    if(!decoded.equals(this)){
      throw new IllegalStateException("Did not encode tree properly");
    }
    //(return)
    return toString();
  }

  private static Pair<ArrayList<Tree<String>>,Integer> decodeChildren(char[] encoded, int pos, int depth){
    ArrayList<Tree<String>> children = new ArrayList<Tree<String>>();
    //(beginning whitespace)
    while(encoded[pos] == ' '){ pos +=1; }
    //(get children)
    while(pos < encoded.length && encoded[pos] != ')'){
      Pair<Tree<String>, Integer> childPair = decodeTree(encoded, pos, depth);
      children.add(childPair.getFirst());
      pos = childPair.getSecond();
    }
    //(return)
    pos += 1;
    return Pair.make(children,pos);
  }
  private static Pair<Tree<String>,Integer> decodeTree(char[] encoded, int pos, int depth){
    //(beginning whitespace)
    while(encoded[pos] == ' '){ pos +=1; }
    //(skip open paren)
    if(encoded[pos] == '('){ pos += 1; }
    //(get head)
    StringBuilder head = new StringBuilder();
    while(encoded[pos] != ' ' && encoded[pos] != ')'){
      head.append(encoded[pos]);
      pos += 1;
    }
    //(get children)
    if(pos >= encoded.length || encoded[pos] == ')'){
      return Pair.make(new Tree<String>(head.toString()),pos);
    } else {
      Pair<ArrayList<Tree<String>>, Integer> childrenPair = decodeChildren(encoded, pos, depth+1);
      return Pair.make(new Tree<String>(head.toString(),childrenPair.getFirst()),childrenPair.getSecond());
    }
  }
  public static Tree<String> decode(String encoded){
    if(!encoded.contains("(") && !encoded.contains(")")){
      return new Tree<String>(encoded);
    } else {
      return decodeTree(encoded.toCharArray(), 0, 0).getFirst();
    }
  }
}
