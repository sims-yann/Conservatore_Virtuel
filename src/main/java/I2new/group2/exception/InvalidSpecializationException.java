package I2new.group2.exception;

/** Sequence diagram: the teacher does not teach the requested instrument. */
public class InvalidSpecializationException extends RuntimeException {

    public InvalidSpecializationException(String message) {
        super(message);
    }
}
