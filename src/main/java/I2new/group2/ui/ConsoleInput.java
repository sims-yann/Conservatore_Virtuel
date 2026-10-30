package I2new.group2.ui;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

/** Reads and validates keyboard input; every read re-asks until the value is valid. */
public class ConsoleInput {

    public static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final BufferedReader reader = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8));

    public String readLine(String prompt) {
        System.out.print(prompt);
        try {
            String line = reader.readLine();
            if (line == null) { // end of input (e.g. piped scenario finished)
                System.out.println();
                System.out.println("End of input - goodbye.");
                System.exit(0);
            }
            return line.trim();
        } catch (IOException e) {
            throw new IllegalStateException("Cannot read the console", e);
        }
    }

    public String readText(String prompt) {
        while (true) {
            String value = readLine(prompt);
            if (!value.isEmpty()) {
                return value;
            }
            System.out.println("  A value is required.");
        }
    }

    /** Empty input returns {@code defaultValue}. */
    public String readOptional(String prompt, String defaultValue) {
        String value = readLine(prompt);
        return value.isEmpty() ? defaultValue : value;
    }

    public int readInt(String prompt) {
        return read(prompt, Integer::parseInt, "a whole number");
    }

    public int readInt(String prompt, int min, int max) {
        while (true) {
            int value = readInt(prompt);
            if (value >= min && value <= max) {
                return value;
            }
            System.out.printf("  Enter a number between %d and %d.%n", min, max);
        }
    }

    public double readDouble(String prompt) {
        return read(prompt, s -> Double.parseDouble(s.replace(',', '.')), "a number (e.g. 1.5)");
    }

    public double readPositiveDouble(String prompt) {
        while (true) {
            double value = readDouble(prompt);
            if (value > 0) {
                return value;
            }
            System.out.println("  The value must be positive.");
        }
    }

    /** Optional number: empty input returns null. */
    public Double readOptionalDouble(String prompt) {
        while (true) {
            String value = readLine(prompt);
            if (value.isEmpty()) {
                return null;
            }
            try {
                return Double.parseDouble(value.replace(',', '.'));
            } catch (NumberFormatException e) {
                System.out.println("  Enter a number or leave empty.");
            }
        }
    }

    public LocalDate readDate(String prompt) {
        return read(prompt + "(yyyy-MM-dd): ", LocalDate::parse, "a date like 2026-11-15");
    }

    /** Optional date: empty input returns {@code defaultValue}. */
    public LocalDate readOptionalDate(String prompt, LocalDate defaultValue) {
        while (true) {
            String value = readLine(prompt + "(yyyy-MM-dd, empty = " + defaultValue + "): ");
            if (value.isEmpty()) {
                return defaultValue;
            }
            try {
                return LocalDate.parse(value);
            } catch (DateTimeParseException e) {
                System.out.println("  Enter a date like 2026-11-15.");
            }
        }
    }

    public LocalDateTime readDateTime(String prompt) {
        return read(prompt + "(yyyy-MM-dd HH:mm): ", s -> LocalDateTime.parse(s, DATE_TIME), "a date like 2026-11-15 14:30");
    }

    public LocalTime readTime(String prompt) {
        return read(prompt + "(HH:mm): ", LocalTime::parse, "a time like 09:30");
    }

    public YearMonth readYearMonth(String prompt) {
        return read(prompt + "(yyyy-MM): ", YearMonth::parse, "a month like 2026-10");
    }

    public boolean readYesNo(String prompt) {
        while (true) {
            String value = readLine(prompt + " (y/n): ").toLowerCase();
            if (value.equals("y") || value.equals("yes") || value.equals("o") || value.equals("oui")) {
                return true;
            }
            if (value.equals("n") || value.equals("no") || value.equals("non")) {
                return false;
            }
            System.out.println("  Answer y or n.");
        }
    }

    public <E extends Enum<E>> E readEnum(String prompt, Class<E> type) {
        E[] values = type.getEnumConstants();
        System.out.println(prompt);
        for (int i = 0; i < values.length; i++) {
            System.out.printf("  %d. %s%n", i + 1, values[i]);
        }
        return values[readInt("  Choice: ", 1, values.length) - 1];
    }

    /** Comma separated ids, e.g. "1, 3, 4". Empty input returns an empty list. */
    public List<Integer> readIdList(String prompt) {
        while (true) {
            String value = readLine(prompt);
            if (value.isEmpty()) {
                return List.of();
            }
            try {
                List<Integer> ids = new ArrayList<>();
                for (String part : Arrays.stream(value.split("[,\\s]+")).filter(p -> !p.isBlank()).toList()) {
                    ids.add(Integer.parseInt(part));
                }
                return ids;
            } catch (NumberFormatException e) {
                System.out.println("  Enter ids separated by commas, e.g. 1, 3");
            }
        }
    }

    public void pause() {
        readLine("Press Enter to continue...");
    }

    private <T> T read(String prompt, Function<String, T> parser, String expected) {
        while (true) {
            String value = readLine(prompt);
            try {
                return parser.apply(value);
            } catch (RuntimeException e) {
                System.out.println("  Please enter " + expected + ".");
            }
        }
    }
}
