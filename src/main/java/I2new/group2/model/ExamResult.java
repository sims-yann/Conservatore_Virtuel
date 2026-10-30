package I2new.group2.model;

public class ExamResult {

    private Student student;
    private boolean passed;
    private Double score;

    public ExamResult(Student student, boolean passed, Double score) {
        if (student == null) {
            throw new IllegalArgumentException("A result belongs to a student");
        }
        this.student = student;
        recordResult(passed, score);
    }

    /** Copy constructor. */
    public ExamResult(ExamResult other) {
        this(new Student(other.student), other.passed, other.score);
    }

    /** Records (or corrects) pass/fail and the optional score out of 100. */
    public void recordResult(boolean passed, Double score) {
        if (score != null && (score < 0 || score > 100)) {
            throw new IllegalArgumentException("Score must be between 0 and 100");
        }
        this.passed = passed;
        this.score = score;
    }

    public Student getStudent() { return student; }
    public boolean isPassed() { return passed; }
    public Double getScore() { return score; }

    @Override
    public String toString() {
        return String.format("%-22s %-4s %s", student.getFullName(), passed ? "PASS" : "FAIL",
                score == null ? "" : String.format("(%.1f/100)", score));
    }
}
