package cs224n.corefsystems;

import java.util.*;
import cs224n.coref.*;
import cs224n.coref.Sentence.*;
import cs224n.ling.*;
import cs224n.util.*;

/*
java -Xmx500m -cp "extlib/*:classes" cs224n.assignments.CoreferenceTester \
     -path /afs/ir/class/cs224n/pa3/data/ \
     -model RuleBased -data dev -documents 100
*/

public class RuleBased implements CoreferenceSystem {

	private Map<Mention, Entity> globalMentions = new HashMap<Mention, Entity>();

	/* Strict String Matching as a first pass */
	private void StrictStringMatch(Document doc) {
		
		Map<String,Entity> clusters = new HashMap<String,Entity>();
		Counter<String> counts = new Counter<String>();

		for(Mention m : doc.getMentions())
			counts.incrementCount(m.gloss(), 1);

		for(Mention m : doc.getMentions()) {
			if(Pronoun.isSomePronoun(m.gloss())) continue;
			if(counts.getCount(m.gloss()) > 1) {
				// this is for when we first see it. the next time we see it, we mark as coreferent
				if(!clusters.containsKey(m.gloss())) clusters.put(m.gloss(), m.markSingleton().entity);
				globalMentions.put(m, m.markCoreferent(clusters.get(m.gloss())).entity);
			}
		}

	}

	/* Try to match Head Words while filtering pronouns */
	private void MatchHeadWords(Document doc) {
		for(Mention m : doc.getMentions()) {
			if(globalMentions.containsKey(m)) continue;
			if(Pronoun.isSomePronoun(m.gloss())) continue;
			for(Mention n : doc.getMentions()) {
				if(m == n) continue;
				String mHead = m.headWord().toLowerCase();
				String nHead = n.headWord().toLowerCase();
				if(mHead.equals(nHead)) {
					if(!globalMentions.containsKey(n))
						globalMentions.put(n, n.markSingleton().entity);

					globalMentions.put(m, m.markCoreferent(globalMentions.get(n)).entity);
					break;
				}
			}
		}
	}

	private void NounPronounMatch(Document doc) {
		for(Mention m : doc.getMentions()) {
			if(globalMentions.containsKey(m)) continue;
			Pronoun p_m = Pronoun.valueOrNull(m.gloss()); // pronoun
			if(!Pronoun.isSomePronoun(m.gloss()) || p_m == null) continue;

			for(Mention n : doc.getMentions()) {
				if(Pronoun.isSomePronoun(n.gloss())) continue;
				Token curNoun = n.headToken();

				if(!Name.gender(curNoun.word()).isCompatible(p_m.gender)) continue;
				if(doc.indexOfSentence(m.sentence) - doc.indexOfSentence(n.sentence) < 0 ||
				   doc.indexOfSentence(m.sentence) - doc.indexOfSentence(n.sentence) > 1) continue;

				if(curNoun.isPluralNoun()) {
					if(p_m.plural) continue;
				} else {
					if(!p_m.plural) continue;
				}

				if(!globalMentions.containsKey(n))
					globalMentions.put(n, n.markSingleton().entity);


				globalMentions.put(m, m.markCoreferent(globalMentions.get(n)).entity);
				break;
			}
		}
	}

	private void MatchStrictPronouns(Document doc) {
		for(Mention m : doc.getMentions()) {
			if(globalMentions.containsKey(m)) continue;
			Pronoun p_m = Pronoun.valueOrNull(m.gloss());
			if(!Pronoun.isSomePronoun(m.gloss()) || p_m == null) continue;
			
			for(Mention n : doc.getMentions()) {
				Pronoun p_n = Pronoun.valueOrNull(m.gloss());
				if(!Pronoun.isSomePronoun(n.gloss()) || p_n == null) continue;

				if(p_m == p_n) {
					if(!globalMentions.containsKey(n))
						globalMentions.put(n, n.markSingleton().entity);

					globalMentions.put(m, m.markCoreferent(globalMentions.get(n)).entity);
					break;
				}
			}
		}
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

		StrictStringMatch(doc);
		NounPronounMatch(doc);
		MatchStrictPronouns(doc);
		MatchHeadWords(doc);

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
