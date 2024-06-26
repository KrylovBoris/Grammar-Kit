/*
 * Copyright 2011-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.intellij.jflex.psi.impl;

import com.intellij.lang.injection.InjectedLanguageManager;
import com.intellij.openapi.application.QueryExecutorBase;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.*;
import com.intellij.psi.search.LocalSearchScope;
import com.intellij.psi.search.SearchScope;
import com.intellij.psi.search.searches.ReferencesSearch;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.Processor;
import com.intellij.util.containers.JBIterable;
import org.intellij.jflex.psi.JFlexDeclarationsSection;
import org.intellij.jflex.psi.JFlexJavaCode;
import org.intellij.jflex.psi.JFlexStateDefinition;
import org.jetbrains.annotations.NotNull;

/**
 * @author gregsh
 */
final class JFlexStateUsageSearcher extends QueryExecutorBase<PsiReference, ReferencesSearch.SearchParameters> implements DumbAware {
  private JFlexStateUsageSearcher() {
    super(true);
  }

  @Override
  public void processQuery(@NotNull ReferencesSearch.SearchParameters queryParameters, @NotNull Processor<? super PsiReference> consumer) {
    PsiElement element = queryParameters.getElementToSearch();
    PsiFile containingFile = element.getContainingFile();
    if (element instanceof PsiField) {
      PsiElement context = containingFile == null ? null : containingFile.getContext();
      if (!(context instanceof JFlexJavaCode)) return;
      String name = ((PsiField)element).getName();
      PsiFile file = context.getContainingFile();
      JFlexStateDefinition state =
        SyntaxTraverser.psiTraverser(
          PsiTreeUtil.findChildOfType(file, JFlexDeclarationsSection.class))
          .filter(JFlexStateDefinition.class)
          .filter(o -> name.equals(o.getName()))
          .first();
      if (state != null) {
        SearchScope scope = queryParameters.getEffectiveSearchScope().intersectWith(new LocalSearchScope(file));
        queryParameters.getOptimizer().searchWord(state.getName(), scope, true, state);
      }
    }
    else if (element instanceof JFlexStateDefinition) {
      JFlexStateDefinition state = (JFlexStateDefinition)element;
      String name = state.getName();
      JFlexJavaCode javaCode = SyntaxTraverser.psiTraverser(containingFile).filter(JFlexJavaCode.class).first();

      if (javaCode == null) return;
      Pair<PsiElement, TextRange> injectedFile =
        JBIterable.from(InjectedLanguageManager.getInstance(javaCode.getProject()).getInjectedPsiFiles(javaCode)).first();
      if (injectedFile != null && injectedFile.first instanceof PsiJavaFile) {
        PsiJavaFile javaFile = (PsiJavaFile)injectedFile.first;
        PsiField field = JBIterable.of(javaFile.getClasses())
          .take(1).flatMap(o -> JBIterable.of(o.getFields()))
          .find(o -> name.equals(o.getName()));
        if (field != null) {
          SearchScope scope = queryParameters.getEffectiveSearchScope().intersectWith(new LocalSearchScope(javaFile));
          queryParameters.getOptimizer().searchWord(name, scope, true, field);
        }
      }
    }
  }
}
