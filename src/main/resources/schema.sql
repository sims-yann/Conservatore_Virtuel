-- =====================================================================
-- Conservatoire Virtuel - MySQL schema
-- One table per class of "UML/basic class diagram.puml", plus one
-- table per many-to-many association.
-- Inheritance (Service, ScheduledActivity) is mapped "joined":
-- a base table + one 1:1 table per concrete subclass.
-- NOTE: no ';' inside string literals (the script is split on ';').
-- =====================================================================

SET FOREIGN_KEY_CHECKS = 0;
DROP TABLE IF EXISTS payment, invoice_item, invoice,
    exam_result, exam_registration,
    enrollment_lesson, lesson_participant,
    exam, room_booking, lesson, scheduled_activity,
    enrollment, rental, course_package, service,
    student_instrument, teacher_instrument, teacher_availability, teacher_qualification,
    room, instrument, teacher, student;
SET FOREIGN_KEY_CHECKS = 1;

-- ===== Instruments and Rooms =====

CREATE TABLE instrument (
    id        INT AUTO_INCREMENT PRIMARY KEY,
    name      VARCHAR(50)  NOT NULL UNIQUE,
    type      VARCHAR(50)  NOT NULL,
    available BOOLEAN      NOT NULL DEFAULT TRUE
);

CREATE TABLE room (
    id       INT AUTO_INCREMENT PRIMARY KEY,
    name     VARCHAR(50) NOT NULL UNIQUE,
    capacity INT         NOT NULL,
    CONSTRAINT chk_room_capacity CHECK (capacity > 0)
);

-- ===== People (Person is abstract: its attributes are repeated in each concrete table) =====

CREATE TABLE student (
    id         INT AUTO_INCREMENT PRIMARY KEY,
    first_name VARCHAR(50)  NOT NULL,
    last_name  VARCHAR(50)  NOT NULL,
    email      VARCHAR(100) NOT NULL UNIQUE,
    phone      VARCHAR(20),
    address    VARCHAR(200),
    birth_date DATE,
    level      ENUM('BEGINNER','INTERMEDIATE','ADVANCED') NOT NULL DEFAULT 'BEGINNER'
);

CREATE TABLE teacher (
    id          INT AUTO_INCREMENT PRIMARY KEY,
    first_name  VARCHAR(50)  NOT NULL,
    last_name   VARCHAR(50)  NOT NULL,
    email       VARCHAR(100) NOT NULL UNIQUE,
    phone       VARCHAR(20),
    hourly_rate DECIMAL(8,2) NOT NULL,
    CONSTRAINT chk_teacher_rate CHECK (hourly_rate >= 0)
);

-- Teacher.qualifications : List<String>
CREATE TABLE teacher_qualification (
    teacher_id    INT          NOT NULL,
    qualification VARCHAR(100) NOT NULL,
    PRIMARY KEY (teacher_id, qualification),
    FOREIGN KEY (teacher_id) REFERENCES teacher(id) ON DELETE CASCADE
);

-- Weekly availability used by Teacher.checkAvailability()
CREATE TABLE teacher_availability (
    id          INT AUTO_INCREMENT PRIMARY KEY,
    teacher_id  INT  NOT NULL,
    day_of_week ENUM('MONDAY','TUESDAY','WEDNESDAY','THURSDAY','FRIDAY','SATURDAY','SUNDAY') NOT NULL,
    start_time  TIME NOT NULL,
    end_time    TIME NOT NULL,
    FOREIGN KEY (teacher_id) REFERENCES teacher(id) ON DELETE CASCADE,
    CONSTRAINT chk_availability_range CHECK (start_time < end_time)
);

-- Teacher "0..*" -- "0..*" Instrument : specializes in
CREATE TABLE teacher_instrument (
    teacher_id    INT NOT NULL,
    instrument_id INT NOT NULL,
    PRIMARY KEY (teacher_id, instrument_id),
    FOREIGN KEY (teacher_id)    REFERENCES teacher(id)    ON DELETE CASCADE,
    FOREIGN KEY (instrument_id) REFERENCES instrument(id)
);

-- Student "0..*" -- "0..*" Instrument : prefers
CREATE TABLE student_instrument (
    student_id    INT NOT NULL,
    instrument_id INT NOT NULL,
    PRIMARY KEY (student_id, instrument_id),
    FOREIGN KEY (student_id)    REFERENCES student(id)    ON DELETE CASCADE,
    FOREIGN KEY (instrument_id) REFERENCES instrument(id)
);

-- ===== Services and Packages =====

-- abstract class Service
CREATE TABLE service (
    id           INT AUTO_INCREMENT PRIMARY KEY,
    name         VARCHAR(100)  NOT NULL,
    price        DECIMAL(10,2) NOT NULL,
    service_type ENUM('COURSE_PACKAGE','RENTAL') NOT NULL,
    CONSTRAINT chk_service_price CHECK (price >= 0)
);

-- class CoursePackage extends Service
CREATE TABLE course_package (
    service_id  INT  PRIMARY KEY,
    total_hours INT  NOT NULL,
    start_date  DATE NOT NULL,
    end_date    DATE NOT NULL,
    type        ENUM('STANDARD','UNLIMITED') NOT NULL,
    FOREIGN KEY (service_id) REFERENCES service(id) ON DELETE CASCADE,
    CONSTRAINT chk_package_dates CHECK (end_date >= start_date),
    CONSTRAINT chk_package_hours CHECK (total_hours >= 0)
);

-- class Rental extends Service  (Instrument "1" -- "0..*" Rental : rented through)
CREATE TABLE rental (
    service_id    INT PRIMARY KEY,
    rental_type   ENUM('INSTRUMENT','ROOM') NOT NULL,
    instrument_id INT NULL,
    FOREIGN KEY (service_id)    REFERENCES service(id) ON DELETE CASCADE,
    FOREIGN KEY (instrument_id) REFERENCES instrument(id)
);

-- ===== Enrollment =====

-- Student "1" -- "0..*" Enrollment : owns
-- Enrollment "*" --> "1" CoursePackage : based on
CREATE TABLE enrollment (
    id              INT AUTO_INCREMENT PRIMARY KEY,
    student_id      INT          NOT NULL,
    package_id      INT          NOT NULL,
    purchase_date   DATE         NOT NULL,
    remaining_hours DECIMAL(6,2) NOT NULL,
    FOREIGN KEY (student_id) REFERENCES student(id),
    FOREIGN KEY (package_id) REFERENCES course_package(service_id),
    CONSTRAINT chk_remaining_hours CHECK (remaining_hours >= 0)
);

-- ===== Scheduling =====

-- abstract class ScheduledActivity
CREATE TABLE scheduled_activity (
    id            INT AUTO_INCREMENT PRIMARY KEY,
    date_time     DATETIME     NOT NULL,
    duration      DECIMAL(4,2) NOT NULL,
    status        ENUM('SCHEDULED','COMPLETED','CANCELLED') NOT NULL DEFAULT 'SCHEDULED',
    activity_type ENUM('LESSON','ROOM_BOOKING','EXAM') NOT NULL,
    INDEX idx_activity_time (date_time),
    CONSTRAINT chk_activity_duration CHECK (duration > 0)
);

-- class Lesson extends ScheduledActivity
-- Teacher "1" -- "0..*" Lesson, Lesson "*" --> "1" Room, Lesson "*" --> "1" Instrument
CREATE TABLE lesson (
    activity_id   INT PRIMARY KEY,
    type          ENUM('INDIVIDUAL','GROUP') NOT NULL,
    teacher_id    INT NOT NULL,
    room_id       INT NOT NULL,
    instrument_id INT NOT NULL,
    FOREIGN KEY (activity_id)   REFERENCES scheduled_activity(id) ON DELETE CASCADE,
    FOREIGN KEY (teacher_id)    REFERENCES teacher(id),
    FOREIGN KEY (room_id)       REFERENCES room(id),
    FOREIGN KEY (instrument_id) REFERENCES instrument(id),
    INDEX idx_lesson_teacher (teacher_id),
    INDEX idx_lesson_room (room_id)
);

-- class RoomBooking extends ScheduledActivity  (RoomBooking "*" --> "1" Room : reserves)
CREATE TABLE room_booking (
    activity_id INT PRIMARY KEY,
    room_id     INT NOT NULL,
    FOREIGN KEY (activity_id) REFERENCES scheduled_activity(id) ON DELETE CASCADE,
    FOREIGN KEY (room_id)     REFERENCES room(id),
    INDEX idx_booking_room (room_id)
);

-- class Exam extends ScheduledActivity  (Instrument "1" -- "0..*" Exam : concerns)
CREATE TABLE exam (
    activity_id   INT PRIMARY KEY,
    name          VARCHAR(100) NOT NULL,
    capacity      INT          NOT NULL,
    instrument_id INT          NOT NULL,
    FOREIGN KEY (activity_id)   REFERENCES scheduled_activity(id) ON DELETE CASCADE,
    FOREIGN KEY (instrument_id) REFERENCES instrument(id),
    CONSTRAINT chk_exam_capacity CHECK (capacity > 0)
);

-- Lesson "0..*" -- "1..*" Student : participants
CREATE TABLE lesson_participant (
    lesson_id  INT NOT NULL,
    student_id INT NOT NULL,
    PRIMARY KEY (lesson_id, student_id),
    FOREIGN KEY (lesson_id)  REFERENCES lesson(activity_id) ON DELETE CASCADE,
    FOREIGN KEY (student_id) REFERENCES student(id)
);

-- Enrollment "0..*" -- "0..*" Lesson : hours consumed  (also the student's usage history)
CREATE TABLE enrollment_lesson (
    enrollment_id  INT          NOT NULL,
    lesson_id      INT          NOT NULL,
    hours_consumed DECIMAL(4,2) NOT NULL,
    consumed_at    DATETIME     NOT NULL,
    PRIMARY KEY (enrollment_id, lesson_id),
    FOREIGN KEY (enrollment_id) REFERENCES enrollment(id),
    FOREIGN KEY (lesson_id)     REFERENCES lesson(activity_id)
);

-- ===== Exams =====

-- Student "0..*" -- "0..*" Exam : registers for
CREATE TABLE exam_registration (
    exam_id       INT      NOT NULL,
    student_id    INT      NOT NULL,
    registered_at DATETIME NOT NULL,
    PRIMARY KEY (exam_id, student_id),
    FOREIGN KEY (exam_id)    REFERENCES exam(activity_id) ON DELETE CASCADE,
    FOREIGN KEY (student_id) REFERENCES student(id)
);

-- Exam "1" *-- "0..*" ExamResult, ExamResult "*" --> "1" Student : belongs to
CREATE TABLE exam_result (
    exam_id    INT          NOT NULL,
    student_id INT          NOT NULL,
    passed     BOOLEAN      NOT NULL,
    score      DECIMAL(5,2) NULL,
    PRIMARY KEY (exam_id, student_id),
    FOREIGN KEY (exam_id, student_id) REFERENCES exam_registration(exam_id, student_id) ON DELETE CASCADE,
    CONSTRAINT chk_exam_score CHECK (score IS NULL OR (score >= 0 AND score <= 100))
);

-- ===== Billing =====

-- Student "1" -- "0..*" Invoice : receives
CREATE TABLE invoice (
    id           INT AUTO_INCREMENT PRIMARY KEY,
    student_id   INT           NOT NULL,
    date         DATE          NOT NULL,
    total_amount DECIMAL(10,2) NOT NULL DEFAULT 0,
    FOREIGN KEY (student_id) REFERENCES student(id)
);

-- Invoice "0..*" --> "1..*" Billable : charges for
-- A Billable is either a Service (package / rental) or a Lesson: exactly one column is set.
CREATE TABLE invoice_item (
    id         INT AUTO_INCREMENT PRIMARY KEY,
    invoice_id INT  NOT NULL,
    service_id INT  NULL,
    lesson_id  INT  NULL,
    FOREIGN KEY (invoice_id) REFERENCES invoice(id) ON DELETE CASCADE,
    FOREIGN KEY (service_id) REFERENCES service(id),
    FOREIGN KEY (lesson_id)  REFERENCES lesson(activity_id),
    CONSTRAINT chk_item_one_billable CHECK ((service_id IS NULL) <> (lesson_id IS NULL))
);

-- Invoice "1" *-- "0..*" Payment
CREATE TABLE payment (
    id           INT AUTO_INCREMENT PRIMARY KEY,
    invoice_id   INT           NOT NULL,
    amount       DECIMAL(10,2) NOT NULL,
    payment_date DATE          NOT NULL,
    FOREIGN KEY (invoice_id) REFERENCES invoice(id) ON DELETE CASCADE,
    CONSTRAINT chk_payment_amount CHECK (amount > 0)
);
