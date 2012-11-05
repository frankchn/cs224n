package cs224n.corefsystems;

import java.util.*;
import cs224n.coref.*;
import cs224n.ling.*;
import cs224n.util.*;

/*
java -Xmx500m -cp "extlib/*:classes" cs224n.assignments.CoreferenceTester \
     -path /afs/ir/class/cs224n/pa3/data/ \
     -model RuleBased -data treebank -documents 100
*/

public class RuleBased implements CoreferenceSystem {

	private HashMap<Mention, Entity> globalMentions = new HashMap<Mention, Entity>();

	private void StrictStringEquivalence(Document doc) {
		List<Mention> ms = doc.getMentions();
		Map<String, Entity> clusters = new HashMap<String, Entity>();

		for(int i = 0; i < ms.size(); i++) {
			for(int j = 0; j < ms.size(); j++) {
				if(i == j) continue;
				if(Pronoun.isSomePronoun(ms.get(i).gloss())) continue;
				if(Pronoun.isSomePronoun(ms.get(j).gloss())) continue;

				if(ms.get(i).gloss().equals(ms.get(j).gloss())) {
					if(!clusters.containsKey(ms.get(i).gloss())) {
						clusters.put(ms.get(i).gloss(), ms.get(i).markSingleton().entity);
					}
					globalMentions.put(ms.get(i), clusters.get(ms.get(i).gloss()));
				}
			}
		}
	}

	private void HeadWordEquivalence(Docuemnt doc) {

	}

	@Override
	public void train(Collection<Pair<Document, List<Entity>>> trainingData) {
		// We are not going to train anything since this is rule-based
		return;
	}

	@Override
	public List<ClusteredMention> runCoreference(Document doc) {

		List<ClusteredMention> mentions = new ArrayList<ClusteredMention>();

		int coreferent = 0;
		int singleton = 0;

		StrictStringEquivalence(doc);


		

		for(Mention m : doc.getMentions()) {
			if(globalMentions.containsKey(m)) {
				mentions.add(m.markCoreferent(globalMentions.get(m)));
				coreferent++;
			} else {
				mentions.add(m.markSingleton());
				singleton++;
			}
		}

		System.out.println("Coreferent: " + coreferent + 
                           ", Singleton: " + singleton + 
                           ", Total: " + (coreferent + singleton));

		return mentions;
	}

}
