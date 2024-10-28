package com.pycalc.intellij;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.*;
import com.intellij.openapi.project.Project;

import java.util.Objects;


public class PyCalcSelected extends AnAction {
    private static final PyCalcState state;

    static {
        state = PyCalcState.getInstance();
    }

    @Override
    public void actionPerformed(AnActionEvent e) {
        Project project = e.getProject();
        if (project == null) {
            return;
        }

        Editor editor = e.getRequiredData(CommonDataKeys.EDITOR);
        SelectionModel selection = editor.getSelectionModel();

        if (!selection.hasSelection()) {
            return;
        }

        String code = selection.getSelectedText();
        if (code == null) {
            return;
        }

        int offset = selection.getSelectionEnd();
        selection.removeSelection();

        String response = Common.executePythonCode(state.py(), project, code, true);

        if (Objects.equals(code.trim(), response.trim())) {
            return;
        }

        String out;
        if (!code.endsWith("\n")) {
            out = "\n" + response.replaceAll("[\n\r]$", "") + "\n";
        } else {
            out = response.replaceAll("[\n\r]$", "") + "\n";
        }

        Caret caret = editor.getCaretModel().getPrimaryCaret();
        Document document = editor.getDocument();

        Runnable runnable = () -> document.insertString(offset, out);
        WriteCommandAction.runWriteCommandAction(editor.getProject(), runnable);

        caret.moveToOffset(offset + out.length());
    }
}
