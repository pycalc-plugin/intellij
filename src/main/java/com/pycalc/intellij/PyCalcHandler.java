package com.pycalc.intellij;

import com.intellij.codeInsight.editorActions.enter.EnterHandlerDelegate;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Caret;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.actionSystem.EditorActionHandler;
import com.intellij.openapi.util.Ref;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

class PycException extends RuntimeException {
    public PycException(String message) {
        super(message);
    }
}

public class PyCalcHandler extends ToggleAction implements EnterHandlerDelegate {
    private static final PyCalcState state;

    private String request;
    private String response;

    static {
        state = PyCalcState.getInstance();
    }

    public PyCalcHandler() {
        this.request = "";
        this.response = "";
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
        e.getPresentation().setIcon(AllIcons.Debugger.EvaluateExpression);

        if (state.isEnabled()) {
            e.getPresentation().setText("pycalc [✓]");
        } else {
            e.getPresentation().setText("pycalc [×]");
        }
    }

    @Override
    public boolean isSelected(@NotNull AnActionEvent e) {
        return state.isEnabled();
    }

    @Override
    public void setSelected(@NotNull AnActionEvent e, boolean value) {
        try {
            Common.executePythonCode(state.py(), null, ";", true);
        } catch (Throwable exc) {
            // nothing
        }

        state.setEnabled(value);
    }

/* need for new api version
    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }
*/

    @Override
    public boolean invokeInsideIndent(int newLineCharOffset, @NotNull Editor editor, @NotNull DataContext dataContext) {
        return EnterHandlerDelegate.super.invokeInsideIndent(newLineCharOffset, editor, dataContext);
    }

    @Override
    public Result preprocessEnter(@NotNull PsiFile file, @NotNull Editor editor, @NotNull Ref<Integer> caretOffset, @NotNull Ref<Integer> caretAdvance, @NotNull DataContext dataContext, @Nullable EditorActionHandler originalHandler) {
        if (!state.isEnabled()) {
            return Result.Continue;
        }

        Caret caret = editor.getCaretModel().getPrimaryCaret();
        int start = caret.getVisualLineStart();
        int end = caret.getOffset();
        this.request = editor.getDocument().getText(new TextRange(start, end));
        this.response = Common.executePythonCode(state.py(), editor.getProject(), this.request, false);

        return Result.Continue;
    }

    @Override
    public Result postProcessEnter(@NotNull PsiFile file, @NotNull Editor editor, @NotNull DataContext dataContext) {
        if (!state.isEnabled()) {
            return Result.Continue;
        }

        if (Objects.equals(this.request.trim(), this.response.trim())) {
            return Result.Continue;
        }

        Caret caret = editor.getCaretModel().getPrimaryCaret();
        int end = caret.getOffset();
        String out = this.response.replaceAll("[\n\r]$", "");
        if (out.isEmpty()) {
            return Result.Continue;
        }
        out += "\n";

        Document document = editor.getDocument();

        String finalOut = out;
        Runnable runnable = () -> document.insertString(end, finalOut);
        WriteCommandAction.runWriteCommandAction(editor.getProject(), runnable);

        caret.moveToOffset(end + out.length());

        return Result.Continue;
    }
}
