package cs224n.deep;
import java.lang.*;
import java.util.*;

import org.ejml.data.*;
import org.ejml.ops.CommonOps;
import org.ejml.ops.SpecializedOps;
import org.ejml.simple.*;

import cc.mallet.optimize.LimitedMemoryBFGS;
import cc.mallet.optimize.Optimizable;


import java.text.*;

public class WindowModel implements Optimizable.ByGradientValue {
    public static final String START_TOKEN = "<s>";
    public static final String END_TOKEN = "</s>";
    
    public final boolean gradCheck;

	protected SimpleMatrix L, W, b1, U, b2;
	public int windowSize, wordSize, hiddenSize;
	public double learningRate;
	public double C = 0.0 / (150000+100);
  
	protected List<Integer> currentYs = new ArrayList<Integer>(1000);
	protected List<List<Integer>> currentWindowIndices = new ArrayList<List<Integer>>(1000);
  
	protected Map<String, Integer> wordToNum;
  
	protected int numParams;
	private List<SimpleMatrix> paramMatrices;
	
	protected LimitedMemoryBFGS optimizer;

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
      //U = new SimpleMatrix(hiddenSize, 1);
      //U.zero();
      b2 = new SimpleMatrix(1,1);
      b2.zero();
      
      paramMatrices = Arrays.asList(L, W, b1, U, b2);
      
      numParams = 0;
      for (SimpleMatrix m: paramMatrices)
        numParams += m.getNumElements();
	}


	/**
	 * Simplest SGD training 
	 */
	public void train(List<Datum> trainData ){
      int batchSize = 1000;
	  int numTrainWords = trainData.size();
      
      optimizer = new LimitedMemoryBFGS(this);

      for (int epoch = 0; epoch < 1; epoch++) {
        System.out.println("***** Iteration - " + epoch);

        for (int i = 0; i < numTrainWords; i+=batchSize) {
          currentYs.clear();
          currentWindowIndices.clear();
          
          for (int j = 0; j < batchSize && i+j < numTrainWords; j++) {
            List<Integer> windowIndices = getWindowIndices(trainData, i+j);
            String label = trainData.get(i+j).label;
            int y = label.equals("PERSON") ? 1 : 0;
  
            currentYs.add(y);
            currentWindowIndices.add(windowIndices);
          }
          
          if (gradCheck)
            doGradCheck();
          
          optimizer.reset();
          optimizer.optimize(20);
          
          System.out.println("Batch done: " + getValue());
        }
      }
	}
  
	public void test(List<Datum> testData){
	  int numWords = testData.size();
      SimpleMatrix x = new SimpleMatrix(windowSize*wordSize, 1);
      
      int truePositives = 0;
      int predictedPositives = 0;
      int goldPositives = 0;      
	  
	  for (int i = 0; i < numWords; i++) {
        String label = testData.get(i).label;
        
        List<Integer> windowIndices = getWindowIndices(testData, i);
        getWordVecs(x, windowIndices);
        PropagationResult res = forwardProp(x, false, 0, false);
        
        String prediction = (res.h >= 0.5) ? "PERSON" : "O";
        
        if (prediction.equals("PERSON")) {
          predictedPositives++;
          
          if (label.equals("PERSON"))
            truePositives++;
        }
        
        if (label.equals("PERSON"))
          goldPositives++;
	  }
    
	  double precision = (double)truePositives/predictedPositives;
	  double recall = (double)truePositives/goldPositives;
      double f1 = (2.0*precision*recall)/(precision+recall);
      
      System.out.println("Precision - " + precision);
      System.out.println("Recall - " + recall);
      System.out.println("F1 score - " + f1);
	}
  
    @Override
    public double getValue() {
      SimpleMatrix x = new SimpleMatrix(windowSize*wordSize, 1);
      
      int n = currentWindowIndices.size();
      //double res = (C/2.0)*(SpecializedOps.elementSumSq(W.getMatrix()) + SpecializedOps.elementSumSq(U.getMatrix()));
      double res = 0.0;
      for (int i = 0; i < n; i++) {
        getWordVecs(x, currentWindowIndices.get(i));
        res -= forwardProp(x, false, currentYs.get(i), true).cost;
      }
      
      return res/n;
    }
    
    @Override
    public void getValueGradient(double[] buffer) {
      SimpleMatrix x = new SimpleMatrix(windowSize*wordSize, 1);
      int n = currentWindowIndices.size();
      
      SimpleMatrix gradW = W.scale(-C/n);
      SimpleMatrix gradU = U.scale(-C/n);
      SimpleMatrix gradb1 = new SimpleMatrix(b1.numRows(), b1.numCols());
      gradb1.zero();
      SimpleMatrix gradb2 = new SimpleMatrix(b2.numRows(), b2.numCols());
      gradb2.zero();
      
      
      for (int i = 0; i < numParams; i++)
        buffer[i] = 0.0;
      
      for (int i = 0; i < n; i++) {
        List<Integer> windowIndices = currentWindowIndices.get(i);
        getWordVecs(x, windowIndices);
        
        PropagationResult res = forwardProp(x, true, currentYs.get(i), false);
        
        CommonOps.addEquals(gradW.getMatrix(), res.gradW.getMatrix());
        CommonOps.addEquals(gradU.getMatrix(), res.gradU.getMatrix());
        CommonOps.addEquals(gradb1.getMatrix(), res.gradb1.getMatrix());
        CommonOps.addEquals(gradb2.getMatrix(), res.gradb2.getMatrix());
        
        // L is a special case
        for (int j = 0; j < windowIndices.size(); j++) {
          int index = windowIndices.get(j);
          
          for (int k = 0; k < wordSize; k++)
            buffer[L.getIndex(k, index)] -= res.gradL.get(j*wordSize+k) / n;
        }
        
      }
      
      int idx = L.getNumElements();
      List<SimpleMatrix> grads = Arrays.asList(gradW, gradb1, gradU, gradb2);
      
      for (SimpleMatrix m: grads)
        for (int j = 0; j < m.getNumElements(); j++)
          buffer[idx++] -= m.get(j) / n;
    }
  
    public PropagationResult forwardProp(SimpleMatrix x, boolean calcGrad, int y, boolean calcCost) {
        // forward prop
        SimpleMatrix z = W.mult(x).plus(b1);
        SimpleMatrix a = z.copy();
        applyTanh(a);
        
        double ua = U.dot(a);
        double h = sigmoid(ua+b2.get(0));
        
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
          res.gradb2 = new SimpleMatrix(1,1);
          res.gradb2.set(0, coeff);
          res.gradW = delta.mult(x.transpose());
          res.gradU = a.scale(coeff);
          res.gradL = delta.transpose().mult(W).transpose();
        }
        
        // calculate cost
        if (calcCost) {
          res.cost = (-y)*Math.log(h)-(1-y)*Math.log(1-h);
        }
        
        return res;
    }
    
    public void doGradCheck() {
      double eps = 1e-4;
      double c1, c2;
      double org;
      
      double diff = 0;
      double grad = 0;
      double gradDiff = 0;
      
      double[] orgGrad = new double[numParams];
      getValueGradient(orgGrad);
      
      for (int i = 0; i < numParams; i++) {
        org = getParameter(i);
        setParameter(i, org+eps);
        c1 = getValue();
        setParameter(i, org-eps);
        c2 = getValue();
        
        setParameter(i, org);
        
        grad = (c1-c2)/(2*eps);
        gradDiff = Math.abs(orgGrad[i]-grad);
        diff += gradDiff*gradDiff;
      }
      
      System.out.print(diff);
      System.out.println(diff < 1e-7 ? " Ok" : " Fail");
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
  
    private void getWordVecs(SimpleMatrix x, List<Integer> indices) {
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
      public SimpleMatrix gradW, gradb1, gradL, gradU, gradb2;
      public double cost;
      
      
      public PropagationResult(double h) {
        this.h = h;
      }
	}
  
	
    @Override
    public int getNumParameters() {
      return numParams;
    }
    
    @Override
    public double getParameter(int index) {
      int idx = 0;
      
      for (SimpleMatrix m: paramMatrices) {
        if (idx+m.getNumElements() > index)
          return m.get(index-idx);
        idx += m.getNumElements();
      }
      
      System.out.println("oops");
      return Double.NaN;
    }
    
    @Override
    public void getParameters(double[] buffer) {
      int idx = 0;
      
      for (SimpleMatrix m: paramMatrices) {
        for (int i = 0; i < m.getNumElements(); i++)
          buffer[idx++] = m.get(i);
      }
    }
    
    @Override
    public void setParameter(int index, double value) {
      int idx = 0;
      
      for (SimpleMatrix m: paramMatrices) {
        if (idx+m.getNumElements() > index) {
          m.set(index-idx, value);
          return;
        }
        idx += m.getNumElements();
      }
      
      System.out.println("oops");
    }
    
    @Override
    public void setParameters(double[] params) {
      int idx = 0;
      
      for (SimpleMatrix m: paramMatrices) {
        for (int i = 0; i < m.getNumElements(); i++)
          m.set(i, params[idx++]);
      }
    }
}
