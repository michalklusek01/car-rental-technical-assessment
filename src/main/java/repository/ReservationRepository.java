package repository;

import model.CarType;
import model.Reservation;

import java.util.List;
import java.util.Optional;

public interface ReservationRepository {
    Reservation save(Reservation reservation);

    List<Reservation> findAllReservationsByCarType(CarType carType);

    Optional<Reservation> getReservationById(long id);
}
