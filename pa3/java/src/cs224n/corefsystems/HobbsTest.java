package cs224n.corefsystems;

import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import cs224n.coref.Document;
import cs224n.coref.Mention;
import cs224n.coref.Sentence;
import cs224n.ling.Tree;
import cs224n.ling.Trees;
import cs224n.ling.Trees.PennTreeReader;
import cs224n.ling.Trees.TreeReader;

public class HobbsTest {

  /**
   * @param args
   */
  public static void main(String[] args) {
    String input = "(S  (NP (DT The) (NN castle)) (VP (VB was) (NP (DT the) (NP (NN home) (PP (PRP of) (NNP Arthur))))) (PP (IN until) (NP (NN 536) (SH (WHB when) (S  (NP (PRP he)) (VP (VBD moved) (NP (PRP it))))))))";
    input += input;
    Reader inputReader = new StringReader(input);
    
    PennTreeReader treeReader = new Trees.PennTreeReader(inputReader);
    Tree<String> tree = treeReader.next();
    Tree<String> tree2 = treeReader.next();
    
    List<String> words = tree.getYield();
    
    Sentence sentence = new Sentence(words, words, tree.getPreTerminalYield(), words, words, tree);
    
    words = tree2.getYield();
    Sentence sentence2 = new Sentence(words, words, tree2.getPreTerminalYield(), words, words, tree2);
    List<Sentence> sentences = new ArrayList<Sentence>();
    sentences.add(sentence2);
    sentences.add(sentence);
    Document doc = new Document("test1", sentences);
    
    Mention pronoun = new Mention(doc, sentence, 12, 13, tree, 12);
    
    Hobbs.getHobbsCandidates(pronoun);
  }

}
