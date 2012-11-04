package cs224n.corefsystems;

import java.util.Collection;
import java.util.List;
import java.util.HashMap;
import java.util.ArrayList;

import cs224n.coref.ClusteredMention;
import cs224n.coref.Document;
import cs224n.coref.Mention;
import cs224n.coref.Entity;
import cs224n.util.Pair;

/*
java -Xmx500m -cp "extlib/*:classes" cs224n.assignments.CoreferenceTester \
     -path /afs/ir/class/cs224n/pa3/data/ \
     -model AllSingleton -data dev -documents 100
*/

public class AllSingleton implements CoreferenceSystem {

	@Override
	public void train(Collection<Pair<Document, List<Entity>>> trainingData) {
		return;
	}

	@Override
	public List<ClusteredMention> runCoreference(Document doc) {
		List<ClusteredMention> rtn = new ArrayList<ClusteredMention>();
		
		for(Mention i: doc.getMentions()) {
			rtn.add(i.markSingleton());
		}

		return rtn;
	}

}
