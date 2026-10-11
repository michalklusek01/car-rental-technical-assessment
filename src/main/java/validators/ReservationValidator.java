package validators;

import exception.ValidationException;
import model.ReservationRequest;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Objects;

public class ReservationValidator {
    private final Clock clock;

    public ReservationValidator() {
        this(Clock.systemDefaultZone());
    }

    public ReservationValidator(Clock clock) {
        this.clock = Objects.requireNonNull(clock, "Clock must not be null");
    }

    public void validateReservation(ReservationRequest request) {
        if (request == null) {
            throw new ValidationException("Request is required");
        }
        if (request.carType() == null) {
            throw new ValidationException("Car type is required");
        }
        if (request.start() == null) {
            throw new ValidationException("Start date is required");
        }
        if (request.numberOfReservationDays() <= 0) {
            throw new ValidationException("Days must be greater than 0");
        }
        if (request.start().isBefore(LocalDateTime.now(clock))) {
            throw new ValidationException("Start date cannot be before the current time");
        }
    }
}
