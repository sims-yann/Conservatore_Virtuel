-- =====================================================================
-- Conservatoire Virtuel - demo scenario (Use Case Preparation)
-- Dates are relative to the day the data is loaded so the scenario
-- always works: valid / expired packages, a lesson starting in less than
-- 24h (late cancellation demo), a nearly full exam, etc.
-- NOTE: no ';' inside string literals (the script is split on ';').
-- =====================================================================

-- ===== Instruments =====
INSERT INTO instrument (id, name, type, available) VALUES
 (1, 'Piano',  'Keyboard',   TRUE),
 (2, 'Violin', 'String',     TRUE),
 (3, 'Guitar', 'String',     TRUE),
 (4, 'Flute',  'Wind',       TRUE),
 (5, 'Drums',  'Percussion', FALSE);   -- out of service: cannot be used in a lesson

-- ===== Rooms =====
INSERT INTO room (id, name, capacity) VALUES
 (1, 'Studio A',     2),
 (2, 'Salle Chopin', 6),
 (3, 'Auditorium',  30);

-- ===== Students =====
INSERT INTO student (id, first_name, last_name, email, phone, address, birth_date, level) VALUES
 (1, 'Lucas',  'Moreau',   'lucas.moreau@mail.com',   '0601020304', '12 rue des Lilas, Paris',      '2008-03-14', 'BEGINNER'),
 (2, 'Emma',   'Lefevre',  'emma.lefevre@mail.com',   '0611223344', '5 avenue Victor Hugo, Lyon',   '2005-07-22', 'INTERMEDIATE'),
 (3, 'Hugo',   'Garnier',  'hugo.garnier@mail.com',   '0622334455', '8 place Bellecour, Lyon',      '2002-11-02', 'ADVANCED'),
 (4, 'Chloe',  'Rousseau', 'chloe.rousseau@mail.com', '0633445566', '27 boulevard Voltaire, Paris', '2010-01-30', 'BEGINNER'),
 (5, 'Nathan', 'Faure',    'nathan.faure@mail.com',   '0644556677', '3 rue Nationale, Lille',       '1999-05-18', 'INTERMEDIATE');

INSERT INTO student_instrument (student_id, instrument_id) VALUES
 (1, 1), (2, 3), (2, 2), (3, 3), (4, 1), (4, 4), (5, 2);

-- ===== Teachers =====
INSERT INTO teacher (id, first_name, last_name, email, phone, hourly_rate) VALUES
 (1, 'Claire',  'Dubois',  'claire.dubois@conservatoire.fr',  '0700000001', 40.00),
 (2, 'Antoine', 'Martin',  'antoine.martin@conservatoire.fr', '0700000002', 35.00),
 (3, 'Sophie',  'Bernard', 'sophie.bernard@conservatoire.fr', '0700000003', 30.00);

INSERT INTO teacher_qualification (teacher_id, qualification) VALUES
 (1, 'Diplome d''Etat de professeur de musique'),
 (1, 'Master Piano - CNSM Paris'),
 (2, 'Prix de violon - Conservatoire de Lyon'),
 (2, 'Licence de musicologie'),
 (3, 'Certificat de batterie jazz');

INSERT INTO teacher_instrument (teacher_id, instrument_id) VALUES
 (1, 1), (1, 4),        -- Claire : Piano, Flute
 (2, 2), (2, 3),        -- Antoine: Violin, Guitar
 (3, 5), (3, 3);        -- Sophie : Drums, Guitar

-- Claire and Antoine work every day, Sophie only on weekday afternoons.
INSERT INTO teacher_availability (teacher_id, day_of_week, start_time, end_time) VALUES
 (1, 'MONDAY', '09:00', '20:00'), (1, 'TUESDAY', '09:00', '20:00'), (1, 'WEDNESDAY', '09:00', '20:00'),
 (1, 'THURSDAY', '09:00', '20:00'), (1, 'FRIDAY', '09:00', '20:00'), (1, 'SATURDAY', '09:00', '20:00'),
 (1, 'SUNDAY', '09:00', '20:00'),
 (2, 'MONDAY', '08:00', '22:00'), (2, 'TUESDAY', '08:00', '22:00'), (2, 'WEDNESDAY', '08:00', '22:00'),
 (2, 'THURSDAY', '08:00', '22:00'), (2, 'FRIDAY', '08:00', '22:00'), (2, 'SATURDAY', '08:00', '22:00'),
 (2, 'SUNDAY', '08:00', '22:00'),
 (3, 'MONDAY', '14:00', '20:00'), (3, 'TUESDAY', '14:00', '20:00'), (3, 'WEDNESDAY', '14:00', '20:00'),
 (3, 'THURSDAY', '14:00', '20:00'), (3, 'FRIDAY', '14:00', '20:00');

-- ===== Services: course packages and rentals =====
INSERT INTO service (id, name, price, service_type) VALUES
 (1, 'Pack Decouverte 10h',      250.00, 'COURSE_PACKAGE'),
 (2, 'Pack Annuel 30h',          650.00, 'COURSE_PACKAGE'),
 (3, 'Pack Illimite Trimestre',  900.00, 'COURSE_PACKAGE'),
 (4, 'Pack Ete 10h (expired)',   200.00, 'COURSE_PACKAGE'),
 (5, 'Violin rental (1 month)',   30.00, 'RENTAL'),
 (6, 'Room rental (1 hour)',      15.00, 'RENTAL');

INSERT INTO course_package (service_id, total_hours, start_date, end_date, type) VALUES
 (1, 10, CURDATE() - INTERVAL 30 DAY,  CURDATE() + INTERVAL 60 DAY,  'STANDARD'),
 (2, 30, CURDATE() - INTERVAL 10 DAY,  CURDATE() + INTERVAL 355 DAY, 'STANDARD'),
 (3,  0, CURDATE() - INTERVAL 5 DAY,   CURDATE() + INTERVAL 85 DAY,  'UNLIMITED'),
 (4, 10, CURDATE() - INTERVAL 120 DAY, CURDATE() - INTERVAL 20 DAY,  'STANDARD');

INSERT INTO rental (service_id, rental_type, instrument_id) VALUES
 (5, 'INSTRUMENT', 2),
 (6, 'ROOM',       NULL);

-- ===== Enrollments (purchased packages) =====
-- Lucas  : 10h pack, 2h used (lesson 1) + 1h reserved (lesson 3)  -> 7h left
-- Emma   : 30h pack, 1h reserved (group lesson 2)                 -> 29h left
-- Hugo   : unlimited pack (remaining hours not tracked)
-- Chloe  : expired summer pack with 6h left (cannot be used any more)
-- Nathan : no package -> pays per lesson
INSERT INTO enrollment (id, student_id, package_id, purchase_date, remaining_hours) VALUES
 (1, 1, 1, CURDATE() - INTERVAL 30 DAY,  7.00),
 (2, 2, 2, CURDATE() - INTERVAL 10 DAY, 29.00),
 (3, 3, 3, CURDATE() - INTERVAL 5 DAY,   0.00),
 (4, 4, 4, CURDATE() - INTERVAL 120 DAY, 6.00);

-- ===== Scheduled activities =====
INSERT INTO scheduled_activity (id, date_time, duration, status, activity_type) VALUES
 -- 1: past individual piano lesson (completed)
 (1, TIMESTAMP(CURDATE() - INTERVAL 7 DAY, '10:00:00'), 2.00, 'COMPLETED', 'LESSON'),
 -- 2: group guitar lesson in 3 days
 (2, TIMESTAMP(CURDATE() + INTERVAL 3 DAY, '15:00:00'), 1.00, 'SCHEDULED', 'LESSON'),
 -- 3: piano lesson starting in ~2-3 hours (cancelling it now = late cancellation)
 (3, DATE_FORMAT(NOW() + INTERVAL 3 HOUR, '%Y-%m-%d %H:00:00'), 1.00, 'SCHEDULED', 'LESSON'),
 -- 4: single paid violin lesson (no package)
 (4, TIMESTAMP(CURDATE() + INTERVAL 2 DAY, '11:00:00'), 1.00, 'SCHEDULED', 'LESSON'),
 -- 5: auditorium booking tomorrow evening
 (5, TIMESTAMP(CURDATE() + INTERVAL 1 DAY, '18:00:00'), 2.00, 'SCHEDULED', 'ROOM_BOOKING'),
 -- 6: upcoming piano exam (capacity 3, 2 registered -> 1 seat left)
 (6, TIMESTAMP(CURDATE() + INTERVAL 14 DAY, '09:00:00'), 3.00, 'SCHEDULED', 'EXAM'),
 -- 7: past guitar exam with recorded results
 (7, TIMESTAMP(CURDATE() - INTERVAL 10 DAY, '14:00:00'), 2.00, 'COMPLETED', 'EXAM');

INSERT INTO lesson (activity_id, type, teacher_id, room_id, instrument_id) VALUES
 (1, 'INDIVIDUAL', 1, 1, 1),
 (2, 'GROUP',      2, 2, 3),
 (3, 'INDIVIDUAL', 1, 1, 1),
 (4, 'INDIVIDUAL', 2, 1, 2);

INSERT INTO room_booking (activity_id, room_id) VALUES (5, 3);

INSERT INTO exam (activity_id, name, capacity, instrument_id) VALUES
 (6, 'Piano Exam - Level 1', 3, 1),
 (7, 'Guitar Exam - Level 2', 10, 3);

INSERT INTO lesson_participant (lesson_id, student_id) VALUES
 (1, 1),
 (2, 2), (2, 3),
 (3, 1),
 (4, 5);

INSERT INTO enrollment_lesson (enrollment_id, lesson_id, hours_consumed, consumed_at) VALUES
 (1, 1, 2.00, NOW() - INTERVAL 8 DAY),
 (2, 2, 1.00, NOW() - INTERVAL 1 DAY),
 (3, 2, 1.00, NOW() - INTERVAL 1 DAY),
 (1, 3, 1.00, NOW() - INTERVAL 2 DAY);

INSERT INTO exam_registration (exam_id, student_id, registered_at) VALUES
 (6, 1, NOW() - INTERVAL 3 DAY),
 (6, 4, NOW() - INTERVAL 2 DAY),
 (7, 2, NOW() - INTERVAL 20 DAY),
 (7, 3, NOW() - INTERVAL 19 DAY);

INSERT INTO exam_result (exam_id, student_id, passed, score) VALUES
 (7, 2, TRUE,  78.50),
 (7, 3, FALSE, 42.00);

-- ===== Invoices, items and payments =====
INSERT INTO invoice (id, student_id, date, total_amount) VALUES
 (1, 1, CURDATE() - INTERVAL 30 DAY,  250.00),   -- Lucas  : 10h pack (paid)
 (2, 2, CURDATE() - INTERVAL 10 DAY,  650.00),   -- Emma   : 30h pack (partially paid)
 (3, 3, CURDATE() - INTERVAL 5 DAY,   930.00),   -- Hugo   : unlimited pack + violin rental (unpaid)
 (4, 5, CURDATE() - INTERVAL 1 DAY,    35.00),   -- Nathan : single lesson 4 (unpaid)
 (5, 4, CURDATE() - INTERVAL 120 DAY, 200.00);   -- Chloe  : summer pack (paid)

INSERT INTO invoice_item (invoice_id, service_id, lesson_id) VALUES
 (1, 1, NULL),
 (2, 2, NULL),
 (3, 3, NULL),
 (3, 5, NULL),
 (4, NULL, 4),
 (5, 4, NULL);

INSERT INTO payment (invoice_id, amount, payment_date) VALUES
 (1, 250.00, CURDATE() - INTERVAL 30 DAY),
 (2, 300.00, CURDATE() - INTERVAL 10 DAY),
 (5, 200.00, CURDATE() - INTERVAL 118 DAY);
