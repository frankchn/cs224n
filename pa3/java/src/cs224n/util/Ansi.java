package cs224n.util;

/**
 * @author Gabor Angeli (angeli at cs.stanford)
 */
public class Ansi {

  public static final String[] colors =  {"\033[31m", "\033[32m", "\033[33m", "\033[34m", "\033[35m", "\033[36m"};
  public static final String[] styles = {"", "\033[1m", "\033[3m", "\033[4m"};
  public static final String endFormatting = "\033[0m";
  private static boolean forceAnsi = false;

  public static boolean supportsAnsi(){
    if(forceAnsi){ return true; }
    String os = System.getProperty("os.name").toLowerCase();
    boolean isUnix = os.contains("unix") || os.contains("linux") || os.contains("solaris");
    return Boolean.getBoolean("Ansi") || (isUnix && System.console()!=null);
  }

  public static void forceAnsi(){
    forceAnsi = true;
  }

  public static String prefix(int uniqueNumber){
    int totalFormats = colors.length * styles.length;
    uniqueNumber = uniqueNumber % totalFormats;
    int colorIndex = uniqueNumber % colors.length;
    int styleIndex = uniqueNumber / colors.length;
    return styles[styleIndex] + colors[colorIndex];
  }
}
