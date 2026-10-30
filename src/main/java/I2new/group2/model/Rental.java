package I2new.group2.model;

public class Rental extends Service {

    private RentalType rentalType;
    private Instrument instrument;

    /** @param instrument the rented instrument, required for an INSTRUMENT rental, null for a ROOM rental */
    public Rental(int id, String name, double price, RentalType rentalType, Instrument instrument) {
        super(id, name, price);
        if (rentalType == null) {
            throw new IllegalArgumentException("Rental type is required");
        }
        if (rentalType == RentalType.INSTRUMENT && instrument == null) {
            throw new IllegalArgumentException("An instrument rental needs an instrument");
        }
        this.rentalType = rentalType;
        this.instrument = rentalType == RentalType.INSTRUMENT ? instrument : null;
    }

    /** Copy constructor. */
    public Rental(Rental other) {
        super(other);
        this.rentalType = other.rentalType;
        this.instrument = other.instrument == null ? null : new Instrument(other.instrument);
    }

    @Override
    public double calculatePrice() {
        return getPrice();
    }

    @Override
    public String describe() {
        return rentalType == RentalType.INSTRUMENT
                ? "Instrument rental: " + instrument.getName()
                : "Room rental";
    }

    public RentalType getRentalType() { return rentalType; }
    public Instrument getInstrument() { return instrument; }
}
