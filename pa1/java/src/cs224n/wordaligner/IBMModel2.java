package cs224n.wordaligner;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import cs224n.util.Counter;
import cs224n.util.CounterMap;
import cs224n.util.Counters;

/**
 * 
 * Implements IBM Model 2
 * 
 * @author Daniel Chia
 * @author Frank Chen
 */

public class IBMModel2 implements WordAligner {
  private static final long serialVersionUID = 1974871312762799978L;
  
  public static final double CONVERGENCE_TOLERANCE = 1e-7;
  public static final int MAX_ITERATIONS = 80;
  
  // counters to keep track of statistics
  // source is key, target are the values
  // corresponds to t(e|f)
  CounterMap<String, String> probTargetGivenSource = null;
  // q(i|j,l,m).
  // key is -> (j,l,m), value is i
  CounterMap<IntegerTriple, Integer> probAlignment = null;

  @Override
  public Alignment align(SentencePair sentencePair) {
    Alignment alignment = new Alignment();
    
    List<String> targetWords = sentencePair.getTargetWords();
    for (int targetWordIndex = 0; targetWordIndex < targetWords.size(); targetWordIndex++) {
      String targetWord = targetWords.get(targetWordIndex);
      List<String> sourceWords = sentencePair.getSourceWords();
      
      double maxProb = -1.0;
      int maxProbSourceWordIndex = 0;
      
      for (int sourceWordIndex = 0; sourceWordIndex <= sourceWords.size(); sourceWordIndex++) {
        // extract source word (being careful of null)
        String sourceWord = (sourceWordIndex < sourceWords.size()) ? 
                            sourceWords.get(sourceWordIndex) :
                            WordAligner.NULL_WORD;
        
        IntegerTriple currentJLM = new IntegerTriple(targetWordIndex, sourceWords.size(), targetWords.size());
        double prob = probAlignment.getCount(currentJLM, sourceWordIndex) * probTargetGivenSource.getCount(sourceWord, targetWord);
        if (prob > maxProb) {
          maxProb = prob;
          maxProbSourceWordIndex = sourceWordIndex;
        }
      }
        
      if (maxProbSourceWordIndex != sourceWords.size())  // skip NULL words
        alignment.addPredictedAlignment(targetWordIndex, maxProbSourceWordIndex);
    }
    
    return alignment;
  }

  @Override
  public void train(List<SentencePair> trainingData) {
    // learn an IBM Model 1 first to get initial probabilities
    // note that IBM Model 1 appends NULL word to the trainingData in train()
    IBMModel1 model1 = new IBMModel1();
    model1.train(trainingData);
    
    
    System.out.println("Training IBM Model 2..");
    
    // seed with model 1 data
    probTargetGivenSource = model1.getProbTargetGivenSource();
    
    // init q(i|j,l,m) to be random
    Random random = new Random();
    probAlignment = new CounterMap<IntegerTriple, Integer>();
    //for (SentencePair sentencePair: trainingData) {
    //  List<String> sourceWords = sentencePair.getSourceWords();
    //  List<String> targetWords = sentencePair.getTargetWords();
    //  
    //  int sourceSentenceLength = sourceWords.size() - 1;
    //  IntegerTriple triple = new IntegerTriple(0, sourceSentenceLength, targetWords.size());
    //  for (int j = 0; j < targetWords.size(); j++) {
    //    triple.x = j;
    //    
    //    for (int i = 0; i <= sourceSentenceLength; i++)
    //      probAlignment.setCount(triple, i, random.nextDouble() + 0.1);
    //  }
    //}
    //probAlignment = Counters.conditionalNormalize(probAlignment);   // normalize
    
    
    for (SentencePair sentencePair: trainingData) {
      List<String> targetWords = sentencePair.getTargetWords();
      List<String> sourceWords = sentencePair.getSourceWords();
      int targetSentenceLength = targetWords.size();
      int sourceSentenceLength = sourceWords.size();
        
      for (int j = 0; j < targetSentenceLength; j++) {
        // p(I|j,l,m)
        Counter<Integer> conditionalProb =
            probAlignment.getCounter(new IntegerTriple(j, sourceSentenceLength,
                targetSentenceLength));
          
        for (int i = 0; i <= sourceSentenceLength; i++) {
          conditionalProb.setCount(i, random.nextDouble() + 0.1);
        }
      }
    }
    
    probAlignment = Counters.conditionalNormalize(probAlignment);   // normalize
    
    
    double deviation;
    int numEntries = 0;
    int iteration = 0;
    
    do {
      //
      // perform EM
      //
      
      // E-step
      
      // source is key, target is value
      CounterMap<String, String> countTargetSource = new CounterMap<String,String>();
      CounterMap<IntegerTriple, Integer> countAlignment = new CounterMap<IntegerTriple, Integer>();
      
      for (SentencePair sentencePair: trainingData) {
        List<String> targetWords = sentencePair.getTargetWords();
        List<String> sourceWords = sentencePair.getSourceWords();
        int targetSentenceLength = targetWords.size();
        int sourceSentenceLength = sourceWords.size();
        
        // precalculate sum_i q(j| i,l^(k),m^(k)) t(e^(k)|f^(k)_i)
        // key is target word, value is j (target word index)
        Counter<Integer> precalcDenominator = new Counter<Integer>();
        
        for (int j = 0; j < targetSentenceLength; j++) {
          String targetWord = targetWords.get(j);
          // p(I|j,l,m)
          Counter<Integer> conditionalProb =
              probAlignment.getCounter(new IntegerTriple(j, sourceSentenceLength,
                  targetSentenceLength));
          
          double sum = 0.0;
          
          for (int i = 0; i <= sourceSentenceLength; i++) {
            String sourceWord = (i < sourceSentenceLength) ? sourceWords.get(i) : WordAligner.NULL_WORD;
            sum += conditionalProb.getCount(i) * probTargetGivenSource.getCount(sourceWord, targetWord);
          }
          
          precalcDenominator.setCount(j, sum);
        }
        
        for (int j = 0; j < targetSentenceLength; j++) {
          String targetWord = targetWords.get(j);
          IntegerTriple currentJLM = new IntegerTriple(j, sourceSentenceLength, targetSentenceLength);
          // p(I|j,l,m)
          Counter<Integer> conditionalProb = probAlignment.getCounter(currentJLM);
          
          for (int i = 0; i <= sourceSentenceLength; i++) {
            String sourceWord = (i < sourceSentenceLength) ? sourceWords.get(i) : WordAligner.NULL_WORD;
            double updateNumerator = conditionalProb.getCount(i) * probTargetGivenSource.getCount(sourceWord, targetWord);
            double updateDenominator = precalcDenominator.getCount(j);
            double update = updateNumerator / updateDenominator;
            
            countTargetSource.incrementCount(sourceWord, targetWord, update);
            countAlignment.incrementCount(currentJLM, i, update);
          }
        }
      }
      
      // M-step
      // normalize to give new probs
      CounterMap<String, String> newProbTargetGivenSource = Counters.conditionalNormalize(countTargetSource);
      CounterMap<IntegerTriple, Integer> newProbAlignment = Counters.conditionalNormalize(countAlignment);
      
      // calculate convergence criteria
      // based on sum (oldprob - newprob)^2 over all entries in the p(e|f) table
      deviation = 0.0;
      numEntries = 0;
      for (String sourceWord: probTargetGivenSource.keySet())
        for (String targetWord: probTargetGivenSource.getCounter(sourceWord).keySet()) {
          double difference = probTargetGivenSource.getCount(sourceWord, targetWord) -
                              newProbTargetGivenSource.getCount(sourceWord, targetWord);
          deviation += difference * difference;
          numEntries++;
        }
      
      for (IntegerTriple jlm: probAlignment.keySet())
        for (Integer index: probAlignment.getCounter(jlm).keySet()) {
          double difference = probAlignment.getCount(jlm, index) -
                              newProbAlignment.getCount(jlm, index);
          deviation += difference * difference;
          numEntries++;
        }
      
      probTargetGivenSource = newProbTargetGivenSource;
      probAlignment = newProbAlignment;
      
      iteration++;
      System.out.println("Iteration " + iteration + ": " + deviation);
    }while(deviation > CONVERGENCE_TOLERANCE * numEntries && iteration < MAX_ITERATIONS);
    
    System.out.println("Training done.");
  }
  
  // simple container class to hold a (int, int, int) set
  class IntegerTriple {
    public int x, y, z;
    
    public IntegerTriple(int x, int y, int z) {
      this.x = x;
      this.y = y;
      this.z = z;
    }
    
    @Override
    public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + getOuterType().hashCode();
      result = prime * result + x;
      result = prime * result + y;
      result = prime * result + z;
      return result;
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj) return true;
      if (obj == null) return false;
      if (getClass() != obj.getClass()) return false;
      IntegerTriple other = (IntegerTriple) obj;
      if (!getOuterType().equals(other.getOuterType())) return false;
      if (x != other.x) return false;
      if (y != other.y) return false;
      if (z != other.z) return false;
      return true;
    }


    private IBMModel2 getOuterType() {
      return IBMModel2.this;
    }
  }
}
