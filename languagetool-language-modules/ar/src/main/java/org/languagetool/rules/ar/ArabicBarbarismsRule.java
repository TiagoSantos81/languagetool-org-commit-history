/*
 * LanguageTool, a natural language style checker
 * Copyright (C) 2020 Sohaib Afifi, Taha Zerrouki
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
package org.languagetool.rules.ar;

import org.languagetool.language.Arabic;
import org.languagetool.rules.AbstractSimpleReplaceRule2;
import org.languagetool.rules.Categories;
import org.languagetool.rules.Example;
import org.languagetool.rules.ITSIssueType;

import java.io.IOException;
import java.util.Locale;
import java.util.ResourceBundle;

/**
 * A rule that matches words which should not be used and suggests correct ones instead. 
 *
 * @author Sohaib AFIFI
 * @since 5.0
 */
public class ArabicBarbarismsRule extends AbstractSimpleReplaceRule2 {

  public static final String PT_BARBARISMS_REPLACE = "AR_BARBARISMS_REPLACE";

  private static final String FILE_NAME = "/ar/barbarisms.txt";

  @Override
  public final String getFileName() {
    return FILE_NAME;
  }

  public ArabicBarbarismsRule(ResourceBundle messages) throws IOException {
    super(messages, new Arabic());
    super.setCategory(Categories.STYLE.getCategory(messages));
    setLocQualityIssueType(ITSIssueType.LocaleViolation);
    addExamplePair(Example.wrong("<marker>طرشي</marker>"),
                   Example.fixed("<marker>فلفل حلو</marker>"));
  }

  @Override
  public final String getId() {
    return PT_BARBARISMS_REPLACE;
  }

  @Override
  public String getDescription() {
    return "كلمات بديلة للكلمات العامية أو الأجنبية";
  }

  @Override
  public String getShort() {
    return "كلمات بديلة للكلمات العامية أو الأجنبية";
  }

  @Override
  public String getSuggestion() {
    return "الكلمة عامية يفضل أن يقال $suggestions";
  }

  @Override
  public String getSuggestionsSeparator() {
    return " أو  ";
  }

  @Override
  public Locale getLocale() {
    return new Locale("ar");
  }

}
