package cs224n.corefsystems;

import cs224n.coref.Document;
import cs224n.coref.Entity;
import cs224n.coref.ClusteredMention;
import cs224n.util.Pair;

import java.util.Collection;
import java.util.List;

/**
 * The framework class for building a coreference system.
 *
 * The class has two methods, corresponding to the training and testing
 * portion of the system. If you are implementing a rule-based system,
 * you can ignore the train method; otherwise, that is where you should create
 * and train your classifier. In both cases, you should implement a test
 * method: runCoreference(). Note that runCoreference() will be run on both your
 * training and test set.
 *
 * @author Gabor Angeli (angeli at cs.stanford)
 */
public interface CoreferenceSystem {

  /**
   * This method is used to train your coreference system, if you are building a learning-based
   * approach.
   *
   * @param trainingData A collection of training instances.
   * Each training instance consists of a document (and its associated mentions via the
   * Document.getMentions() method), as well as a list of Entities representing the
   * gold coreference clusters.
   */
  public void train(Collection<Pair<Document, List<Entity>>> trainingData);

  /**
   * The method is used to run coreferencec on a given document.
   * You are not given other documents (nothing will be coreferent across
   * documents), nor the gold entities; your task is to assign every Mention in
   * the given document to some previously created Entity, or a singleton Entity.
   *
   * @param doc The document to run coreference on
   * @return A list of ClusteredMentions, corresponding to each mention in the document
   * assigned to some Entity.
   */
  public List<ClusteredMention> runCoreference(Document doc);
}
