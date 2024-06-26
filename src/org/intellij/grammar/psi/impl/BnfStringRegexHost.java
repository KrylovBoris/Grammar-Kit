/*
 * Copyright 2011-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.intellij.grammar.psi.impl;

import com.intellij.psi.impl.JavaRegExpHost;

/**
 * @author gregsh
 */
final class BnfStringRegexHost extends JavaRegExpHost {
  @Override
  public boolean characterNeedsEscaping(char c, boolean isInClass) {
    return c == '\"';
  }
}
