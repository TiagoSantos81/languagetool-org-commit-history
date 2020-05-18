/* LanguageTool, a natural language style checker
 * Copyright (C) 2019 Sohaib Afifi, Taha Zerrouki
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
package org.languagetool.synthesis.ar;

import morfologik.stemming.IStemmer;
import morfologik.stemming.WordData;
import org.languagetool.AnalyzedToken;
import org.languagetool.Language;
import org.languagetool.synthesis.BaseSynthesizer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Arabic word form synthesizer.
 * Based on part-of-speech lists in Public Domain. See readme.txt for details,
 * the POS tagset is described in arabic_tags_description.txt.
 * <p>
 * There are two special additions:
 * <ol>
 *    <li>+GF - tag that adds  feminine gender to word</li>
 *    <li>+GM - a tag that adds masculine gender to word</li>
 * </ol>
 *
 * @author Taha Zerrouki
 * @since 4.8
 */
public class ArabicSynthesizer extends BaseSynthesizer {

  private static final String RESOURCE_FILENAME = "/ar/arabic_synth.dict";
  private static final String TAGS_FILE_NAME = "/ar/arabic_tags.txt";

  // A special tag to remove pronouns properly
  private static final String REMOVE_PRONOUN = "(\\+RP)?";


  public ArabicSynthesizer(Language lang) {
    super(RESOURCE_FILENAME, TAGS_FILE_NAME, lang);
  }

  /**
   * Get a form of a given AnalyzedToken, where the form is defined by a
   * part-of-speech tag.
   *
   * @param token  AnalyzedToken to be inflected.
   * @param posTag A desired part-of-speech tag.
   * @return String value - inflected word.
   */
  @Override
  public String[] synthesize(AnalyzedToken token, String posTag) throws IOException {
    IStemmer synthesizer = createStemmer();
    String myPosTag = posTag;
    // a flag to correct special case of posTag
    String correctionFlag = "";
    // extract special signature if exists
    correctionFlag = extractSignature(myPosTag);
    myPosTag = removeSignature(myPosTag);
    // correct postag according to special signature if exists
    myPosTag = correctTag(myPosTag, correctionFlag);
    List<WordData> wordData = synthesizer.lookup(token.getLemma() + "|" + myPosTag);
    List<String> wordForms = new ArrayList<>();
    for (WordData wd : wordData) {
      wordForms.add(wd.getStem().toString());
    }
    return wordForms.toArray(new String[0]);
  }


  /* Extract  */
  public String extractSignature(String postag) {
    String tmp = postag;
    String correctionFlag = "";
    if (tmp.endsWith(REMOVE_PRONOUN)) {
      correctionFlag += "+RP";
    }
    return correctionFlag;
  }

  /* Extract  */
  public String removeSignature(String postag) {
    String tmp = postag;
    if (tmp.endsWith(REMOVE_PRONOUN)) {
      // remove the code
      tmp = tmp.substring(0, tmp.indexOf(REMOVE_PRONOUN));
    }
    return tmp;
  }

  /* remove the flag to an encoded tag */
  public String removeTag(String postag, String flag) {
    StringBuilder tmp = new StringBuilder(postag);
    if (tmp != null) {
      if (flag.equals("H") && tmp.charAt(tmp.length() - 1) == 'H') {

        tmp.setCharAt(tmp.length() - 1, '-');
      }
    }
    return tmp.toString();
  }

  /* correct tags  */
  public String correctTag(String postag, String correctionFlag) {
    if (postag == null) return null;
    String tmp = postag;
    // remove attached pronouns 
    if (correctionFlag.equals("+RP")) {
      tmp = removeTag(tmp, "H");
    }
    return tmp;
  }
}

