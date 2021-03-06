/******************************************************************************
* Copyright 2013-2015 LASIGE                                                  *
*                                                                             *
* Licensed under the Apache License, Version 2.0 (the "License"); you may     *
* not use this file except in compliance with the License. You may obtain a   *
* copy of the License at http://www.apache.org/licenses/LICENSE-2.0           *
*                                                                             *
* Unless required by applicable law or agreed to in writing, software         *
* distributed under the License is distributed on an "AS IS" BASIS,           *
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.    *
* See the License for the specific language governing permissions and         *
* limitations under the License.                                              *
*                                                                             *
*******************************************************************************
* Matches Ontologies by finding literal full-name matches between their       *
* Lexicons after extension with the WordNet.                                  *
*                                                                             *
* @author Daniel Faria                                                        *
* @date 10-09-2014                                                            *
******************************************************************************/
package aml.match;

import java.util.HashSet;
import java.util.Set;
import java.util.Vector;

import aml.AML;
import aml.match.PrimaryMatcher;
import aml.ontology.Lexicon;
import aml.settings.LexicalType;
import aml.util.StringParser;
import aml.util.WordNet;

public class WordNetMatcher implements PrimaryMatcher, LexiconExtender
{
	
//Attributes

	//The WordNet class
	private WordNet wn;
	//The type of lexical entry generated by this LexiconExtender
	private final LexicalType TYPE = LexicalType.EXTERNAL_MATCH;
	//The source of this LexiconExtender
	private final String SOURCE = "WordNet";
	//The confidence score of WordNet
	private final double CONFIDENCE = 0.9;
	
//Constructors

	/**
	 * Constructs a new WordNetMatcher
	 */
	public WordNetMatcher()
	{
		wn = new WordNet();
	}

//Public Methods

	@Override
	public void extendLexicons(double thresh)
	{
		System.out.println("Extending Lexicons with WordNet Matcher");
		long time = System.currentTimeMillis()/1000;
		AML aml = AML.getInstance();
		extendLexicon(aml.getSource().getLexicon(),thresh);
		extendLexicon(aml.getTarget().getLexicon(),thresh);
		time = System.currentTimeMillis()/1000 - time;
		System.out.println("Finished in " + time + " seconds");
	}
	
	@Override
	public Alignment match(double thresh)
	{
		System.out.println("Running WordNet Matcher");
		long time = System.currentTimeMillis()/1000;
		AML aml = AML.getInstance();
		Lexicon ext1 = getExtensionLexicon(aml.getSource().getLexicon(),thresh);
		Lexicon ext2 = getExtensionLexicon(aml.getTarget().getLexicon(),thresh);
		Alignment a = match(ext1, ext2, thresh);
		time = System.currentTimeMillis()/1000 - time;
		System.out.println("Finished in " + time + " seconds");
		return a;
	}
	
//Private Methods

	private void extendLexicon(Lexicon l, double thresh)
	{
		//Get the original Lexicon names into a Vector since the
		//Lexicon will be extended during the iteration (otherwise
		//we'd get a concurrentModificationException)
		Vector<String> names = new Vector<String>(l.getNames());
		//Iterate through the original Lexicon names
		for(String s : names)
		{
			//We don't match formulas to WordNet
			if(StringParser.isFormula(s))
				continue;
			//Find all wordForms in WordNet for each name
			HashSet<String> wordForms = wn.getAllWordForms(s);
			int size = wordForms.size();
			if(size == 0)
				continue;
			double conf = CONFIDENCE - 0.01*size;
			if(conf < thresh)
				continue;
			Set<Integer> terms = l.getInternalClasses(s);
			//Add each term with the name to the extension Lexicon
			for(Integer i : terms)
			{
				double weight = conf * l.getWeight(s, i);
				if(weight < thresh)
					continue;
				for(String w : wordForms)
					l.addClass(i, w, TYPE, SOURCE, weight);
			}
		}
	}
	
	//Returns a copy of the given Lexicon, extended with WordNet
	private Lexicon getExtensionLexicon(Lexicon l, double thresh)
	{
		Lexicon ext = new Lexicon(l);
		Set<String> names = l.getNames();
		//Iterate through the original Lexicon names
		for(String s : names)
		{
			//We don't match formulas to WordNet
			if(StringParser.isFormula(s))
				continue;
			//Find all wordForms in WordNet for each name
			HashSet<String> wordForms = wn.getAllWordForms(s);
			int size = wordForms.size();
			if(size == 0)
				continue;
			double conf = CONFIDENCE - 0.01*size;
			if(conf < thresh)
				continue;
			Set<Integer> terms = l.getInternalClasses(s);
			//Add each term with the name to the extension Lexicon
			for(Integer i : terms)
			{
				double weight = conf * l.getWeight(s, i);
				if(weight < thresh)
					continue;
				for(String w : wordForms)
					ext.addClass(i, w, "en", TYPE, SOURCE, weight);
			}
		}
		return ext;
	}
	
	//Matches two given Lexicons (after extension with WordNet)
	private Alignment match(Lexicon source, Lexicon target, double thresh)
	{
		Alignment maps = new Alignment();
		Lexicon larger, smaller;
		//To minimize iterations, we want to iterate through the
		//ontology with the smallest Lexicon
		boolean sourceIsSmaller = (source.nameCount() <= target.nameCount());
		double weight, similarity;
		if(sourceIsSmaller)
		{
			smaller = source;
			larger = target;
		}
		else
		{
			smaller = target;
			larger = source;
		}
		//Get the smaller ontology names
		Set<String> names = smaller.getNames();
		for(String s : names)
		{
			//Get all term indexes for the name in both ontologies
			Set<Integer> largerIndexes = larger.getClasses(s);
			Set<Integer> smallerIndexes = smaller.getClasses(s);
			if(largerIndexes == null)
				continue;
			//Otherwise, match all indexes
			for(Integer i : smallerIndexes)
			{
				//Get the weight of the name for the term in the smaller lexicon
				weight = smaller.getCorrectedWeight(s, i);
				Set<String> smallerSources = smaller.getSources(s, i);
				for(Integer j : largerIndexes)
				{
					Set<String> largerSources = larger.getSources(s, j);
					//We only consider matches involving at least one WordNet synonym
					//and not envolving any external synonyms
					boolean check = (smallerSources.contains(SOURCE) && largerSources.contains(SOURCE)) ||
							(smallerSources.contains(SOURCE) && largerSources.contains("")) ||
							(smallerSources.contains("") && largerSources.contains(SOURCE));
					if(!check)
						continue;
					//Get the weight of the name for the term in the larger lexicon
					similarity = larger.getCorrectedWeight(s, j);
					//Then compute the similarity, by multiplying the two weights
					similarity *= weight;
					//If the similarity is above threshold
					if(similarity >= thresh)
					{
						//Add the mapping, taking into account the order of the ontologies
						if(sourceIsSmaller)
							maps.add(new Mapping(i, j, similarity));
						else
							maps.add(new Mapping(j, i, similarity));
					}
				}
			}
		}
		return maps;	
	}
}