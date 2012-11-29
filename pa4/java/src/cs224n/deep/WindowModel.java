package cs224n.deep;
import java.lang.*;
import java.util.*;

import org.ejml.data.*;
import org.ejml.simple.*;


import java.text.*;

public class WindowModel {
    public static final String START_TOKEN = "<s>";
    public static final String END_TOKEN = "</s>";

	protected SimpleMatrix L, W, b1, U;
	double b2;
	//
	public int windowSize, wordSize, hiddenSize;
	public double learningRate;

  
	public WindowModel(int _windowSize, int _hiddenSize, double _lr){
      L = FeatureFactory.allVecs;
      
	  windowSize = _windowSize;
	  wordSize = L.numRows();
      learningRate = _lr;
      hiddenSize = _hiddenSize;
	}

	/**
	 * Initializes the weights randomly. 
	 */
	public void initWeights(){
      Random rand = new Random();
      double e = Math.sqrt(6.0/(wordSize*windowSize+hiddenSize));
      
      W = SimpleMatrix.random(hiddenSize, windowSize*wordSize, -e, e, rand);
      b1 = new SimpleMatrix(hiddenSize, 1);
      b1.zero();
      
      U = SimpleMatrix.random(hiddenSize+1, 1, -0.1, 0.1, rand);
      b2 = 0.0;
	}


	/**
	 * Simplest SGD training 
	 */
	public void train(List<Datum> trainData ){
	  int numTrainWords = trainData.size();
      SimpleMatrix trainExample = new SimpleMatrix(windowSize*wordSize, 1);

	  for (int i = 0; i < numTrainWords; i++) {
        List<Integer> windowIndices = getWindowIndices(trainData, i);
        String label = trainData.get(i).label;
        
        getTrainExample(trainExample, windowIndices);
        
        // forward prop
        SimpleMatrix z = W.mult(trainExample).plus(b1);
        SimpleMatrix a = z.copy();
        applyTanh(a);
        
        double ua = U.dot(a) + b2;
        double h = sigmoid(ua);
	  }
	}
  
	
	public void test(List<Datum> testData){
		// TODO
	}
  
	private void applyTanh(SimpleMatrix x) {
      int numElements = x.getNumElements();
      
      for (int i = 0; i < numElements; i++) {
        double elem = x.get(i);
        x.set(i, Math.tanh(elem));
      }
	}
  
	private double sigmoid(double x) {
      return 1.0 / (1 + Math.exp(-x));
	}
  
    private void getTrainExample(SimpleMatrix x, List<Integer> indices) {
      for (int i = 0; i < indices.size(); i++) {
        x.insertIntoThis(i*wordSize, 0, L.extractVector(false, indices.get(i)));
      }
    }
	
	private List<Integer> getWindowIndices(List<Datum> trainData, int center) {
      List<Integer> indices = new ArrayList<Integer>(windowSize);
      
      for (int current = center-windowSize/2; current <= center+windowSize/2; current++) {
        String word;
        
        if (current < 0) {
          word = START_TOKEN;
        }
        else if (current >= trainData.size()) {
          word = END_TOKEN;
        }
        else
          word = trainData.get(current).word;
        
        if (FeatureFactory.wordToNum.containsKey(word))
          indices.add(FeatureFactory.wordToNum.get(word));
        else
          indices.add(0);
      }
      
      return indices;
	}
}
