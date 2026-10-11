package repository;

import model.CarType;
import model.Reservation;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class InMemoryReservationRepository implements ReservationRepository {
    private final List<Reservation> allReservations = new ArrayList<>();

    @Override
    public synchronized Reservation save(Reservation reservation) {
        Objects.requireNonNull(reservation, "Reservation must not be null");
        allReservations.add(reservation);
        return reservation;
    }

    @Override
    public synchronized List<Reservation> findAllReservationsByCarType(CarType carType) {
        Objects.requireNonNull(carType, "Car type must not be null");
        return allReservations.stream()
                .filter(reservation -> reservation.getCarType() == carType)
                .toList();
    }

    @Override
    public synchronized Optional<Reservation> getReservationById(long id) {
        return allReservations.stream()
                .filter(reservation -> reservation.getId() == id)
                .findFirst();
    }
}
