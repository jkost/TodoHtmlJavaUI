package todo.model;

import java.util.List;
import todo.viewmodel.Task;

public interface TaskManagerInterface {

    void addTask(Task task) throws ValidationException;

    void updateTask(final Task task) throws ValidationException;

    void removeTask(final int id);

    List<Task> listAllTasks(boolean priorityOrDate);

    List<Task> listTasksWithAlert() throws ModelException;

    void markAsCompleted(final int id, final boolean completed);
}