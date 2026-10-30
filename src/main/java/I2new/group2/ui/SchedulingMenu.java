package I2new.group2.ui;

import I2new.group2.model.LessonType;
import I2new.group2.model.RoomBooking;
import I2new.group2.model.ScheduledActivity;
import I2new.group2.model.Student;
import I2new.group2.service.PeopleService;
import I2new.group2.service.SchedulingService;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;

/** c. Manage scheduling and booking. */
public class SchedulingMenu extends Menu {

    private final SchedulingService scheduling;
    private final PeopleService people;

    public SchedulingMenu(ConsoleInput in, SchedulingService scheduling, PeopleService people) {
        super(in);
        this.scheduling = scheduling;
        this.people = people;
    }

    @Override
    public String getTitle() {
        return "Scheduling and booking";
    }

    @Override
    protected List<String> getOptions() {
        return List.of("Full schedule (lessons, bookings, exams)", "Next 7 days", "Schedule a lesson",
                "Book a room", "Cancel an activity", "Mark an activity as completed");
    }

    @Override
    protected void handle(int choice) throws SQLException {
        switch (choice) {
            case 1 -> printSchedule(scheduling.getSchedule());
            case 2 -> printSchedule(scheduling.getUpcoming(7));
            case 3 -> scheduleLesson();
            case 4 -> {
                printAll(scheduling.listRooms().stream().map(r -> "#" + r.getId() + " " + r).toList());
                RoomBooking booking = scheduling.bookRoom(in.readInt("Room id: "),
                        in.readDateTime("Start "), in.readPositiveDouble("Duration in hours: "));
                System.out.println("Booked: " + booking);
            }
            case 5 -> {
                int id = in.readInt("Activity id: ");
                System.out.println(scheduling.cancelActivity(id, LocalDateTime.now()));
            }
            case 6 -> {
                scheduling.completeActivity(in.readInt("Activity id: "));
                System.out.println("Marked as completed.");
            }
            default -> { }
        }
    }

    /** Polymorphic collection: each activity prints its own type and details. */
    private void printSchedule(List<ScheduledActivity> activities) {
        printAll(activities);
    }

    private void scheduleLesson() throws SQLException {
        people.listTeachers().forEach(t -> System.out.println("  " + t.describe()));
        int teacherId = in.readInt("Teacher id: ");
        scheduling.listInstruments().forEach(i -> System.out.println("  " + i.getId() + ". " + i.getName()
                + (i.isAvailable() ? "" : " (out of service)")));
        int instrumentId = in.readInt("Instrument id: ");
        LessonType type = in.readEnum("Lesson type:", LessonType.class);
        for (Student s : people.listStudents()) {
            System.out.println("  " + s.describe());
        }
        List<Integer> studentIds = type == LessonType.INDIVIDUAL
                ? List.of(in.readInt("Student id: "))
                : in.readIdList("Student ids (comma separated): ");
        scheduling.listRooms().forEach(r -> System.out.println("  #" + r.getId() + " " + r));
        int roomId = in.readInt("Room id: ");
        LocalDateTime start = in.readDateTime("Start ");
        double duration = in.readPositiveDouble("Duration in hours: ");

        SchedulingService.SchedulingOutcome outcome =
                scheduling.scheduleLesson(teacherId, roomId, instrumentId, studentIds, type, start, duration);
        System.out.println("Lesson confirmed: " + outcome.lesson());
        outcome.notes().forEach(n -> System.out.println("  - " + n));
    }
}
