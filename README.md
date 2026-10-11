# Car Rental

A simple in-memory car reservation system built with Java 17, Maven and JUnit 5.
It creates reservations for SEDAN, SUV and VAN cars and allows lookup by ID or car type.

## Rules

- A reservation has a start date/time and a positive number of days.
- The start cannot be in the past. The end is calculated as `start.plusDays(days)`.
- Back-to-back reservations are allowed: the end time is excluded from the reservation period.
- Default capacities are SEDAN: 5, SUV: 3 and VAN: 2.
- Availability is checked using a sweep line to count simultaneous reservations.
  A request is rejected if that count would exceed capacity.

## Run tests

Requires JDK 17 or newer and Maven:

```sh
mvn test
```

## Limitations

Data is stored only in memory. Dates use `LocalDateTime`, without time zone conversion.
Reservation creation is synchronized within one service instance. The example assumes
one service with an initially empty repository; separate instances do not share locks
or ID counters.
