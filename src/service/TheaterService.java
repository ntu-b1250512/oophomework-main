// src/service/TheaterService.java
package service;

import dao.TheaterDAO;
import model.Theater;

import java.util.List;
import java.util.Optional;

public class TheaterService {
    private final TheaterDAO theaterDAO = new TheaterDAO();

    /**
     * Lists all theaters available in the system.
     * @return List of Theater objects.
     */
    public List<Theater> listTheaters() {
        return theaterDAO.getAllTheaters();
    }

    /**
     * Retrieves a theater by its unique ID.
     * @param theaterId The UID of the theater.
     * @return Optional containing Theater if found, otherwise empty.
     */
    public Optional<Theater> getTheaterById(int theaterId) {
        return Optional.ofNullable(theaterDAO.getTheaterById(theaterId));
    }
}
