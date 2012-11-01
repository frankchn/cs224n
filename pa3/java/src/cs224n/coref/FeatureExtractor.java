package cs224n.coref;

import edu.stanford.nlp.stats.ClassicCounter;
import edu.stanford.nlp.stats.Counter;

/**
 * @author Gabor Angeli (angeli at cs.stanford)
 */
public abstract class FeatureExtractor<Input, Encoding, Output> {

  public Counter<Encoding> extractFeatures(Input input){
    Counter <Encoding> in = new ClassicCounter<Encoding>();
		Counter <Encoding> out = new ClassicCounter <Encoding> ();
		fillFeatures(input, in, null, out);
    return in;
  }

	public Counter<Encoding> extractFeatures(Input input, Output output){
		Counter <Encoding> in = new ClassicCounter<Encoding>();
		Counter <Encoding> out = new ClassicCounter <Encoding> ();
		fillFeatures(input, in, output, out);

		Counter <Encoding> rtn = new ClassicCounter <Encoding> ();
		for(Encoding e1 : in.keySet()){
			for(Encoding e2 : out.keySet()){
				rtn.setCount(concat(e2, e1), in.getCount(e1) * out.getCount(e2));
			}
		}

		return rtn;
	}

	protected abstract void fillFeatures(
			Input input,
			Counter <Encoding> inFeatures,
			Output output,
			Counter <Encoding> outFeatures);
	protected abstract Encoding concat(Encoding a, Encoding b);

}
