package cs224n.coref;

/**
 * Denotes a mention which is tagged with the entity it refers to.
 * @author Gabor Angeli (angeli at cs.stanford)
 */
public class ClusteredMention {
  private static final long serialVersionUID = 1L;
  /**
   * The mention associated with this clustered mention
   */
  public final Mention mention;
  /**
   * The entity this clustered mention refers to
   */
  public final Entity entity;
  protected ClusteredMention(Mention mention, Entity entity){
    this.mention = mention;
    this.entity = entity;
  }

  @Override
  public boolean equals(Object o){
    if(o instanceof ClusteredMention){
      ClusteredMention other = (ClusteredMention) o;
      return other.mention.equals(this.mention) && other.entity == this.entity;
    } else {
      return false;
    }
  }
  @Override
  public int hashCode(){ return mention.hashCode() ^ entity.hashCode(); }
  @Override
  public String toString(){
    return mention.toString() + " in " + entity.toString();
  }
}
