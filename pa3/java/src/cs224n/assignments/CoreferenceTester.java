package cs224n.assignments;

import cs224n.coref.ClusteredMention;
import cs224n.coref.Document;
import cs224n.coref.Entity;
import cs224n.coref.Mention;
import cs224n.corefsystems.BaselineCoreferenceSystem;
import cs224n.corefsystems.CoreferenceSystem;
import cs224n.util.*;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.*;
import java.util.PriorityQueue;

/**
 * The framework for running your coreference system.
 * Please refer to the handout for a detailed description of the
 * command line options available.
 *
 * You should not change the NUM_TEST_EXAMPLES and NUM_DEV_EXAMPLES
 * variables when reporting results.
 *
 * @author Gabor Angeli (angeli at cs.stanford)
 */
public class CoreferenceTester<SYS extends CoreferenceSystem> {
  private static final int NUM_TEST_EXAMPLES = 202;
  private static final int NUM_DEV_EXAMPLES = 63;
  private static final int MAX_TRAIN_EXAMPLES = 1600;
  private static final boolean plaintext = true;

  public static String dataPath = "/afs/ir/class/cs224n/pa3/data/";

  public static enum DataType {
    TRAIN, DEV, TEST
  }

  public static class SerializedDatum implements Serializable, Decodable {
    public final Document document;
    public final List<Mention> goldMentions;
    public final List<Mention> predictedMentions;
    public final List<Entity> goldClusters;
    public SerializedDatum(Document document, List<Mention> goldMentions, List<Mention> extractedMentions, List<Entity> goldClusters){
      this.document = document;
      this.goldMentions = goldMentions;
      this.predictedMentions = extractedMentions;
      this.goldClusters = goldClusters;
    }

    public String encode() {
      StringBuilder b = new StringBuilder();
      b.append("---------gloss--------\n");
      Ansi.forceAnsi();
      document.setMentions(goldMentions);
      b.append(document.prettyPrint(goldClusters)).append("\n");
      b.append("(end gloss)\n");
      b.append("---------document--------\n");
      b.append(document.encode()).append("\n");
      b.append("(end document)\n");
      b.append("---------gold mentions--------\n");
      for(Mention m : goldMentions){
        b.append(m.encode()).append("\n");
      }
      b.append("(end gold mentions)\n");
      b.append("---------predicted mentions--------\n");
      for(Mention m : predictedMentions){
        b.append(m.encode()).append("\n");
      }
      b.append("(end predicted mentions)\n");
      b.append("---------gold clusters--------\n");
      for(Entity e : goldClusters){
        b.append(e.encode()).append(" | ").append(e).append("\n");
      }
      b.append("(end gold clusters)");
      return b.toString();
    }

    public static SerializedDatum decode(String encoded){
      String[] lines = encoded.split("\n");
      int index = 0;
      //--Gloss
      if(!lines[0].equals("---------gloss--------")){ throw new IllegalStateException("Could not deserialize"); }
      index += 1;
      while(!lines[index].equals("(end gloss)")){
        index += 1;
      }
      index += 1;
      //--Sentences
      if(!lines[index].equals("---------document--------")){ throw new IllegalStateException("Could not deserialize"); }
      index += 1;
      StringBuilder encodedDoc = new StringBuilder();
      while(!lines[index].equals("(end document)")){
        encodedDoc.append(lines[index]).append("\n");
        index += 1;
      }
      index += 1;
      Document doc = Document.decode(encodedDoc.toString());
      //--Gold Mentions
      if(!lines[index].equals("---------gold mentions--------")){ throw new IllegalStateException("Could not deserialize"); }
      index += 1;
      List<Mention> goldMentions = new ArrayList<Mention>();
      while(!lines[index].equals("(end gold mentions)")){
        goldMentions.add(Mention.decode(lines[index], doc));
        index += 1;
      }
      index += 1;
      //--Predicted Mentions
      if(!lines[index].equals("---------predicted mentions--------")){ throw new IllegalStateException("Could not deserialize"); }
      index += 1;
      List<Mention> predictedMentions = new ArrayList<Mention>();
      while(!lines[index].equals("(end predicted mentions)")){
        predictedMentions.add(Mention.decode(lines[index], doc));
        index += 1;
      }
      index += 1;
      //--Gold Clusters
      if(!lines[index].equals("---------gold clusters--------")){ throw new IllegalStateException("Could not deserialize"); }
      index += 1;
      List<Entity> goldCLusters = new ArrayList<Entity>();
      while(!lines[index].equals("(end gold clusters)")){
        goldCLusters.add(Entity.decode(lines[index].substring(0,lines[index].indexOf(" |")), goldMentions));
        index += 1;
      }
      index += 1;
      //--Return
      if(index != lines.length){ throw new IllegalStateException("Extra lines in file: " + index + " read of " + lines.length); }
      return new SerializedDatum(doc,goldMentions,predictedMentions,goldCLusters);
    }
  }

  public static class CoreferenceScore {
    private Collection<Entity> responses = new ArrayList<Entity>();
    private Collection<Entity> keys = new ArrayList<Entity>();


    public void report(){
      System.out.println("    MUC");
      System.out.println("MUC Precision: " + precisionMUC());
      System.out.println("MUC Recall:    " + recallMUC());
      System.out.println("MUC F1:        " + f1MUC());
      System.out.println("    B^3");
      System.out.println("B^3 Precision: " + precisionB3());
      System.out.println("B^3 Recall:    " + recallB3());
      System.out.println("B^3 F1:        " + f1B3());
    }

    public double precisionMUC(){
      double prec = precisionMUC(responses,keys);
//      double check = recallMUC(keys, responses);
//      if(Math.abs(prec-check) > 1e-6){ throw new IllegalStateException("MUC scores do not agree (not your fault)!"); }
      return prec;
    }
    public double recallMUC(){
      double recall = precisionMUC(keys,responses);
//      double check = recallMUC(responses, keys);
//      if(Math.abs(recall-check) > 1e-6){ throw new IllegalStateException("MUC scores do not agree (not your fault)!"); }
      return recall;
    }
    public double f1MUC(){
      double prec = precisionMUC();
      double rec = recallMUC();
      return 2.0*(prec*rec)/(prec+rec);
    }

    public double precisionB3(){
      return precisionB3(responses,keys);
    }
    public double recallB3(){
      return precisionB3(keys,responses);
    }
    public double f1B3(){
      double prec = precisionB3();
      double rec = recallB3();
      return 2.0*(prec*rec)/(prec+rec);
    }

    /**
     * Adapted from CoreNLP MUCScorer recall() method.
     * This should agree with precisionMUC().
     * @param guesses The system's response clusters
     * @param golds The true clusters
     * @return The MUC precision score
     */
    public static double recallMUC(Collection<Entity> guesses, Collection<Entity> golds) {
      int num = 0;
      int den = 0;

      Map<Mention,Entity> guessMap = new HashMap<Mention,Entity>();
      for(Entity guessEntity : guesses){
        for(Mention m : guessEntity.mentions){
          guessMap.put(m,guessEntity);
        }
      }

      for(Entity goldEntity : golds){
        if(goldEntity.mentions.isEmpty()){ continue; }
        num += goldEntity.size();
        Set<Entity> partitions = new HashSet<Entity>();
        for(Mention m : goldEntity.mentions){
          Entity e = guessMap.get(m);
          if(e == null){ throw new IllegalStateException("Did not calculate GuessMap correctly"); }
          partitions.add(e);
        }
        num -= partitions.size();
        den += goldEntity.size() - 1;
      }
      return (double)num/(double)den;
    }

    /**
     * Adapted from the JavaNLP code
     */
    private static double precisionB3(Collection<Entity> guesses, Collection<Entity> golds){
      //--Variables
      //(gold map)
      Map<Mention,Entity> goldMap = new HashMap<Mention,Entity>();
      for(Entity goldEntity : golds){
        for(Mention m : goldEntity.mentions){
          goldMap.put(m,goldEntity);
        }
      }
      //(guess map)
      Map<Mention,Entity> guessMap = new HashMap<Mention,Entity>();
      for(Entity guessEntity : guesses){
        for(Mention m : guessEntity.mentions){
          guessMap.put(m,guessEntity);
        }
      }
      //(documents)
      Set<Document> documents = new HashSet<Document>();
      for(Entity e : guesses){
        for(Mention m : e.mentions){
          documents.add(m.doc);
        }
      }
      //--Get Score
      double numer = 0.0;
      double denom = 0.0;
      for(Document doc : documents){
        double n = 0.0;
        for(Mention m : doc.getMentions()){
          //(variables)
          double correct = 0.0;
          double total = 0.0;
          //(calculate something)
          Entity guessEntity = guessMap.get(m);
          if(guessEntity != null){
            for(Mention guessMention : guessEntity.mentions){
              if(goldMap.containsKey(guessMention) && goldMap.get(guessMention).equals(goldMap.get(m))){
                correct += 1;
              }
              total += 1;
            }
          }
          //(increment score)
          if(total > 0){
            n += correct / total;
          } else if(correct != 0){
            throw new IllegalStateException("Bad B Cubed score update (not your fault)!");
          }
        }
        numer += n;
        denom += (double) doc.getMentions().size();
      }
      //--Return Score
      if(numer > denom || numer < 0 || denom <= 0){
        throw new IllegalStateException("Bad B Cubed score about to be returned (not your fault)!");
      }
      return numer / denom;
    }

    /**
     * As per Vilain 1995: "A Model Theoretic Coreference Scoring Scheme" (http://acl.ldc.upenn.edu/M/M95/M95-1005.pdf)
     * @param responses The system's guessed clusters
     * @param keys The true clusters
     * @return A precision (between 0 and 1) score
     */
    private static double precisionMUC(Collection<Entity> responses, Collection<Entity> keys){
      //--Auxilliary Structures
      //(populate key map)
      Map<Mention,Entity> keyMap = new HashMap<Mention,Entity>();
      for(Entity gold : keys){
        for(Mention mention : gold.mentions){
          if(keyMap.containsKey(mention)){ throw new IllegalStateException("Mention mapped to multiple entities (in gold! you're all sorts of broken...): " + mention); }
          keyMap.put(mention, gold);
        }
      }
      //--Calculate Numerator
      int numer = 0;
      for(Entity response : responses){
        //(get partitions)
        Set<Entity> partitions = new HashSet<Entity>();
        int extraPartitions = 0;
        for(Mention m : response) {
          if(!keyMap.containsKey(m)){
            //(case: a mention that didn't exist in the gold)
            extraPartitions += 1;
          } else {
            //(case: a mention that appeared in the gold)
            partitions.add(keyMap.get(m));
          }
        }
        //(debug)
        int numAccountedFor = 0;
        for(Entity e : partitions) {
          numAccountedFor += e.size();
        }
        //(increment numerator)
        int p = partitions.size() + extraPartitions;
        int S = response.size();
        numer += S - p;
      }
      //--Calculate Denominator
      int denom = 0;
      for(Entity response : responses){
        //(increment denominator)
        int S = response.size();
        denom += S-1;
      }
      //--Error Checks
      if(numer > denom){ throw new IllegalStateException("MUC precision is broken (not your fault!)"); }
      if(denom == 0){
        if(numer != 0){ throw new IllegalStateException("MUC precisions is hella broken (not your fault!)"); }
        return 1.0;
      }
      //--Return
      return ((double) numer) / ((double) denom);
    }

    public CoreferenceScore enter(Document doc, Collection<ClusteredMention> guess, Collection<Entity> gold){
      responses.addAll(Entity.fromMentions(guess));
      keys.addAll(gold);
      return this;
    }
  }

  public static String conllData(DataType type){
    String path = "conll2011.";
    switch(type){
      case TRAIN:
        path += "train";
        break;
      case DEV:
        path += "dev";
        break;
      case TEST:
        path += "test";
        break;
      default:
        throw new IllegalArgumentException("Unhandled datatype: " + type);
    }
    path += ".ser/";
    return path;
  }


  private SYS system;

  private CoreferenceTester(SYS system){
    this.system = system;
  }

  public CoreferenceScore train(final File[] data, final Properties props){
    //--Create Data
    //(get properties)
    final String mentionType = props.getProperty("mentionExtractor", "gold");
    //(convert data)
    Collection<Pair<Document, List<Entity>>> dataToPass = new WeakReferenceList<Pair<Document,List<Entity>>>(new WeakReferenceList.RefreshFunction<Pair<Document,List<Entity>>>(){
      public Pair<Document,List<Entity>> get(int i) {
        File f = data[i];
        SerializedDatum datum = getDatum(f);
        //((get mentions))
        List<Mention> mentions = null;
        if(mentionType.equalsIgnoreCase("gold")){
          mentions = datum.goldMentions;
        } else if(mentionType.equalsIgnoreCase("predicted")) {
          mentions = datum.predictedMentions;
        } else {
          throw new IllegalArgumentException("Unknown mention extractor: " + mentionType);
        }
        //((set mentions))
        datum.document.setMentions(mentions);
        //((sanity checks))
        for(Entity e : datum.goldClusters){
          if(e.size() == 0){ throw new IllegalStateException("Gold cluster has size 0 (not your fault)!"); }
        }
        //((return))
        return Pair.make(datum.document, datum.goldClusters);
      }
      public int size() {
        return data.length;
      }
    });
    //--Train
    system.train(dataToPass);
    //--Return
    return test(data,props);
  }

  public CoreferenceScore test(File[] data, Properties props){
    //--Variables
    //(get properties)
    String mentionType = props.getProperty("mentionExtractor", "gold");
    //(scorer)
    CoreferenceScore score = new CoreferenceScore();
    //--Run Coreference
    for(File f : data){
      SerializedDatum datum = getDatum(f);
      Document doc = datum.document;
      //(get mentions)
      List<Mention> mentions = null;
      if(mentionType.equalsIgnoreCase("gold")){
        mentions = datum.goldMentions;
      } else if(mentionType.equalsIgnoreCase("predicted")) {
        mentions = datum.predictedMentions;
      } else {
        throw new IllegalArgumentException("Unknown mention extractor: " + mentionType);
      }
      //(set mentions)
      datum.document.setMentions(mentions);
      //(run coreference)
      Collection<ClusteredMention> guess = system.runCoreference(datum.document);
      HashSet<ClusteredMention> uniqueCheck = new HashSet<ClusteredMention>();
      for(ClusteredMention m : guess){ uniqueCheck.add(m); }
      if(uniqueCheck.size() != guess.size()){
        throw new IllegalStateException("You added the same mention to the return list twice");
      }
      if(guess.size() != datum.document.getMentions().size()){
        throw new IllegalStateException("You did not assign every entity to a cluster (returned a different sized list)");
      }
      if(!datum.document.areAllMentionsClustered()){
        throw new IllegalStateException("You did not assign every entity to a cluster");
      }
      //(enter score)
      Collection<Entity> gold = datum.goldClusters;
      score.enter(datum.document, guess, gold);
    }
    //--Return
    return score;
  }

  public String debug(File[] data, Properties props){
    //--Variables
    //(get properties)
    String mentionType = props.getProperty("mentionExtractor", "gold");
    String numDocumentsStr = props.getProperty("mistakes");
    int numDocuments = 0;
    if(numDocumentsStr.equalsIgnoreCase("true")){
      numDocuments = 5;
    } else {
      numDocuments = Integer.parseInt(numDocumentsStr);
    }
    //(debug printout)
    StringBuilder debug = new StringBuilder();
    //(documents read)
    int numDocumentsRead = 0;
    //--Run Coreference
    for(File f : data){
      if(numDocumentsRead >= numDocuments){ break; }
      numDocumentsRead += 1;
      SerializedDatum datum = getDatum(f);
      Document doc = datum.document;
      //(get mentions)
      List<Mention> mentions = null;
      if(mentionType.equalsIgnoreCase("gold")){
        mentions = datum.goldMentions;
      } else if(mentionType.equalsIgnoreCase("predicted")) {
        mentions = datum.predictedMentions;
      } else {
        throw new IllegalArgumentException("Unknown mention extractor: " + mentionType);
      }
      //(set mentions)
      datum.document.setMentions(mentions);
      //(run coreference)
      Collection<ClusteredMention> guess = system.runCoreference(datum.document);
      //(enter score)
      Collection<Entity> gold = datum.goldClusters;
      debug.append("=====Document " + doc.id.replaceAll("/",".") + "=====\n")
          .append(doc.debug(guess,gold)).append("\n");
    }
    //--Return
    return debug.toString();
  }

  private static SerializedDatum getDatum(File serializedDatum){
    try{
        if(plaintext){
          //(case: plaintext)
          return SerializedDatum.decode(IOUtils.slurpFile(serializedDatum));
        } else {
          //(case: serialized)
          return  IOUtils.readObjectFromFile(serializedDatum);
        }
      } catch (IOException e) {
        throw new RuntimeException(e);
      } catch (ClassNotFoundException e) {
        throw new RuntimeException(e);
      }
  }

  private static File[] getData(String dataPath, DataType dataType, int count){
    File[] data = new File[count];
    int i=0;
    Iterable<File> files = plaintext
        ? IOUtils.iterFilesRecursive(new File(dataPath + "/" + conllData(dataType)), ".dat")
        : IOUtils.iterFilesRecursive(new File(dataPath + "/" + conllData(dataType)), ".ser.gz");
    for(File serializedDatum : files){
      if(i >= data.length){ break; }
      data[i] = serializedDatum;
      i += 1;
    }
    if(i != data.length) { throw new IllegalArgumentException("Could not load " + count + " documents; only " + i + " available"); }
    return data;
  }

  public static void main(String[] args) {
    //--Get Properties
    Properties props = StringUtils.argsToProperties(args);
    //(order keys)
    PriorityQueue<String> keys = new PriorityQueue<String>();
    int maxLength = 0;
    for(Object key : props.keySet()){
      keys.add(key.toString());
      maxLength = Math.max(maxLength, key.toString().length());
    }
    //(print header)
    System.out.println("--------------------");
    System.out.println(" COREFERENCE TESTER");
    System.out.println("--------------------");
    System.out.println("Options:");
    //(print keys)
    for(String key : keys){
      System.out.print("  -" + key);
      for(int i=0; i<maxLength-key.length(); i++){
        System.out.print(" ");
      }
      System.out.println("    " + props.getProperty(key));
    }
    System.out.println();

    //--Create Coreference Class
    System.out.print("Creating model...");
    //(classname)
    String systemClass = props.getProperty("model","baseline");
    if(systemClass.equalsIgnoreCase("baseline")){ systemClass = BaselineCoreferenceSystem.class.getName(); }
    //(create)
    CoreferenceSystem system;
    try{
      //((try loading the class))
      system = MetaClass.create(systemClass).createInstance();
    } catch(MetaClass.ClassCreationException e){
      //((maybe you forgot to include the package))
      try{
        system = MetaClass.create("cs224n.corefsystems."+systemClass).createInstance();
      } catch(MetaClass.ClassCreationException e2){
        //((give up loading the class))
        throw e;
      }
    }
    System.out.println("done");

    //--Read Data
    System.out.print("Reading documents (lazily)...");
    //(get path)
    dataPath = props.getProperty("path","/afs/ir/class/cs224n/pa3/data/");
    System.out.print("["+dataPath+"]...");
    if(!new File(dataPath).exists()){
      System.out.println("ERROR: no such path");
      System.exit(1);
    }
    if(!new File(dataPath).isDirectory()){
      System.out.println("ERROR: not a directory");
      System.exit(1);
    }
    //(get number of documents)
    int numDocs = -1;
    try {
      numDocs = Integer.parseInt(props.getProperty("documents", "100"));
    } catch(NumberFormatException e) {
      System.out.println("ERROR: not a valid number of documents: " + props.getProperty("documents"));
      System.exit(1);
    }
    if(numDocs <= 0 || numDocs > MAX_TRAIN_EXAMPLES){
      System.out.println("ERROR: not a valid number of documents: " + numDocs + " (must be between 0 and " + MAX_TRAIN_EXAMPLES + ")");
      System.exit(1);
    }
    System.out.print("[" + numDocs + " train]...");
    //(get serialized data)
    //((train))
    File[] train = getData(dataPath, DataType.TRAIN, numDocs);
    //((dev/test))
    String dataTypeString = props.getProperty("data","dev");
    File[] test = null;
    DataType dataType = null;
    try {
      dataType = DataType.valueOf(dataTypeString.toUpperCase());
      System.out.print("["+(dataType == DataType.TEST ? NUM_TEST_EXAMPLES : NUM_DEV_EXAMPLES)+" " + dataType + "]...");
    } catch (IllegalArgumentException e) {
      System.out.println("ERROR: bad -data flag type: " + dataTypeString);
      System.exit(1);
    }
    test = getData(dataPath, dataType, dataType == DataType.TEST ? NUM_TEST_EXAMPLES : NUM_DEV_EXAMPLES);
    System.out.println("done");

    //--TRAIN/TEST
    //(create)
    CoreferenceTester<CoreferenceSystem> tester = new CoreferenceTester<CoreferenceSystem>(system);
    //(traing)
    System.out.println("----------");
    System.out.println(" TRAINING");
    System.out.println("----------");
    CoreferenceScore trainScore = tester.train(train, props);
    //(test)
    System.out.println("---------");
    System.out.println(" TESTING");
    System.out.println("---------");
    CoreferenceScore testScore = tester.test(test, props);
    //(debug)
    if(props.containsKey("mistakes")){
      System.out.println("----------------");
      System.out.println(" DEBUG PRINTOUT");
      System.out.println("----------------");
      System.out.println(tester.debug(test, props));
    }
    //(report)
    System.out.println("--------------------");
    System.out.println(" COREFERENCE SCORES");
    System.out.println("--------------------");
    System.out.println("--Training--");
    trainScore.report();
    System.out.println();
    System.out.println("--"+props.getProperty("data","dev")+"--");
    testScore.report();

  }

}
