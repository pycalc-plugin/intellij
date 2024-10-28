package com.pycalc.intellij;

import com.intellij.notification.NotificationGroupManager;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import org.python.core.PyException;
import org.python.util.InteractiveConsole;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class Common {
    public static void notifyError(Project project, String content) {
        if (project == null) {
            return;
        }

        NotificationGroupManager.getInstance()
                .getNotificationGroup("notify")
                .createNotification("pycalc", content, NotificationType.ERROR)
                .notify(project);
    }

    public static String runCode(InteractiveConsole py, String code, Boolean multiline) throws PyException {
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        ByteArrayOutputStream berr = new ByteArrayOutputStream();
        PrintStream cout = new PrintStream(bout);
        PrintStream cerr = new PrintStream(berr);

        py.setOut(cout);
        py.setErr(cerr);

        if (multiline) {
            py.exec(code);
        } else {
            String line;
            if (code.endsWith("\n")) {
                line = code.substring(0, code.length() - 1);
            } else {
                line = code;
            }
            py.push(line);
        }

        if (!berr.toString().isEmpty()) {
            throw new PycException(berr.toString());
        }

        return bout.toString();
    }

    public static String executePythonCode(InteractiveConsole py, Project project, String code, Boolean multiline) {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Future<String> future = executor.submit(() -> runCode(py, code, multiline));

        int duration = 0;
        while (!future.isDone()) {
            try {
                Thread.sleep(50);
            } catch (InterruptedException ignored) {
            }

            if (duration == 200) {
                if (project != null) {
                    int ret = Messages.showYesNoDialog(project, "The Python code has been running for a long time. Do you want to terminate it?", "Long Running", Messages.getInformationIcon());
                    if (ret == Messages.YES) {
                        future.cancel(true);
                    } else {
                        duration = 0;
                    }
                } else {
                    future.cancel(true);
                }
            }
            duration += 1;
        }

        try {
            return future.get();
        } catch (ExecutionException e) {
            if (e.getCause() instanceof PycException) {
                notifyError(project, e.getCause().getMessage());
            } else {
                py.resetbuffer();
                notifyError(project, e.toString());
            }
            return "";
        } catch (Exception e) {
            py.resetbuffer();
            notifyError(project, e.toString());
            return "";
        } finally {
            executor.shutdown();
        }
    }
}
