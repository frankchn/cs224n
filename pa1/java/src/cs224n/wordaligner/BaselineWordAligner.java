package cs224n.wordaligner;  

import cs224n.util.*;
import java.util.List;

/**
 * Simple word alignment baseline model that maps source positions to target 
 * positions along the diagonal of the alignment grid.
 * 
 * IMPORTANT: Make sure that you read the comments in the
 * cs224n.wordaligner.WordAligner interface.
 * 
 * @author Dan Klein
 * @author Spence Green
 */
public class BaselineWordAligner implements WordAligner {

  private static final long serialVersionUID = 1315751943476440515L;
  
  // TODO: Use arrays or Counters for collecting sufficient statistics
  // from the training data.
  private CounterMap<String,String> sourceTargetCounts;

  public Alignment align(SentencePair sentencePair) {
    // Placeholder code below. 
    // TODO Implement an inference algorithm for Eq.1 in the assignment
    // handout to predict alignments based on the counts you collected with train().
    Alignment alignment = new Alignment();
    int numSourceWords = sentencePair.getSourceWords().size();
    int numTargetWords = sentencePair.getTargetWords().size();

    for (int srcIndex = 0; srcIndex < numSourceWords; srcIndex++) {
      int tgtIndex = srcIndex;
      if (tgtIndex < numTargetWords) {
        // Discard null alignments
        alignment.addPredictedAlignment(tgtIndex, srcIndex);
      }
    }
    return alignment;
  }

  public void train(List<SentencePair> trainingPairs) {
    sourceTargetCounts = new CounterMap<String,String>();
    for(SentencePair pair : trainingPairs){
      List<String> targetWords = pair.getTargetWords();
      List<String> sourceWords = pair.getSourceWords();
      for(String source : sourceWords){
        for(String target : targetWords){
          // TODO: Warm-up. Your code here for collecting sufficient statistics.
          sourceTargetCounts.setCount(source, target, 1.0);
        }
      }
    }
  }
}
