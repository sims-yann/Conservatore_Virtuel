package I2new.group2.ui;

import I2new.group2.exception.InvalidSpecializationException;
import I2new.group2.exception.ScheduleConflictException;

import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.util.Collection;
import java.util.List;

/**
 * A text menu: subclasses provide the title, the options and what each option does.
 * The loop and error handling are shared (template method).
 */
public abstract class Menu {

    protected final ConsoleInput in;

    protected Menu(ConsoleInput in) {
        this.in = in;
    }

    public abstract String getTitle();

    protected abstract List<String> getOptions();

    /** @param choice 1-based index in {@link #getOptions()} */
    protected abstract void handle(int choice) throws SQLException;

    /** Label of option 0. */
    protected String getExitLabel() {
        return "Back";
    }

    public void run() {
        while (true) {
            System.out.println();
            System.out.println("=== " + getTitle() + " ===");
            List<String> options = getOptions();
            for (int i = 0; i < options.size(); i++) {
                System.out.printf("%2d. %s%n", i + 1, options.get(i));
            }
            System.out.printf("%2d. %s%n", 0, getExitLabel());
            int choice = in.readInt("> ", 0, options.size());
            if (choice == 0) {
                return;
            }
            System.out.println();
            try {
                handle(choice);
            } catch (InvalidSpecializationException e) {
                System.out.println("[Invalid specialization] " + e.getMessage());
            } catch (ScheduleConflictException e) {
                System.out.println("[Schedule conflict] " + e.getMessage());
            } catch (SQLIntegrityConstraintViolationException e) {
                System.out.println("[Refused by the database] " + e.getMessage());
            } catch (SQLException e) {
                System.out.println("[Database error] " + e.getMessage());
            } catch (IllegalArgumentException | IllegalStateException e) {
                System.out.println("[Error] " + e.getMessage());
            }
        }
    }

    protected static void printAll(Collection<?> items) {
        if (items.isEmpty()) {
            System.out.println("  (none)");
        }
        for (Object item : items) {
            System.out.println("  " + item);
        }
    }
}
