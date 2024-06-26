/*
 * Copyright 2011-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.intellij.grammar.intention;

import com.intellij.codeInsight.intention.IntentionAction;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;

/**
 * Created by IntelliJ IDEA.
 * Date: 9/1/11
 * Time: 6:42 PM
 *
 * @author Vadim Romansky
 */
public class BnfFlipChoiceIntentionTest extends BasePlatformTestCase {
  public void testCaretAtSeparator() {doTest("rule ::= a <caret>| b", "rule ::= b | a");}
  public void testCaretBeforeSeparator() {doTest("rule ::= a<caret> | b","rule ::= b | a");}
  public void testCaretAfterSeparator() {doTest("rule ::= a | <caret>b","rule ::= b | a");}
  public void testCaretInComment() {doTest("rule ::= a | /* <caret>*/ b","rule ::= b | /* */ a");}
  public void testMultipleChoice() {doTest("rule ::= a | b | c <caret>| d","rule ::= a | b | d | c");}
  public void testComplexCase() {doTest("rule ::= a | b | c c c <caret>| d [d d]","rule ::= a | b | d [d d] | c c c");}

  private void doTest(/*@Language("BNF")*/ String text, /*@Language("BNF")*/ String expected) {
    myFixture.configureByText("a.bnf", text);
    IntentionAction action = new BnfFlipChoiceIntention();
    assertTrue("intention not available", action.isAvailable(getProject(), myFixture.getEditor(), myFixture.getFile()));
    WriteCommandAction.runWriteCommandAction(getProject(), () ->
      action.invoke(getProject(), myFixture.getEditor(), myFixture.getFile()));
    assertSameLines(expected, myFixture.getFile().getText());
  }
}
