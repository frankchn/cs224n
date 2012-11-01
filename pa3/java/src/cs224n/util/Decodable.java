package cs224n.util;

/**
 * @author Gabor Angeli (angeli at cs.stanford)
 */
public interface Decodable {
  public static final char ESCAPE_CHAR = '\\';
	public String encode();
}
