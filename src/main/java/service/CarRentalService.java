package service;

import exception.NoAvailabilityException;
import exception.ReservationNotFoundException;
import exception.ValidationException;
import model.CarType;
import model.Reservation;
import model.ReservationRequest;
import repository.ReservationRepository;
import validators.ReservationValidator;

import java.time.LocalDateTime;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

public class CarRentalService {
    private long lastReservationId;
    private final Map<CarType, Integer> capacity = new EnumMap<>(CarType.class);
    private final ReservationRepository reservationRepository;
    private final ReservationValidator reservationValidator;

    public CarRentalService(ReservationRepository reservationRepository, ReservationValidator reservationValidator) {
        this(reservationRepository, reservationValidator,
                Map.of(CarType.SUV, 3, CarType.VAN, 2, CarType.SEDAN, 5));
    }

    public CarRentalService(ReservationRepository reservationRepository,
                            ReservationValidator reservationValidator,
                            Map<CarType, Integer> capacity) {
        this.reservationRepository = Objects.requireNonNull(reservationRepository);
        this.reservationValidator = Objects.requireNonNull(reservationValidator);
        Objects.requireNonNull(capacity, "Capacity must not be null");
        capacity.forEach((type, count) -> {
            if (type == null || count == null || count < 0) {
                throw new IllegalArgumentException("Capacity requires a car type and a non-negative count");
            }
            this.capacity.put(type, count);
        });
    }

    public synchronized Reservation reserve(ReservationRequest reservationRequest) {
        reservationValidator.validateReservation(reservationRequest);

        LocalDateTime start = reservationRequest.start();
        LocalDateTime end = start.plusDays(reservationRequest.numberOfReservationDays());

        CarType type = reservationRequest.carType();

        Integer capacityForType = capacity.get(type);
        if (capacityForType == null) {
            throw new IllegalArgumentException("No capacity configured for " + type);
        }

        if (!hasAvailability(type, start, end, capacityForType)) {
            throw new NoAvailabilityException("No availability for " + type);
        }

        long id = ++lastReservationId;
        Reservation reservation = new Reservation(id, type, start, end);
        reservationRepository.save(reservation);
        return reservation;
    }

    private boolean hasAvailability(CarType type, LocalDateTime start, LocalDateTime end, int limit) {
        Map<LocalDateTime, Integer> events = new TreeMap<>();
        events.merge(start, 1, Integer::sum);
        events.merge(end, -1, Integer::sum);
        for (Reservation reservation : reservationRepository.findAllReservationsByCarType(type)) {
            if (reservation.overlaps(start, end)) {
                LocalDateTime overlapStart = reservation.getStart().isBefore(start) ? start : reservation.getStart();
                LocalDateTime overlapEnd = reservation.getEnd().isAfter(end) ? end : reservation.getEnd();
                events.merge(overlapStart, 1, Integer::sum);
                events.merge(overlapEnd, -1, Integer::sum);
            }
        }
        int concurrentReservations = 0;
        // Combine starts and ends at the same time so back-to-back reservations do not overlap.
        for (int change : events.values()) {
            concurrentReservations += change;
            if (concurrentReservations > limit) {
                return false;
            }
        }
        return true;
    }

    public List<Reservation> getReservationsByType(CarType carType) {
        if (carType == null) {
            throw new ValidationException("Car type cannot be null");
        }
        return reservationRepository.findAllReservationsByCarType(carType);
    }

    public Reservation getReservation(long id) {
        return reservationRepository.getReservationById(id)
                .orElseThrow(() -> new ReservationNotFoundException("Reservation with ID " + id + " not found"));
    }
}
