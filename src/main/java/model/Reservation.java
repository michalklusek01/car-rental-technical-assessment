package model;

import java.time.LocalDateTime;
import java.util.Objects;

public class Reservation {
    private final long id;
    private final CarType carType;
    private final LocalDateTime start;
    private final LocalDateTime end;

    public Reservation(long id, CarType carType, LocalDateTime start, LocalDateTime end) {
        this.id = id;
        this.carType = Objects.requireNonNull(carType);
        this.start = Objects.requireNonNull(start);
        this.end = Objects.requireNonNull(end);
    }

    public long getId() {
        return id;
    }

    public CarType getCarType() {
        return carType;
    }

    public LocalDateTime getStart() {
        return start;
    }

    public LocalDateTime getEnd() {
        return end;
    }

    public boolean overlaps(LocalDateTime otherStart, LocalDateTime otherEnd) {
        return otherStart.isBefore(end) && start.isBefore(otherEnd);
    }
}
