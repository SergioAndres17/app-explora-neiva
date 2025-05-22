package com.corhuila.app_explora_neiva.Service;

import java.util.Optional;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.corhuila.app_explora_neiva.DTO.LoginDto;
import com.corhuila.app_explora_neiva.DTO.UserDto;
import com.corhuila.app_explora_neiva.Entity.User;
import com.corhuila.app_explora_neiva.Entity.VerificationCode;
import com.corhuila.app_explora_neiva.repository.UserRepository;
import com.corhuila.app_explora_neiva.repository.VerificationCodeRepository;

import java.time.LocalDateTime;
import java.util.Random;

@Service
public class AuthService {
    private final UserRepository userRepository;
    private final VerificationCodeRepository codeRepository;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;

    public AuthService(UserRepository userRepository, VerificationCodeRepository codeRepository,
            EmailService emailService, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.codeRepository = codeRepository;
        this.emailService = emailService;
        this.passwordEncoder = passwordEncoder;
    }

    public void registerUser(UserDto userDto) {
        User user = new User();
        user.setDocumentType(userDto.getDocumentType());
        user.setDocumentNumber(userDto.getDocumentNumber());
        user.setFullName(userDto.getFullName());
        user.setBirthDate(userDto.getBirthDate());
        user.setEmail(userDto.getEmail());
        user.setPhone(userDto.getPhone());
        user.setPassword(passwordEncoder.encode(userDto.getPassword()));

        userRepository.save(user);

        String code = generateVerificationCode();
        VerificationCode verificationCode = new VerificationCode();
        verificationCode.setCode(code);
        verificationCode.setEmail(userDto.getEmail());
        verificationCode.setExpirationTime(LocalDateTime.now().plusMinutes(15));
        codeRepository.save(verificationCode);

        emailService.sendVerificationEmail(userDto.getEmail(), code);
    }

    public boolean verifyEmail(String email, String code) {
        Optional<VerificationCode> verificationCode = codeRepository.findByEmailAndCode(email, code);
        if (verificationCode.isPresent() && LocalDateTime.now().isBefore(verificationCode.get().getExpirationTime())) {
            User user = userRepository.findByEmail(email).orElseThrow();
            user.setVerified(true);
            userRepository.save(user);
            return true;
        }
        return false;
    }

    public User authenticate(LoginDto loginDto) {
        User user = userRepository.findByEmail(loginDto.getEmail())
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
        if (!passwordEncoder.matches(loginDto.getPassword(), user.getPassword())) {
            throw new RuntimeException("Contraseña incorrecta");
        }
        if (!user.isVerified()) {
            throw new RuntimeException("Email no verificado");
        }
        return user;
    }

    private String generateVerificationCode() {
        return String.format("%04d", new Random().nextInt(10000));
    }
}
