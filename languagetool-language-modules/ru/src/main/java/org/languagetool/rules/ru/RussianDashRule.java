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

package org.languagetool.rules.ru;

import com.hankcs.algorithm.AhoCorasickDoubleArrayTrie;
import org.languagetool.rules.AbstractDashRule;

/**
 * Check for compounds written with dashes instead of hyphens.
 * @since 4.2
 */
public class RussianDashRule extends AbstractDashRule {

  private static final AhoCorasickDoubleArrayTrie<String> trie = loadCompoundFile("/ru/compounds.txt");

  public RussianDashRule() {
    super(trie);
    setDefaultTempOff(); // Slows down start up. See GitHub issue #1016.
  }

  @Override
  public String getId() {
    return "RU_DASH_RULE";
  }

  @Override
  public String getDescription() {
    return "Проверка на использование тире вместо дефиса (то есть «из — за» вместо «из-за»).";
  }

  @Override
  public String getMessage() {
    return "Использовано тире вместо дефиса.";
  }

}
