package cs224n.corefsystems;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.Stack;

import cs224n.coref.Mention;
import cs224n.coref.Document;
import cs224n.coref.Sentence;
import cs224n.ling.Tree;
import cs224n.util.Pair;
import edu.stanford.nlp.util.IdentityHashSet;

public class Hobbs {
  public static void getHobbsCandidates(Mention pronoun) {
    Document doc = pronoun.doc;
    Sentence sentence = pronoun.sentence;
    
    Tree<String> parse = sentence.parse;
    
    Stack<Tree<String>> ancestors = new Stack<Tree<String>>();
    findMentionNP(pronoun, parse, ancestors);
    
    for (Tree<String> item: ancestors) {
      System.out.println(item.getLabel());
    }
    
    Set<Tree<String>> p = new IdentityHashSet<Tree<String>>();
    Tree<String> X = walkUpFindNPorS(ancestors, p);
    
    List<Tree<String>> candidates = leftRightBFS(X, p, true);
    
    while ((X = walkUpFindNPorS(ancestors, p)) != null) {
      if (X.getLabel().equals("NP")) {
        boolean passThroughDominatedNominal = false;
        
        for (Tree<String> child: candidates)
          if (p.contains(child)) {
            String label = child.getLabel();
            if (label.equals("NN") || label.equals("NNS") || label.equals("NNP") || label.equals("NNPS"))
              passThroughDominatedNominal = true;
          }
        
        if (!passThroughDominatedNominal)
          candidates.add(X);
      }
      
      candidates.addAll(leftRightBFS(X, p, false));
      
      if (X.getLabel().equals("S"))
        candidates.addAll(stepEight(X, p));
    }
    
    // go through previous sentences
    
    System.out.println();
    for (Tree<String> cand: candidates)
      System.out.println(cand.getYield());
    
  }
  
  public static void findMentionNP(Mention pronoun, Tree<String> parse, Stack<Tree<String>> path) {
    path.push(parse);
    findMentionNPInternal(0, pronoun.headWordIndex, parse, path); 
    
    while(!path.peek().getLabel().equals("NP"))
      path.pop();
  }
  
  public static void findMentionNPInternal(int currentIndex, int desiredIndex, Tree<String> tree, Stack<Tree<String>> path) {
    for (Tree<String> child: tree.getChildren()) {
      int yield = child.getYield().size();
      
      if (desiredIndex < currentIndex + yield) {
        path.push(child);
        findMentionNPInternal(currentIndex, desiredIndex, child, path);
        return;
      }
      
      currentIndex += yield;
    }
  }
  
  public static Tree<String> walkUpFindNPorS(Stack<Tree<String>> ancestors, Set<Tree<String>> p) {
    if (ancestors.size() < 2)
      return null;
    
    p.add(ancestors.pop());
    Tree<String> X = ancestors.pop();
    
    while (!(X.getLabel().equals("NP") || X.getLabel().equals("S"))) {
      p.add(X);
      
      if (ancestors.isEmpty())
        return null;
      X = ancestors.pop();
    }
    
    p.add(X);
    return X;
  }
  
  public static List<Tree<String>> leftRightBFS(Tree<String> start, Set<Tree<String>> blocker, boolean requireIntermediate) {
    Queue<Pair<Tree<String>, Boolean>> bfsQ = new LinkedList<Pair<Tree<String>, Boolean>>();
    List<Tree<String>> candidates = new ArrayList<Tree<String>>();
    
    for (Tree<String> child: start.getChildren()) {
      if (blocker.contains(child))
        break;
        
      bfsQ.add(new Pair<Tree<String>, Boolean>(child, !requireIntermediate));
    }
    
    while (!bfsQ.isEmpty()) {
      Pair<Tree<String>, Boolean> current = bfsQ.poll();
      
      Tree<String> tree = current.getFirst();
      boolean hasNPorSbetween = current.getSecond();
      
      if (tree.getLabel().equals("S") || tree.getLabel().equals("NP")) {
        if (hasNPorSbetween)
          candidates.add(tree);
        else
          hasNPorSbetween = true;
      }
      
      for (Tree<String> child: tree.getChildren()) {
        if (blocker.contains(child))
          break;
        
        bfsQ.add(new Pair<Tree<String>, Boolean>(child, hasNPorSbetween));
      }
    }
    
    return candidates;
  }
  
  public static List<Tree<String>> stepEight(Tree<String> start, Set<Tree<String>> blocker) {
    Queue<Tree<String>> bfsQ = new LinkedList<Tree<String>>();
    List<Tree<String>> candidates = new ArrayList<Tree<String>>();
    
    boolean onRight = false;
    for (Tree<String> child: start.getChildren()) {
      if (blocker.contains(child)) {
        onRight = true;
        continue;
      }
        
      if (onRight)
        bfsQ.add(child);
    }
    
    while (!bfsQ.isEmpty()) {
      Tree<String> tree = bfsQ.poll();
      
      if (tree.getLabel().equals("S") || tree.getLabel().equals("NP")) {
        if (tree.getLabel().equals("NP"))
          candidates.add(tree);
        
        continue;
      }
      
      for (Tree<String> child: tree.getChildren()) {
        bfsQ.add(child);
      }
    }
    
    return candidates;
  }
}
