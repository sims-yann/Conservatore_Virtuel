package I2new.group2.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Business rules and OOP behaviour of the model, without database. */
class BusinessRulesTest {

    private Instrument piano;
    private Teacher teacher;
    private Room studio;
    private Student alice;
    private Student bob;

    @BeforeEach
    void setUp() {
        piano = new Instrument(1, "Piano", "Keyboard", true);
        teacher = new Teacher(1, "Claire", "Dubois", "claire@school.fr", null, 40);
        teacher.addSpecialization(piano);
        teacher.addAvailability(new Teacher.Availability(DayOfWeek.MONDAY, LocalTime.of(9, 0), LocalTime.of(18, 0)));
        studio = new Room(1, "Studio", 2);
        alice = new Student(1, "Alice", "Martin", "alice@mail.com", null, null, null, StudentLevel.BEGINNER);
        bob = new Student(2, "Bob", "Durand", "bob@mail.com", null, null, null, StudentLevel.ADVANCED);
    }

    private CoursePackage tenHourPackage(LocalDate start, LocalDate end) {
        return new CoursePackage(1, "Pack 10h", 250, 10, start, end, PackageType.STANDARD);
    }

    // ----- rule 1: package hours expire at the package end date

    @Test
    void expiredPackageHoursCannotBeUsed() {
        LocalDate today = LocalDate.now();
        Enrollment enrollment = new Enrollment(1, tenHourPackage(today.minusDays(60), today.minusDays(1)), today.minusDays(60));
        assertTrue(enrollment.getCoursePackage().isExpired());
        assertFalse(enrollment.isValid());
        assertThrows(IllegalStateException.class, () -> enrollment.consumeHours(1));
        assertEquals(10, enrollment.getRemainingHours());
    }

    @Test
    void consumingHoursOnValidPackage() {
        LocalDate today = LocalDate.now();
        Enrollment enrollment = new Enrollment(1, tenHourPackage(today.minusDays(1), today.plusDays(30)), today);
        enrollment.consumeHours(1.5);
        assertEquals(8.5, enrollment.getRemainingHours(), 1e-9);
        assertThrows(IllegalStateException.class, () -> enrollment.consumeHours(9));
    }

    // ----- rule 2: cancellation less than 24h before counts as consumed

    @Test
    void lateCancellationIsDetected() {
        LocalDateTime start = LocalDateTime.now().plusHours(10);
        Lesson lesson = new Lesson(1, start, 1, ActivityStatus.SCHEDULED, LessonType.INDIVIDUAL, teacher, studio, piano);
        lesson.cancel(LocalDateTime.now());
        assertEquals(ActivityStatus.CANCELLED, lesson.getStatus());
        assertTrue(lesson.wasLateCancellation());
    }

    @Test
    void earlyCancellationIsFree() {
        LocalDateTime start = LocalDateTime.now().plusDays(3);
        Lesson lesson = new Lesson(1, start, 1, ActivityStatus.SCHEDULED, LessonType.INDIVIDUAL, teacher, studio, piano);
        lesson.cancel(LocalDateTime.now());
        assertFalse(lesson.wasLateCancellation());
        assertThrows(IllegalStateException.class, lesson::cancel, "cannot cancel twice");
    }

    // ----- rule 3: exam registration closes at capacity (activity diagram order)

    @Test
    void examRegistrationClosesWhenFull() {
        Exam exam = new Exam(1, LocalDateTime.now().plusDays(10), 2, ActivityStatus.SCHEDULED, "Piano L1", 1, piano);
        assertTrue(exam.registerStudent(alice));
        assertTrue(exam.isFull());
        IllegalStateException e = assertThrows(IllegalStateException.class, () -> exam.registerStudent(bob));
        assertEquals("Exam registration is closed", e.getMessage());
    }

    @Test
    void duplicateExamRegistrationIsRefused() {
        Exam exam = new Exam(1, LocalDateTime.now().plusDays(10), 2, ActivityStatus.SCHEDULED, "Piano L1", 5, piano);
        exam.registerStudent(alice);
        IllegalStateException e = assertThrows(IllegalStateException.class, () -> exam.registerStudent(alice));
        assertEquals("Student is already registered", e.getMessage());
    }

    @Test
    void examScoreMustBeBetween0And100() {
        assertThrows(IllegalArgumentException.class, () -> new ExamResult(alice, true, 120.0));
        assertEquals(null, new ExamResult(alice, false, null).getScore());
    }

    // ----- rule 4: a group lesson consumes the duration from each participant

    @Test
    void groupLessonConsumesHoursFromEachParticipant() {
        Lesson lesson = new Lesson(1, LocalDateTime.now().plusDays(1), 1, ActivityStatus.SCHEDULED,
                LessonType.GROUP, teacher, studio, piano);
        lesson.addStudent(alice);
        lesson.addStudent(bob);
        assertEquals(1.0, lesson.calculateConsumedHours());
        assertThrows(IllegalStateException.class, () -> lesson.addStudent(
                new Student(3, "Carl", "Petit", "carl@mail.com", null, null, null, StudentLevel.BEGINNER)),
                "room capacity is 2");
    }

    @Test
    void individualLessonHasOneStudent() {
        Lesson lesson = new Lesson(1, LocalDateTime.now().plusDays(1), 1, ActivityStatus.SCHEDULED,
                LessonType.INDIVIDUAL, teacher, studio, piano);
        lesson.addStudent(alice);
        assertThrows(IllegalStateException.class, () -> lesson.addStudent(bob));
    }

    // ----- scheduling helpers

    @Test
    void teacherAvailabilityAndOverlap() {
        LocalDateTime nextMonday = LocalDate.now().plusWeeks(1).with(DayOfWeek.MONDAY).atTime(10, 0);
        assertTrue(teacher.isAvailable(nextMonday, 2));
        assertFalse(teacher.isAvailable(nextMonday.withHour(17), 2), "ends after 18:00");
        assertFalse(teacher.isAvailable(nextMonday.plusDays(1), 1), "not available on Tuesday");

        Lesson lesson = new Lesson(1, nextMonday, 1, ActivityStatus.SCHEDULED, LessonType.INDIVIDUAL, teacher, studio, piano);
        assertTrue(lesson.overlaps(nextMonday.plusMinutes(30), 1));
        assertFalse(lesson.overlaps(nextMonday.plusHours(1), 1), "back-to-back is allowed");
    }

    // ----- billing and polymorphism

    @Test
    void invoiceTotalUsesPolymorphicBillables() {
        Lesson lesson = new Lesson(9, LocalDateTime.now().plusDays(1), 1.5, ActivityStatus.SCHEDULED,
                LessonType.INDIVIDUAL, teacher, studio, piano);
        Rental rental = new Rental(5, "Piano rental", 30, RentalType.INSTRUMENT, piano);
        Invoice invoice = new Invoice(1, alice.getId(), LocalDate.now());
        for (Billable item : List.<Billable>of(tenHourPackage(LocalDate.now(), LocalDate.now().plusDays(90)), rental, lesson)) {
            invoice.addItem(item);
        }
        assertEquals(250 + 30 + 60, invoice.calculateTotal(), 1e-9);

        invoice.addPayment(new Payment(0, 100, LocalDate.now()));
        assertEquals(240, invoice.getOutstandingBalance(), 1e-9);
        assertThrows(IllegalArgumentException.class, () -> invoice.addPayment(new Payment(0, 500, LocalDate.now())));
    }

    @Test
    void polymorphicScheduleDescribesEachActivity() {
        List<ScheduledActivity> schedule = List.of(
                new Lesson(1, LocalDateTime.now().plusDays(1), 1, ActivityStatus.SCHEDULED, LessonType.INDIVIDUAL, teacher, studio, piano),
                new RoomBooking(2, LocalDateTime.now().plusDays(2), 2, ActivityStatus.SCHEDULED, studio),
                new Exam(3, LocalDateTime.now().plusDays(3), 3, ActivityStatus.SCHEDULED, "Piano L1", 5, piano));
        assertEquals(List.of("Lesson", "Room booking", "Exam"),
                schedule.stream().map(ScheduledActivity::getActivityType).toList());
    }

    // ----- copy constructors protect internal state

    @Test
    void copyConstructorsAreIndependent() {
        LocalDate today = LocalDate.now();
        Enrollment original = new Enrollment(1, tenHourPackage(today, today.plusDays(30)), today);
        alice.enroll(original);

        Student copy = new Student(alice);
        copy.getEnrollments().getFirst().consumeHours(3);
        assertEquals(10, alice.getEnrollments().getFirst().getRemainingHours());
        assertEquals(7, copy.getEnrollments().getFirst().getRemainingHours());
        assertNotSame(alice.getEnrollments().getFirst(), copy.getEnrollments().getFirst());

        Teacher teacherCopy = new Teacher(teacher);
        teacherCopy.setHourlyRate(99);
        assertEquals(40, teacher.getHourlyRate());

        Exam exam = new Exam(1, LocalDateTime.now().plusDays(10), 2, ActivityStatus.SCHEDULED, "Piano L1", 5, piano);
        exam.registerStudent(alice);
        Exam examCopy = new Exam(exam);
        examCopy.registerStudent(bob);
        assertEquals(1, exam.getRegisteredStudents().size());
        assertEquals(2, examCopy.getRegisteredStudents().size());
    }
}
