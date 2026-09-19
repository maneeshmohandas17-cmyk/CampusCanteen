package com.canteen.campuscanteen.controller;

import com.canteen.campuscanteen.model.Student;
import com.canteen.campuscanteen.service.CanteenService;
import com.canteen.campuscanteen.service.StudentService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class StudentAuthController {

    public static final String SESSION_STUDENT = "currentStudent";

    private final StudentService studentService;
    private final CanteenService canteenService;

    public StudentAuthController(StudentService studentService, CanteenService canteenService) {
        this.studentService = studentService;
        this.canteenService = canteenService;
    }

    @GetMapping("/login")
    public String loginPage(@RequestParam(required = false) String redirect,
                            HttpSession session, Model model) {
        if (session.getAttribute(SESSION_STUDENT) != null) {
            return "redirect:/menu";
        }
        model.addAttribute("redirect", redirect);
        model.addAttribute("canteens", canteenService.getAllCanteenNames());
        return "login";
    }

    @PostMapping("/login")
    public String handleLogin(@RequestParam String rollNumber,
                              @RequestParam String password,
                              @RequestParam(required = false) String redirect,
                              HttpSession session,
                              RedirectAttributes redirectAttributes) {

        Student student = studentService.login(rollNumber, password);

        if (student != null) {
            session.setAttribute(SESSION_STUDENT, student);
            if (redirect != null && !redirect.trim().isEmpty() && !redirect.contains("login")) {
                return "redirect:" + redirect;
            }
            return "redirect:/menu";
        }

        redirectAttributes.addFlashAttribute("error", "Invalid Roll Number or Password! Please try again.");
        return "redirect:/login" + (redirect != null ? "?redirect=" + redirect : "");
    }

    @GetMapping("/register")
    public String registerPage(HttpSession session, Model model) {
        if (session.getAttribute(SESSION_STUDENT) != null) {
            return "redirect:/menu";
        }
        model.addAttribute("student", new Student());
        return "register";
    }

    @PostMapping("/register")
    public String handleRegister(@ModelAttribute Student student,
                                 HttpSession session,
                                 RedirectAttributes redirectAttributes) {
        try {
            Student saved = studentService.register(student);
            session.setAttribute(SESSION_STUDENT, saved);
            redirectAttributes.addFlashAttribute("success", "Account created successfully! Welcome to Campus Canteen.");
            return "redirect:/menu";
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
            return "redirect:/register";
        }
    }

    @GetMapping("/logout")
    public String logout(HttpSession session, RedirectAttributes redirectAttributes) {
        session.removeAttribute(SESSION_STUDENT);
        redirectAttributes.addFlashAttribute("info", "You have been logged out.");
        return "redirect:/login";
    }
}
