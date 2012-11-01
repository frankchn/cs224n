package cs224n.coref;

import java.util.HashSet;
import java.util.Set;

/**
 *
 * An enumeration of common pronouns and some of their properties.
 *
 * Gender denotes the gender of the pronoun; Speaker is one of First Person, Second Person, or
 * Third person; Type denotes the usage of the pronoun; plural denotes whether the pronoun
 * is plural.
 * On occasion, a word can act as multiple pronouns (e.g. "you"); in this case the most common
 * version (or an arbitrary version) is chosen to be characterized.
 *
 * In addition, allPronouns() returns a larger list of pronouns (unclassified), and
 * isSomPronoun() return true if the given String is a pronoun in this larger list.
 * Note that some [rare] pronouns will match isSomePronoun() but will not be in the enum.
 *
 * @author Gabor Angeli (angeli at cs.stanford)
 */
public enum Pronoun {

  I          ( Gender.EITHER,      Speaker.FIRST_PERSON,   Type.SUBJECTIVE,           false ),
  ME         ( Gender.EITHER,      Speaker.FIRST_PERSON,   Type.OBJECTIVE,            false ),
  MYSELF     ( Gender.EITHER,      Speaker.FIRST_PERSON,   Type.REFLEXIVE,            false ),
  MINE       ( Gender.EITHER,      Speaker.FIRST_PERSON,   Type.POSESSIVE_PRONOUN,    false ),
  MY         ( Gender.EITHER,      Speaker.FIRST_PERSON,   Type.POSESSIVE_DETERMINER, false ),
  WE         ( Gender.EITHER,      Speaker.FIRST_PERSON,   Type.SUBJECTIVE,           true  ),
  US         ( Gender.EITHER,      Speaker.FIRST_PERSON,   Type.OBJECTIVE,            true  ),
  OURSELF    ( Gender.EITHER,      Speaker.FIRST_PERSON,   Type.REFLEXIVE,            true  ),
  OURSELVES  ( Gender.EITHER,      Speaker.FIRST_PERSON,   Type.REFLEXIVE,            true  ),
  OURS       ( Gender.EITHER,      Speaker.FIRST_PERSON,   Type.POSESSIVE_PRONOUN,    true  ),
  OUR        ( Gender.EITHER,      Speaker.FIRST_PERSON,   Type.POSESSIVE_DETERMINER, true  ),
  
	YOU        ( Gender.EITHER,      Speaker.SECOND_PERSON,  Type.SUBJECTIVE,           false ),
  YOURSELF   ( Gender.EITHER,      Speaker.SECOND_PERSON,  Type.REFLEXIVE,            false ),
  YOURS      ( Gender.EITHER,      Speaker.SECOND_PERSON,  Type.POSESSIVE_PRONOUN,    false ),
  YOUR       ( Gender.EITHER,      Speaker.SECOND_PERSON,  Type.POSESSIVE_DETERMINER, false ),
  YOURSELVES ( Gender.EITHER,      Speaker.SECOND_PERSON,  Type.REFLEXIVE,            true  ),
	
	HE         ( Gender.MALE,        Speaker.THIRD_PERSON,   Type.SUBJECTIVE,           false ),
	HIM        ( Gender.MALE,        Speaker.THIRD_PERSON,   Type.OBJECTIVE,            false ),
  HIMSELF    ( Gender.MALE,        Speaker.THIRD_PERSON,   Type.REFLEXIVE,            false ),
  HIS        ( Gender.MALE,        Speaker.THIRD_PERSON,   Type.POSESSIVE_PRONOUN,    false ),
	SHE        ( Gender.FEMALE,      Speaker.THIRD_PERSON,   Type.SUBJECTIVE,           false ),
	HER        ( Gender.FEMALE,      Speaker.THIRD_PERSON,   Type.OBJECTIVE,            false ),
  HERSELF    ( Gender.FEMALE,      Speaker.THIRD_PERSON,   Type.REFLEXIVE,            false ),
  HERS       ( Gender.FEMALE,      Speaker.THIRD_PERSON,   Type.POSESSIVE_PRONOUN,    false ),
	IT         ( Gender.NEUTRAL,     Speaker.THIRD_PERSON,   Type.SUBJECTIVE,           false ),
  ITSELF     ( Gender.NEUTRAL,     Speaker.THIRD_PERSON,   Type.REFLEXIVE,            false ),
  ITS        ( Gender.NEUTRAL,     Speaker.THIRD_PERSON,   Type.POSESSIVE_PRONOUN,    false ),
	THEY       ( Gender.EITHER,      Speaker.THIRD_PERSON,   Type.SUBJECTIVE,           true  ),
	THEM       ( Gender.EITHER,      Speaker.THIRD_PERSON,   Type.OBJECTIVE,            true  ),
  THEMSELVES ( Gender.EITHER,      Speaker.THIRD_PERSON,   Type.REFLEXIVE,            true  ),
  THEIRSELVES( Gender.EITHER,      Speaker.THIRD_PERSON,   Type.REFLEXIVE,            true  ),
  THEIRS     ( Gender.EITHER,      Speaker.THIRD_PERSON,   Type.POSESSIVE_PRONOUN,    true  ),
  THEIR      ( Gender.EITHER,      Speaker.THIRD_PERSON,   Type.POSESSIVE_DETERMINER, true  );


  public static enum Type { SUBJECTIVE, OBJECTIVE, REFLEXIVE, POSESSIVE_PRONOUN, POSESSIVE_DETERMINER }
  public static enum Speaker{ FIRST_PERSON, SECOND_PERSON, THIRD_PERSON }

  public final Gender gender;
  public final Speaker speaker;
  public final Type type;
  public final boolean plural;

  private Pronoun(Gender gender, Speaker speaker, Type type, boolean plural){
    this.gender = gender;
    this.speaker = speaker;
    this.type = type;
    this.plural = plural;
  }

  private static String[] pronounList = new String[]{
      "i",
      "all",
      "he",
      "her",
      "hers",
      "herself",
      "him",
      "himself",
      "his",
      "hisself",
      "it",
      "its",
      "itself",
      "me",
      "mine",
      "my",
      "myself",
      "one",
      "one's",
      "oneself",
      "our",
      "ours",
      "ourself",
      "ourselves",
      "she",
      "thee",
      "their",
      "theirs",
      "theirselves",
      "them",
      "themself",
      "themselves",
      "they",
      "thine",
      "thou",
      "thy",
      "thyself",
      "us",
      "we",
      "y'all",
      "y'all's",
      "y'all's selves",
      "ye",
      "you",
      "you all",
      "your",
      "yours",
      "yourself",
      "yourselves",
      "youse",
  };
  private static Set<String> pronounSet = new HashSet<String>();

  /**
   * The known pronouns
   * @return An iterable over the known pronouns
   */
  public static Iterable<String> allPronouns(){
    return pronounSet;
  }

  /**
   * Determine whether a candidate String is a pronoun
   * @param cand The candidate String
   * @return true if the String is a pronoun
   */
  public static boolean isSomePronoun(String cand){
    return pronounSet.contains(cand.toLowerCase());
  }

  public static Pronoun valueOrNull(String value){
    for(Pronoun p : values()){
      if(p.name().equalsIgnoreCase(value)){ return p; }
    }
    return null;
  }

  static {
    for(String pronoun : pronounList){
      if(pronounSet.contains(pronoun)){ throw new IllegalStateException("Duplicate pronoun: " + pronoun); }
      pronounSet.add(pronoun);
    }
  }
}
