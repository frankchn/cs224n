package cs224n.wordaligner;

import java.util.List;

/**
 * A holder for a pair of sentences, each a list of strings.  Sentences in
 * the test sets have integer IDs, as well, which are used to retreive the
 * gold standard alignments for those sentences.
 * 
 * @author Dan Klein
 * @author Spence Green
 */
public class SentencePair {
  int sentenceID;
  String sourceFile;
  List<String> targetWords;
  List<String> sourceWords;

  public int getSentenceID() {
    return sentenceID;
  }

  public String getSourceFile() {
    return sourceFile;
  }

  public List<String> getTargetWords() {
    return targetWords;
  }

  public List<String> getSourceWords() {
    return sourceWords;
  }

  public String toString() {
    StringBuilder sb = new StringBuilder();
    for (int englishPosition = 0; englishPosition < targetWords.size(); englishPosition++) {
      String englishWord = targetWords.get(englishPosition);
      sb.append(englishPosition);
      sb.append(":");
      sb.append(englishWord);
      sb.append(" ");
    }
    sb.append("\n");
    for (int frenchPosition = 0; frenchPosition < sourceWords.size(); frenchPosition++) {
      String frenchWord = sourceWords.get(frenchPosition);
      sb.append(frenchPosition);
      sb.append(":");
      sb.append(frenchWord);
      sb.append(" ");
    }
    sb.append("\n");
    return sb.toString();
  }

  public SentencePair(int sentenceID, String sourceFile, List<String> targetWords, List<String> sourceWords) {
    this.sentenceID = sentenceID;
    this.sourceFile = sourceFile;
    this.targetWords = targetWords;
    this.sourceWords = sourceWords;
  }
}