package cs224n.coref;

import cs224n.util.Pair;

import java.util.Set;

/**
 * @author Gabor Angeli (angeli at cs.stanford)
 */
public interface Feature {

  //-----------------------------------------------------------
  // TEMPLATE FEATURE TEMPLATES
  //-----------------------------------------------------------
  public static class PairFeature implements Feature {
    public final Pair<Feature,Feature> content;
    public PairFeature(Feature a, Feature b){ this.content = Pair.make(a, b); }
    public String toString(){ return content.toString(); }
    public boolean equals(Object o){ return o instanceof PairFeature && ((PairFeature) o).content.equals(content); }
    public int hashCode(){ return content.hashCode(); }
  }

  public static abstract class Indicator implements Feature {
    public final boolean value;
    public Indicator(boolean value){ this.value = value; }
    public boolean equals(Object o){ return o instanceof Indicator && o.getClass().equals(this.getClass()) && ((Indicator) o).value == value; }
    public int hashCode(){ 
    	return this.getClass().hashCode() ^ Boolean.valueOf(value).hashCode(); }
    public String toString(){ 
    	return this.getClass().getSimpleName() + "(" + value + ")"; }
  }

  public static abstract class IntIndicator implements Feature {
    public final int value;
    public IntIndicator(int value){ this.value = value; }
    public boolean equals(Object o){ return o instanceof IntIndicator && o.getClass().equals(this.getClass()) && ((IntIndicator) o).value == value; }
    public int hashCode(){ 
    	return this.getClass().hashCode() ^ value; 
    }
    public String toString(){ return this.getClass().getSimpleName() + "(" + value + ")"; }
  }

  public static abstract class BucketIndicator implements Feature {
    public final int bucket;
    public final int numBuckets;
    public BucketIndicator(int value, int max, int numBuckets){
      this.numBuckets = numBuckets;
      bucket = value * numBuckets / max;
      if(bucket < 0 || bucket >= numBuckets){ throw new IllegalStateException("Bucket out of range: " + value + " max="+max+" numbuckets="+numBuckets); }
    }
    public boolean equals(Object o){ return o instanceof IntIndicator && o.getClass().equals(this.getClass()) && ((IntIndicator) o).value == bucket; }
    public int hashCode(){ return this.getClass().hashCode() ^ bucket; }
    public String toString(){ return this.getClass().getSimpleName() + "(" + bucket + "/" + numBuckets + ")"; }
  }

  public static abstract class Placeholder implements Feature {
    public Placeholder(){ }
    public boolean equals(Object o){ return o instanceof Placeholder && o.getClass().equals(this.getClass()); }
    public int hashCode(){ return this.getClass().hashCode(); }
    public String toString(){ return this.getClass().getSimpleName(); }
  }

  public static abstract class StringIndicator implements Feature {
    public final String str;
    public StringIndicator(String str){ this.str = str; }
    public boolean equals(Object o){ return o instanceof StringIndicator && o.getClass().equals(this.getClass()) && ((StringIndicator) o).str.equals(this.str); }
    public int hashCode(){ return this.getClass().hashCode() ^ str.hashCode(); }
    public String toString(){ return this.getClass().getSimpleName() + "(" + str + ")"; }
  }

  public static abstract class SetIndicator implements Feature {
    public final Set<String> set;
    public SetIndicator(Set<String> set){ this.set = set; }
    public boolean equals(Object o){ return o instanceof SetIndicator && o.getClass().equals(this.getClass()) && ((SetIndicator) o).set.equals(this.set); }
    public int hashCode(){ return this.getClass().hashCode() ^ set.hashCode(); }
    public String toString(){
      StringBuilder b = new StringBuilder();
      b.append(this.getClass().getSimpleName());
      b.append("( ");
      for(String s : set){
        b.append(s).append(" ");
      }
      b.append(")");
      return b.toString();
    }
  }
  
  /*
   * TODO: If necessary, add new feature types
   */

  //-----------------------------------------------------------
  // REAL FEATURE TEMPLATES
  //-----------------------------------------------------------

  public static class CoreferentIndicator extends Indicator {
    public CoreferentIndicator(boolean coreferent){ super(coreferent); }
  }

  public static class ExactMatch extends Indicator {
    public ExactMatch(boolean exactMatch){ super(exactMatch); }
  }
  
  public static class HeadWordMatch extends Indicator {
    public HeadWordMatch(boolean match){ super(match); }
  }
  
  public static class StrictGenderMatch extends Indicator {
    public StrictGenderMatch(boolean strictGenderMatch){ super(strictGenderMatch); }
  }
  
  public static class PronounI extends Indicator {
    public PronounI(boolean pronounI){ super(pronounI); }
  }
  
  public static class PronounJ extends Indicator {
    public PronounJ(boolean pronounJ){ super(pronounJ); }
  }
  
  public static class ProperNounI extends Indicator {
    public ProperNounI(boolean properNounI){ super(properNounI); }
  }
  
  public static class ProperNounJ extends Indicator {
    public ProperNounJ(boolean properNounJ){ super(properNounJ); }
  }
  
  public static class PossessivePronounI extends Indicator {
    public PossessivePronounI(boolean possessive){ super(possessive); }
  }
  
  public static class PossessivePronounJ extends Indicator {
    public PossessivePronounJ(boolean possessive){ super(possessive); }
  }
  
  public static class ReflexivePronounI extends Indicator {
    public ReflexivePronounI(boolean reflexive){ super(reflexive); }
  }
  
  public static class ReflexivePronounJ extends Indicator {
    public ReflexivePronounJ(boolean reflexive){ super(reflexive); }
  }
  
  public static class SentenceDistance extends IntIndicator {
    public SentenceDistance(int distance){ super(distance); }
  }
  
  public static class NumberAgreement extends Indicator {
    public NumberAgreement(boolean agree){ super(agree); }
  }
  
  public static class NERAgreement extends Indicator {
    public NERAgreement(boolean agree){ super(agree); }
  }
  
  public static class NERCandidate extends StringIndicator {
    public NERCandidate(String type){ super(type); }
  }
  
  public static class GenderAgreement extends IntIndicator {
    public GenderAgreement(int agree){ super(agree); }
  }
  
  public static class BothProperNoun extends Indicator {
    public BothProperNoun(boolean has){ super(has); }
  }
  
  public static class HasPronoun extends Indicator {
    public HasPronoun(boolean has){ super(has); }
  }
  
  /*
   * TODO: Add values to the indicators here.
   */

}
