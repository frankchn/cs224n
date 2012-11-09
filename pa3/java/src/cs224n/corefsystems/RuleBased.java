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

	HashMap<String, HashSet<String>> commonCo = new HashMap<String, HashSet<String>>();

	@Override
	public void train(Collection<Pair<Document, List<Entity>>> trainingData) {
		for(Pair<Document, List<Entity>> pair : trainingData){
			//--Get Variables
			Document doc = pair.getFirst();
			List<Entity> clusters = pair.getSecond();
			List<Mention> mentions = doc.getMentions();

			//--Iterate Over Coreferent Mention Pairs
			for(Entity e : clusters){
				for(Pair<Mention, Mention> mentionPair : e.orderedMentionPairs()){
					String first = mentionPair.getFirst().headWord();
					String second = mentionPair.getSecond().headWord();

					if(!commonCo.containsKey(first))
						commonCo.put(first, new HashSet<String>());
					if(!commonCo.containsKey(second))
						commonCo.put(second, new HashSet<String>());

					commonCo.get(first).add(second);
					commonCo.get(second).add(first);
				}
			}
		}
	}

	private void mergeSets(Set<Mention> a, Set<Mention> b) {
		a.addAll(b);
		b.removeAll(b);
	}

	private boolean areMentionsAppositive(Document doc, Mention m, Mention n) {
		if(!m.parse.getLabel().equals("NP")) return false;
		if(!n.parse.getLabel().equals("NP")) return false;
		if(!m.sentence.equals(n.sentence)) return false;

		if(doc.indexOfMention(m) + 1 == doc.indexOfMention(n)) return true;
		if(doc.indexOfMention(m) + 2 == doc.indexOfMention(n)) 
			if(doc.getMentions().get(doc.indexOfMention(m) + 1).headToken().posTag().equals("VBD")) return true;

		return false;
	}

	private boolean BaselineCoreferenceTest(Set<Mention> a, Set<Mention> b) {
		boolean shouldMerge = false;

		for(Mention m : a) {
			for(Mention n : b) {
				if(commonCo.get(m.headWord()) != null && commonCo.get(m.headWord()).contains(n.headWord())) shouldMerge = true;
			}
		}	

		if(shouldMerge) mergeSets(a, b);
		return shouldMerge;
	}

	private boolean SpeakerTest(Set<Mention> a, Set<Mention> b) {
		boolean shouldMerge = false;

		for(Mention m : a) {
			for(Mention n : b) {
				Pronoun p = Pronoun.valueOrNull(m.gloss());
				if(p == null) continue;
				if(!m.headToken().isQuoted()) continue;
	
				if(m.headToken().speaker().equals(n.gloss()) && (p.speaker == Pronoun.Speaker.FIRST_PERSON))
					shouldMerge = true;
			}
		}

		if(shouldMerge) mergeSets(a, b);
		return shouldMerge;
	}

	private boolean ExactStringTest(Set<Mention> a, Set<Mention> b) {
		boolean shouldMerge = false;
		for(Mention m : a) {
			for(Mention n : b) {
				if(Pronoun.isSomePronoun(m.gloss())) continue;
				if(Pronoun.isSomePronoun(n.gloss())) continue;
				if(m.gloss().equalsIgnoreCase(n.gloss())) { shouldMerge = true; break; }
			}
		}
		if(shouldMerge) mergeSets(a, b);
		return shouldMerge;
	}

	private boolean AppositiveTest(Document doc, Set<Mention> a, Set<Mention> b) {
		boolean shouldMerge = false;
		for(Mention m : a) {
			for(Mention n : b) {
				if(areMentionsAppositive(doc, m, n) && sameAttributes(m, n)) shouldMerge = true;
			}
		}
		if(shouldMerge) mergeSets(a, b);
		return shouldMerge;
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

	private boolean NounPronounTest(Document doc, Set<Mention> a, Set<Mention> b) {
		boolean shouldMerge = true;
		for(Mention m : a) {
			Token m_token = m.headToken();
			Pronoun m_pronoun = Pronoun.valueOrNull(m.gloss());
			if(m_pronoun == null) { shouldMerge = false; continue; }

			for(Mention n : b) {
				Token n_token = n.headToken();
				if(Pronoun.isSomePronoun(n.gloss())) { shouldMerge = false; continue; }

				if(n_token.isPluralNoun()) {
					if(!m_pronoun.plural) continue;
					if(!sameAttributes(m, n)) continue;
					if(doc.indexOfSentence(m.sentence) <= doc.indexOfSentence(n.sentence)) continue;
					shouldMerge = false;
				} else if(!n_token.isQuoted()) {
					if(m_pronoun.plural) continue;
					if(!sameAttributes(m, n)) continue;
					if(doc.indexOfSentence(m.sentence) <= doc.indexOfSentence(n.sentence)) continue;
					shouldMerge = false;
				}
			}
		}
		if(shouldMerge) mergeSets(a, b);
		return shouldMerge;
	}

	private boolean HeadWordTest(Set<Mention> a, Set<Mention> b) {
		boolean shouldMerge = false;
		for(Mention m : a) {
			for(Mention n : b) {
				if(Pronoun.isSomePronoun(m.gloss())) continue;
				if(Pronoun.isSomePronoun(n.gloss())) continue;
				if(m.headWord().equalsIgnoreCase(n.headWord()) && sameAttributes(m, n)) { shouldMerge = true; break; }
			}
		}
		if(shouldMerge) mergeSets(a, b);
		return shouldMerge;
	}

	private boolean InclusionTest(Set<Mention> a, Set<Mention> b) {
		boolean shouldMerge = false;
		for(Mention m : a) {
			for(Mention n : b) {
				if(n.gloss().length() < 8) continue;
				if(m.gloss().toUpperCase().indexOf(n.gloss().toUpperCase()) != -1 && sameAttributes(m, n)) { shouldMerge = true; break; }
			}
		}
		if(shouldMerge) mergeSets(a, b);
		return shouldMerge;
	}

	private Set<String> retrieveModifiers(Mention m) {
		Set<String> r = new HashSet<String>();
		for(int i = m.beginIndexInclusive; i < m.endIndexExclusive; i++) {
			if(m.sentence.posTags.get(i).equals("NP")) r.add(m.sentence.words.get(i));	
		}
		return r;
	}

	private boolean SimilarModifiers(Set<Mention> a, Set<Mention> b) {
		boolean shouldMerge = true;
		for(Mention m : a) {
			Set<String> modifiers_m = retrieveModifiers(m);
			for(Mention n : b) {
				Set<String> modifiers_n = retrieveModifiers(n);
				if(modifiers_n.size() == 0 || modifiers_m.size() == 0) shouldMerge = false;
				for(String z : modifiers_m) {
					if(!modifiers_n.contains(z)) shouldMerge = false;
				}
			}
		}
		if(shouldMerge) mergeSets(a, b);
		return shouldMerge;
	}

	private boolean HobbsTest(Set<Mention> a, Set<Mention> b, Document doc) {
		boolean shouldMerge = false;
		for(Mention m : a) {
			if(!Pronoun.isSomePronoun(m.gloss()) || Pronoun.valueOrNull(m.gloss()) == null) continue;
			Map<Hobbs.Candidate, Integer> m_hobbs = Hobbs.getHobbsCandidates(m);
			for(Mention n : b) {
				if(!sameAttributes(m, n)) break;
				for(Hobbs.Candidate h : m_hobbs.keySet()) {
					if(doc.indexOfSentence(n.sentence) == h.sentenceIndex &&
					   n.beginIndexInclusive == h.wordIndexStart &&
					   n.endIndexExclusive == h.wordIndexEnd) {
						shouldMerge = true;
					}
				}
			}
		}
		return shouldMerge;
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
				if(ExactStringTest(a, b)) break;
			}
		}

/*
		for(Set<Mention> a : clusters) {
			for(Set<Mention> b : clusters) {
				if(a.equals(b)) continue;
				if(NounPronounTest(doc, a, b)) break;
			}
		}
*/

		for(Set<Mention> a : clusters) {
			for(Set<Mention> b : clusters) {
				if(a.equals(b)) continue;
				if(BaselineCoreferenceTest(a, b)) break;
			}
		}


		for(Set<Mention> a : clusters) {
			for(Set<Mention> b : clusters) {
				if(a.equals(b)) continue;
				if(HobbsTest(a, b, doc)) break;
			}
		}

		for(Set<Mention> a : clusters) {
			for(Set<Mention> b : clusters) {
				if(a.equals(b)) continue;
				if(HeadWordTest(a, b)) break;
			}
		}


/*
		for(Set<Mention> a : clusters) {
			for(Set<Mention> b : clusters) {
				if(a.equals(b)) continue;
				if(InclusionTest(a, b)) break;
			}
		}
*/



		for(Set<Mention> a : clusters) {
			for(Set<Mention> b : clusters) {
				if(a.equals(b)) continue;
				if(SpeakerTest(a, b)) break;
			}
		} 


		for(Set<Mention> a : clusters) {
			for(Set<Mention> b : clusters) {
				if(a.equals(b)) continue;
				if(SimilarModifiers(a, b)) break;
			}
		}


/*
		for(Set<Mention> a : clusters) {
			for(Set<Mention> b : clusters) {
				if(a.equals(b)) continue;
				if(AppositiveTest(doc, a, b)) break;
			}
		}


*/

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
