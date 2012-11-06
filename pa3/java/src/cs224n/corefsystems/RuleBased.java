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

	@Override
	public void train(Collection<Pair<Document, List<Entity>>> trainingData) {

	}

	private void mergeSets(Set<Mention> a, Set<Mention> b) {
		a.addAll(b);
		b.removeAll(b);
	}

	private void ExactStringTest(Set<Mention> a, Set<Mention> b) {
		boolean shouldMerge = false;
		for(Mention m : a) {
			for(Mention n : b) {
				if(Pronoun.isSomePronoun(m.gloss())) continue;
				if(Pronoun.isSomePronoun(n.gloss())) continue;
				if(m.gloss().equalsIgnoreCase(n.gloss())) { shouldMerge = true; break; }
			}
		}
		if(shouldMerge) mergeSets(a, b);
	}

	private boolean sameAttributes(Mention m, Mention n) {
		Pair<Boolean, Boolean> gender = Util.haveGenderAndAreSameGender(m, n);
		if(gender.getFirst() && !gender.getSecond()) return false;

		Pair<Boolean, Boolean> number = Util.haveNumberAndAreSameNumber(m, n);
		if(number.getFirst() && !number.getSecond()) return false;

		if(!m.headToken().nerTag().equals(n.headToken().nerTag()) && 
		   !m.headToken().nerTag().equals("O") &&
		   !n.headToken().nerTag().equals("O")) return false;

		return true;
	}

	// Doesn't work!
	private void ExactPronounPronounTest(Set<Mention> a, Set<Mention> b) {
		boolean shouldMerge = true;
		for(Mention m : a) {
			for(Mention n : b) {
				Pronoun p_m = Pronoun.valueOrNull(m.gloss());
				Pronoun p_n = Pronoun.valueOrNull(m.gloss());
				if(p_m == null || p_n == null) continue;
				
				if(!sameAttributes(m, n)) shouldMerge = false;

				if(!shouldMerge) break;
			}
			if(!shouldMerge) break;
		}
		if(shouldMerge) mergeSets(a, b);
	}

	private void HeadWordTest(Set<Mention> a, Set<Mention> b) {
		boolean shouldMerge = false;
		for(Mention m : a) {
			for(Mention n : b) {
				if(Pronoun.isSomePronoun(m.gloss())) continue;
				if(Pronoun.isSomePronoun(n.gloss())) continue;
				if(m.headWord().equalsIgnoreCase(n.headWord()) && sameAttributes(m, n)) { shouldMerge = true; break; }
			}
		}
		if(shouldMerge) mergeSets(a, b);
	}

	@Override
	public List<ClusteredMention> runCoreference(Document doc) {
		List<ClusteredMention> mentions = new ArrayList<ClusteredMention>();
		Set<Set<Mention>> clusters = new HashSet<Set<Mention>>();

		for(Mention m : doc.getMentions())
			clusters.add(new HashSet<Mention>(Arrays.asList(new Mention[] { m })));

		for(Set<Mention> a : clusters) {
			for(Set<Mention> b : clusters) {
				if(a.equals(b)) continue;
				ExactStringTest(a, b);
			}
		}

		for(Set<Mention> a : clusters) {
			for(Set<Mention> b : clusters) {
				if(a.equals(b)) continue;
				HeadWordTest(a, b);
			}
		}

		for(Set<Mention> a : clusters) {
			ClusteredMention c = null;
			for(Mention m : a) {
				if(c != null) 
					mentions.add(m.markCoreferent(c));
				else {
					c = m.markSingleton();
					mentions.add(c);
				}
			}
		}

		return mentions;
	}

}
