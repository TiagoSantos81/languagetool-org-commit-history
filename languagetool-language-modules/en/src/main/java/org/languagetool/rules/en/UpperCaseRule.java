/* LanguageTool, a natural language style checker 
 * Copyright (C) 2020 Daniel Naber (http://www.danielnaber.de)
 * 
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301
 * USA
 */
package org.languagetool.rules.en;

import com.hankcs.algorithm.AhoCorasickDoubleArrayTrie;
import org.languagetool.AnalyzedSentence;
import org.languagetool.AnalyzedTokenReadings;
import org.languagetool.Language;
import org.languagetool.LinguServices;
import org.languagetool.UserConfig;
import org.languagetool.rules.*;
import org.languagetool.rules.patterns.PatternToken;
import org.languagetool.rules.spelling.CachingWordListLoader;
import org.languagetool.tagging.disambiguation.rules.DisambiguationPatternRule;
import org.languagetool.tools.StringTools;

import java.io.IOException;
import java.util.*;

import static org.languagetool.rules.patterns.PatternRuleBuilderHelper.*;

/**
 * Finds some(!) words written uppercase that should be spelled lowercase.
 * @since 5.0
 */
public class UpperCaseRule extends Rule {

  private static MorfologikAmericanSpellerRule spellerRule;
  private static LinguServices linguServices = null;
  private static Set<String> exceptions = new HashSet<>(Arrays.asList(
    "Bin", "Spot",  // names
    "French", "Roman", "Hawking", "Square", "Japan", "Premier"
  ));
  private static final AhoCorasickDoubleArrayTrie<String> exceptionTrie = new AhoCorasickDoubleArrayTrie<>();
  private static final List<List<PatternToken>> ANTI_PATTERNS = Arrays.asList(
    Arrays.asList(
      token("Hugs"), token("and"), token("Kisses")
    ),
    Arrays.asList( // Please go to File and select Options. 
      token("go"),
      token("to"),
      tokenRegex("[A-Z].+")
    ),
    Arrays.asList(
      tokenRegex("[A-Z].+"),
      token(","),
      tokenRegex("[Aa]nd|[Oo]r|&"),
      tokenRegex("[A-Z].+")
    ),
    Arrays.asList( // "The goal is to Develop, Discuss and Learn.""
      tokenRegex("[A-Z].+"),
      token(","),
      tokenRegex("[A-Z].+"),
      tokenRegex("[Aa]nd|[Oo]r|&|,"),
      tokenRegex("[A-Z].+")
    ),
    Arrays.asList(
      tokenRegex("[A-Z].+"),
      token(">"),
      tokenRegex("[A-Z].+")
    ),
    Arrays.asList(
      pos("SENT_START"), // Two-word phrases with "?" or "!": "What Happened?", "Catch Up!" (can be headlines)
      tokenRegex("[A-Z].+"),
      tokenRegex("[A-Z].+"),
      tokenRegex("[\\!\\?]")
    ),
    Arrays.asList(
      pos("SENT_START"), // Step1 - Watch the full episode.
      tokenRegex(".*\\w.*"),
      tokenRegex("-|–"),
      tokenRegex("[A-Z].+")
    ),
    Arrays.asList(
      pos("SENT_START"), // Step 1 - Watch the full episode.
      tokenRegex(".*\\w.*"),
      tokenRegex("[0-9]+"),
      tokenRegex("-|–"),
      tokenRegex("[A-Z].+")
    ),
    Arrays.asList(
      pos("SENT_START"), // Markdowm headline # Show some
      token("#"),
      tokenRegex("[A-Z].+")
    ),
    Arrays.asList(
      pos("SENT_START"), // Markdowm headline ## Show some
      token("#"),
      token("#"),
      tokenRegex("[A-Z].+")
    ),
    Arrays.asList(
      pos("SENT_START"), // Markdowm headline ## Show some
      token("#"),
      token("#"),
      token("#"),
      tokenRegex("[A-Z].+")
    ),
    Arrays.asList( // Scene 4, Lines 93-96
      tokenRegex("[A-Z].+"),
      tokenRegex("\\d+"),
      tokenRegex("-|–|,"),
      tokenRegex("[A-Z].+"),
      tokenRegex("\\d+")
    ),
    Arrays.asList( // 1.- Sign up for ...
      tokenRegex("\\d+"),
      token("."),
      tokenRegex("-|–"),
      tokenRegex("[A-Z].+")
    ),
    Arrays.asList( // French Quote + Uppercase word
      tokenRegex("«"),
      tokenRegex("[A-Z].+")
    ),
    Arrays.asList( // H1 What's wrong?
      tokenRegex("H[1-6]"),
      tokenRegex("[A-Z].+")
    ),
    Arrays.asList(
      pos("SENT_START"), // ii) Enabling you to ...
      tokenRegex("[a-z]{1,2}"),
      token(")"),
      tokenRegex("[A-Z].+")
    ),
    Arrays.asList(
      pos("SENT_START"), // bullet point
      token("•"),
      tokenRegex("[A-Z].+")
    ),
    Arrays.asList( // Dash + Uppercase word
      tokenRegex("-|–"),
      tokenRegex("[A-Z].+")
    ),
    Arrays.asList(
      tokenRegex("Step|Grade"), // I finished Step 6
      tokenRegex("\\d+")
    ),
    Arrays.asList(
      tokenRegex("the|our|their"), // Let's talk to the Onboarding team.
      tokenRegex("[A-Z].+"),
      tokenRegex("team|department")
    ),
    Arrays.asList(
      pos("SENT_START"), // 12.3 Game.
      tokenRegex("\\d+"),
      tokenRegex("\\.|/"),
      tokenRegex("\\d+"),
      tokenRegex("[A-Z].+")
    ),
    Arrays.asList(
      pos("SENT_START"), // Lesson #1 - Learn the alphabet.
      tokenRegex(".*\\w.*"),
      token("#"),
      tokenRegex("[0-9]+"),
      tokenRegex("-|–"),
      tokenRegex("[A-Z].+")
    ),
    Arrays.asList(
      token("BBC"),
      token("Culture")
    ),
    Arrays.asList(
      token("Amazon"),
      token("Live")
    ),
    Arrays.asList( // Company name
      token("Volvo"),
      token("Buses")
    ),
    Arrays.asList(
      tokenRegex("[A-Z].+"),
      token("/"),
      tokenRegex("[A-Z].+")
    ),
    Arrays.asList( // "Order #76540"
      csRegex("[A-Z].+"),
      token("#"),
      tokenRegex("\\d+")
    ),
    Arrays.asList( // "He plays games at Games.co.uk."
      csRegex("[A-Z].+"),
      token("."),
      tokenRegex("com?|de|us|gov|net|info|org|es|mx|ca|uk|at|ch|it|pl|ru|nl|ie|be|fr")
    ),
    Arrays.asList(
      tokenRegex("[A-Z].+"),  // He's Ben (Been)
      token("("),
      tokenRegex("[A-Z].+"),
      token(")")
    ),
    Arrays.asList(
      token("["),
      tokenRegex("[A-Z].+"),
      token("]")
    ),
    Arrays.asList(
      token("Pay"),
      token("per"),
      tokenRegex("[A-Z].+")
    ),
    Arrays.asList(
      tokenRegex("Hi|Hello|Heya?"),
      token(","),
      tokenRegex("[A-Z].+")
    ),
    Arrays.asList( // "C stands for Curse."
      tokenRegex("[A-Z]"),
      tokenRegex("is|stands"),
      token("for"),
      tokenRegex("[A-Z].+")
    ),
    Arrays.asList(
      pos("SENT_START"), // Stop & Jot: (short headlines with colon)
      tokenRegex("[A-Z].+"),
      token("&"),
      tokenRegex("[A-Z].+"),
      token(":")
    ),
    Arrays.asList(
      pos("SENT_START"), // The Story: (short headlines with colon)
      tokenRegex("[A-Z].+"),
      tokenRegex("[A-Z].+"),
      token(":")
    ),
    Arrays.asList(
      pos("SENT_START"), // Easy to Use: (short headlines with colon)
      tokenRegex("[A-Z].+"),
      tokenRegex("[a-z].+"),
      tokenRegex("[A-Z].+"),
      token(":")
    ),
    Arrays.asList(
      tokenRegex("[A-Z].+"),  // e.g. "Top 10% Lunch Deals"
      tokenRegex("\\d+%?"),
      tokenRegex("[A-Z].+")
    ),
    Arrays.asList(
      tokenRegex("[0-9]+"),  // e.g. "6) Have a beer"
      tokenRegex("[)\\]]"),
      tokenRegex("[A-Z].+")
    ),
    Arrays.asList(
      tokenRegex("[a-z]"),  // e.g. "a) Have a beer"
      tokenRegex("[)\\]]"),
      tokenRegex("[A-Z].+")
    ),
    Arrays.asList(
      tokenRegex("[\\(\\]]"),  // e.g. "(b) Have a beer"
      tokenRegex("[a-z0-9]"),
      tokenRegex("[)\\]]")
    ),
    Arrays.asList(
      tokenRegex("[A-Z].+"),  // e.g. "Freelance 2.0"
      tokenRegex("[0-9]+"),
      tokenRegex("."),
      tokenRegex("[0-9]+")
    ),
    Arrays.asList(
      tokenRegex("[A-Z].*"),  // e.g. "You Don't Know" or "Kuiper’s Belt"
      tokenRegex("['’`´‘]"),
      tokenRegex("t|d|ve|s|re|m|ll"),
      tokenRegex("[A-Z].+")
    ),
    Arrays.asList(
      tokenRegex("[A-Z].+"),  // e.g. "Culture, People , Nature", probably a title
      token(","),
      tokenRegex("[A-Z].+"),
      token(","),
      tokenRegex("[A-Z].+")
    ),
    Arrays.asList(
      tokenRegex("The"),  // e.g. "The Sea is Watching", probably a title
      tokenRegex("[A-Z].+"),
      token("is"),
      tokenRegex("[A-Z].+")
    ),
    Arrays.asList( // My name is Gentle.
      token("name"),
      token("is"),
      tokenRegex("[A-Z].+")
    ),
    Arrays.asList( // They called it Greet.
      tokenRegex("call|calls|called"),
      token("it"),
      tokenRegex("[A-Z].+")
    ),
    Arrays.asList(
      token("Professor"),
      tokenRegex("[A-Z].+")
    ),
    Arrays.asList(
      token("Time"),
      token("magazine")
    ),
    Arrays.asList( // What is Foreshadowing?
      tokenRegex("Who|What"),
      tokenRegex("is|are|was|were"),
      tokenRegex("[A-Z].+"),
      token("?")
    ),
    Arrays.asList( // His name is Carp.
      token("name"),
      tokenRegex("is|was"),
      tokenRegex("[A-Z].+")
    ),
    Arrays.asList( // FDM Group
      tokenRegex("[A-Z].*"),
      token("Group")
    ),
    Arrays.asList( // Victor or Rabbit as everyone calls him.
      pos("NNP"),
      tokenRegex("or|and|&"),
      tokenRegex("[A-Z].*")
    ),
    Arrays.asList(
      tokenRegex("Google|Microsoft"), // Microsoft Teams / Google Maps (not tagged as NNP)
      tokenRegex("Teams|Maps|Canvas|Remind|Switch|Gems?") 
    ),
    Arrays.asList(
      pos("SENT_START"), // Music and Concepts.
      tokenRegex("[A-Z].*"),
      tokenRegex("or|and|&"),
      tokenRegex("[A-Z].*"),
      pos("SENT_END")
    )
  );

  private final Language lang;

  public UpperCaseRule(ResourceBundle messages, Language lang, UserConfig userConfig) {
    super(messages);
    super.setCategory(Categories.CASING.getCategory(messages));
    this.lang = lang;
    setLocQualityIssueType(ITSIssueType.Misspelling);
    addExamplePair(Example.wrong("I really <marker>Like</marker> spaghetti."),
                   Example.fixed("I really <marker>like</marker> spaghetti"));
    if (userConfig != null && linguServices == null) {
      linguServices = userConfig.getLinguServices();
      initTrie();
    }
    if (spellerRule == null) {
      initTrie();
      try {
        spellerRule = new MorfologikAmericanSpellerRule(messages, lang);
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }
  }

  private void initTrie() {
    CachingWordListLoader cachingWordListLoader = new CachingWordListLoader();
    List<String> words = new ArrayList<>();
    words.addAll(cachingWordListLoader.loadWords("en/specific_case.txt"));
    words.addAll(cachingWordListLoader.loadWords("spelling_global.txt"));
    Map<String,String> map = new HashMap<>();
    for (String word : words) {
      map.put(word, word);
    }
    synchronized (exceptionTrie) {
      exceptionTrie.build(map);
    }
  }

  @Override
  public List<DisambiguationPatternRule> getAntiPatterns() {
    return makeAntiPatterns(ANTI_PATTERNS, lang);
  }

  @Override
  public final String getId() {
    return "EN_UPPER_CASE";
  }

  @Override
  public String getDescription() {
    return "Checks wrong uppercase spelling of words that are not proper nouns";
  }

  @Override
  public RuleMatch[] match(AnalyzedSentence sentence) throws IOException {
    List<RuleMatch> matches = new ArrayList<>();
    AnalyzedTokenReadings[] tokens = getSentenceWithImmunization(sentence).getTokensWithoutWhitespace();
    boolean atSentStart = true;

    boolean isSentence = isSentence(tokens);
    if (!isSentence) {
      // might be a headline, so skip it
      return toRuleMatchArray(matches);
    }

    for (int i = 0; i < tokens.length; i++) {
      AnalyzedTokenReadings token = tokens[i];
      String tokenStr = token.getToken();
      //System.out.println(i + ": " + prevIsUpperCase(tokens, i) + " - " + tokenStr);
      if (tokenStr.length() > 0
          && !token.isImmunized()
          && Character.isUpperCase(tokenStr.charAt(0))
          && !StringTools.isAllUppercase(tokenStr)
          && !atSentStart
          && token.hasPosTagStartingWith("VB")   // start only with these to avoid false alarms. TODO: extend
          && !token.hasPosTagStartingWith("NNP")
          && token.isTagged()
          && (!prevIsUpperCase(tokens, i) || (prevIsUpperCase(tokens, i) && i == 2))  // probably a name, like "Sex Pistols", but not c
          && !nextIsUpperCase(tokens, i)
          && !prevIsOneOf(tokens, i, Arrays.asList(":", "née", "of", "\"", "'"))  // probably a title like "The history of XYZ"
          && !nextIsOneOfThenUppercase(tokens, i, Arrays.asList("of"))
          && !tokenStr.matches("I")
          && !exceptions.contains(tokenStr)
          && !isMisspelled(StringTools.lowercaseFirstChar(tokenStr))    // e.g. "German" is correct, "german" isn't
          && !trieMatches(sentence.getText(), token)
      ) {
        String msg = "Only proper nouns start with an uppercase character (there are exceptions for headlines).";
        RuleMatch match = new RuleMatch(this, sentence, token.getStartPos(), token.getEndPos(), msg);
        match.setSuggestedReplacement(StringTools.lowercaseFirstChar(tokenStr));
        matches.add(match);
      }
      if (!token.isSentenceStart() && !tokenStr.isEmpty() && !token.isNonWord()) {
        atSentStart = false;
      }
    }
    return toRuleMatchArray(matches);
  }

  private boolean trieMatches(String text, AnalyzedTokenReadings token) {
    List<AhoCorasickDoubleArrayTrie.Hit<String>> hits = exceptionTrie.parseText(text);
    for (AhoCorasickDoubleArrayTrie.Hit<String> hit : hits) {
      if (hit.begin <= token.getStartPos() && hit.end >= token.getEndPos()) {
        return true;
      }
    }
    return false;
  }

  private boolean prevIsOneOf(AnalyzedTokenReadings[] tokens, int i, List<String> strings) {
    return i > 0 && strings.contains(tokens[i-1].getToken());
  }

  // e.g. "The history of Xyz"
  //           ^^^^^^^
  private boolean nextIsOneOfThenUppercase(AnalyzedTokenReadings[] tokens, int i, List<String> strings) {
    return i + 2 < tokens.length && strings.contains(tokens[i+1].getToken()) && StringTools.startsWithUppercase(tokens[i+2].getToken());
  }

  private boolean prevIsUpperCase(AnalyzedTokenReadings[] tokens, int i) {
    return i > 0 && StringTools.startsWithUppercase(tokens[i-1].getToken());
  }

  private boolean nextIsUpperCase(AnalyzedTokenReadings[] tokens, int i) {
    return i + 1 < tokens.length && StringTools.startsWithUppercase(tokens[i+1].getToken());
  }

  boolean isMisspelled(String word) throws IOException {
    return (linguServices == null ? spellerRule.isMisspelled(word) : !linguServices.isCorrectSpell(word, lang));
  }

  private boolean isSentence(AnalyzedTokenReadings[] tokens) {
    boolean isSentence = false;
    for (int i = tokens.length - 1; i > 0; i--) {
      if (tokens[i].getToken().matches("[.!?:]")) {
        isSentence = true;
        break;
      }
      if (!tokens[i].isParagraphEnd() && !tokens[i].isNonWord()) {
        break;
      }
    }
    return isSentence;
  }

}
