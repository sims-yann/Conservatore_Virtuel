package I2new.group2.ui;

import I2new.group2.dao.DatabaseConnection;
import I2new.group2.dao.DatabaseInitializer;
import I2new.group2.dao.InvoiceDao;
import I2new.group2.model.ActivityStatus;
import I2new.group2.model.CoursePackage;
import I2new.group2.model.Exam;
import I2new.group2.model.Instrument;
import I2new.group2.model.Invoice;
import I2new.group2.model.LessonType;
import I2new.group2.model.Rental;
import I2new.group2.model.ScheduledActivity;
import I2new.group2.model.Service;
import I2new.group2.model.Student;
import I2new.group2.model.Teacher;
import I2new.group2.service.BillingService;
import I2new.group2.service.EnrollmentService;
import I2new.group2.service.ExamService;
import I2new.group2.service.PeopleService;
import I2new.group2.service.ReportService;
import I2new.group2.service.SchedulingService;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.UIManager;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;

public final class DesktopApp {

    private static final Color INK = new Color(31, 41, 55);
    private static final Color MUTED = new Color(107, 114, 128);
    private static final Color ACCENT = new Color(91, 73, 164);
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final PeopleService people;
    private final EnrollmentService enrollments;
    private final SchedulingService scheduling;
    private final ExamService exams;
    private final BillingService billing;
    private final ReportService reports;
    private final JFrame frame = new JFrame("Conservatoire Virtuel");
    private final JLabel status = new JLabel("Prêt");
    private final JLabel overview = new JLabel("Chargement des données…");
    private final JTable studentsTable = table();
    private final JTable teachersTable = table();
    private final JTable scheduleTable = table();
    private final JTable examsTable = table();
    private final JTable invoicesTable = table();
    private final JTable servicesTable = table();
    private final JTable instrumentsTable = table();
    private List<ScheduledActivity> activities = List.of();
    private List<Exam> examRows = List.of();
    private List<Invoice> invoiceRows = List.of();
    private List<Instrument> instrumentRows = List.of();

    public DesktopApp(PeopleService people, EnrollmentService enrollments, SchedulingService scheduling,
                      ExamService exams, BillingService billing, ReportService reports) {
        this.people = people;
        this.enrollments = enrollments;
        this.scheduling = scheduling;
        this.exams = exams;
        this.billing = billing;
        this.reports = reports;
    }

    public void show() {
        configureLookAndFeel();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setMinimumSize(new Dimension(980, 650));
        frame.setSize(1180, 760);
        frame.setLocationRelativeTo(null);

        JPanel root = new JPanel(new BorderLayout(0, 16));
        root.setBorder(BorderFactory.createEmptyBorder(22, 24, 12, 24));
        root.setBackground(new Color(247, 248, 252));
        root.add(buildHeader(), BorderLayout.NORTH);
        root.add(buildTabs(), BorderLayout.CENTER);
        status.setForeground(MUTED);
        status.setBorder(BorderFactory.createEmptyBorder(4, 2, 0, 2));
        root.add(status, BorderLayout.SOUTH);
        frame.setContentPane(root);
        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent event) {
                closeConnection();
            }
        });
        frame.setVisible(true);
        refreshAll();
    }

    private JPanel buildHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        JPanel brand = new JPanel(new GridLayout(2, 1, 0, 3));
        brand.setOpaque(false);
        JLabel title = new JLabel("Conservatoire Virtuel");
        title.setFont(new Font("SansSerif", Font.BOLD, 25));
        title.setForeground(INK);
        JLabel subtitle = new JLabel("Gestion de l'école de musique");
        subtitle.setForeground(MUTED);
        brand.add(title);
        brand.add(subtitle);
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        actions.setOpaque(false);
        JButton reset = actionButton("Réinitialiser les données de démo");
        reset.setBackground(new Color(153, 55, 55));
        reset.addActionListener(event -> resetDemoData());
        JButton refresh = actionButton("Actualiser");
        refresh.addActionListener(event -> refreshAll());
        header.add(brand, BorderLayout.WEST);
        actions.add(reset);
        actions.add(refresh);
        header.add(actions, BorderLayout.EAST);
        return header;
    }

    private JTabbedPane buildTabs() {
        JTabbedPane tabs = new JTabbedPane();
        tabs.setFont(new Font("SansSerif", Font.BOLD, 13));
        tabs.addTab("Vue d'ensemble", buildOverview());
        tabs.addTab("Élèves", buildPeopleTab(studentsTable, "Élèves inscrits", "Ajouter un élève", this::addStudent));
        tabs.addTab("Professeurs", buildPeopleTab(teachersTable, "Équipe pédagogique", "Ajouter un professeur", this::addTeacher));
        tabs.addTab("Planning", buildScheduleTab());
        tabs.addTab("Examens", buildExamsTab());
        tabs.addTab("Facturation", buildBillingTab());
        tabs.addTab("Offres & instruments", buildServicesTab());
        return tabs;
    }

    private JPanel buildOverview() {
        JPanel panel = contentPanel();
        overview.setFont(new Font("SansSerif", Font.PLAIN, 16));
        overview.setForeground(INK);
        overview.setVerticalAlignment(SwingConstants.TOP);
        overview.setBorder(BorderFactory.createEmptyBorder(24, 20, 20, 20));
        panel.add(overview, BorderLayout.NORTH);
        return panel;
    }

    private JPanel buildPeopleTab(JTable table, String title, String addLabel, Runnable addAction) {
        JPanel panel = contentPanel();
        panel.add(sectionHeading(title), BorderLayout.NORTH);
        panel.add(new JScrollPane(table), BorderLayout.CENTER);
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 8));
        actions.setOpaque(false);
        JButton add = actionButton(addLabel);
        add.addActionListener(event -> addAction.run());
        JButton remove = actionButton("Supprimer la sélection");
        remove.addActionListener(event -> deletePerson(table == studentsTable));
        actions.add(add);
        actions.add(remove);
        panel.add(actions, BorderLayout.SOUTH);
        return panel;
    }

    private JPanel buildScheduleTab() {
        JPanel panel = contentPanel();
        panel.add(sectionHeading("Activités et réservations"), BorderLayout.NORTH);
        panel.add(new JScrollPane(scheduleTable), BorderLayout.CENTER);
        JPanel actions = actionBar(
                button("Planifier un cours", this::scheduleLesson),
                button("Terminer", () -> changeActivityStatus(true)),
                button("Annuler", () -> changeActivityStatus(false))
        );
        panel.add(actions, BorderLayout.SOUTH);
        return panel;
    }

    private JPanel buildExamsTab() {
        JPanel panel = contentPanel();
        panel.add(sectionHeading("Examens et inscriptions"), BorderLayout.NORTH);
        panel.add(new JScrollPane(examsTable), BorderLayout.CENTER);
        panel.add(actionBar(
                button("Créer un examen", this::createExam),
                button("Inscrire un élève", this::registerStudent)
        ), BorderLayout.SOUTH);
        return panel;
    }

    private JPanel buildBillingTab() {
        JPanel panel = contentPanel();
        panel.add(sectionHeading("Factures et paiements"), BorderLayout.NORTH);
        panel.add(new JScrollPane(invoicesTable), BorderLayout.CENTER);
        panel.add(actionBar(
                button("Enregistrer un paiement", this::recordPayment),
                button("Acheter une offre", this::purchaseService)
        ), BorderLayout.SOUTH);
        return panel;
    }

    private JPanel buildServicesTab() {
        JPanel panel = contentPanel();
        JTabbedPane inner = new JTabbedPane();
        inner.addTab("Offres", new JScrollPane(servicesTable));
        inner.addTab("Instruments", new JScrollPane(instrumentsTable));
        panel.add(sectionHeading("Catalogue de l'école"), BorderLayout.NORTH);
        panel.add(inner, BorderLayout.CENTER);
        panel.add(actionBar(button("Changer la disponibilité", this::toggleInstrument)), BorderLayout.SOUTH);
        return panel;
    }

    private JPanel contentPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 12));
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createEmptyBorder(18, 18, 12, 18));
        return panel;
    }

    private JLabel sectionHeading(String text) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("SansSerif", Font.BOLD, 18));
        label.setForeground(INK);
        return label;
    }

    private JPanel actionBar(JButton... buttons) {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 8));
        panel.setOpaque(false);
        for (JButton button : buttons) {
            panel.add(button);
        }
        return panel;
    }

    private JButton button(String label, Runnable action) {
        JButton button = actionButton(label);
        button.addActionListener(event -> action.run());
        return button;
    }

    private JButton actionButton(String label) {
        JButton button = new JButton(label);
        button.setFocusPainted(false);
        button.setBackground(ACCENT);
        button.setForeground(Color.WHITE);
        button.setBorder(BorderFactory.createEmptyBorder(9, 14, 9, 14));
        return button;
    }

    private static JTable table() {
        JTable table = new JTable();
        table.setFillsViewportHeight(true);
        table.setRowHeight(28);
        table.setAutoCreateRowSorter(true);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setFont(new Font("SansSerif", Font.PLAIN, 13));
        table.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 12));
        return table;
    }

    private void refreshAll() {
        status.setText("Chargement…");
        execute(() -> new Snapshot(
                people.listStudents(),
                people.listTeachers(),
                scheduling.getSchedule(),
                exams.listExams(),
                billing.listInvoices(),
                enrollments.listServices(),
                scheduling.listInstruments(),
                reports.summaryPerStudent()
        ), snapshot -> {
            setRows(studentsTable, new String[]{"N°", "Nom", "E-mail", "Niveau", "Heures restantes"},
                    snapshot.students.stream().map(student -> new Object[]{
                            student.getId(), student.getFullName(), student.getEmail(), student.getLevel(),
                            student.hasUnlimitedPackage(LocalDate.now()) ? "Illimité" : String.format("%.1f h", student.getRemainingHours())
                    }).toList());
            setRows(teachersTable, new String[]{"N°", "Nom", "E-mail", "Tarif horaire", "Spécialités"},
                    snapshot.teachers.stream().map(teacher -> new Object[]{
                            teacher.getId(), teacher.getFullName(), teacher.getEmail(),
                            String.format("%.2f € / h", teacher.getHourlyRate()),
                            teacher.getSpecializations().stream().map(Instrument::getName).reduce((a, b) -> a + ", " + b).orElse("—")
                    }).toList());
            activities = snapshot.activities;
            setRows(scheduleTable, new String[]{"N°", "Type", "Date et heure", "Durée", "Statut", "Détails"},
                    activities.stream().map(activity -> new Object[]{
                            activity.getId(), activity.getActivityType(),
                            activity.getDateTime().format(DATE_TIME),
                            String.format("%.1f h", activity.getDuration()), activity.getStatus(), activity.describe()
                    }).toList());
            examRows = snapshot.exams;
            setRows(examsTable, new String[]{"N°", "Examen", "Instrument", "Date", "Inscrits", "Places restantes", "Statut"},
                    examRows.stream().map(exam -> new Object[]{
                            exam.getId(), exam.getName(), exam.getInstrument().getName(),
                            exam.getDateTime().format(DATE_TIME),
                            exam.getRegisteredStudents().size() + " / " + exam.getCapacity(),
                            exam.getRemainingSeats(), exam.getStatus()
                    }).toList());
            invoiceRows = snapshot.invoices;
            setRows(invoicesTable, new String[]{"N°", "Élève", "Date", "Total", "Payé", "Solde"},
                    invoiceRows.stream().map(invoice -> new Object[]{
                            invoice.getId(), studentName(snapshot.students, invoice.getStudentId()), invoice.getDate(),
                            money(invoice.getTotalAmount()), money(invoice.getAmountPaid()), money(invoice.getOutstandingBalance())
                    }).toList());
            setRows(servicesTable, new String[]{"N°", "Offre", "Prix", "Description"},
                    snapshot.services.stream().map(service -> new Object[]{
                            service.getId(), service.getName(), money(service.calculatePrice()), service.describe()
                    }).toList());
            instrumentRows = snapshot.instruments;
            setRows(instrumentsTable, new String[]{"N°", "Instrument", "Type", "Disponibilité"},
                    instrumentRows.stream().map(instrument -> new Object[]{
                            instrument.getId(), instrument.getName(), instrument.getType(),
                            instrument.isAvailable() ? "Disponible" : "Hors service"
                    }).toList());
            overview.setText("<html><h2>Bienvenue au conservatoire</h2>"
                    + "<p>Élèves : <b>" + snapshot.students.size() + "</b>"
                    + " &nbsp;&nbsp; Professeurs : <b>" + snapshot.teachers.size() + "</b>"
                    + " &nbsp;&nbsp; Activités à venir : <b>" + snapshot.activities.stream()
                    .filter(activity -> activity.getStatus() == ActivityStatus.SCHEDULED
                            && !activity.getDateTime().isBefore(LocalDateTime.now())).count() + "</b></p>"
                    + "<p>Examens : <b>" + snapshot.exams.size() + "</b>"
                    + " &nbsp;&nbsp; Factures ouvertes : <b>" + snapshot.invoices.stream().filter(invoice -> !invoice.isPaid()).count()
                    + "</b> &nbsp;&nbsp; Encaissé : <b>" + money(snapshot.summaries.stream()
                    .mapToDouble(InvoiceDao.StudentSummary::paid).sum()) + "</b></p>"
                    + "<p>Dernière actualisation : " + LocalDateTime.now().format(DATE_TIME) + "</p></html>");
            status.setText("Données actualisées");
        });
    }

    private void scheduleLesson() {
        execute(() -> new Catalogs(people.listStudents(), people.listTeachers(), scheduling.listRooms(),
                scheduling.listInstruments()), catalogs -> {
            if (catalogs.students.isEmpty() || catalogs.teachers.isEmpty() || catalogs.rooms.isEmpty()
                    || catalogs.instruments.isEmpty()) {
                showError(new IllegalStateException("Il faut au moins un élève, un professeur, une salle et un instrument."));
                return;
            }
            JComboBox<Teacher> teacher = new JComboBox<>(catalogs.teachers.toArray(Teacher[]::new));
            JComboBox< I2new.group2.model.Room> room = new JComboBox<>(catalogs.rooms.toArray(I2new.group2.model.Room[]::new));
            JComboBox<Instrument> instrument = new JComboBox<>(catalogs.instruments.toArray(Instrument[]::new));
            JComboBox<LessonType> type = new JComboBox<>(LessonType.values());
            JTextField start = new JTextField(LocalDateTime.now().plusDays(1).withMinute(0).withSecond(0).withNano(0)
                    .format(DATE_TIME), 16);
            JTextField duration = new JTextField("1", 6);
            JList<Student> students = new JList<>(catalogs.students.toArray(Student[]::new));
            students.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
            students.setVisibleRowCount(Math.min(6, catalogs.students.size()));
            JPanel fields = formPanel(
                    "Professeur", teacher, "Salle", room, "Instrument", instrument,
                    "Type", type, "Début (yyyy-MM-dd HH:mm)", start, "Durée (heures)", duration
            );
            fields.add(new JLabel("Élèves"));
            fields.add(new JScrollPane(students));
            if (JOptionPane.showConfirmDialog(frame, fields, "Planifier un cours",
                    JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE) != JOptionPane.OK_OPTION) {
                return;
            }
            try {
                List<Integer> studentIds = students.getSelectedValuesList().stream().map(Student::getId).toList();
                if (studentIds.isEmpty()) {
                    throw new IllegalArgumentException("Sélectionnez au moins un élève.");
                }
                int teacherId = ((Teacher) teacher.getSelectedItem()).getId();
                int roomId = ((I2new.group2.model.Room) room.getSelectedItem()).getId();
                int instrumentId = ((Instrument) instrument.getSelectedItem()).getId();
                LessonType lessonType = (LessonType) type.getSelectedItem();
                LocalDateTime dateTime = LocalDateTime.parse(start.getText().trim(), DATE_TIME);
                double hours = Double.parseDouble(duration.getText().trim());
                execute(() -> scheduling.scheduleLesson(teacherId, roomId, instrumentId, studentIds,
                                lessonType, dateTime, hours).notes(),
                        notes -> showMessage(String.join("\n", notes)));
            } catch (RuntimeException exception) {
                showError(exception);
            }
        });
    }

    private void createExam() {
        execute(scheduling::listInstruments, instruments -> {
            if (instruments.isEmpty()) {
                showError(new IllegalStateException("Aucun instrument n'est enregistré."));
                return;
            }
            JTextField name = new JTextField(20);
            JComboBox<Instrument> instrument = new JComboBox<>(instruments.toArray(Instrument[]::new));
            JTextField start = new JTextField(LocalDateTime.now().plusDays(7).withMinute(0).withSecond(0).withNano(0)
                    .format(DATE_TIME), 16);
            JTextField duration = new JTextField("1", 6);
            JTextField capacity = new JTextField("10", 6);
            JPanel fields = formPanel("Nom", name, "Instrument", instrument,
                    "Début (yyyy-MM-dd HH:mm)", start, "Durée (heures)", duration, "Capacité", capacity);
            if (JOptionPane.showConfirmDialog(frame, fields, "Créer un examen",
                    JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE) != JOptionPane.OK_OPTION) {
                return;
            }
            try {
                String examName = name.getText().trim();
                int instrumentId = ((Instrument) instrument.getSelectedItem()).getId();
                LocalDateTime dateTime = LocalDateTime.parse(start.getText().trim(), DATE_TIME);
                double hours = Double.parseDouble(duration.getText().trim());
                int seats = Integer.parseInt(capacity.getText().trim());
                execute(() -> exams.createExam(examName, instrumentId, dateTime, hours, seats),
                        result -> showMessage("Examen créé : " + result.getName()));
            } catch (RuntimeException exception) {
                showError(exception);
            }
        });
    }

    private void registerStudent() {
        execute(exams::listExams, allExams -> {
            List<Exam> scheduled = allExams.stream().filter(exam -> exam.getStatus() == ActivityStatus.SCHEDULED
                    && !exam.isFull()).toList();
            if (scheduled.isEmpty()) {
                showError(new IllegalStateException("Aucun examen ouvert aux inscriptions."));
                return;
            }
            Exam selectedExam = (Exam) JOptionPane.showInputDialog(frame, "Choisissez un examen", "Inscription",
                    JOptionPane.PLAIN_MESSAGE, null, scheduled.toArray(), scheduled.get(0));
            if (selectedExam == null) {
                return;
            }
            execute(() -> exams.candidatesFor(selectedExam), candidates -> {
                if (candidates.isEmpty()) {
                    showError(new IllegalStateException("Aucun élève éligible (instrument préféré requis)."));
                    return;
                }
                Student student = (Student) JOptionPane.showInputDialog(frame, "Choisissez un élève",
                        "Inscription à " + selectedExam.getName(), JOptionPane.PLAIN_MESSAGE, null,
                        candidates.toArray(), candidates.get(0));
                if (student != null) {
                    execute(() -> {
                        exams.registerStudent(selectedExam.getId(), student.getId());
                        return null;
                    }, ignored -> showMessage(student.getFullName() + " est inscrit à l'examen."));
                }
            });
        });
    }

    private void changeActivityStatus(boolean complete) {
        int id = selectedId(scheduleTable);
        if (id < 0) {
            showError(new IllegalStateException("Sélectionnez une activité dans le planning."));
            return;
        }
        execute(() -> {
            if (complete) {
                scheduling.completeActivity(id);
                return "Activité terminée.";
            }
            return scheduling.cancelActivity(id, LocalDateTime.now());
        }, this::showMessage);
    }

    private void recordPayment() {
        int id = selectedId(invoicesTable);
        Invoice selected = invoiceRows.stream().filter(invoice -> invoice.getId() == id).findFirst().orElse(null);
        if (selected == null) {
            showError(new IllegalStateException("Sélectionnez une facture."));
            return;
        }
        if (selected.isPaid()) {
            showError(new IllegalStateException("Cette facture est déjà réglée."));
            return;
        }
        String amountText = JOptionPane.showInputDialog(frame,
                "Solde restant : " + money(selected.getOutstandingBalance()) + "\nMontant du paiement (€)", "");
        if (amountText == null) {
            return;
        }
        try {
            double amount = Double.parseDouble(amountText.trim().replace(',', '.'));
            execute(() -> billing.recordPayment(id, amount, LocalDate.now()), updated ->
                    showMessage("Paiement enregistré. Solde restant : " + money(updated.getOutstandingBalance())));
        } catch (RuntimeException exception) {
            showError(exception);
        }
    }

    private void purchaseService() {
        execute(() -> new Catalogs(people.listStudents(), enrollments.listServices()), catalogs -> {
            List<Student> students = catalogs.students;
            List<Service> offerings = catalogs.services;
            if (students.isEmpty() || offerings.isEmpty()) {
                showError(new IllegalStateException("Il faut au moins un élève et une offre au catalogue."));
                return;
            }
            Student student = choose("Choisissez un élève", "Achat", students);
            if (student == null) {
                return;
            }
            List<Service> available = offerings.stream()
                    .filter(item -> !(item instanceof Rental rental)
                            || rental.getInstrument() == null || rental.getInstrument().isAvailable())
                    .toList();
            Service offering = choose("Choisissez une offre", "Achat pour " + student.getFullName(), available);
            if (offering == null) {
                return;
            }
            execute(() -> {
                if (offering instanceof CoursePackage coursePackage) {
                    return enrollments.purchasePackage(student.getId(), coursePackage.getId());
                }
                if (offering instanceof Rental) {
                    return enrollments.rent(student.getId(), offering.getId());
                }
                throw new IllegalArgumentException("Type d'offre non pris en charge.");
            }, invoice -> showMessage("Achat enregistré dans la facture #" + invoice.getId()));
        });
    }

    private void toggleInstrument() {
        int id = selectedId(instrumentsTable);
        Instrument instrument = instrumentRows.stream().filter(item -> item.getId() == id).findFirst().orElse(null);
        if (instrument == null) {
            showError(new IllegalStateException("Sélectionnez un instrument."));
            return;
        }
        boolean available = !instrument.isAvailable();
        execute(() -> {
            scheduling.setInstrumentAvailable(id, available);
            return null;
        }, ignored -> showMessage(instrument.getName() + (available ? " est disponible." : " est hors service.")));
    }

    private void resetDemoData() {
        int answer = JOptionPane.showConfirmDialog(frame,
                "Toutes les modifications seront supprimées et les données de démonstration rechargées.",
                "Réinitialiser les données ?", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (answer != JOptionPane.YES_OPTION) {
            return;
        }
        execute(() -> {
            DatabaseInitializer.resetDemoData();
            return null;
        }, ignored -> showMessage("Les données de démonstration ont été rechargées."));
    }

    private void addStudent() {
        execute(scheduling::listInstruments, instruments -> {
            JTextField firstName = new JTextField(18);
            JTextField lastName = new JTextField(18);
            JTextField email = new JTextField(18);
            JTextField phone = new JTextField(18);
            JTextField address = new JTextField(18);
            JTextField birthDate = new JTextField(10);
            JComboBox<I2new.group2.model.StudentLevel> level =
                    new JComboBox<>(I2new.group2.model.StudentLevel.values());
            JList<Instrument> preferences = new JList<>(instruments.toArray(Instrument[]::new));
            preferences.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
            preferences.setVisibleRowCount(Math.min(5, Math.max(2, instruments.size())));
            JPanel fields = formPanel("Prénom", firstName, "Nom", lastName, "E-mail", email,
                    "Téléphone", phone, "Adresse", address, "Naissance (yyyy-MM-dd, facultatif)", birthDate,
                    "Niveau", level, "Instruments préférés", new JScrollPane(preferences));
            if (JOptionPane.showConfirmDialog(frame, fields, "Ajouter un élève",
                    JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE) != JOptionPane.OK_OPTION) {
                return;
            }
            try {
                String dateText = birthDate.getText().trim();
                LocalDate date = dateText.isEmpty() ? null : LocalDate.parse(dateText);
                Student student = new Student(0, firstName.getText(), lastName.getText(), email.getText(),
                        phone.getText(), address.getText(), date,
                        (I2new.group2.model.StudentLevel) level.getSelectedItem());
                preferences.getSelectedValuesList().forEach(student::addPreferredInstrument);
                execute(() -> {
                    people.addStudent(student);
                    return null;
                }, ignored -> showMessage("Élève ajouté."));
            } catch (RuntimeException exception) {
                showError(exception);
            }
        });
    }

    private void addTeacher() {
        execute(scheduling::listInstruments, instruments -> {
            JTextField firstName = new JTextField(18);
            JTextField lastName = new JTextField(18);
            JTextField email = new JTextField(18);
            JTextField phone = new JTextField(18);
            JTextField rate = new JTextField("30", 8);
            JComboBox<java.time.DayOfWeek> day = new JComboBox<>(java.time.DayOfWeek.values());
            JTextField from = new JTextField("09:00", 6);
            JTextField to = new JTextField("17:00", 6);
            JList<Instrument> specializations = new JList<>(instruments.toArray(Instrument[]::new));
            specializations.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
            specializations.setVisibleRowCount(Math.min(5, Math.max(2, instruments.size())));
            JPanel fields = formPanel("Prénom", firstName, "Nom", lastName, "E-mail", email,
                    "Téléphone", phone, "Tarif horaire (€)", rate, "Jour disponible chaque semaine", day,
                    "De (HH:mm)", from, "À (HH:mm)", to, "Spécialités", new JScrollPane(specializations));
            if (JOptionPane.showConfirmDialog(frame, fields, "Ajouter un professeur",
                    JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE) != JOptionPane.OK_OPTION) {
                return;
            }
            try {
                Teacher teacher = new Teacher(0, firstName.getText(), lastName.getText(), email.getText(),
                        phone.getText(), Double.parseDouble(rate.getText().trim().replace(',', '.')));
                specializations.getSelectedValuesList().forEach(teacher::addSpecialization);
                teacher.addAvailability(new Teacher.Availability((java.time.DayOfWeek) day.getSelectedItem(),
                        java.time.LocalTime.parse(from.getText().trim()), java.time.LocalTime.parse(to.getText().trim())));
                execute(() -> {
                    people.addTeacher(teacher);
                    return null;
                }, ignored -> showMessage("Professeur ajouté."));
            } catch (RuntimeException exception) {
                showError(exception);
            }
        });
    }

    private void deletePerson(boolean student) {
        JTable table = student ? studentsTable : teachersTable;
        int id = selectedId(table);
        if (id < 0) {
            showError(new IllegalStateException("Sélectionnez une personne."));
            return;
        }
        int answer = JOptionPane.showConfirmDialog(frame, "Supprimer cette fiche ?",
                "Confirmation", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (answer != JOptionPane.YES_OPTION) {
            return;
        }
        execute(() -> {
            if (student) {
                people.deleteStudent(id);
            } else {
                people.deleteTeacher(id);
            }
            return null;
        }, ignored -> showMessage("Fiche supprimée."));
    }

    private <T> void execute(SqlTask<T> task, Consumer<T> success) {
        status.setText("Traitement en cours…");
        new SwingWorker<T, Void>() {
            @Override
            protected T doInBackground() throws Exception {
                return task.run();
            }

            @Override
            protected void done() {
                try {
                    T result = get();
                    status.setText("Opération terminée");
                    refreshDataAfterAction();
                    success.accept(result);
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    status.setText("Opération interrompue");
                    showError(exception);
                } catch (ExecutionException exception) {
                    status.setText("Échec de l'opération");
                    showError(exception.getCause() == null ? exception : exception.getCause());
                } catch (RuntimeException exception) {
                    status.setText("Échec de l'affichage");
                    showError(exception);
                }
            }
        }.execute();
    }

    private void refreshDataAfterAction() {
        refreshAll();
    }

    private void showError(Throwable error) {
        String message = error.getMessage() == null ? error.toString() : error.getMessage();
        JOptionPane.showMessageDialog(frame, message, "Une erreur est survenue", JOptionPane.ERROR_MESSAGE);
    }

    private void showMessage(String message) {
        JOptionPane.showMessageDialog(frame, message, "Conservatoire Virtuel", JOptionPane.INFORMATION_MESSAGE);
    }

    private static JPanel formPanel(Object... labelsAndComponents) {
        JPanel panel = new JPanel(new GridLayout(0, 2, 10, 8));
        panel.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        for (int i = 0; i < labelsAndComponents.length; i += 2) {
            panel.add(new JLabel(String.valueOf(labelsAndComponents[i])));
            panel.add((Component) labelsAndComponents[i + 1]);
        }
        return panel;
    }

    private static String studentName(List<Student> students, int id) {
        return students.stream().filter(student -> student.getId() == id).map(Student::getFullName)
                .findFirst().orElse("#" + id);
    }

    private static String money(double amount) {
        return String.format("%.2f €", amount);
    }

    private static int selectedId(JTable table) {
        int viewRow = table.getSelectedRow();
        if (viewRow < 0) {
            return -1;
        }
        int modelRow = table.convertRowIndexToModel(viewRow);
        return ((Number) table.getModel().getValueAt(modelRow, 0)).intValue();
    }

    private static <T> T choose(String prompt, String title, List<T> choices) {
        if (choices.isEmpty()) {
            return null;
        }
        @SuppressWarnings("unchecked")
        T selected = (T) JOptionPane.showInputDialog(null, prompt, title, JOptionPane.PLAIN_MESSAGE,
                null, choices.toArray(), choices.get(0));
        return selected;
    }

    private static void setRows(JTable table, String[] columns, List<Object[]> rows) {
        DefaultTableModel model = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        rows.forEach(model::addRow);
        table.setModel(model);
    }

    private static void configureLookAndFeel() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (ReflectiveOperationException | javax.swing.UnsupportedLookAndFeelException exception) {
            System.err.println("Cannot apply system look and feel: " + exception.getMessage());
        }
    }

    private static void closeConnection() {
        DatabaseConnection.getInstance().close();
    }

    private record Snapshot(List<Student> students, List<Teacher> teachers, List<ScheduledActivity> activities,
                            List<Exam> exams, List<Invoice> invoices, List<Service> services,
                            List<Instrument> instruments, List<InvoiceDao.StudentSummary> summaries) {
    }

    private record Catalogs(List<Student> students, List<Teacher> teachers,
                            List<I2new.group2.model.Room> rooms, List<Instrument> instruments,
                            List<Service> services) {
        Catalogs(List<Student> students, List<Teacher> teachers,
                 List<I2new.group2.model.Room> rooms, List<Instrument> instruments) {
            this(students, teachers, rooms, instruments, List.of());
        }

        Catalogs(List<Student> students, List<Service> services) {
            this(students, List.of(), List.of(), List.of(), services);
        }
    }

    @FunctionalInterface
    private interface SqlTask<T> {
        T run() throws Exception;
    }
}
