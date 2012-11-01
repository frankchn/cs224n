package edu.stanford.nlp.util.logging;

import java.util.Properties;

/**
 * @author Gabor Angeli (angeli at cs.stanford)
 */
public class StanfordRedwoodConfiguration extends RedwoodConfiguration {
  /**
   * Private constructor to prevent use of "new RedwoodConfiguration()"
   */
  private StanfordRedwoodConfiguration() {
    super();
  }

  public static void apply(Properties props){
    //--Tweak Properties
    //(capture system streams)
    if(props.getProperty("log.captureStderr") == null){
      props.setProperty("log.captureStderr", "true");
    }
    //(log to stderr)
    if(props.getProperty("log.toStderr") == null){
      props.setProperty("log.toStderr", "true");
    }
    //(apply properties)
    RedwoodConfiguration.apply(props);

    //--Strange Tweaks
    //(adapt legacy logging systems)
    JavaUtilLoggingAdaptor.adapt();
    //(skip stack trace elements from this class)
    Redwood.addLoggingClass("edu.stanford.nlp.kbp.slotfilling.common.Log");

    // TODO: Redwood.setIgnorableClassPrefix("edu.stanford.nlp")
  }

  public static void setup(){
    apply(new Properties());
  }

  public static void main(String[] args){
    setup();
    Redwood.startTrack("A Track");
    Redwood.log("a message");
    Redwood.endTrack("A Track");
    RedwoodConfiguration.current().printChannels(40).apply();
    Redwood.log("indented");
    System.out.println("Should print normally");
  }
}
