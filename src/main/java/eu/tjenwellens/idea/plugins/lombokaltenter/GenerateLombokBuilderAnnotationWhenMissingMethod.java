
// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package eu.tjenwellens.idea.plugins.lombokaltenter;

import com.intellij.codeInsight.intention.IntentionAction;
import com.intellij.codeInsight.intention.PsiElementBaseIntentionAction;
import com.intellij.lang.jvm.JvmMethod;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.psi.codeStyle.CodeStyleManager;
import com.intellij.psi.codeStyle.JavaCodeStyleManager;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

/**
 * Implements an intention action to replace a ternary statement with if-then-else
 *
 * @author dsl
 */
@NonNls
public class GenerateLombokBuilderAnnotationWhenMissingMethod extends PsiElementBaseIntentionAction implements IntentionAction {

  public static final String LOMBOK_BUILDER = "lombok.Builder";

  /**
   * If this action is applicable, returns the text to be shown in the list of
   * intention actions available.
   */
  @NotNull
  public String getText() {
    return "Add lombok builder";
  }


  /**
   * Returns text for name of this family of intentions. It is used to externalize
   * "auto-show" state of intentions.
   * It is also the directory name for the descriptions.
   *
   * @see     com.intellij.codeInsight.intention.IntentionManager#registerIntentionAndMetaData(IntentionAction, String...)
   * @return  the intention family name.
   */
  @NotNull
  public String getFamilyName() {
    return "GenerateLombokBuilderAnnotationWhenMissingMethod";
  }


  /**
   * Checks whether this intention is available at the caret offset in file - the caret
   * must sit just before a "?" character in a ternary statement. If this condition is met,
   * this intention's entry is shown in the available intentions list.
   *
   * Note: this method must do its checks quickly and return.
   *
   * @param project a reference to the Project object being edited.
   * @param editor  a reference to the object editing the project source
   * @param element a reference to the PSI element currently under the caret
   * @return
   * <ul>
   * <li> true if the caret is in a literal string element, so this functionality
   * should be added to the intention menu.</li>
   * <li> false for all other types of caret positions</li>
   * </ul>
   */
  public boolean isAvailable(@NotNull Project project, Editor editor, @Nullable PsiElement element) {
    if (element == null) {
      return false;
    }

    // Is this a identifier named builder
    if (!(element instanceof PsiIdentifier)) {
      return false;
    }
    final PsiIdentifier identifier = (PsiIdentifier) element;

    if(!identifier.getText().equals("builder"))
      return false;

    // ClassName.builder
    PsiElement parent = identifier.getParent();
    PsiReferenceExpression classReference = (PsiReferenceExpression) parent.getFirstChild();

    // ClassName.builder()
    PsiElement parens = parent.getParent().getLastChild();
    PsiJavaToken lParenth = (PsiJavaToken) parens.getFirstChild();
    PsiJavaToken rParenth = (PsiJavaToken) parens.getLastChild();

    if(lParenth.getTokenType() != JavaTokenType.LPARENTH
      ||rParenth.getTokenType() != JavaTokenType.RPARENTH)
      return false;

    PsiClass psiClass =(PsiClass) classReference.resolve();
    JvmMethod[] builders = psiClass.findMethodsByName("builder", false);
    if(builders.length > 0){
      return false;
    }

    if(psiClass.getModifierList().hasAnnotation(LOMBOK_BUILDER)){
      return false;
    }

    return true;
  }

  private Optional<PsiClass> findRelevantClass(PsiElement element){
    final PsiIdentifier identifier = (PsiIdentifier) element;
    PsiElement parent = identifier.getParent();
    PsiReferenceExpression classReference = (PsiReferenceExpression) parent.getFirstChild();
    PsiClass psiClass =(PsiClass) classReference.resolve();
    return Optional.of(psiClass);
  }

  /**
   * Modifies the Psi to change a ternary expression to an if-then-else statement.
   * If the ternary is part of a declaration, the declaration is separated and
   * moved above the if-then-else statement. Called when user selects this intention action
   * from the available intentions list.
   *
   *   @param  project   a reference to the Project object being edited.
   *   @param  editor    a reference to the object editing the project source
   *   @param  element   a reference to the PSI element currently under the caret
   *   @throws IncorrectOperationException Thrown by underlying (Psi model) write action context
   *   when manipulation of the psi tree fails.
   *   @see GenerateLombokBuilderAnnotationWhenMissingMethod#startInWriteAction()
   */
  public void invoke(@NotNull Project project, Editor editor, PsiElement element) throws IncorrectOperationException {
    // Get the factory for making new PsiElements, and the code style manager to format new statements
    final PsiElementFactory factory = JavaPsiFacade.getInstance(project).getElementFactory();
    final CodeStyleManager codeStylist = CodeStyleManager.getInstance(project);
    final JavaCodeStyleManager javaCodeStyleManager =  JavaCodeStyleManager.getInstance(project);

    findRelevantClass(element)
            .map(PsiModifierListOwner::getModifierList)
            .flatMap(Optional::ofNullable)
            .map(psiModifierList -> psiModifierList.addAnnotation(LOMBOK_BUILDER))
            .ifPresent(javaCodeStyleManager::shortenClassReferences);
  }

  /**
   * Indicates this intention action expects the Psi framework to provide the write action
   * context for any changes.
   *
   * @return <ul>
   * <li> true if the intention requires a write action context to be provided</li>
   * <li> false if this intention action will start a write action</li>
   * </ul>
   */
  public boolean startInWriteAction() {return true;}


}

