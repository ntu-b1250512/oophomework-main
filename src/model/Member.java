package model;

import java.time.LocalDate;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

public class Member {
    private int uid;
    private String email;
    private String password;
    private String birthDate;

    public Member(int uid, String email, String password, String birthDate) {
        this.uid = uid;
        this.email = email;
        this.password = password;
        this.birthDate = birthDate;
    }

    public int getUid() {
        return uid;
    }

    public String getEmail() {
        return email;
    }

    public String getPassword() {
        return password;
    }

    public String getBirthDate() {
        return birthDate;
    }
    
    /**
     * 獲取會員的用戶名，使用電子郵件作為用戶名
     * @return 會員的用戶名（電子郵件）
     */
    public String getUsername() {
        return email;
    }

    /**
     * Calculates the age of the member based on their birth date.
     * Assumes birthDate is in "YYYY-MM-DD" format.
     * Returns -1 if the birth date format is invalid.
     */
    public int getAge() {
        try {
            LocalDate birthLocalDate = LocalDate.parse(this.birthDate, DateTimeFormatter.ISO_LOCAL_DATE);
            LocalDate currentDate = LocalDate.now(); // Use current date for age calculation
            return Period.between(birthLocalDate, currentDate).getYears();
        } catch (DateTimeParseException e) {
            System.err.println("Invalid birth date format for member " + this.email + ": " + this.birthDate);
            return -1; // Indicate error
        }
    }

    @Override
    public String toString() {
        return String.format("Member [uid=%d, email=%s, birthDate=%s]", uid, email, birthDate);
    }
}