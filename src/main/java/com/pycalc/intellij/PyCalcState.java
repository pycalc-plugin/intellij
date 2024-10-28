package com.pycalc.intellij;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.*;

import com.intellij.openapi.components.PersistentStateComponent;
import org.jetbrains.annotations.NotNull;
import org.python.core.CompilerFlags;
import org.python.util.InteractiveConsole;


@State(name = "PyCalcState", storages = {@Storage("pycalc_settings.xml")})
public class PyCalcState implements PersistentStateComponent<PyCalcState> {

    private boolean enabled;
    private transient InteractiveConsole py;

    public PyCalcState() {
        enabled = true;

        this.py = new InteractiveConsole() {
            {
                cflags = new CompilerFlags(CompilerFlags.PyCF_SOURCE_IS_UTF8);
            }
        };

        String init_code =
            "from __future__ import absolute_import\n" +
            "from __future__ import division\n" +
            "from __future__ import generators\n" +
            "from __future__ import nested_scopes\n" +
            "from __future__ import print_function\n" +
            "from __future__ import with_statement\n";
        this.py.exec(init_code);
    }

    public static PyCalcState getInstance() {
        return ApplicationManager.getApplication().getService(PyCalcState.class);
    }

    @Override
    public void initializeComponent() {
        PersistentStateComponent.super.initializeComponent();
    }

    @Override
    public PyCalcState getState() {
        return this;
    }

    @Override
    public void loadState(@NotNull PyCalcState state) {
        setEnabled(state.isEnabled());
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    InteractiveConsole py() {
        return py;
    }
}
