/* LanguageTool, a natural language style checker
 * Copyright (C) 2005 Daniel Naber (http://www.danielnaber.de)
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
package org.languagetool.rules.ga;

import org.junit.Before;
import org.junit.Test;
import org.languagetool.JLanguageTool;
import org.languagetool.TestTools;
import org.languagetool.language.Irish;
import org.languagetool.rules.RuleMatch;

import java.io.IOException;

import static org.junit.Assert.assertEquals;

public class DhaNoBeirtRuleTest {
  private DhaNoBeirtRule rule;
  private JLanguageTool langTool;

  @Before
  public void setUp() throws IOException {
    rule = new DhaNoBeirtRule(TestTools.getMessages("ga"));
    langTool = new JLanguageTool(new Irish());
  }

  @Test
  public void testRule() throws IOException {
    assertCorrect("Seo abairt bheag.");
    assertCorrect("Tá beirt dheartháireacha agam.");
    assertIncorrectMessage("Tá dhá dheartháireacha agam.", "Ba chóir duit <suggestion>beirt dheartháireacha</suggestion> a scríobh");
    // this next sentence is incorrect, just want a list of words between dhá and déag
    assertIncorrectMessage("Tá dhá dheartháireacha níos aosta déag agam.", "Ba chóir duit <suggestion>dháréag dheartháireacha níos aosta</suggestion> a scríobh");
  }

  private void assertCorrect(String sentence) throws IOException {
    RuleMatch[] matches = rule.match(langTool.getAnalyzedSentence(sentence));
    assertEquals(0, matches.length);
  }

  private void assertIncorrectMessage(String sentence, String expected) throws IOException {
    RuleMatch[] matches = rule.match(langTool.getAnalyzedSentence(sentence));
    assertEquals(1, matches.length);
    assertEquals(expected, matches[0].getMessage());
  }

}