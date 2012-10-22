package cs224n.assignments;

import cs224n.io.PennTreebankReader;
import cs224n.ling.Tree;
import cs224n.ling.Trees;
import cs224n.parser.EnglishPennTreebankParseEvaluator;
import cs224n.util.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Harness for PCFG Parser project.
 *
 * @author Dan Klein
 */
public class PCFGParserTester {


	// Parser interface ===========================================================

	/**
	 * Parsers are required to map sentences to trees.  How a parser is
	 * constructed and trained is not specified.
	 */
	public static interface Parser {
		public void train(List<Tree<String>> trainTrees);
		public Tree<String> getBestParse(List<String> sentence);
	}


	// PCFGParser =================================================================
    public static class CounterLog<E> extends Counter<E> {
      @Override
      public double getCount(E key) {
        Double value = entries.get(key);
        if (value == null)
          return Double.NEGATIVE_INFINITY;
        return value;
      }
    }


	/**
	 * The PCFG Parser you will implement.
	 */
	public static class PCFGParser implements Parser {

		private Grammar grammar;
		private Lexicon lexicon;

		private void debugPrintln(String s) {
			if(false) System.out.println(s);
		}

		public void train(List<Tree<String>> trainTrees) {
			
			System.out.println("Starting Training Process....");

			ArrayList<Tree<String>> aTrees = new ArrayList<Tree<String>>();
			for(Tree<String> tree : trainTrees) {
				aTrees.add(TreeAnnotations.annotateTree(tree));
			}

			lexicon = new Lexicon(aTrees);
			grammar = new Grammar(aTrees);

			System.out.println("Training Process Done.");
			debugPrintln("Grammar:");
			System.out.println(grammar);

		}

		public Tree<String> getBestParse(List<String> sentence) {

			long startTime = System.currentTimeMillis();

			Set<String> nonterminals = grammar.getNonterminals();

			// score = new double[words+1][words+1][nonterms]
			ArrayList<ArrayList<Counter<String>>> score = new ArrayList<ArrayList<Counter<String>>>();
			ArrayList<ArrayList<Map<String, Triplet<Integer, String, String>>>> back = 
				new ArrayList<ArrayList<Map<String, Triplet<Integer, String, String>>>>();

			for(int i = 0; i <= sentence.size(); i++) {
				score.add(i, new ArrayList<Counter<String>>());
				back.add(i, new ArrayList<Map<String, Triplet<Integer, String, String>>>());
				for(int j = 0; j <= sentence.size(); j++) {
					score.get(i).add(j, new CounterLog<String>());
					back.get(i).add(j, new HashMap<String, Triplet<Integer, String, String>>());
				}
			}

			// To process the current word to a tag as the first step
			// for i = 0; i < words.len; i++
			for(int i = 0; i < sentence.size(); i++) {
				String word = sentence.get(i);
				for (String tag : lexicon.getAllTags()) {
					//debugPrintln("(1) set " + i + ", " + (i + 1) + ", " + tag + " to " + lexicon.scoreTagging(word, tag));
					score.get(i).get(i+1).setCount(tag, Math.log(lexicon.scoreTagging(word, tag)));
				}

				// handle unaries
				boolean added = true;
				while(added) {
					added = false;
					for(String b : nonterminals) {
						for(UnaryRule r : grammar.getUnaryRulesByChild(b)) {
							double p = Math.log(r.getScore()) + score.get(i).get(i+1).getCount(b);
							String a = r.getParent();
							//debugPrintln("Unary transition " + a + " -> " + b + " at " + i + " : " + p);
							if(p > score.get(i).get(i+1).getCount(a)) {
								//debugPrintln("(2) set " + i + ", " + (i + 1) + ", " + a + " to " + p);
								score.get(i).get(i+1).setCount(a, p);
								back.get(i).get(i+1).put(a, new Triplet<Integer, String, String>(-1, b, null));
								added = true;
							}
						}
					}
				}
			}

			for(int span = 2; span <= sentence.size(); span++) {
				for(int begin = 0; begin <= sentence.size() - span; begin++) {
					int end = begin + span;
					for(int split = begin + 1; split <= end - 1; split++) {
						for(String b : nonterminals) {
							// we are looking at r: a -> b c
							for(BinaryRule r : grammar.getBinaryRulesByLeftChild(b)) {
								String a = r.getParent();
								String c = r.getRightChild();

								double p = score.get(begin).get(split).getCount(b) +
										    score.get(split).get(end).getCount(c) +
											Math.log(r.getScore());

								//debugPrintln(begin + "->" + split + " (" + b + "):" + score.get(begin).get(split).getCount(b) + 
								//	", " + split + "->" + end + " (" + c + "):" + score.get(split).get(end).getCount(c) +
								//	", r:" + r.getScore());
								//debugPrintln("p:" + p + ", score:" + score.get(begin).get(end).getCount(a));

								if(p > score.get(begin).get(end).getCount(a)) {
									//debugPrintln("(3) set " + begin + ", " + end + ", " + a + " to " + p);
									score.get(begin).get(end).setCount(a, p);
									back.get(begin).get(end).put(a, new Triplet<Integer, String, String>(split, b, c));
								}

								//debugPrintln(" ");
							}
						}
					}

					// handle unaries again
					boolean added = true;
					while(added) {
						added = false;
						for(String b : nonterminals) {
							for(UnaryRule r : grammar.getUnaryRulesByChild(b)) {
								double p = Math.log(r.getScore()) + score.get(begin).get(end).getCount(b);
								String a = r.getParent();
								if(p > score.get(begin).get(end).getCount(a)) {
									score.get(begin).get(end).setCount(a, p);
									back.get(begin).get(end).put(a, new Triplet<Integer, String, String>(-1, b, null));
									added = true;
									//debugPrintln("(4) set " + begin + ", " + end + ", " + a + " to " + p);
								}
								//debugPrintln(" ");
							}
						}
					}
				}
			} 

			Tree<String> finalTree = TreeAnnotations.unAnnotateTree(buildTree(sentence, score, back));

			long endTime = System.currentTimeMillis();
			System.out.println("Parsing this tree from sentence length " + sentence.size() + " took " + (endTime - startTime) + " milliseconds.");

			return finalTree;
		}

		private Tree<String> buildTree(List<String> sentence,
										ArrayList<ArrayList<Counter<String>>> score,
										ArrayList<ArrayList<Map<String, Triplet<Integer, String, String>>>> back) {

		
			return buildTreeRecursive(sentence, score, back, "ROOT", 0, sentence.size());
		}

		private Tree<String> buildTreeRecursive(List<String> sentence,
												 ArrayList<ArrayList<Counter<String>>> score,
												 ArrayList<ArrayList<Map<String, Triplet<Integer, String, String>>>> back,
												 String label,
												 int begin,
												 int end) {

			//debugPrintln("BTR " + label + " " + begin + " " + end);
	
			ArrayList<Tree<String>> children = new ArrayList<Tree<String>>();
			Triplet<Integer, String, String> path = back.get(begin).get(end).get(label);

			if(back.get(begin).get(end).containsKey(label)) {
				// the back tracing path contains the label we want					
				if(path.getFirst() >= 0) {
					// binary rule
					children.add(buildTreeRecursive(sentence, score, back, path.getSecond(), begin, path.getFirst()));
					children.add(buildTreeRecursive(sentence, score, back, path.getThird(), path.getFirst(), end));
				} else {
					// unary rule
					children.add(buildTreeRecursive(sentence, score, back, path.getSecond(), begin, end));
				}
			} else {
				children.add(new Tree<String>(sentence.get(begin)));
			}

			return new Tree<String>(label, children);

		}

	}

	// BaselineParser =============================================================

	/**
	 * Baseline parser (though not a baseline I've ever seen before).  Tags the
	 * sentence using the baseline tagging method, then either retrieves a known
	 * parse of that tag sequence, or builds a right-branching parse for unknown
	 * tag sequences.
	 */
	public static class BaselineParser implements Parser {

		CounterMap<List<String>,Tree<String>> knownParses;
		CounterMap<Integer,String> spanToCategories;
		Lexicon lexicon;

		public void train(List<Tree<String>> trainTrees) {
			lexicon = new Lexicon(trainTrees);
			knownParses = new CounterMap<List<String>, Tree<String>>();
			spanToCategories = new CounterMap<Integer, String>();
			for (Tree<String> trainTree : trainTrees) {
				List<String> tags = trainTree.getPreTerminalYield();
				knownParses.incrementCount(tags, trainTree, 1.0);
				tallySpans(trainTree, 0);
			}
		}

		public Tree<String> getBestParse(List<String> sentence) {
			List<String> tags = getBaselineTagging(sentence);
			if (knownParses.keySet().contains(tags)) {
				return getBestKnownParse(tags, sentence);
			}
			return buildRightBranchParse(sentence, tags);
		}

		/* Builds a tree that branches to the right.  For pre-terminals it
		 * uses the most common tag for the word in the training corpus.
		 * For all other non-terminals it uses the tag that is most common
		 * in training corpus of tree of the same size span as the tree
		 * that is being labeled. */
		private Tree<String> buildRightBranchParse(List<String> words, List<String> tags) {
			int currentPosition = words.size() - 1;
			Tree<String> rightBranchTree = buildTagTree(words, tags, currentPosition);
			while (currentPosition > 0) {
				currentPosition--;
				rightBranchTree = merge(buildTagTree(words, tags, currentPosition),
						rightBranchTree);
			}
			rightBranchTree = addRoot(rightBranchTree);
			return rightBranchTree;
		}

		private Tree<String> merge(Tree<String> leftTree, Tree<String> rightTree) {
			int span = leftTree.getYield().size() + rightTree.getYield().size();
			String mostFrequentLabel = spanToCategories.getCounter(span).argMax();
			List<Tree<String>> children = new ArrayList<Tree<String>>();
			children.add(leftTree);
			children.add(rightTree);
			return new Tree<String>(mostFrequentLabel, children);
		}

		private Tree<String> addRoot(Tree<String> tree) {
			return new Tree<String>("ROOT", Collections.singletonList(tree));
		}

		private Tree<String> buildTagTree(List<String> words,
				List<String> tags,
				int currentPosition) {
			Tree<String> leafTree = new Tree<String>(words.get(currentPosition));
			Tree<String> tagTree = new Tree<String>(tags.get(currentPosition), 
					Collections.singletonList(leafTree));
			return tagTree;
		}

		private Tree<String> getBestKnownParse(List<String> tags, List<String> sentence) {
			Tree<String> parse = knownParses.getCounter(tags).argMax().deepCopy();
			parse.setWords(sentence);
			return parse;
		}

		private List<String> getBaselineTagging(List<String> sentence) {
			List<String> tags = new ArrayList<String>();
			for (String word : sentence) {
				String tag = getBestTag(word);
				tags.add(tag);
			}
			return tags;
		}

		private String getBestTag(String word) {
			double bestScore = Double.NEGATIVE_INFINITY;
			String bestTag = null;
			for (String tag : lexicon.getAllTags()) {
				double score = lexicon.scoreTagging(word, tag);
				if (bestTag == null || score > bestScore) {
					bestScore = score;
					bestTag = tag;
				}
			}
			return bestTag;
		}

		private int tallySpans(Tree<String> tree, int start) {
			if (tree.isLeaf() || tree.isPreTerminal()) 
				return 1;
			int end = start;
			for (Tree<String> child : tree.getChildren()) {
				int childSpan = tallySpans(child, end);
				end += childSpan;
			}
			String category = tree.getLabel();
			if (! category.equals("ROOT"))
				spanToCategories.incrementCount(end - start, category, 1.0);
			return end - start;
		}

	}


	// TreeAnnotations ============================================================

	/**
	 * Class which contains code for annotating and binarizing trees for
	 * the parser's use, and debinarizing and unannotating them for
	 * scoring.
	 */
	public static class TreeAnnotations {

		public static Tree<String> annotateTree(Tree<String> unAnnotatedTree) {
            Tree<String> parentAnnotatedTree = annotateParent(unAnnotatedTree, null);

			return binarizeTree(parentAnnotatedTree);

		}
    
		private static Tree<String> annotateParent(Tree<String> tree, String parentLabel) {
          String transformedLabel = tree.getLabel();
          if (tree.isPhrasal() && parentLabel != null)
            transformedLabel += "^" + parentLabel;
          if (tree.isLeaf()) {
              return new Tree<String>(transformedLabel);
          }
          List<Tree<String>> transformedChildren = new ArrayList<Tree<String>>();
          for (Tree<String> child : tree.getChildren()) {
              transformedChildren.add(annotateParent(child, tree.getLabel()));
          }
          return new Tree<String>(transformedLabel, transformedChildren);
		}

		private static Tree<String> binarizeTree(Tree<String> tree) {
			String label = tree.getLabel();
			if (tree.isLeaf())
				return new Tree<String>(label);
			if (tree.getChildren().size() == 1) {
				return new Tree<String>
				(label, 
						Collections.singletonList(binarizeTree(tree.getChildren().get(0))));
			}
			// otherwise, it's a binary-or-more local tree, 
			// so decompose it into a sequence of binary and unary trees.
			String intermediateLabel = "@"+label+"->";
			Tree<String> intermediateTree =
					binarizeTreeHelper(tree, 0, intermediateLabel);
			return new Tree<String>(label, intermediateTree.getChildren());
		}

		private static Tree<String> binarizeTreeHelper(Tree<String> tree,
				int numChildrenGenerated, 
				String intermediateLabel) {
			Tree<String> leftTree = tree.getChildren().get(numChildrenGenerated);
			List<Tree<String>> children = new ArrayList<Tree<String>>();
			children.add(binarizeTree(leftTree));
			if (numChildrenGenerated < tree.getChildren().size() - 1) {
				Tree<String> rightTree = 
						binarizeTreeHelper(tree, numChildrenGenerated + 1, 
								intermediateLabel + "_" + leftTree.getLabel());
				children.add(rightTree);
			}
			return new Tree<String>(intermediateLabel, children);
		} 

		public static Tree<String> unAnnotateTree(Tree<String> annotatedTree) {

			// Remove intermediate nodes (labels beginning with "@"
			// Remove all material on node labels which follow their base symbol 
			// (cuts at the leftmost -, ^, or : character)
			// Examples: a node with label @NP->DT_JJ will be spliced out, 
			// and a node with label NP^S will be reduced to NP

			Tree<String> debinarizedTree =
					Trees.spliceNodes(annotatedTree, new Filter<String>() {
						public boolean accept(String s) {
							return s.startsWith("@");
						}
					});
			Tree<String> unAnnotatedTree = 
					(new Trees.FunctionNodeStripper()).transformTree(debinarizedTree);
			return unAnnotatedTree;
		}
	}


	// Lexicon ====================================================================

	/**
	 * Simple default implementation of a lexicon, which scores word,
	 * tag pairs with a smoothed estimate of P(tag|word)/P(tag).
	 */
	public static class Lexicon {

		CounterMap<String,String> wordToTagCounters = new CounterMap<String, String>();
		double totalTokens = 0.0;
		double totalWordTypes = 0.0;
		Counter<String> tagCounter = new Counter<String>();
		Counter<String> wordCounter = new Counter<String>();
		Counter<String> typeTagCounter = new Counter<String>();

		public Set<String> getAllTags() {
			return tagCounter.keySet();
		}

		public boolean isKnown(String word) {
			return wordCounter.keySet().contains(word);
		}

		/* Returns a smoothed estimate of P(word|tag) */
		public double scoreTagging(String word, String tag) {
			double p_tag = tagCounter.getCount(tag) / totalTokens;
			double c_word = wordCounter.getCount(word);
			double c_tag_and_word = wordToTagCounters.getCount(word, tag);
			if (c_word < 10) { // rare or unknown
				c_word += 1.0;
				c_tag_and_word += typeTagCounter.getCount(tag) / totalWordTypes;
			}
			double p_word = (1.0 + c_word) / (totalTokens + totalWordTypes);
			double p_tag_given_word = c_tag_and_word / c_word;
			return p_tag_given_word / p_tag * p_word;
		}

		/* Builds a lexicon from the observed tags in a list of training trees. */
		public Lexicon(List<Tree<String>> trainTrees) {
			for (Tree<String> trainTree : trainTrees) {
				List<String> words = trainTree.getYield();
				List<String> tags = trainTree.getPreTerminalYield();
				for (int position = 0; position < words.size(); position++) {
					String word = words.get(position);
					String tag = tags.get(position);
					tallyTagging(word, tag);
				}
			}
		}

		private void tallyTagging(String word, String tag) {
			if (! isKnown(word)) {
				totalWordTypes += 1.0;
				typeTagCounter.incrementCount(tag, 1.0);
			}
			totalTokens += 1.0;
			tagCounter.incrementCount(tag, 1.0);
			wordCounter.incrementCount(word, 1.0);
			wordToTagCounters.incrementCount(word, tag, 1.0);
		}
	}


	// Grammar ====================================================================

	/**
	 * Simple implementation of a PCFG grammar, offering the ability to
	 * look up rules by their child symbols.  Rule probability estimates
	 * are just relative frequency estimates off of training trees.
	 */
	public static class Grammar {

		Map<String, List<BinaryRule>> binaryRulesByLeftChild = 
				new HashMap<String, List<BinaryRule>>();
		Map<String, List<BinaryRule>> binaryRulesByRightChild = 
				new HashMap<String, List<BinaryRule>>();
		Map<String, List<UnaryRule>> unaryRulesByChild = 
				new HashMap<String, List<UnaryRule>>();

		Set<String> nonterminals = null;

		/* Gets All Nonterminals */
		public Set<String> getNonterminals() {
			if(nonterminals != null) {
				return nonterminals;
			} else {
				nonterminals = new HashSet<String>();
				for(String leftChild : binaryRulesByLeftChild.keySet()) {
					for(BinaryRule b : getBinaryRulesByLeftChild(leftChild)) {
						nonterminals.add(b.getParent());
						nonterminals.add(b.getLeftChild());
						nonterminals.add(b.getRightChild());
					}
				}
				for(String child : unaryRulesByChild.keySet())	{
					for(UnaryRule u : getUnaryRulesByChild(child)) {
						nonterminals.add(u.getParent());
						nonterminals.add(child);
					}
				}
				return nonterminals;
			}
		}

		/* Rules in grammar are indexed by child for easy access when
		 * doing bottom up parsing. */
		public List<BinaryRule> getBinaryRulesByLeftChild(String leftChild) {
			return CollectionUtils.getValueList(binaryRulesByLeftChild, leftChild);
		}

		public List<BinaryRule> getBinaryRulesByRightChild(String rightChild) {
			return CollectionUtils.getValueList(binaryRulesByRightChild, rightChild);
		}

		public List<UnaryRule> getUnaryRulesByChild(String child) {
			return CollectionUtils.getValueList(unaryRulesByChild, child);
		}

		public String toString() {
			StringBuilder sb = new StringBuilder();
			List<String> ruleStrings = new ArrayList<String>();
			for (String leftChild : binaryRulesByLeftChild.keySet()) {
				for (BinaryRule binaryRule : getBinaryRulesByLeftChild(leftChild)) {
					ruleStrings.add(binaryRule.toString());
				}
			}
			for (String child : unaryRulesByChild.keySet()) {
				for (UnaryRule unaryRule : getUnaryRulesByChild(child)) {
					ruleStrings.add(unaryRule.toString());
				}
			}
			for (String ruleString : CollectionUtils.sort(ruleStrings)) {
				sb.append(ruleString);
				sb.append("\n");
			}
			return sb.toString();
		}

		private void addBinary(BinaryRule binaryRule) {
			CollectionUtils.addToValueList(binaryRulesByLeftChild, 
					binaryRule.getLeftChild(), binaryRule);
			CollectionUtils.addToValueList(binaryRulesByRightChild, 
					binaryRule.getRightChild(), binaryRule);
		}

		private void addUnary(UnaryRule unaryRule) {
			CollectionUtils.addToValueList(unaryRulesByChild, 
					unaryRule.getChild(), unaryRule);
		}

		/* A builds PCFG using the observed counts of binary and unary
		 * productions in the training trees to estimate the probabilities
		 * for those rules.  */ 
		public Grammar(List<Tree<String>> trainTrees) {
			Counter<UnaryRule> unaryRuleCounter = new Counter<UnaryRule>();
			Counter<BinaryRule> binaryRuleCounter = new Counter<BinaryRule>();
			Counter<String> symbolCounter = new Counter<String>();
			for (Tree<String> trainTree : trainTrees) {
				tallyTree(trainTree, symbolCounter, unaryRuleCounter, binaryRuleCounter);
			}
			for (UnaryRule unaryRule : unaryRuleCounter.keySet()) {
				double unaryProbability = 
						unaryRuleCounter.getCount(unaryRule) / 
						symbolCounter.getCount(unaryRule.getParent());
				unaryRule.setScore(unaryProbability);
				addUnary(unaryRule);
			}
			for (BinaryRule binaryRule : binaryRuleCounter.keySet()) {
				double binaryProbability = 
						binaryRuleCounter.getCount(binaryRule) / 
						symbolCounter.getCount(binaryRule.getParent());
				binaryRule.setScore(binaryProbability);
				addBinary(binaryRule);
			}
		}

		private void tallyTree(Tree<String> tree, Counter<String> symbolCounter,
				Counter<UnaryRule> unaryRuleCounter, 
				Counter<BinaryRule> binaryRuleCounter) {
			if (tree.isLeaf()) return;
			if (tree.isPreTerminal()) return;
			if (tree.getChildren().size() == 1) {
				UnaryRule unaryRule = makeUnaryRule(tree);
				symbolCounter.incrementCount(tree.getLabel(), 1.0);
				unaryRuleCounter.incrementCount(unaryRule, 1.0);
			}
			if (tree.getChildren().size() == 2) {
				BinaryRule binaryRule = makeBinaryRule(tree);
				symbolCounter.incrementCount(tree.getLabel(), 1.0);
				binaryRuleCounter.incrementCount(binaryRule, 1.0);
			}
			if (tree.getChildren().size() < 1 || tree.getChildren().size() > 2) {
				throw new RuntimeException("Attempted to construct a Grammar with an illegal tree: "+tree);
			}
			for (Tree<String> child : tree.getChildren()) {
				tallyTree(child, symbolCounter, unaryRuleCounter,  binaryRuleCounter);
			}
		}

		private UnaryRule makeUnaryRule(Tree<String> tree) {
			return new UnaryRule(tree.getLabel(), tree.getChildren().get(0).getLabel());
		}

		private BinaryRule makeBinaryRule(Tree<String> tree) {
			return new BinaryRule(tree.getLabel(), tree.getChildren().get(0).getLabel(), 
					tree.getChildren().get(1).getLabel());
		}
	}


	// BinaryRule =================================================================

	/* A binary grammar rule with score representing its probability. */
	public static class BinaryRule {

		String parent;
		String leftChild;
		String rightChild;
		double score;

		public String getParent() {
			return parent;
		}

		public String getLeftChild() {
			return leftChild;
		}

		public String getRightChild() {
			return rightChild;
		}

		public double getScore() {
			return score;
		}

		public void setScore(double score) {
			this.score = score;
		}

		public boolean equals(Object o) {
			if (this == o) return true;
			if (!(o instanceof BinaryRule)) return false;

			final BinaryRule binaryRule = (BinaryRule) o;

			if (leftChild != null ? !leftChild.equals(binaryRule.leftChild) : binaryRule.leftChild != null) 
				return false;
			if (parent != null ? !parent.equals(binaryRule.parent) : binaryRule.parent != null) 
				return false;
			if (rightChild != null ? !rightChild.equals(binaryRule.rightChild) : binaryRule.rightChild != null) 
				return false;

			return true;
		}

		public int hashCode() {
			int result;
			result = (parent != null ? parent.hashCode() : 0);
			result = 29 * result + (leftChild != null ? leftChild.hashCode() : 0);
			result = 29 * result + (rightChild != null ? rightChild.hashCode() : 0);
			return result;
		}

		public String toString() {
			return parent + " -> " + leftChild + " " + rightChild + " %% "+score;
		}

		public BinaryRule(String parent, String leftChild, String rightChild) {
			this.parent = parent;
			this.leftChild = leftChild;
			this.rightChild = rightChild;
		}
	}


	// UnaryRule ==================================================================

	/** A unary grammar rule with score representing its probability. */
	public static class UnaryRule {

		String parent;
		String child;
		double score;

		public String getParent() {
			return parent;
		}

		public String getChild() {
			return child;
		}

		public double getScore() {
			return score;
		}

		public void setScore(double score) {
			this.score = score;
		}

		public boolean equals(Object o) {
			if (this == o) return true;
			if (!(o instanceof UnaryRule)) return false;

			final UnaryRule unaryRule = (UnaryRule) o;

			if (child != null ? !child.equals(unaryRule.child) : unaryRule.child != null) return false;
			if (parent != null ? !parent.equals(unaryRule.parent) : unaryRule.parent != null) return false;

			return true;
		}

		public int hashCode() {
			int result;
			result = (parent != null ? parent.hashCode() : 0);
			result = 29 * result + (child != null ? child.hashCode() : 0);
			return result;
		}

		public String toString() {
			return parent + " -> " + child + " %% "+score;
		}

		public UnaryRule(String parent, String child) {
			this.parent = parent;
			this.child = child;
		}
	}


	// PCFGParserTester ===========================================================

	// Longest sentence length that will be tested on.
	private static int MAX_LENGTH = 20;

	private static void testParser(Parser parser, List<Tree<String>> testTrees) {
		EnglishPennTreebankParseEvaluator.LabeledConstituentEval<String> eval = 
				new EnglishPennTreebankParseEvaluator.LabeledConstituentEval<String>
		(Collections.singleton("ROOT"), 
				new HashSet<String>(Arrays.asList(new String[] {"''", "``", ".", ":", ","})));
		for (Tree<String> testTree : testTrees) {
			List<String> testSentence = testTree.getYield();
			if (testSentence.size() > MAX_LENGTH)
				continue;
			Tree<String> guessedTree = parser.getBestParse(testSentence);
			System.out.println("Guess:\n"+Trees.PennTreeRenderer.render(guessedTree));
			System.out.println("Gold:\n"+Trees.PennTreeRenderer.render(testTree));
			eval.evaluate(guessedTree, testTree);
		}
		eval.display(true);
	}

	private static List<Tree<String>> readTrees(String basePath, int low,
			int high) {
		Collection<Tree<String>> trees = PennTreebankReader.readTrees(basePath,
				low, high);
		// normalize trees
		Trees.TreeTransformer<String> treeTransformer = new Trees.StandardTreeNormalizer();
		List<Tree<String>> normalizedTreeList = new ArrayList<Tree<String>>();
		for (Tree<String> tree : trees) {
			Tree<String> normalizedTree = treeTransformer.transformTree(tree);
			// System.out.println(Trees.PennTreeRenderer.render(normalizedTree));
			normalizedTreeList.add(normalizedTree);
		}
		return normalizedTreeList;
	}

	public static void main(String[] args) {

		// set up default options ..............................................
		Map<String, String> options = new HashMap<String, String>();
		options.put("-path",      "/afs/ir/class/cs224n/pa2/data/");
		options.put("-data",      "miniTest");
		options.put("-parser",    "cs224n.assignments.PCFGParserTester$PCFGParser");
		options.put("-maxLength", "20");

		// let command-line options supersede defaults .........................
		options.putAll(CommandLineUtils.simpleCommandLineParser(args));
		System.out.println("PCFGParserTester options:");
		for (Map.Entry<String, String> entry: options.entrySet()) {
			System.out.printf("  %-12s: %s%n", entry.getKey(), entry.getValue());
		}
		System.out.println();

		MAX_LENGTH = Integer.parseInt(options.get("-maxLength"));

		Parser parser;
		try {
			Class parserClass = Class.forName(options.get("-parser"));
			parser = (Parser) parserClass.newInstance();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		System.out.println("Using parser: " + parser);

		String basePath = options.get("-path");
		String dataSet = options.get("-data");
		if (!basePath.endsWith("/"))
			basePath += "/";
		//basePath += dataSet;
		System.out.println("Data will be loaded from: " + basePath + "\n");

		List<Tree<String>> trainTrees = new ArrayList<Tree<String>>(),
				validationTrees = new ArrayList<Tree<String>>(),
				testTrees = new ArrayList<Tree<String>>();

		if (!basePath.endsWith("/"))
			basePath += "/";
		basePath += dataSet;
		if (dataSet.equals("miniTest")) {
			System.out.print("Loading training trees...");
			trainTrees = readTrees(basePath, 1, 3);
			System.out.println("done.");
			System.out.print("Loading test trees...");
			testTrees = readTrees(basePath, 4, 4);
			System.out.println("done.");
		}
		else if (dataSet.equals("treebank")) {
			System.out.print("Loading training trees...");
			trainTrees = readTrees(basePath, 200, 2199);
			System.out.println("done.");
			System.out.print("Loading validation trees...");
			validationTrees = readTrees(basePath, 2200, 2299);
			System.out.println("done.");
			System.out.print("Loading test trees...");
			testTrees = readTrees(basePath, 2300, 2319);
			System.out.println("done.");
		}
		else {
			throw new RuntimeException("Bad data set mode: "+ dataSet+", use miniTest, or treebank."); 
		}
		parser.train(trainTrees);
		testParser(parser, testTrees);
	}
}
