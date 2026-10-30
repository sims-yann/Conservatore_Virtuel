package I2new.group2.ui;

import I2new.group2.model.CoursePackage;
import I2new.group2.model.Enrollment;
import I2new.group2.model.Instrument;
import I2new.group2.model.Invoice;
import I2new.group2.model.PackageType;
import I2new.group2.model.Rental;
import I2new.group2.model.RentalType;
import I2new.group2.model.Room;
import I2new.group2.model.Service;
import I2new.group2.service.EnrollmentService;
import I2new.group2.service.SchedulingService;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

/** b. Manage course packages and individual lessons (services, purchases, instruments, rooms). */
public class ServicesMenu extends Menu {

    private final EnrollmentService enrollments;
    private final SchedulingService scheduling;

    public ServicesMenu(ConsoleInput in, EnrollmentService enrollments, SchedulingService scheduling) {
        super(in);
        this.enrollments = enrollments;
        this.scheduling = scheduling;
    }

    @Override
    public String getTitle() {
        return "Course packages, lessons and services";
    }

    @Override
    protected List<String> getOptions() {
        return List.of("List services and prices", "Add course package", "Add rental service",
                "Sell a course package to a student", "Rent an instrument / room to a student",
                "Student packages and remaining hours", "List instruments", "Add instrument",
                "Set instrument in / out of service", "List rooms", "Add room");
    }

    @Override
    protected void handle(int choice) throws SQLException {
        switch (choice) {
            case 1 -> listServices();
            case 2 -> addPackage();
            case 3 -> addRental();
            case 4 -> {
                listServices();
                Invoice invoice = enrollments.purchasePackage(in.readInt("Student id: "), in.readInt("Package id: "));
                System.out.println("Package sold. " + invoice);
            }
            case 5 -> {
                listServices();
                Invoice invoice = enrollments.rent(in.readInt("Student id: "), in.readInt("Rental service id: "));
                System.out.println("Rental billed. " + invoice);
            }
            case 6 -> {
                int studentId = in.readInt("Student id: ");
                List<Enrollment> list = enrollments.getEnrollments(studentId);
                printAll(list);
                double hours = list.stream().filter(e -> e.isValid() && !e.isUnlimited())
                        .mapToDouble(Enrollment::getRemainingHours).sum();
                System.out.printf("Usable hours today: %.1fh%s%n", hours,
                        list.stream().anyMatch(e -> e.isValid() && e.isUnlimited()) ? " + unlimited package" : "");
            }
            case 7 -> printAll(scheduling.listInstruments().stream()
                    .map(i -> String.format("#%-3d %-10s %-12s %s", i.getId(), i.getName(), i.getType(),
                            i.isAvailable() ? "available" : "OUT OF SERVICE"))
                    .toList());
            case 8 -> {
                Instrument instrument = new Instrument(0, in.readText("Name: "), in.readText("Type (String, Wind...): "), true);
                scheduling.addInstrument(instrument);
                System.out.println("Created instrument #" + instrument.getId());
            }
            case 9 -> {
                int id = in.readInt("Instrument id: ");
                scheduling.setInstrumentAvailable(id, in.readYesNo("Available for lessons?"));
                System.out.println("Updated.");
            }
            case 10 -> printAll(scheduling.listRooms().stream().map(r -> "#" + r.getId() + " " + r).toList());
            case 11 -> {
                Room room = new Room(0, in.readText("Name: "), in.readInt("Capacity: ", 1, 1000));
                scheduling.addRoom(room);
                System.out.println("Created room #" + room.getId());
            }
            default -> { }
        }
    }

    private void listServices() throws SQLException {
        // polymorphic collection: describe() / calculatePrice() of CoursePackage or Rental
        List<Service> services = enrollments.listServices();
        printAll(services);
        System.out.println("  Single lessons without package are billed at the teacher's hourly rate x duration.");
    }

    private void addPackage() throws SQLException {
        String name = in.readText("Name: ");
        double price = in.readDouble("Price (EUR): ");
        PackageType type = in.readEnum("Type:", PackageType.class);
        int hours = type == PackageType.UNLIMITED ? 0 : in.readInt("Number of hours: ", 1, 1000);
        LocalDate start = in.readOptionalDate("Valid from ", LocalDate.now());
        LocalDate end = in.readDate("Valid until ");
        CoursePackage coursePackage = new CoursePackage(0, name, price, hours, start, end, type);
        enrollments.addService(coursePackage);
        System.out.println("Created " + coursePackage);
    }

    private void addRental() throws SQLException {
        String name = in.readText("Name: ");
        double price = in.readDouble("Price (EUR): ");
        RentalType type = in.readEnum("Rental of:", RentalType.class);
        Instrument instrument = null;
        if (type == RentalType.INSTRUMENT) {
            List<Instrument> instruments = scheduling.listInstruments();
            instruments.forEach(i -> System.out.println("  " + i.getId() + ". " + i.getName()));
            int id = in.readInt("Instrument id: ");
            instrument = instruments.stream().filter(i -> i.getId() == id).findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("No instrument #" + id));
        }
        Rental rental = new Rental(0, name, price, type, instrument);
        enrollments.addService(rental);
        System.out.println("Created " + rental);
    }
}
