# Conservatoire Virtuel

Information system for **Conservatoire Virtuel**, a private music school. This is a Java 25 desktop application backed by a MySQL / MariaDB database through plain JDBC. Its graphical interface is built with Swing.

## Project context

The school needs one application for its day-to-day activities. The full specification is in `Conservatoire-Virtuel-Project.md` / `.pdf`. The application manages:

- **Students and teachers**:
  - Students have a level and preferred instruments.
  - Teachers have specializations, an hourly rate, qualifications and weekly availability.
- **Course packages and services**:
  - Packages of N hours or unlimited packages, each with validity dates.
  - Single paid lessons, instrument rentals and room rentals.
  - Purchased hours, remaining hours and usage history are tracked.
- **Scheduling**:
  - Individual or group lessons (teacher + students + room + instrument), room bookings and exam sessions.
  - Each activity has a status: scheduled, completed or cancelled.
  - Specialization, availability and schedule conflicts are checked.
- **Exams**: name, instrument, date and capacity, with student registration and results (pass/fail plus an optional score).
- **Payments and billing**:
  - Invoices, partial payments and outstanding balances.
  - Total spent per student, summary per month and summary per student.

### Business rules
1. Package hours expire at the package end date.
2. A lesson cancelled less than 24 h before it starts counts as consumed. Cancelled earlier, its hours are given back and unpaid charges for it are removed.
3. Exam registration closes when the maximum capacity is reached. Duplicate registrations are refused.
4. A group lesson consumes its duration from each participant's package.

### Design (UML folder)
- `UML/basic class diagram.puml`: the domain model implemented in `model/` and in the database schema.
- `UML/SeqDiagram.puml`: the lesson-scheduling flow implemented in `SchedulingService.scheduleLesson`.
- `UML/ExamRegistration.puml`: the exam registration flow implemented in `ExamService` / `Exam.registerStudent`.

### Object-oriented requirements
| Requirement | Where |
|---|---|
| Abstract classes | `Person`, `Service`, `ScheduledActivity`, `ui.Menu` |
| Interfaces | `Billable` (services and lessons), `Schedulable` (teachers and rooms) |
| Polymorphic collections | `List<ScheduledActivity>` (schedule), `List<Billable>` (invoice items), `List<Person>` (directory), `List<Menu>` (main menu) |
| Overriding / dynamic dispatch | `calculatePrice()`, `describe()`, `getActivityType()`, `Lesson.cancel(...)` (24 h rule), `Menu.handle(...)` |
| Copy constructors | `Student`, `Teacher`, `CoursePackage`, `Rental`, `Enrollment`, `Lesson`, `Exam`, `Invoice`, ... |
| Encapsulation / composition | private validated fields, unmodifiable lists; `Invoice` owns its `Payment`s, `Exam` owns its `ExamResult`s |

## Prerequisites

- **JDK 25**
- **Maven 3.9+**. The version bundled with IntelliJ is fine.
- **MySQL 8+ or MariaDB 10.4+**, running locally (default port 3306).
- Optional: IntelliJ IDEA, and MySQL Shell / Workbench for the empty-database setup.

## Running the project

### 1. Configure the database connection
Copy the example file and set your credentials:

```powershell
copy src\main\resources\db.properties.example src\main\resources\db.properties
```

```properties
db.url=jdbc:mysql://localhost:3306/conservatoire?createDatabaseIfNotExist=true
db.user=root
db.password=your-password
```

`db.properties` is git-ignored, so everyone keeps their own password locally. The `conservatoire` database is created automatically if it does not exist.

### 2a. Run the graphical interface (default)
Open the project in IntelliJ (JDK 25) and run `I2new.group2.Main`.

On the first run, the app creates the tables (`schema.sql`) and loads the demo scenario (`data.sql`). The main menu option **Reset demo data** reloads them at any time and deletes all changes.

You can also run it from the command line once the dependencies are downloaded:

```powershell
mvn compile exec:java
```

The graphical interface provides views for students, teachers, the schedule, exams, invoices, services and instruments. It supports lesson scheduling, exam creation and registration, activity status changes, invoice payments, purchases, and availability updates.

### 2b. Run the console interface
The original console menus are still available by passing `--console`:

```powershell
mvn compile exec:java "-Dexec.args=--console"
```

### 2c. Run from scratch, without any data
The app loads the demo data only when the tables do not exist yet. To start empty, create the tables yourself before the first run:

```powershell
mysqlsh -u root -p --sql -e "DROP DATABASE IF EXISTS conservatoire; CREATE DATABASE conservatoire;"
mysqlsh -u root -p --sql -D conservatoire --file src/main/resources/schema.sql
```

The first command deletes the existing `conservatoire` database. You can also run `schema.sql` from MySQL Workbench or DataGrip on an empty `conservatoire` database. Then run `Main`: every list starts empty. Don't use **Reset demo data**, because it reloads the demo scenario.

Some records depend on others, so enter data in this order:

1. **b)** Instruments and rooms.
2. **a)** Teachers, with their specializations and weekly availability. A teacher without availability can never be scheduled. Then students, with their preferred instruments.
3. **b)** Course packages and rentals, then sell packages to students.
4. **c)** Schedule lessons and book rooms.
5. **e)** Create exams and register students.
6. **d)** Record payments and view the reports.

Note: specializations, availability and preferred instruments can only be set when the person is created.

### 3. Run the unit tests
The tests cover the business rules and copy constructors, and need no database:

```powershell
mvn test
```

## Console menu

```
=== CONSERVATOIRE VIRTUEL ===
 1. a) Students and teachers              list / details / add / update / delete, school directory
 2. b) Course packages, lessons and services   packages, rentals, sales, remaining hours, instruments, rooms
 3. c) Scheduling and booking             schedule, lessons, room bookings, cancel / complete
 4. d) Payments and billing               invoices, payments, balances, monthly and per-student summaries
 5. e) Exams and results                  exams, registration, results
 6. Reset demo data
 0. Quit
```

Dates are typed as `yyyy-MM-dd` and date-times as `yyyy-MM-dd HH:mm`.

## Demo scenario highlights (`data.sql`)
Dates are relative to the day the data is loaded, so the scenario always works.

- **Lesson #3** starts within about 3 hours. Cancelling it is a late cancellation, so the hours stay consumed.
- **Lesson #2** is a group lesson in 3 days. Cancelling it gives the hour back to each participant.
- **Students:**
  - Chloe's package is expired, so her 6 remaining hours cannot be used.
  - Nathan has no package and pays per lesson.
  - Hugo has an unlimited package.
- **Teachers and instruments:**
  - Antoine does not teach piano (try it to see the specialization error).
  - Sophie only teaches on weekday afternoons.
  - The drums are out of service.
- **Exams:** "Piano Exam - Level 1" has 1 seat left. "Guitar Exam - Level 2" already has results.

## Project structure

```
src/main/java/I2new/group2/
├── Main.java        wiring of DAOs, services and user interface
├── model/           domain classes, Billable / Schedulable interfaces, enums
├── dao/             JDBC access (one DAO per aggregate), DatabaseConnection, DatabaseInitializer
├── service/         business rules - reusable by the future GUI
├── exception/       InvalidSpecializationException, ScheduleConflictException
└── ui/              Swing desktop interface and console menus a-e
src/main/resources/
├── schema.sql       tables (one per class / association)
├── data.sql         demo scenario
└── db.properties.example
src/test/java/       JUnit 5 tests of the business rules
UML/                 PlantUML diagrams
```

## Troubleshooting

| Problem | Fix |
|---|---|
| `Missing db.properties` | Do step 1. |
| `Access denied for user` | Wrong user or password in `db.properties`. |
| `Communications link failure` | MySQL is not running, or it uses a different port in `db.url`. |
| `No suitable driver found` when running `java` by hand | The MySQL connector jar is not on the classpath. Run from IntelliJ or with `mvn exec:java`. In Git Bash, use PowerShell instead because Git Bash breaks `;` classpaths. |
| Demo data reappears | It is loaded only when the tables are missing, or through **Reset demo data**. For an empty start, follow step 2b. |
