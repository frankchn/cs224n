package cs224n.coref;

import cs224n.assignments.CoreferenceTester;
import cs224n.util.IOUtils;
import cs224n.util.Pair;

import java.io.IOException;
import java.util.HashMap;

/**
 * Denotes some information about a name; the static class
 * contains a collection of possible names (stored in a file and initialized
 * on first use) and some information about them.
 *
 * @author Gabor Angeli (angeli at cs.stanford)
 */
public class Name {

  public final String gloss;
  public final Gender gender;
  public final double percentUse;
  public final double percentUseMale;
  public final double percentUseFemale;
  public final Pair<Integer,Integer> yearsUsed;

  private Name(String gloss, Gender gender, double percentUseMale, double percentUseFemale, double percentUse, Pair<Integer,Integer> yearsUsed){
    this.gloss = gloss;
    this.gender = gender;
    this.percentUseMale = percentUseMale;
    this.percentUseFemale = percentUseFemale;
    this.percentUse = percentUse;
    this.yearsUsed = yearsUsed;
  }

  /**
   * Give the most common gender for this name
   * @return Male or Female, depending on which usage is more likely
   */
  public Gender mostLikelyGender(){
    if(this.gender == Gender.EITHER){
      if(this.percentUseMale > this.percentUseFemale){
        return Gender.MALE;
      } else {
        return Gender.FEMALE;
      }
    } else {
      return this.gender;
    }
  }

  private static class NameInfo {
    private int yearsUsed = 0;
    private Gender gender = Gender.NEUTRAL;
    private double percentSum = 0.0;
    private double percentMale = 0.0;
    private double percentFemale = 0.0;
    private int startYear = 3000;
    private int endYear = 0;
  }

  private static HashMap<String,Name> names;

  /**
   * Deterimine whether the given String is a name
   * @param name The name to look up
   * @return true if the supplied name is listed as a name
   */
  public static boolean isName(String name){ return names.keySet().contains(name.toLowerCase()); }

  /**
   * Find the name associated with the given String
   * @param name The name to look up
   * @return A Name object representing our knowledge of that name
   */
  public static Name get(String name){ return names.get(name.toLowerCase()); }

  /**
   * Find the declared gender of a given name
   * @param name The name to look up
   * @return The gender of that name, if it exists; alternately Gender.EITHER if the name is not found
   */
  public static Gender gender(String name){
    if(names.containsKey(name.toLowerCase())){
      return names.get(name.toLowerCase()).gender;
    } else {
      return Gender.NEUTRAL;
    }
  }

  /**
   * Find the most likely gender of a given name
   * @param name The name to look up
   * @return The gender of that name, if it exists; alternately Gender.EITHER if the name is not found
   */
  public static Gender mostLikelyGender(String name){
    if(names.containsKey(name.toLowerCase())){
      return names.get(name.toLowerCase()).mostLikelyGender();
    } else {
      return Gender.NEUTRAL;
    }
  }


  static {
    try {
      //--Read File
      String[] names = IOUtils.slurpFile(CoreferenceTester.dataPath+"/baby_names.dat").split("\n");
      HashMap<String,NameInfo> nameInfo = new HashMap<String,NameInfo>();
      //--Collect Info
      for(String name : names){
        //(split info)
        String[] rawInfo = name.split(" ");
        //(parse info)
        int year = Integer.parseInt(rawInfo[0]);
        String gloss = rawInfo[1];
        double percent = Double.parseDouble(rawInfo[2]);
        Gender gender;
        if(rawInfo[3].equals("girl")){
          gender = Gender.FEMALE;
        } else if(rawInfo[3].equals("boy")){
          gender = Gender.MALE;
        } else{
          throw new IllegalArgumentException("Unknown gender: " + rawInfo[3]);
        }
        //(ensure map entry)
        if(!nameInfo.containsKey(gloss)){ nameInfo.put(gloss, new NameInfo()); }
        //(update entry)
        NameInfo info = nameInfo.get(gloss);
        info.yearsUsed += 1;
        info.percentSum += percent;
        if(gender == Gender.MALE){
          info.percentMale += percent;
        } else {
          info.percentFemale += percent;
        }

        info.startYear = Math.min(info.startYear, year);
        info.endYear = Math.max(info.endYear, year);
        if(info.gender == Gender.NEUTRAL){
          info.gender = gender;
        } else {
          info.gender = Gender.EITHER;
        }
      }
      //--Build Names
      Name.names = new HashMap<String, Name>();
      for(String gloss : nameInfo.keySet()){
        NameInfo info = nameInfo.get(gloss);
        Name.names.put(
            gloss.toLowerCase(),
            new Name(
                gloss,
                info.gender,
                info.percentMale / (double) info.yearsUsed,
                info.percentFemale / (double) info.yearsUsed,
                info.percentSum / (double) info.yearsUsed,
                Pair.make(info.startYear,info.endYear)) );
      }
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}
