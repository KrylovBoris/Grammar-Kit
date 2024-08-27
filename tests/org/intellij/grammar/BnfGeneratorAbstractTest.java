package org.intellij.grammar;

import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.io.FileUtilRt;
import com.intellij.openapi.vfs.CharsetToolkit;
import org.intellij.grammar.generator.GenOptions;
import org.intellij.grammar.generator.GeneratorBase;
import org.intellij.grammar.generator.ParserGenerator;
import org.intellij.grammar.psi.BnfFile;
import org.jetbrains.annotations.NonNls;

import java.io.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * @author gregsh
 */
public abstract class BnfGeneratorAbstractTest extends BnfGeneratorTestCase {

  public BnfGeneratorAbstractTest(String s) {
    super(s);
  }

  protected Collection<GeneratorBase> newTestGenerator() {
    var bnfFile = (BnfFile)myFile;
    return List.of((new ParserGenerator(bnfFile, "", myFullDataPath, "") {

                     @Override
                     protected PrintWriter openOutputInner(String className, File file) throws IOException {
                       String grammarName = FileUtil.getNameWithoutExtension(myFile.getName());
                       String fileName = FileUtil.getNameWithoutExtension(file);
                       String name = grammarName + (fileName.startsWith(grammarName) || fileName.endsWith("Parser") ? "" : ".PSI") + ".java";
                       File targetFile = new File(FileUtilRt.getTempDirectory(), name);
                       targetFile.getParentFile().mkdirs();
                       FileOutputStream outputStream = new FileOutputStream(targetFile, true);
                       PrintWriter out = new PrintWriter(new OutputStreamWriter(outputStream, myFile.getVirtualFile().getCharset()));
                       out.println("// ---- " + file.getName() + " -----------------");
                       return out;
                     }
                   }
                   )
    );
  }

  @Override
  protected String loadFile(@NonNls String name) throws IOException {
    if (name.equals("SelfBnf.bnf")) return super.loadFile("../../grammars/Grammar.bnf");
    if (name.equals("SelfFlex.bnf")) return super.loadFile("../../grammars/JFlex.bnf");
    return super.loadFile(name);
  }

  public void doGenTest(boolean generatePsi) throws Exception {
    String name = getTestName(false);
    String text = loadFile(name + "." + myFileExt);
    myFile = createPsiFile(name, text.replaceAll("generatePsi=[^\n]*", "generatePsi=" + generatePsi));
    List<File> filesToCheck = new ArrayList<>();
    filesToCheck.add(new File(FileUtilRt.getTempDirectory(), name + ".java"));
    if (generatePsi) {
      filesToCheck.add(new File(FileUtilRt.getTempDirectory(), name + ".PSI.java"));
    }
    for (File file : filesToCheck) {
      if (file.exists()) {
        assertTrue(file.delete());
      }
    }

    for (GeneratorBase generator : newTestGenerator()) {
      if (generator instanceof ParserGenerator parserGenerator) {
        if (generatePsi) {
          parserGenerator.generate();
        }
        else {
          parserGenerator.generateParser();
        }
      }
      else {
        generator.generate();
      }
    }


    for (File file : filesToCheck) {
      assertTrue("Generated file not found: " + file, file.exists());
      String expectedName = FileUtil.getNameWithoutExtension(file) + ".expected.java";
      String result = FileUtil.loadFile(file, CharsetToolkit.UTF8, true);
      doCheckResult(myFullDataPath, expectedName, result);
    }
  }
}
