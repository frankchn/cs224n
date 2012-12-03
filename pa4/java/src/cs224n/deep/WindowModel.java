package cs224n.deep;
import java.lang.*;
import java.util.*;

import org.ejml.data.*;
import org.ejml.ops.CommonOps;
import org.ejml.simple.*;


import java.text.*;

public class WindowModel {
    public static final String START_TOKEN = "<s>";
    public static final String END_TOKEN = "</s>";
    
    public final boolean gradCheck;

	protected SimpleMatrix L, W, b1, U;
	double b2;
	//
	public int windowSize, wordSize, hiddenSize;
	public double learningRate;
  
	protected Map<String, Integer> wordToNum;

	public WindowModel(int _windowSize, int _hiddenSize, double _lr, SimpleMatrix L,
	                   Map<String, Integer> wordToNum){
      this(_windowSize, _hiddenSize, _lr, L, wordToNum, false);
	}
  
	public WindowModel(int _windowSize, int _hiddenSize, double _lr, SimpleMatrix L,
	                   Map<String, Integer> wordToNum, boolean gradCheck){
      this.L = L;
      this.wordToNum = wordToNum;
      
	  windowSize = _windowSize;
	  wordSize = L.numRows();
      learningRate = _lr;
      hiddenSize = _hiddenSize;
      
      this.gradCheck = gradCheck;
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
      
      U = SimpleMatrix.random(hiddenSize, 1, -0.1, 0.1, rand);
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
        int y = label.equals("PERSON") ? 1 : 0;
        
        getTrainExample(trainExample, windowIndices);
        PropagationResult res = forwardProp(trainExample, true, y);
        
        if (gradCheck)
          doGradCheck(res, trainExample, y);
	  }
	}
  
    // no weight decay yet
    public PropagationResult forwardProp(SimpleMatrix x, boolean calcGrad, int y) {
        // forward prop
        SimpleMatrix z = W.mult(x).plus(b1);
        SimpleMatrix a = z.copy();
        applyTanh(a);
        
        double ua = U.dot(a);
        double h = sigmoid(ua+b2);
        
        PropagationResult res = new PropagationResult(h);
        
        if (calcGrad) {
          double coeff = (-y)+h;
          SimpleMatrix a_prime = a.copy();
          CommonOps.elementMult(a_prime.getMatrix(), a_prime.getMatrix());
          CommonOps.changeSign(a_prime.getMatrix());
          CommonOps.add(a_prime.getMatrix(), 1.0);
          
          SimpleMatrix delta = U.scale(coeff);
          CommonOps.elementMult(delta.getMatrix(), a_prime.getMatrix());
          
          res.gradb1 = delta;
          res.gradb2 = coeff;
          res.gradW = delta.mult(x.transpose());
          res.gradU = a.scale(coeff);
          res.gradL = delta.transpose().mult(W).transpose();
        }
        
        // calculate cost
        res.cost = (-y)*Math.log(h)-(1-y)*Math.log(1-h);
        
        return res;
    }
    
    public void doGradCheck(PropagationResult res, SimpleMatrix x, int y) {
      double diff = 0.0;
      final double eps = 1e-4;
      
      double c1, c2, grad, graddiff, org;
      
      for (int i = 0; i < x.getNumElements(); i++) {
        org = x.get(i);
        x.set(i, org+eps);
        c1 = forwardProp(x, false, y).cost;
        x.set(i, org-eps);
        c2 = forwardProp(x, false, y).cost;
        
        grad = (c1-c2) / (2*eps);
        graddiff = Math.abs(res.gradL.get(i) - grad);
        diff += graddiff * graddiff;
        
        x.set(i, org);
      }
      
      for (int i = 0; i < W.getNumElements(); i++) {
        org = W.get(i);
        W.set(i, org+eps);
        c1 = forwardProp(x, false, y).cost;
        W.set(i, org-eps);
        c2 = forwardProp(x, false, y).cost;
        
        grad = (c1-c2) / (2*eps);
        graddiff = Math.abs(res.gradW.get(i) - grad);
        diff += graddiff * graddiff;
        
        W.set(i, org);
      }
      
      for (int i = 0; i < U.getNumElements(); i++) {
        org = U.get(i);
        U.set(i, org+eps);
        c1 = forwardProp(x, false, y).cost;
        U.set(i, org-eps);
        c2 = forwardProp(x, false, y).cost;
        
        grad = (c1-c2) / (2*eps);
        graddiff = Math.abs(res.gradU.get(i) - grad);
        diff += graddiff * graddiff;
        
        U.set(i, org);
      }
      
      for (int i = 0; i < b1.getNumElements(); i++) {
        org = b1.get(i);
        b1.set(i, org+eps);
        c1 = forwardProp(x, false, y).cost;
        b1.set(i, org-eps);
        c2 = forwardProp(x, false, y).cost;
        
        grad = (c1-c2) / (2*eps);
        graddiff = Math.abs(res.gradb1.get(i) - grad);
        diff += graddiff * graddiff;
        
        b1.set(i, org);
      }
      
      org = b2;
      b2 += eps;
      c1 = forwardProp(x, false, y).cost;
      b2 = org - eps;
      c2 = forwardProp(x, false, y).cost;
        
      grad = (c1-c2) / (2*eps);
      graddiff = Math.abs(res.gradb2 - grad);
      diff += graddiff * graddiff;
        
      b2 = org;
      
      System.out.print(diff);
      System.out.println(diff < 1e-7 ? " Ok" : " Fail");
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
        
        if (wordToNum.containsKey(word))
          indices.add(wordToNum.get(word));
        else
          indices.add(0);
      }
      
      return indices;
	}
  
	public class PropagationResult {
      public double h;
      public SimpleMatrix gradW, gradb1, gradL, gradU;
      public double gradb2;
      public double cost;
      
      
      public PropagationResult(double h) {
        this.h = h;
      }
	}
}
