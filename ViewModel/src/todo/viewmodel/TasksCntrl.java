package todo.viewmodel;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.java.html.json.Model;
import net.java.html.json.Function;
import net.java.html.json.Property;
import net.java.html.json.ComputedProperty;
import org.netbeans.api.htmlui.OpenHTMLRegistration;
import org.netbeans.api.htmlui.HTMLDialog;
import org.openide.util.NbBundle;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.util.Lookup;
import todo.model.*;

/**
 * HTML page which displays a window and also a dialog.
 */
@Model(className = "Tasks", targetId = "", properties = {
    @Property(name = "tasks", type = Task.class, array = true),
    @Property(name = "showCompleted", type = boolean.class),
    @Property(name = "sortByPriority", type = boolean.class),
    @Property(name = "selected", type = Task.class),
    @Property(name = "edited", type = Task.class)
})
public final class TasksCntrl {

    private static final SimpleDateFormat DATE_FORMATTER
            = new SimpleDateFormat("yyyy-MM-dd");  // HSQLDB uses this format

    @ComputedProperty
    static String templateName() {
        return "tasks";
    }

    @Function
    static void showAlerts(Tasks model) {
        String reply = Pages.showAlertsDialog(getExpiredTasks(model.getTasks()));
    }

    @ActionID(
            category = "Tools",
            id = "todo.controller.Tasks"
    )
    @ActionReferences({
//        @ActionReference(path = "Menu/Tools"),
        @ActionReference(path = "Toolbars/File"),})
    @NbBundle.Messages("CTL_Tasks=Open Tasks from ViewModel")
    @OpenHTMLRegistration(
            url = "Tasks.html",
            displayName = "#CTL_Tasks",
            iconBase = "todo/viewmodel/resources/icons/addtsk_tsk.gif"
    )
    public static Tasks onPageLoad() {
        TaskManagerInterface taskManager = 
            Lookup.getDefault().lookup(TaskManagerInterface.class);
        Tasks tasks = new Tasks();
        tasks.getTasks().addAll(taskManager.listAllTasks(false));
        return tasks.applyBindings();
    }

    @Model(className = "Task", targetId = "", properties = {
        @Property(name = "id", type = int.class),
        @Property(name = "description", type = String.class),
        @Property(name = "priority", type = int.class),
        @Property(name = "dueDate", type = String.class),
        @Property(name = "alert", type = boolean.class),
        @Property(name = "daysBefore", type = int.class),
        @Property(name = "obs", type = String.class),
        @Property(name = "completed", type = boolean.class)
    })
    public static class TaskModel {

        @ComputedProperty
        public static boolean isLate(String dueDate) {
            if (dueDate == null || dueDate.isEmpty()) {
                return false;
            }
            Date dateDue = null;
            try {
                dateDue = DATE_FORMATTER.parse(dueDate);
            } catch (ParseException ex) {
                Logger.getLogger("")
                        .log(Level.WARNING, null, ex);
            }
            return (dateDue == null) ? false
                    : dateDue.compareTo(Calendar.getInstance().getTime()) < 0;
        }

        @ComputedProperty
        static boolean hasAlert(String dueDate, boolean alert, int daysBefore) {
            if (dueDate == null || dueDate.isEmpty()) {
                return false;
            }
            Date dateDue = null;
            try {
                dateDue = DATE_FORMATTER.parse(dueDate);
            } catch (ParseException ex) {
                Logger.getLogger("")
                        .log(Level.SEVERE, null, ex);
            }
            if (!alert || dateDue == null) {
                return false;
            } else {
                Calendar cal = Calendar.getInstance();
                cal.setTime(dateDue);
                int dias = cal.get(Calendar.DAY_OF_YEAR)
                        - Calendar.getInstance().get(Calendar.DAY_OF_YEAR);
                return dias <= daysBefore;
            }
        }

        @ComputedProperty
        static String validate(String description, int priority, String dueDate, int daysBefore) {
            String errorMsg = null;
            if (description == null || description.isEmpty()) {
                errorMsg = "Specify a description";
            }
            if (errorMsg == null && (priority < 1 || priority > 10)) {
                errorMsg = "Priority must be an integer in the range 1-10";
            }
            if (errorMsg == null) {
                if (dueDate == null) {
                    errorMsg = "Specify a valid due date";
                } else {
                    try {
                        Date dateDue = DATE_FORMATTER.parse(dueDate);
                        if (dateDue == null) {
                            errorMsg = "Specify a valid due date";
                        }
                    } catch (ParseException e) {
                        errorMsg = "Specify a valid due date";
                    }
                }
            }
            if (errorMsg == null && (daysBefore < 0 || daysBefore > 365)) {
                errorMsg = "Days before must be an integer in the range 0-365";
            }

            return errorMsg;
        }
    }

    @Function
    static void addNew(Tasks tasks) {
        tasks.setSelected(null);
        tasks.setEdited(new Task());
        Pages.showEditorDialog(new Task());
    }

    @Function
    static void edit(Tasks tasks, Task data) {
        tasks.setSelected(data);
        tasks.setEdited(data.clone());
        Pages.showEditorDialog(data);
    }

    @Function
    public static void removeTask(Tasks tasks, Task data) {
        tasks.getTasks().remove(data);
    }

    private static boolean validate(Task task) {
        String invalid = null;
        if (task.getValidate() != null) {
            invalid = task.getValidate();
        }
        return invalid == null;
    }

    @Function
    public static void commit(Tasks tasks) {
        final Task task = tasks.getEdited();
        if (task == null || !validate(task)) {
            return;
        }
        final Task selectedTask = tasks.getSelected();
        if (selectedTask != null) {
            tasks.getTasks().set(tasks.getTasks().indexOf(selectedTask), task);
        } else {
            tasks.getTasks().add(task);
        }
        tasks.setEdited(null);
    }

    @Function
    public static void cancel(Tasks tasks) {
        tasks.setSelected(null);
        tasks.setEdited(null);
    }

    private static List<Task> listTasksWithAlert(List<Task> tasks) {
        List<Task> result = new ArrayList<>(tasks.size());
        for (Task task : tasks) {
            if (task.isAlert()) {
                result.add(task);
            }
        }
        return result;
    }

    @ComputedProperty
    public static int numberOfTasksWithAlert(List<Task> tasks) {
        return listTasksWithAlert(tasks).size();
    }

    @ComputedProperty
    public static List<Task> sortedAndFilteredTasks(List<Task> tasks,
            boolean sortByPriority, boolean showCompleted) {
        List<Task> result = new ArrayList<>();
        if (showCompleted) {
            for (Task task : tasks) {
                if (task.isCompleted()) {
                    result.add(task);
                }
            }
        } else {
            result.addAll(tasks);
        }

        if (sortByPriority) {
            result.sort(new PriorityComparator());
        } else {
            result.sort(new DueDateComparator());
        }
        return result;
    }

    private static class PriorityComparator implements Comparator<Task> {

        @Override
        public int compare(final Task t1, final Task t2) {
            if (t1.getPriority() == t2.getPriority()) {
                return 0;
            } else if (t1.getPriority() > t2.getPriority()) {
                return 1;
            } else {
                return -1;
            }
        }
    }

    private static class DueDateComparator implements Comparator<Task> {

        @Override
        public int compare(final Task t1, final Task t2) {
            try {
                Date t1DateDue = DATE_FORMATTER.parse(t1.getDueDate());
                Date t2DateDue = DATE_FORMATTER.parse(t2.getDueDate());
                return t1DateDue.compareTo(t2DateDue);
            } catch (ParseException ex) {
                Logger.getLogger("").log(Level.WARNING, null, ex);
                return -1;
            }
        }
    }

    static String getExpiredTasks(final List<Task> tasks) {
        StringBuilder sb = new StringBuilder();
        List<Task> listTasksWithAlert = listTasksWithAlert(tasks);
        sb.append("<ul>");
        for (Task task : listTasksWithAlert) {
            sb.append("<li>").append("Task: '").append(task.getDescription()).append("' expired on ").append(task.getDueDate()).append("</li>");
        }
        sb.append("</ul>");
        return sb.toString();
    }

//
    // dialog UI
    // 
    @HTMLDialog(url = "Tasks.html")
    static void showAlertsDialog(String t) {
        new ShowAlertsDialog(t).applyBindings();
    }

    @Model(className = "ShowAlertsDialog", targetId = "", properties = {
        @Property(name = "text", type = String.class)})
    static final class ShowAlertsDialogCntrl {

        @ComputedProperty
        static String templateName() {
            return "alerts";
        }
    }

    @HTMLDialog(url = "Tasks.html")
    static void showEditorDialog(Task t) {
        new EditorDialog(t).applyBindings();
    }
    @Model(className = "EditorDialog", targetId = "", properties = {
        @Property(name = "task", type = Task.class)})
    static final class TaskEditorCntrl {

        @ComputedProperty
        static String templateName() {
            return "editor";
        }
    }
}
