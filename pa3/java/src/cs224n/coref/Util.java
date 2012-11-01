package cs224n.coref;

import cs224n.util.Pair;

/**
 * @author Gabor Angeli (angeli at cs.stanford)
 */
public class Util {

  public static Pair<Boolean,Boolean> haveGenderAndAreSameGender(Mention a, Mention b){
    //(names)
    Name nameA = Name.get(a.gloss());
    Name nameB = Name.get(b.gloss());
    //(pronouns)
    Pronoun proA = Pronoun.valueOrNull(a.gloss().toUpperCase().replaceAll(" ", "_"));
    Pronoun proB = Pronoun.valueOrNull(b.gloss().toUpperCase().replaceAll(" ", "_"));
    //(error conditions)
    if(nameA == null && proA == null){ return Pair.make(false, false); }
    if(nameB == null && proB == null){ return Pair.make(false, false); }
    //(compare genders)
    Gender genderA = proA == null ? nameA.gender : proA.gender;
    Gender genderB = proB == null ? nameB.gender : proB.gender;
    return Pair.make( true, genderA.isCompatible(genderB) );
  }

  public static Pair<Boolean,Boolean> haveGenderAndAreSameGender(Mention a, Entity entity){
    for(Mention m : entity.mentions){
      Pair<Boolean, Boolean> pair = haveGenderAndAreSameGender(a, m);
      if(pair.getFirst()){ return Pair.make(true, pair.getSecond()); }
    }
    return Pair.make(false, false);
  }

  public static Pair<Boolean,Boolean> haveNumberAndAreSameNumber(Mention a, Mention b){
    //(names)
    boolean nounA = a.headToken().isNoun();
    boolean nounB = b.headToken().isNoun();
    //(pronouns)
    Pronoun proA = Pronoun.valueOrNull(a.gloss().toUpperCase().replaceAll(" ","_"));
    Pronoun proB = Pronoun.valueOrNull(b.gloss().toUpperCase().replaceAll(" ","_"));
    //(error conditions)
    if(!nounA && proA == null){ return Pair.make(false, false); }
    if(!nounB && proB == null){ return Pair.make(false, false); }
    //(compare genders)
    boolean pluralA = proA == null ? a.headToken().isPluralNoun() : proA.plural;
    boolean pluralB = proB == null ? b.headToken().isPluralNoun() : proB.plural;
    return Pair.make( true, pluralA == pluralB );
  }

  public static Pair<Boolean,Boolean> haveNumberAndAreSameNumber(Mention a, Entity entity){
    for(Mention m : entity.mentions){
      Pair<Boolean, Boolean> pair = haveNumberAndAreSameNumber(a, m);
      if(pair.getFirst()){ return Pair.make(true, pair.getSecond()); }
    }
    return Pair.make(false, false);
  }

}
