package model;

import java.time.LocalDateTime;

public record ReservationRequest(CarType carType, LocalDateTime start, long numberOfReservationDays) {
}
