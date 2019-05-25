package todo.model.impl;

import java.util.*;
import org.openide.util.lookup.ServiceProvider;
import todo.model.ModelException;
import todo.viewmodel.Task;
import todo.model.TaskManagerInterface;
import todo.model.ValidationException;

@ServiceProvider(service = TaskManagerInterface.class)
public class TaskManager implements TaskManagerInterface {

    private final List<Task> tasks = new ArrayList<>();

    public TaskManager() {
        tasks.add(new Task(1, "Hotel Reservation", 5, "2019-06-01", true, 2, "", false));
        tasks.add(new Task(2, "Book conference room!", 10, "2019-05-25", false, 2, "", false));
    }

    @Override
    public List<Task> listAllTasks(final boolean priorityOrDate) {
        Collections.sort(tasks, priorityOrDate ? new PriorityComparator() : new DueDateComparator());
        return Collections.unmodifiableList(tasks);
    }

    @Override
    public List<Task> listTasksWithAlert() throws ModelException {
        final List<Task> tasksWithAlert = new ArrayList<>(tasks.size());
        for (Task task : tasks) {
            if (task.isAlert()) {
                tasksWithAlert.add(task);
            }
        }
        return Collections.unmodifiableList(tasksWithAlert);
    }

    @Override
    public void addTask(final Task task) throws ValidationException {
        validate(task);
        tasks.add(task);
    }

    @Override
    public void updateTask(final Task task) throws ValidationException {
        validate(task);
        Task oldTask = findTask(task.getId());
        tasks.set(tasks.indexOf(oldTask), task);
    }

    @Override
    public void markAsCompleted(final int id, final boolean completed) {
        Task task = findTask(id);
        task.setCompleted(completed);
    }

    @Override
    public void removeTask(final int id) {
        tasks.remove(findTask(id));
    }

    private boolean isEmpty(final String str) {
        return str == null || str.trim().length() == 0;
    }

    private void validate(final Task task) throws ValidationException {
        if (isEmpty(task.getDescription())) {
            throw new ValidationException("Must provide a task description");
        }
    }

    private Task findTask(final int id) {
        for (Task task : tasks) {
            if (id == task.getId()) {
                return task;
            }
        }
        return null;
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
            return t1.getDueDate().compareTo(t2.getDueDate());
        }
    }
}