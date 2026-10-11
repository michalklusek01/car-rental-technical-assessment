import exception.NoAvailabilityException;
import exception.ReservationNotFoundException;
import exception.ValidationException;
import model.CarType;
import model.Reservation;
import model.ReservationRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import repository.InMemoryReservationRepository;
import repository.ReservationRepository;
import service.CarRentalService;
import validators.ReservationValidator;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class CarRentalServiceTest {
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2029-01-01T10:00:00Z"), ZoneOffset.UTC);

    private ReservationRepository repository;
    private CarRentalService service;

    @BeforeEach
    void setUp() {
        repository = new InMemoryReservationRepository();

        Map<CarType, Integer> capacity = new EnumMap<>(CarType.class);
        capacity.put(CarType.SEDAN, 1);
        capacity.put(CarType.SUV, 1);
        capacity.put(CarType.VAN, 2);

        service = new CarRentalService(repository, new ReservationValidator(CLOCK), capacity);
    }

    @Test
    void shouldAllowReservationSpanningTwoBackToBackReservations() {
        LocalDateTime start = LocalDateTime.of(2030, 1, 1, 10, 0);
        service.reserve(new ReservationRequest(CarType.VAN, start, 1));
        service.reserve(new ReservationRequest(CarType.VAN, start.plusDays(1), 1));

        Reservation created = assertDoesNotThrow(() ->
                service.reserve(new ReservationRequest(CarType.VAN, start, 2)));

        assertEquals(created.getId(), service.getReservation(created.getId()).getId());
        assertEquals(3, service.getReservationsByType(CarType.VAN).size());
    }

    @Test
    void shouldReserveCarWhenAvailable() {
        LocalDateTime start = LocalDateTime.of(2030, 1, 1, 10, 0);
        ReservationRequest request = new ReservationRequest(CarType.SEDAN, start, 2);

        Reservation reservation = service.reserve(request);

        assertTrue(reservation.getId() > 0);
        assertEquals(CarType.SEDAN, reservation.getCarType());
        assertEquals(start, reservation.getStart());
        assertEquals(start.plusDays(2), reservation.getEnd());
        assertEquals(1, service.getReservationsByType(CarType.SEDAN).size());
    }

    @Test
    void shouldRejectWhenStartDateIsInThePast() {
        LocalDateTime start = LocalDateTime.now(CLOCK).minusDays(1);
        ReservationRequest request = new ReservationRequest(CarType.SUV, start, 1);

        assertThrows(ValidationException.class, () -> service.reserve(request));
    }

    @Test
    void shouldRejectWhenReservationDaysIsNotPositive() {
        LocalDateTime start = LocalDateTime.of(2030, 1, 1, 10, 0);
        ReservationRequest request = new ReservationRequest(CarType.SUV, start, -1);

        assertThrows(ValidationException.class, () -> service.reserve(request));
    }

    @Test
    void shouldRejectReservationWhenCapacityExceededAndOverlaps() {
        LocalDateTime start = LocalDateTime.of(2030, 1, 1, 10, 0);

        service.reserve(new ReservationRequest(CarType.VAN, start, 3));
        service.reserve(new ReservationRequest(CarType.VAN, start.plusHours(1), 1));

        assertThrows(NoAvailabilityException.class, () ->
                service.reserve(new ReservationRequest(CarType.VAN, start.plusHours(2), 1))
        );

        assertEquals(2, service.getReservationsByType(CarType.VAN).size());
    }

    @Test
    void shouldAllowBackToBackReservations() {
        LocalDateTime start1 = LocalDateTime.of(2030, 1, 1, 10, 0);

        service.reserve(new ReservationRequest(CarType.SUV, start1, 2));

        LocalDateTime start2 = start1.plusDays(2);
        assertDoesNotThrow(() ->
                service.reserve(new ReservationRequest(CarType.SUV, start2, 1))
        );

        assertEquals(2, service.getReservationsByType(CarType.SUV).size());
    }

    @Test
    void shouldNotBlockDifferentCarTypes() {
        LocalDateTime start = LocalDateTime.of(2030, 1, 1, 10, 0);

        service.reserve(new ReservationRequest(CarType.SEDAN, start, 2));

        assertDoesNotThrow(() -> service.reserve(new ReservationRequest(CarType.SUV, start, 1)));

        assertEquals(1, service.getReservationsByType(CarType.SUV).size());
    }

    @Test
    void shouldGetReservationById() {
        LocalDateTime start = LocalDateTime.of(2030, 1, 1, 10, 0);
        Reservation created = service.reserve(new ReservationRequest(CarType.SEDAN, start, 1));

        Reservation found = service.getReservation(created.getId());

        assertEquals(created.getId(), found.getId());
        assertEquals(created.getCarType(), found.getCarType());
        assertEquals(created.getStart(), found.getStart());
        assertEquals(created.getEnd(), found.getEnd());
    }

    @Test
    void shouldThrowWhenReservationNotFound() {
        assertThrows(ReservationNotFoundException.class, () -> service.getReservation(9999L));
    }

    @Test
    void shouldRejectNullRequest() {
        assertThrows(ValidationException.class, () -> service.reserve(null));
    }

    @Test
    void shouldRejectNullCarType() {
        assertThrows(ValidationException.class, () -> service.reserve(
                new ReservationRequest(null, LocalDateTime.now(CLOCK), 1)));
    }

    @Test
    void shouldRejectNullStartWithoutSaving() {
        assertThrows(ValidationException.class, () -> service.reserve(
                new ReservationRequest(CarType.SUV, null, 1)));
        assertTrue(service.getReservationsByType(CarType.SUV).isEmpty());
    }

    @Test
    void shouldRejectNullTypeWhenListingReservations() {
        assertThrows(ValidationException.class, () -> service.getReservationsByType(null));
    }

    @Test
    void shouldRejectZeroDaysWithoutSaving() {
        assertThrows(ValidationException.class, () -> service.reserve(
                new ReservationRequest(CarType.SUV, LocalDateTime.now(CLOCK), 0)));
        assertTrue(service.getReservationsByType(CarType.SUV).isEmpty());
    }

    @Test
    void shouldAcceptStartExactlyAtCurrentTime() {
        assertDoesNotThrow(() -> service.reserve(
                new ReservationRequest(CarType.SUV, LocalDateTime.now(CLOCK), 1)));
    }

    @Test
    void shouldRejectStartJustBeforeCurrentTime() {
        assertThrows(ValidationException.class, () -> service.reserve(
                new ReservationRequest(CarType.SUV, LocalDateTime.now(CLOCK).minusNanos(1), 1)));
    }

    @Test
    void shouldDetectCapacityExceededLaterInRequestedPeriod() {
        LocalDateTime start = LocalDateTime.of(2030, 1, 1, 10, 0);
        service.reserve(new ReservationRequest(CarType.VAN, start.plusDays(1), 2));
        service.reserve(new ReservationRequest(CarType.VAN, start.plusDays(2), 1));
        assertThrows(NoAvailabilityException.class, () -> service.reserve(
                new ReservationRequest(CarType.VAN, start, 4)));
        assertEquals(2, service.getReservationsByType(CarType.VAN).size());
    }

    @Test
    void shouldCountReservationsAlreadyActiveAtRequestedStart() {
        LocalDateTime start = LocalDateTime.of(2030, 1, 1, 10, 0);
        service.reserve(new ReservationRequest(CarType.VAN, start, 4));
        service.reserve(new ReservationRequest(CarType.VAN, start.plusDays(1), 2));
        assertThrows(NoAvailabilityException.class, () -> service.reserve(
                new ReservationRequest(CarType.VAN, start.plusDays(2), 1)));
        assertEquals(2, service.getReservationsByType(CarType.VAN).size());
    }

    @Test
    void shouldRejectInvalidCapacityConfiguration() {
        ReservationValidator validator = new ReservationValidator(CLOCK);
        assertThrows(NullPointerException.class, () -> new CarRentalService(repository, validator, null));
        assertThrows(IllegalArgumentException.class, () -> new CarRentalService(
                repository, validator, Map.of(CarType.VAN, -1)));
        Map<CarType, Integer> capacity = new EnumMap<>(CarType.class);
        capacity.put(CarType.VAN, null);
        assertThrows(IllegalArgumentException.class, () -> new CarRentalService(repository, validator, capacity));
        Map<CarType, Integer> nullTypeCapacity = new HashMap<>();
        nullTypeCapacity.put(null, 1);
        assertThrows(IllegalArgumentException.class, () -> new CarRentalService(repository, validator, nullTypeCapacity));
    }

    @Test
    void shouldRejectZeroCapacityAndUnconfiguredTypeWithoutSaving() {
        service = new CarRentalService(repository, new ReservationValidator(CLOCK), Map.of(CarType.VAN, 0));
        LocalDateTime start = LocalDateTime.now(CLOCK);
        assertThrows(NoAvailabilityException.class, () -> service.reserve(new ReservationRequest(CarType.VAN, start, 1)));
        assertThrows(IllegalArgumentException.class, () -> service.reserve(new ReservationRequest(CarType.SUV, start, 1)));
        assertTrue(service.getReservationsByType(CarType.VAN).isEmpty());
        assertTrue(service.getReservationsByType(CarType.SUV).isEmpty());
    }

    @Test
    void shouldCopyCapacityConfiguration() {
        Map<CarType, Integer> capacity = new EnumMap<>(CarType.class);
        capacity.put(CarType.VAN, 1);
        service = new CarRentalService(repository, new ReservationValidator(CLOCK), capacity);
        capacity.put(CarType.VAN, 2);
        LocalDateTime start = LocalDateTime.now(CLOCK);
        service.reserve(new ReservationRequest(CarType.VAN, start, 1));
        assertThrows(NoAvailabilityException.class, () -> service.reserve(new ReservationRequest(CarType.VAN, start, 1)));
    }

    @Test
    void shouldUseDefaultCapacity() {
        service = new CarRentalService(repository, new ReservationValidator(CLOCK));
        for (CarType type : CarType.values()) {
            int limit = switch (type) {
                case SEDAN -> 5;
                case SUV -> 3;
                case VAN -> 2;
            };
            ReservationRequest request = new ReservationRequest(type, LocalDateTime.now(CLOCK), 1);
            for (int i = 0; i < limit; i++) {
                service.reserve(request);
            }
            assertThrows(NoAvailabilityException.class, () -> service.reserve(request));
            assertEquals(limit, service.getReservationsByType(type).size());
        }
    }
}
