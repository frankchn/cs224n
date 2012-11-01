package cs224n.coref;

/**
 * Denotes a Gender. Can be one of:
 * <ul>
 *   <li><b>MALE: </b> The male gender</li>
 *   <li><b>FEMALE: </b> The female gender</li>
 *   <li><b>NEUTRAL: </b>: </b> Explicitly specifies a lack of gender</li>
 *   <li><b>EITHER: </b> Does not specify a gender</li>
 * </ul>
 *
 * @author Gabor Angeli (angeli at cs.stanford)
 */
public enum Gender {
  MALE, FEMALE, NEUTRAL, EITHER;

  public boolean isCompatible(Gender other){
    return other == this || other == EITHER || this == EITHER;
  }

  public boolean isAnimate(){
    return this != NEUTRAL;
  }
}
