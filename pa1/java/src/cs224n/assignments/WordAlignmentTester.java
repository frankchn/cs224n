package cs224n.assignments;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import cs224n.util.CommandLineUtils;
import cs224n.util.Pair;
import cs224n.wordaligner.Alignment;
import cs224n.wordaligner.SentencePair;
import cs224n.wordaligner.WordAligner;

/**
 * Harness for testing wordl alignments.  The code is hard-wired for the
 * alignment source to be French/Hindi/Chinese and target to be English.
 *
 * TODO: Implement new models of word alignment, which can be loaded, trained, and
 * evaluated by this class.
 *
 * @author Dan Klein
 * @author Spence Green
 */
public final class WordAlignmentTester {

  /**
   * Default location of the training and test corpora.
   */
  public static final String DATA_PATH = "/afs/ir/class/cs224n/pa1/data/";

  public static final String ENGLISH = "english";
  public static final String FRENCH = "french";
  public static final String HINDI = "hindi";
  public static final String CHINESE = "chinese";
  public static final String ENGLISH_EXT = "e";
  public static final String FRENCH_EXT = "f";
  public static final String HINDI_EXT = "h";
  public static final String CHINESE_EXT = "z";

  // SGML patterns
  public static final Pattern SGML_OPEN_TAG = Pattern.compile("<s snum=(\\d+)>");
  public static final Pattern SGML_CLOSE_TAG = Pattern.compile("</s>");

  public static String GetLanguageExtension(String language){
    if(language.equalsIgnoreCase(ENGLISH)){ return ENGLISH_EXT; }
    if(language.equalsIgnoreCase(HINDI)){ return HINDI_EXT; }
    if(language.equalsIgnoreCase(CHINESE)){ return CHINESE_EXT; }
    if(language.equalsIgnoreCase(FRENCH)){ return FRENCH_EXT; }

    // Default for miniTest and generic source corpora
    return "f";
  }

  /**
   * 
   * @param args
   */
  public static void main(String[] args) {
    // Parse command line
    final Map<String,String> argMap = CommandLineUtils.simpleCommandLineParser(args);
    final int maxTrainingSentences = argMap.containsKey("-trainSentences") ? 
        Integer.parseInt(argMap.get("-trainSentences")) : Integer.MAX_VALUE;
    final boolean verbose = argMap.containsKey("-verbose");
    final String model = argMap.containsKey("-model") ? 
        argMap.get("-model") : "cs224n.wordaligner.BaselineWordAligner";
    final String language = argMap.containsKey("-language") ? argMap.get("-language") : FRENCH;
    final String outputFile = argMap.containsKey("-outputAlignments") ? argMap.get("-outputAlignments") : "";
    String dataset = argMap.containsKey("-evalSet") ? argMap.get("-evalSet") : "miniTest";
    if (outputFile.length() > 0) dataset = "";
    String basePath = argMap.containsKey("-dataPath") ? argMap.get("-dataPath") : DATA_PATH;
    basePath += dataset.equalsIgnoreCase("miniTest") ? "/mini" : "/"+language;

    // Target language is hard-coded as English
    final String sourceFileExtension = GetLanguageExtension(language);
    final String targetFileExtension = "e";

    // Read training set.
    System.out.println("/// CS224n Word Alignment Tester ///");
    if (! dataset.equalsIgnoreCase("miniTest")) System.out.println("Language: " + language);
    System.out.println("Data path: "+basePath);
    if (! dataset.equals("")) System.out.println("Evaluation set: "+dataset);
    System.out.printf("Using up to %d training sentences.%n", maxTrainingSentences);
    List<SentencePair> trainingSentencePairs = new ArrayList<SentencePair>();
    if ( !dataset.equalsIgnoreCase("miniTest") && maxTrainingSentences > 0) {
      trainingSentencePairs = loadTrainingData(basePath+"/training", maxTrainingSentences);
    }
    System.out.printf("Training set size: %d%n", trainingSentencePairs.size());

    // Read test set, if specified.
    List<SentencePair> testSentencePairs = new ArrayList<SentencePair>();
    Map<Integer,Alignment> goldAlignments = new HashMap<Integer, Alignment>();
    if (dataset.equalsIgnoreCase("test")) {
      testSentencePairs = loadTestData(basePath+"/test", "test."+sourceFileExtension, "test."+targetFileExtension);
      goldAlignments = readGoldAlignments(basePath+"/test/test.wa");
    } else if (dataset.equalsIgnoreCase("dev")) {
      testSentencePairs = loadTestData(basePath+"/trial", "trial."+sourceFileExtension, "trial."+targetFileExtension);
      goldAlignments = readGoldAlignments(basePath+"/trial/trial.wa");
    } else if (dataset.equalsIgnoreCase("miniTest")) {
      testSentencePairs = loadTestData(basePath, "mini."+sourceFileExtension, "mini."+targetFileExtension);
      goldAlignments = readGoldAlignments(basePath+"/mini.wa");
    } else {
      System.out.println("Model learning only. No evaluation will be performed.");
    }
    System.out.printf("Evaluation set size: %d%n", testSentencePairs.size());

    // Add the test sentences to the training data. This is an unsupervised learner.
    trainingSentencePairs.addAll(testSentencePairs);

    // Train model
    System.out.println("Model: "+model);
    WordAligner wordAligner = loadModel(model);
    wordAligner.train(trainingSentencePairs);

    // Run inference and evaluate
    if (outputFile.length() > 0) {
      write(wordAligner, trainingSentencePairs, outputFile);
    } else {
      test(wordAligner, testSentencePairs, goldAlignments, verbose);
    }
  }

  /**
   * Load word alignment model by reflection.
   * 
   * @param model
   * @return
   */
  private static WordAligner loadModel(String model) {
    WordAligner wordAligner = null;
    try {
      Class modelClass = Class.forName(model);
      wordAligner = (WordAligner) modelClass.newInstance();
    } catch (ClassNotFoundException e) {
      e.printStackTrace();
    } catch (InstantiationException e) {
      e.printStackTrace();
    } catch (IllegalAccessException e) {
      e.printStackTrace();
    }
    return wordAligner;
  }

  /**
   * Evaluate the aligner on a gold test set using Alignment Error Rate (AER).
   * 
   * @param wordAligner
   * @param testSentencePairs
   * @param testAlignments
   * @param verbose
   */
  private static void test(WordAligner wordAligner, List<SentencePair> testSentencePairs, Map<Integer, Alignment> testAlignments, boolean verbose) {
    int proposedSureCount = 0;
    int proposedPossibleCount = 0;
    int sureCount = 0;
    int proposedCount = 0;

    for (SentencePair sentencePair : testSentencePairs) {
      final Alignment proposedAlignment = wordAligner.align(sentencePair);
      final Alignment referenceAlignment = testAlignments.get(sentencePair.getSentenceID());
      if (referenceAlignment == null) {
        throw new RuntimeException("No reference alignment found for sentenceID "+sentencePair.getSentenceID());
      }
      if (verbose) {
        System.out.println("Alignment:\n"+Alignment.render(referenceAlignment,proposedAlignment,sentencePair));
      }

      final int nSourceWords = sentencePair.getSourceWords().size();
      final int nTargetWords = sentencePair.getTargetWords().size();
      for (int srcIndex = 0; srcIndex < nSourceWords; srcIndex++) {
        for (int tgtIndex = 0; tgtIndex < nTargetWords; tgtIndex++) {
          boolean proposed = proposedAlignment.containsSureAlignment(tgtIndex, srcIndex);
          boolean sure = referenceAlignment.containsSureAlignment(tgtIndex, srcIndex);
          boolean possible = referenceAlignment.containsPossibleAlignment(tgtIndex, srcIndex);
          if (proposed && sure) proposedSureCount++;
          if (proposed && possible) proposedPossibleCount++;
          if (proposed) proposedCount++;
          if (sure) sureCount++;
        }
      }
    }
    System.out.println();
    System.out.println("### Evaluation Results ###");
    System.out.printf("%s:\t%.4f%n", "Precision", proposedPossibleCount/(double)proposedCount);
    System.out.printf("%s:\t%.4f%n", "Recall", proposedSureCount/(double)sureCount);
    System.out.printf("%s:\t%.4f%n", "AER", (1.0-(proposedSureCount+proposedPossibleCount)/(double)(sureCount+proposedCount)));
  }


  /**
   * Write alignments in GIZA++ format. Assumes that NULL alignments are not included in the Alignment object.
   * 
   * For more information on this format, see: http://www.statmt.org/moses/?n=FactoredTraining.AlignWords
   * 
   * @param wordAligner
   * @param trainingSentencePairs
   * @param outputFile
   */
  private static void write(WordAligner wordAligner, List<SentencePair> trainingSentencePairs, String outputFile) {
    try {
      PrintWriter pw = new PrintWriter(new BufferedOutputStream(new FileOutputStream(outputFile)));
      for (SentencePair sentence : trainingSentencePairs) {
        Alignment alignment = wordAligner.align(sentence);
        int numTargetTokens = sentence.getTargetWords().size();
        int numSourceTokens = sentence.getSourceWords().size();
        for (int tgtIndex = 0; tgtIndex < numTargetTokens; ++tgtIndex) {
          Set<Integer> alignedSourceIndices = alignment.getAlignedSources(tgtIndex);
          for (int srcIndex : alignedSourceIndices) {
            if (srcIndex < 0 || srcIndex >= numSourceTokens) {
              throw new RuntimeException(String.format("Source index out of bounds: idx: %d src_len: %d", srcIndex, numSourceTokens));
            }
            pw.printf("%d-%d ", srcIndex, tgtIndex);
          }
        }
        pw.println();
      }
      pw.close();

    } catch (FileNotFoundException e) {
      e.printStackTrace();
    }
  }

  /**
   * Read gold alignments file in format from NAACL-03 / ACL-05 shared tasks on word alignment.
   * 
   * @param fileName
   * @return
   */
  private static Map<Integer, Alignment> readGoldAlignments(String fileName) {
    Map<Integer,Alignment> alignments = new HashMap<Integer, Alignment>();
    try {
      BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(fileName), "UTF-8"));
      for (String line; (line = in.readLine()) != null;) {
        String[] words = line.split("\\s+");
        if (words.length < 3) {
          throw new RuntimeException("Bad alignment file "+fileName+", bad line was "+line);
        }
        Integer sentenceID = Integer.parseInt(words[0]);
        // Subtract one since the gold alignments assume that position 0
        // is the null alignment.
        Integer targetPosition = Integer.parseInt(words[1])-1;
        Integer sourcePosition = Integer.parseInt(words[2])-1;
        String type = words.length == 4 ? words[3] : "S";
        // Some languages don't make the S/P distinction (e.g., the Hindi data)
        final boolean isSure = type.equals("S");

        Alignment alignment = alignments.containsKey(sentenceID) ? 
            alignments.get(sentenceID) : new Alignment();
            alignment.addGoldAlignment(targetPosition, sourcePosition, isSure);
            alignments.put(sentenceID, alignment);
      }
      in.close();

    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    return alignments;
  }

  /**
   * Read test files that correspond to gold word alignments.
   * 
   * @param path
   * @param srcExtension
   * @param tgtExtension
   * @return
   */
  private static List<SentencePair> loadTestData(String path, String srcFile, String tgtFile) {
    srcFile = path + "/" + srcFile;
    tgtFile = path + "/" + tgtFile;
    return readAlignedSentences(new Pair<String,String>(tgtFile, srcFile));
  }

  /**
   * Load aligned sentences from training data.
   * 
   * @param path
   * @param maxSentencePairs - a list of (source,target) sentences
   * @return
   */
  private static List<SentencePair> loadTrainingData(String path, int maxSentencePairs) {
    List<SentencePair> sentencePairs = new ArrayList<SentencePair>();
    List<Pair<String,String>> baseFileNames = getAlignedFileList(path);
    for (Pair<String,String> filePair: baseFileNames) {
      List<SentencePair> fileSentences = readAlignedSentences(filePair);
      if (sentencePairs.size() + fileSentences.size() <= maxSentencePairs) {
        sentencePairs.addAll(fileSentences);
      } else {
        int maxIdx = maxSentencePairs - sentencePairs.size();
        sentencePairs.addAll(fileSentences.subList(0, maxIdx));
        break;
      }
    }
    return sentencePairs;
  }

  /**
   * Read a training data specification in the format of the NAACL-03 / ACL-05 shared task on
   * word alignment.
   * 
   * @param path
   * @return
   */
  private static List<Pair<String,String>> getAlignedFileList(String path) {
    File fileName = new File(path + "/FilePairs.training");
    if ( ! fileName.exists()) {
      throw new RuntimeException("Training file does not exist: " + fileName.getAbsolutePath());
    }
    List<Pair<String,String>> filePairs = new ArrayList<Pair<String,String>>();
    try {
      BufferedReader br = new BufferedReader(new FileReader(fileName));
      for (String line; (line = br.readLine()) != null;) {
        String[] filePair = line.trim().split("\\s+");
        if (filePair.length != 2) {
          throw new RuntimeException("Malformed training file index: " + line);
        }
        // first: target file  ; second: source file
        filePairs.add(new Pair<String,String>(path + "/" + filePair[0], path + "/" + filePair[1]));
      }
      br.close();

    } catch (FileNotFoundException e) {
      e.printStackTrace();
    } catch (IOException e) {
      e.printStackTrace();
    }
    return filePairs;
  }

  /**
   * Read a set of sentences from aligned files.
   * 
   * @param filePair
   * @return
   */
  private static List<SentencePair> readAlignedSentences(Pair<String,String> filePair) {
    List<SentencePair> sentencePairs = new ArrayList<SentencePair>();
    String targetFileName = filePair.getFirst();
    String sourceFileName = filePair.getSecond();
    try {
      BufferedReader brTarget = new BufferedReader(new InputStreamReader(new FileInputStream(targetFileName), "UTF-8"));
      BufferedReader brSource = new BufferedReader(new InputStreamReader(new FileInputStream(sourceFileName), "UTF-8"));
      while (brTarget.ready() && brSource.ready()) {
        String englishLine = brTarget.readLine();
        String frenchLine = brSource.readLine();
        Pair<Integer,List<String>> targetSentenceAndID = tokenizeAndIntern(englishLine);
        Pair<Integer,List<String>> sourceSentenceAndID = tokenizeAndIntern(frenchLine);
        if (! targetSentenceAndID.getFirst().equals(sourceSentenceAndID.getFirst())) {
          throw new RuntimeException("Sentence ID confusion in file "+targetFileName+", lines were:\n\t"+englishLine+"\n\t"+frenchLine);
        }
        sentencePairs.add(new SentencePair(targetSentenceAndID.getFirst(), targetFileName, targetSentenceAndID.getSecond(), sourceSentenceAndID.getSecond()));
      }
      brTarget.close();
      brSource.close();

    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    return sentencePairs;
  }

  /**
   * Perform whitespace tokenization and intern strings.
   * 
   * @param line
   * @return
   */
  private static Pair<Integer, List<String>> tokenizeAndIntern(String line) {
    // Strip off SGML and extract segment id
    int id = -1;
    Matcher mOpen = SGML_OPEN_TAG.matcher(line);
    if (mOpen.find()) {
      id = Integer.parseInt(mOpen.group(1));
      line = mOpen.replaceFirst("");
    }
    line = SGML_CLOSE_TAG.matcher(line).replaceAll("");

    // Intern the tokens for memory efficiency
    String[] tokens = line.trim().split("\\s+");
    List<String> tokenList = new ArrayList<String>(tokens.length);
    for (int i = 0; i < tokens.length; ++i) {
      tokenList.add(tokens[i].intern());
    }

    return new Pair<Integer,List<String>>(id, tokenList);
  }

}
