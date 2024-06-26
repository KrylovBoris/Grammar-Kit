/*
 * Copyright 2011-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.intellij.grammar.psi;

import com.intellij.openapi.application.QueryExecutorBase;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiReference;
import com.intellij.psi.search.LocalSearchScope;
import com.intellij.psi.search.SearchScope;
import com.intellij.psi.search.searches.ReferencesSearch;
import com.intellij.util.Processor;
import org.intellij.grammar.psi.impl.BnfStringImpl;
import org.jetbrains.annotations.NotNull;

/**
 * @author gregsh
 */
final class BnfAttrPatternRefSearcher extends QueryExecutorBase<PsiReference, ReferencesSearch.SearchParameters> {
  BnfAttrPatternRefSearcher() {
    super(true);
  }

  @Override
  public void processQuery(@NotNull ReferencesSearch.SearchParameters queryParameters, @NotNull Processor<? super PsiReference> consumer) {
    PsiElement target = queryParameters.getElementToSearch();
    if (!(target instanceof BnfRule)) return;

    SearchScope scope = queryParameters.getEffectiveSearchScope();
    if (!(scope instanceof LocalSearchScope)) return;

    PsiFile file = target.getContainingFile();
    if (!(file instanceof BnfFile)) return;

    for (BnfAttrs attrs : ((BnfFile)file).getAttributes()) {
      for (BnfAttr attr : attrs.getAttrList()) {
        BnfAttrPattern pattern = attr.getAttrPattern();
        if (pattern == null) continue;
        BnfStringLiteralExpression patternExpression = pattern.getLiteralExpression();

        PsiReference ref = BnfStringImpl.matchesElement(patternExpression, target) ? patternExpression.getReference() : null;
        if (ref != null && ref.isReferenceTo(target)) {
          if (!consumer.process(ref)) return;
        }
      }
    }
  }
}
