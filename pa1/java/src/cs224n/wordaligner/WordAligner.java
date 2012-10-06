package cs224n.wordaligner;

import java.io.Serializable;
import java.util.List;


/**
 * Interface for generative word alignment models. Implement this interface so
 * that your model can be loaded by WordAlignmentTester.
 * 
 * @author Spence Green
 */
public interface WordAligner extends Serializable {

  /**
   * A convenient string for the NULL word (for words aligned to NULL)
   */
  public static final String NULL_WORD = "<NULL>";

  /**
   * Compute the best alignment for a given sentence pair. You can generate one-to-many 
   * alignments, that is one source word can be aligned to multiple target words. 
   * Put another way, assignments to alignment variables need not be unique.
   * 
   * IMPORTANT: Use the Alignment.addPredictedAlignment() method to add model 
   * predictions to the Alignment object.
   * 
   * @param sentencePair The sentence pair to align.
   * @return The best alignment according to your model for the given sentence pair.
   */
  public Alignment align(SentencePair sentencePair);

  /**
   * Learn the model parameters from the collection of parallel sentences.
   * 
   * @param trainingData The sentence pairs for training the aligner
   */
  public void train(List<SentencePair> trainingData);
}
