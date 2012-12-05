package cs224n.deep;

import java.util.*;
import java.io.*;

import org.ejml.simple.SimpleMatrix;


public class NER {
    
    public static void main(String[] args) throws IOException {
		if (args.length < 2) {
		    System.out.println("USAGE: java -cp classes NER ../data/train ../data/dev");
		    return;
		}	    
		
		// this reads in the train and test datasets
		List<Datum> trainData = FeatureFactory.readTrainData(args[0]);
		List<Datum> testData = FeatureFactory.readTestData(args[1]);	
		
		//	read the train and test data
		//TODO: Implement this function (just reads in vocab and word vectors)
		FeatureFactory.initializeVocab("../data/vocab.txt");
		SimpleMatrix allVecs= FeatureFactory.readWordVectors("../data/wordVectors.txt");
    
		// initialize model 
		int[] wschoices = {7};
		int[] hschoices = {50, 100, 200};
		double[] lrchoices = {0.0002, 0.001, 0.005};
		
		for(int ws : wschoices) {
			for(int hs : hschoices) {
				for(double lr : lrchoices) {
					System.out.println("Window size: " + ws + ", Hidden size: " + hs + ", Learning Rate: " + lr);
					
					WindowModel model = new WindowModel(ws, hs, lr, 20.0, allVecs, FeatureFactory.wordToNum);
					model.initWeights();
				
					model.train(trainData);
					model.test(trainData, false);
					model.test(testData, false);
				}
			}
		}
    }
}