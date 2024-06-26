/*
 * Copyright 2011-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.intellij.jflex.editor;

import com.intellij.ide.structureView.*;
import com.intellij.ide.structureView.impl.common.PsiTreeElementBase;
import com.intellij.ide.util.treeView.smartTree.SortableTreeElement;
import com.intellij.lang.PsiStructureViewFactory;
import com.intellij.openapi.editor.Editor;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.SyntaxTraverser;
import com.intellij.util.containers.JBIterable;
import org.intellij.jflex.psi.*;
import org.intellij.jflex.psi.impl.JFlexFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

import static com.intellij.openapi.util.Conditions.instanceOf;
import static com.intellij.openapi.util.text.StringUtil.*;

/**
 * @author gregsh
 */
final class JFlexStructureViewFactory implements PsiStructureViewFactory {
  @Override
  public StructureViewBuilder getStructureViewBuilder(@NotNull PsiFile psiFile) {
    return new TreeBasedStructureViewBuilder() {
      @Override
      public @NotNull StructureViewModel createStructureViewModel(@Nullable Editor editor) {
        return new MyModel(psiFile);
      }

      @Override
      public boolean isRootNodeShown() {
        return false;
      }
    };
  }

  static class MyModel extends StructureViewModelBase implements StructureViewModel.ElementInfoProvider {

    static final Class<?>[] CLASSES = {JFlexOption.class, JFlexMacroDefinition.class, JFlexRule.class};

    protected MyModel(@NotNull PsiFile psiFile) {
      super(psiFile, new MyElement(psiFile));
      withSuitableClasses(CLASSES);
    }

    @Override
    public boolean isAlwaysShowsPlus(StructureViewTreeElement element) {
      return false;
    }

    @Override
    public boolean isAlwaysLeaf(StructureViewTreeElement element) {
      Object o = element.getValue();
      return o instanceof JFlexOption || o instanceof JFlexMacroDefinition;
    }

    @Override
    public boolean shouldEnterElement(Object element) {
      return false;
    }

  }

  static class MyElement extends PsiTreeElementBase<PsiElement> implements SortableTreeElement {

    MyElement(PsiElement element) {
      super(element);
    }

    @Override
    public @NotNull String getAlphaSortKey() {
      return notNullize(getPresentableText());
    }

    @Override
    public @NotNull Collection<StructureViewTreeElement> getChildrenBase() {
      PsiElement o = getElement();
      if (o == null) return Collections.emptyList();
      if (o instanceof JFlexFile) {
        return SyntaxTraverser.psiTraverser(o)
          .expand(instanceOf(JFlexFile.class, JFlexFileSection.class))
          .traverse()
          .filter(instanceOf(MyModel.CLASSES))
          .map(MyElement::new)
          .addAllTo(new ArrayList<>());
      }
      else if (o instanceof JFlexRule) {
        return SyntaxTraverser.psiApi().children(o)
          .filter(instanceOf(MyModel.CLASSES))
          .map(MyElement::new)
          .addAllTo(new ArrayList<>());
      }
      return Collections.emptyList();
    }

    @Override
    public String getPresentableText() {
      PsiElement o = getElement();
      if (o == null) return null;
      if (o instanceof JFlexFile) {
        return ((JFlexFile)o).getName();
      }
      else if (o instanceof JFlexOption) {
        return trimEnd(o.getFirstChild().getText(), "{");
      }
      else if (o instanceof JFlexMacroDefinition) {
        return ((JFlexMacroDefinition)o).getName();
      }
      else if (o instanceof JFlexRule) {
        JFlexStateList states = ((JFlexRule)o).getStateList();
        JFlexExpression expr = ((JFlexRule)o).getExpression();
        StringBuilder sb = new StringBuilder();
        if (states != null) {
          sb.append("<");
          sb.append(join(JBIterable.from(states.getStateReferenceList()).map(PsiElement::getText), ", "));
          sb.append(">");
        }
        if (expr != null) {
          if (states != null) sb.append(" ");
          sb.append(firstLast(expr.getText(), 40));
        }
        return sb.toString();
      }
      return o.getClass().getSimpleName();
    }

    @Override
    public Icon getIcon(boolean open) {
      PsiElement o = getElement();
      if (o == null) return null;
      return o.getIcon(0);
    }
  }
}
