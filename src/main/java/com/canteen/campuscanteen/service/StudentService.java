package com.canteen.campuscanteen.service;

import com.canteen.campuscanteen.model.Student;
import com.canteen.campuscanteen.repository.StudentRepository;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class StudentService {

    private final StudentRepository studentRepository;

    public StudentService(StudentRepository studentRepository) {
        this.studentRepository = studentRepository;
    }

    public Student login(String rollNumber, String password) {
        if (rollNumber == null || password == null) return null;
        Optional<Student> studentOpt = studentRepository.findById(rollNumber.trim().toUpperCase());

        if (studentOpt.isPresent() && studentOpt.get().getPassword().equals(password.trim())) {
            return studentOpt.get();
        }

        return null;
    }

    public Student register(Student student) throws IllegalArgumentException {
        if (student.getRollNumber() == null || student.getRollNumber().trim().isEmpty()) {
            throw new IllegalArgumentException("Roll Number is required");
        }
        student.setRollNumber(student.getRollNumber().trim().toUpperCase());

        if (studentRepository.existsByRollNumber(student.getRollNumber())) {
            throw new IllegalArgumentException("Roll number is already registered. Please log in.");
        }

        if (student.getEmail() != null && studentRepository.existsByEmail(student.getEmail().trim())) {
            throw new IllegalArgumentException("Email is already registered. Please log in.");
        }

        return studentRepository.save(student);
    }

    public Optional<Student> getByRollNumber(String rollNumber) {
        if (rollNumber == null) return Optional.empty();
        return studentRepository.findById(rollNumber.trim().toUpperCase());
    }

    public Student save(Student student) {
        return studentRepository.save(student);
    }
}
