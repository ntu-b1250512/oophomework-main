package dao;

import model.Movie;
import util.DBUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class MovieDAO {

    /**
     * Adds a new movie to the database.
     * @param movie The Movie object to add (UID can be 0).
     * @return The generated UID of the newly added movie, or -1 on failure.
     */
    public int addMovie(Movie movie) {
        String sql = "INSERT INTO movie (name, duration, description, rating) VALUES (?, ?, ?, ?)";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, movie.getName());
            stmt.setInt(2, movie.getDuration());
            stmt.setString(3, movie.getDescription());
            stmt.setString(4, movie.getRating());
            int affectedRows = stmt.executeUpdate();

            if (affectedRows == 0) {
                return -1; // Insert failed
            }

            try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    return generatedKeys.getInt(1); // Return generated UID
                } else {
                    return -1; // Failed to get generated key
                }
            }
        } catch (SQLException e) {
            // Handle potential unique constraint violation (name)
            if (e.getMessage() != null && e.getMessage().contains("UNIQUE constraint failed: movie.name")) {
                 System.err.println("Error adding movie: Name '" + movie.getName() + "' already exists.");
            } else {
                e.printStackTrace();
            }
            return -1;
        }
    }

    public Movie getMovieById(int id) {
        String sql = "SELECT * FROM movie WHERE uid = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return new Movie(
                            rs.getInt("uid"),
                            rs.getString("name"),
                            rs.getInt("duration"),
                            rs.getString("description"),
                            rs.getString("rating")
                    );
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Retrieves a movie by its exact name.
     * @param name The name of the movie.
     * @return The Movie object if found, null otherwise.
     */
    public Movie getMovieByName(String name) {
        String sql = "SELECT * FROM movie WHERE name = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, name);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return new Movie(
                            rs.getInt("uid"),
                            rs.getString("name"),
                            rs.getInt("duration"),
                            rs.getString("description"),
                            rs.getString("rating")
                    );
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public List<Movie> getAllMovies() {
        List<Movie> movies = new ArrayList<>();
        String sql = "SELECT * FROM movie";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                movies.add(new Movie(
                        rs.getInt("uid"),
                        rs.getString("name"),
                        rs.getInt("duration"),
                        rs.getString("description"),
                        rs.getString("rating")
                ));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return movies;
    }

    /**
     * Updates an existing movie in the database.
     * @param movie The Movie object with updated details (must have correct UID).
     * @return true if the update affected 1 row, false otherwise.
     */
    public boolean updateMovie(Movie movie) {
        String sql = "UPDATE movie SET name = ?, duration = ?, description = ?, rating = ? WHERE uid = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, movie.getName());
            stmt.setInt(2, movie.getDuration());
            stmt.setString(3, movie.getDescription());
            stmt.setString(4, movie.getRating());
            stmt.setInt(5, movie.getUid());
            int rowsAffected = stmt.executeUpdate();
            return rowsAffected > 0; // Return true if 1 row was updated
        } catch (SQLException e) {
             // Handle potential unique constraint violation (name)
            if (e.getMessage() != null && e.getMessage().contains("UNIQUE constraint failed: movie.name")) {
                 System.err.println("Error updating movie: Name '" + movie.getName() + "' already exists for another movie.");
            } else {
                e.printStackTrace();
            }
            return false;
        }
    }

    /**
     * Deletes a movie from the database by its ID.
     * @param id The UID of the movie to delete.
     * @return true if the deletion affected 1 row, false otherwise.
     */
    public boolean deleteMovie(int id) {
        String sql = "DELETE FROM movie WHERE uid = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            int rowsAffected = stmt.executeUpdate();
            return rowsAffected > 0; // Return true if 1 row was deleted
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean existsByName(String name) {
        String sql = "SELECT 1 FROM movie WHERE name = ?";
        try (var conn = DBUtil.getConnection();
             var stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, name);
            try (var rs = stmt.executeQuery()) {
                return rs.next();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    // initializeMovies might be better placed in DBUtil or a setup script
    // public void initializeMovies() { ... }
}