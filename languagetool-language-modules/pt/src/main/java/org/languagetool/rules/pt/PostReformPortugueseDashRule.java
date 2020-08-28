/* LanguageTool, a natural language style checker
 * Copyright (C) 2016 Marcin Miłkowski (http://www.languagetool.org)
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
package org.languagetool.rules.pt;

import com.hankcs.algorithm.AhoCorasickDoubleArrayTrie;
import org.languagetool.rules.AbstractDashRule;
import org.languagetool.rules.ITSIssueType;

import java.util.ResourceBundle;

/**
 * Check for compounds written with dashes instead of hyphens.
 * @since 3.8
 */
public class PostReformPortugueseDashRule extends AbstractDashRule {

  private static volatile AhoCorasickDoubleArrayTrie<String> trie;
  
  public PostReformPortugueseDashRule(ResourceBundle messages) {
    super(messages);
    setLocQualityIssueType(ITSIssueType.Typographical);
    setDefaultOff(); //     Slows down start up and checking time too much. See 20170916: https://languagetool.org/regression-tests/performance-data.csv
  }

  @Override
  public String getId() {
    return "PT_POSAO_DASH_RULE";
  }

  @Override
  public String getDescription() {
    return "Travessões no lugar de hífens";
  }

  @Override
  public String getMessage() {
    return "Um travessão foi utilizado em vez de um hífen.";
  }

  @Override
  protected boolean isBoundary(String s) {
    return !s.matches("[a-zA-ZÂâÃãÇçÊêÓóÔôÕõü]");  // chars from http://unicode.e-workers.de/portugiesisch.php
  }

  @Override
  protected AhoCorasickDoubleArrayTrie<String> getCompoundsData() {
    AhoCorasickDoubleArrayTrie<String> data = trie;
    if (data == null) {
      synchronized (PostReformPortugueseDashRule.class) {
        data = trie;
        if (data == null) {
          trie = data = loadCompoundFile("/pt/post-reform-compounds.txt");
        }
      }
    }

    return data;
  }

}
