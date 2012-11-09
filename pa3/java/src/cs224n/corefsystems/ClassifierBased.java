package cs224n.corefsystems;

import cs224n.coref.*;
import cs224n.corefsystems.Hobbs.Candidate;
import cs224n.util.Pair;
import edu.stanford.nlp.classify.LinearClassifier;
import edu.stanford.nlp.classify.LinearClassifierFactory;
import edu.stanford.nlp.classify.RVFDataset;
import edu.stanford.nlp.ling.RVFDatum;
import edu.stanford.nlp.stats.Counter;
import edu.stanford.nlp.util.Triple;
import edu.stanford.nlp.util.logging.RedwoodConfiguration;
import edu.stanford.nlp.util.logging.StanfordRedwoodConfiguration;

import java.text.DecimalFormat;
import java.util.*;
import java.util.concurrent.locks.Condition;

import static edu.stanford.nlp.util.logging.Redwood.Util.*;

/**
 * @author Gabor Angeli (angeli at cs.stanford)
 */
public class ClassifierBased implements CoreferenceSystem {

	private static <E> Set<E> mkSet(E[] array){
		Set<E> rtn = new HashSet<E>();
		Collections.addAll(rtn, array);
		return rtn;
	}

	private static final Set<Object> ACTIVE_FEATURES = mkSet(new Object[]{

			/*
			 * TODO: Create a set of active features
			 */

			Feature.HeadWordMatch.class,
            Pair.make(Feature.HasPronoun.class, Feature.NumberAgreement.class),
            Feature.GenderAgreement.class,
            Feature.NumberAgreement.class,
            Feature.PossessivePronounI.class,
            Feature.PossessivePronounJ.class,
            Feature.ReflexivePronounI.class,
            Feature.ReflexivePronounJ.class,
            Pair.make(Feature.PronounI.class, Feature.HobbsDistance.class),
            Feature.SentenceDistance.class,
            Feature.MentionPair.class,
            Feature.POSPair.class,
            Pair.make(Feature.NERCandidate.class, Feature.MentionI.class),
            Feature.PronounStrictGender.class,
            //Pair.make(Feature.HasPronoun.class, Feature.NERAgreement.class),
            //Pair.make(Feature.GenderAgreement.class, Feature.NumberAgreement.class),
            //Feature.PronounJ.class,
            //Pair.make(Feature.HasPronoun.class, Feature.HeadWordMatch.class),
            //Pair.make(Feature.PronounI.class, Feature.PronounJ.class),
            //Pair.make(Feature.SentenceDistance.class, Feature.PronounI.class),
            //Pair.make(Feature.ProperNounJ.class, Feature.PronounI.class),
            //Feature.NERAgreement.class,
            //Pair.make(Feature.HasPronoun.class, Feature.NumberAgreement.class),
            //Pair.make(Feature.HasPronoun.class, Feature.StrictGenderMatch.class),
            //Feature.PronounI.class,
            //Feature.PronounJ.class,
            //Feature.ProperNounI.class,
            //Feature.ProperNounJ.class,
	});
	
	private Map<Mention, Map<Hobbs.Candidate, Integer>> hobbsCache = 
	  new HashMap<Mention, Map<Hobbs.Candidate, Integer>>();


	private LinearClassifier<Boolean,Feature> classifier;

	public ClassifierBased(){
		StanfordRedwoodConfiguration.setup();
		RedwoodConfiguration.current().collapseApproximate().apply();
	}

	public FeatureExtractor<Pair<Mention,ClusteredMention>,Feature,Boolean> extractor = new FeatureExtractor<Pair<Mention, ClusteredMention>, Feature, Boolean>() {
		private <E> Feature feature(Class<E> clazz, Pair<Mention,ClusteredMention> input, Option<Double> count){
			
			//--Variables
			Mention onPrix = input.getFirst(); //the first mention (referred to as m_i in the handout)
			Mention candidate = input.getSecond().mention; //the second mention (referred to as m_j in the handout)
			Entity candidateCluster = input.getSecond().entity; //the cluster containing the second mention
      
			//--Features
			if(clazz.equals(Feature.ExactMatch.class)){
				//(exact string match)
				return new Feature.ExactMatch(onPrix.gloss().equals(candidate.gloss()));
			}
			else if (clazz.equals(Feature.HeadWordMatch.class)) {
                return new Feature.HeadWordMatch(onPrix.headWord().toLowerCase().equals(candidate.headWord().toLowerCase()));
			}
			else if (clazz.equals(Feature.HasPronoun.class)) {
                return new Feature.HasPronoun(Pronoun.isSomePronoun(onPrix.gloss()) || Pronoun.isSomePronoun(candidate.gloss()));
			}
			else if (clazz.equals(Feature.BothProperNoun.class)) {
			    return new Feature.BothProperNoun(onPrix.headToken().isProperNoun() && candidate.headToken().isProperNoun());
			}
			else if (clazz.equals(Feature.PronounI.class)) {
                return new Feature.PronounI(Pronoun.isSomePronoun(onPrix.gloss()));
			}
			else if (clazz.equals(Feature.PronounJ.class)) {
                return new Feature.PronounJ(Pronoun.isSomePronoun(candidate.gloss()));
			}
			else if (clazz.equals(Feature.ProperNounI.class)) {
                return new Feature.ProperNounI(onPrix.headToken().isProperNoun());
			}
			else if (clazz.equals(Feature.ProperNounJ.class)) {
                return new Feature.ProperNounJ(candidate.headToken().isProperNoun());
			}
			else if (clazz.equals(Feature.PossessivePronounI.class)) {
                Pronoun pn = Pronoun.valueOrNull(onPrix.headWord());
                
                if (pn != null && (pn.type == Pronoun.Type.POSESSIVE_DETERMINER || pn.type == Pronoun.Type.POSESSIVE_PRONOUN))
                    return new Feature.PossessivePronounI(true);
                
                  return new Feature.PossessivePronounI(false);
			}
			else if (clazz.equals(Feature.PossessivePronounJ.class)) {
                Pronoun pn = Pronoun.valueOrNull(candidate.headWord());
                
                if (pn != null && (pn.type == Pronoun.Type.POSESSIVE_DETERMINER || pn.type == Pronoun.Type.POSESSIVE_PRONOUN))
                    return new Feature.PossessivePronounJ(true);
                
                  return new Feature.PossessivePronounJ(false);
			}
			else if (clazz.equals(Feature.ReflexivePronounI.class)) {
                Pronoun pn = Pronoun.valueOrNull(onPrix.headWord());
                
                if (pn != null && pn.type == Pronoun.Type.REFLEXIVE)
                    return new Feature.ReflexivePronounI(true);
                
                  return new Feature.ReflexivePronounI(false);
			}
			else if (clazz.equals(Feature.ReflexivePronounJ.class)) {
                Pronoun pn = Pronoun.valueOrNull(candidate.headWord());
                
                if (pn != null && pn.type == Pronoun.Type.REFLEXIVE)
                    return new Feature.ReflexivePronounJ(true);
                
                  return new Feature.ReflexivePronounJ(false);
			}
			else if (clazz.equals(Feature.SentenceDistance.class)) {
                int onPrixIndex = onPrix.doc.indexOfSentence(onPrix.sentence);
                int candidateIndex = candidate.doc.indexOfSentence(candidate.sentence);
                
                return new Feature.SentenceDistance(onPrixIndex - candidateIndex);
			}
			else if (clazz.equals(Feature.NumberAgreement.class)) {
			    Pair<Boolean, Boolean> numberAgreement = Util.haveNumberAndAreSameNumber(onPrix, candidate);
              
			    return new Feature.NumberAgreement(numberAgreement.getFirst() && numberAgreement.getSecond());
			}
			else if (clazz.equals(Feature.NERAgreement.class)) {
                String headNER = onPrix.headToken().nerTag();
                String candidateNER = candidate.headToken().nerTag();
                return new Feature.NERAgreement(headNER.equals("O") || candidateNER.equals("O") ||
                    (headNER.equals(candidateNER)));
			}
			else if (clazz.equals(Feature.NERCandidate.class)) {
                return new Feature.NERCandidate(candidate.headToken().nerTag());
			}
			else if (clazz.equals(Feature.MentionI.class)) {
                return new Feature.MentionI(onPrix.gloss());
			}
			else if (clazz.equals(Feature.GenderAgreement.class)) {
			    Pair<Boolean, Boolean> genderMatch = Util.haveGenderAndAreSameGender(onPrix, candidate);
                int retval = 0;
                
                if (genderMatch.getFirst())
                    retval = genderMatch.getSecond() ? 1 : -1;
                
                return new Feature.GenderAgreement(retval);
			}
			else if (clazz.equals(Feature.PrefixMatch.class)) {
			    return new Feature.PrefixMatch(onPrix.gloss().indexOf(candidate.gloss()) != -1);
			}
			else if (clazz.equals(Feature.MentionPair.class)) {
                return new Feature.MentionPair(onPrix.gloss() + "#" + candidate.gloss());
			}
			else if (clazz.equals(Feature.POSPair.class)) {
                return new Feature.POSPair(onPrix.headToken().posTag() + "#" + candidate.headToken().posTag());
			}
			else if (clazz.equals(Feature.PronounStrictGender.class)) {
                Pronoun pronounI = Pronoun.valueOrNull(onPrix.gloss());
                Pronoun pronounJ = Pronoun.valueOrNull(candidate.gloss());
                
                if (pronounI == null || pronounJ == null)
                  return new Feature.PronounStrictGender("n/a");
                
                if (pronounI.gender == Gender.NEUTRAL || pronounJ.gender == Gender.NEUTRAL)
                  return new Feature.PronounStrictGender(pronounI.gender == pronounJ.gender ? "agree" : "disagree");
                
                if (pronounI.gender.isCompatible(pronounJ.gender))
                  return new Feature.PronounStrictGender("agree");
                else
                  return new Feature.PronounStrictGender("disagree");
			}
			else if (clazz.equals(Feature.HobbsDistance.class)) {
                if (!Pronoun.isSomePronoun(onPrix.gloss()))
                    return new Feature.HobbsDistance(100);
                
                if (!hobbsCache.containsKey(onPrix)) 
                    hobbsCache.put(onPrix, Hobbs.getHobbsCandidates(onPrix));
                
                int dist = 100;
                
                try {
                  dist = hobbsCache.get(onPrix).get(new Hobbs.Candidate(candidate.doc.indexOfSentence(candidate.sentence), candidate.beginIndexInclusive, candidate.endIndexExclusive));
                }
                catch (Exception e){
                }
                
                return new Feature.HobbsDistance(dist);
			}
			else {
				throw new IllegalArgumentException("Unregistered feature: " + clazz);
			}
		}

		@SuppressWarnings({"unchecked"})
		@Override
		protected void fillFeatures(Pair<Mention, ClusteredMention> input, Counter<Feature> inFeatures, Boolean output, Counter<Feature> outFeatures) {
			//--Input Features
			for(Object o : ACTIVE_FEATURES){
				if(o instanceof Class){
					//(case: singleton feature)
					Option<Double> count = new Option<Double>(1.0);
					Feature feat = feature((Class) o, input, count);
					if(count.get() > 0.0){
						inFeatures.incrementCount(feat, count.get());
					}
				} else if(o instanceof Pair){
					//(case: pair of features)
					Pair<Class,Class> pair = (Pair<Class,Class>) o;
					Option<Double> countA = new Option<Double>(1.0);
					Option<Double> countB = new Option<Double>(1.0);
					Feature featA = feature(pair.getFirst(), input, countA);
					Feature featB = feature(pair.getSecond(), input, countB);
					if(countA.get() * countB.get() > 0.0){
						inFeatures.incrementCount(new Feature.PairFeature(featA, featB), countA.get() * countB.get());
					}
				}
			}

			//--Output Features
			if(output != null){
				outFeatures.incrementCount(new Feature.CoreferentIndicator(output), 1.0);
			}
		}

		@Override
		protected Feature concat(Feature a, Feature b) {
			return new Feature.PairFeature(a,b);
		}
	};

	public void train(Collection<Pair<Document, List<Entity>>> trainingData) {
		startTrack("Training");
		//--Variables
		RVFDataset<Boolean, Feature> dataset = new RVFDataset<Boolean, Feature>();
		LinearClassifierFactory<Boolean, Feature> fact = new LinearClassifierFactory<Boolean,Feature>();
		//--Feature Extraction
		startTrack("Feature Extraction");
		for(Pair<Document,List<Entity>> datum : trainingData){
			//(document variables)
			Document doc = datum.getFirst();
			List<Entity> goldClusters = datum.getSecond();
			List<Mention> mentions = doc.getMentions();
			Map<Mention,Entity> goldEntities = Entity.mentionToEntityMap(goldClusters);
			startTrack("Document " + doc.id);
			//(for each mention...)
			for(int i=0; i<mentions.size(); i++){
				//(get the mention and its cluster)
				Mention onPrix = mentions.get(i);
				Entity source = goldEntities.get(onPrix);
				if(source == null){ throw new IllegalArgumentException("Mention has no gold entity: " + onPrix); }
				//(for each previous mention...)
				int oldSize = dataset.size();
				for(int j=i-1; j>=0; j--){
					//(get previous mention and its cluster)
					Mention cand = mentions.get(j);
					Entity target = goldEntities.get(cand);
					if(target == null){ throw new IllegalArgumentException("Mention has no gold entity: " + cand); }
					//(extract features)
					Counter<Feature> feats = extractor.extractFeatures(Pair.make(onPrix, cand.markCoreferent(target)));
					//(add datum)
					dataset.add(new RVFDatum<Boolean, Feature>(feats, target == source));
					//(stop if
					if(target == source){ break; }
				}
				//logf("Mention %s (%d datums)", onPrix.toString(), dataset.size() - oldSize);
			}
			endTrack("Document " + doc.id);
		}
		endTrack("Feature Extraction");
		//--Train Classifier
		startTrack("Minimizer");
		this.classifier = fact.trainClassifier(dataset);
		endTrack("Minimizer");
		//--Dump Weights
		startTrack("Features");
		//(get labels to print)
		Set<Boolean> labels = new HashSet<Boolean>();
		labels.add(true);
		//(print features)
		for(Triple<Feature,Boolean,Double> featureInfo : this.classifier.getTopFeatures(labels, 0.0, true, 100, true)){
			Feature feature = featureInfo.first();
			Boolean label = featureInfo.second();
			Double magnitude = featureInfo.third();
			log(FORCE,new DecimalFormat("0.000").format(magnitude) + " [" + label + "] " + feature);
		}
		end_Track("Features");
		endTrack("Training");
	}

	public List<ClusteredMention> runCoreference(Document doc) {
		//--Overhead
		startTrack("Testing " + doc.id);
		//(variables)
		List<ClusteredMention> rtn = new ArrayList<ClusteredMention>(doc.getMentions().size());
		List<Mention> mentions = doc.getMentions();
		int singletons = 0;
		//--Run Classifier
		for(int i=0; i<mentions.size(); i++){
			//(variables)
			Mention onPrix = mentions.get(i);
			int coreferentWith = -1;
			//(get mention it is coreferent with)
			for(int j=i-1; j>=0; j--){
				ClusteredMention cand = rtn.get(j);
				boolean coreferent = classifier.classOf(new RVFDatum<Boolean, Feature>(extractor.extractFeatures(Pair.make(onPrix, cand))));
				if(coreferent){
					coreferentWith = j;
					break;
				}
			}
			//(mark coreference)
			if(coreferentWith < 0){
				singletons += 1;
				rtn.add(onPrix.markSingleton());
			} else {
				//log("Mention " + onPrix + " coreferent with " + mentions.get(coreferentWith));
				rtn.add(onPrix.markCoreferent(rtn.get(coreferentWith)));
			}
		}
		//log("" + singletons + " singletons");
		//--Return
		endTrack("Testing " + doc.id);
		return rtn;
	}

	private class Option<T> {
		private T obj;
		public Option(T obj){ this.obj = obj; }
		public Option(){};
		public T get(){ return obj; }
		public void set(T obj){ this.obj = obj; }
		public boolean exists(){ return obj != null; }
	}
}
