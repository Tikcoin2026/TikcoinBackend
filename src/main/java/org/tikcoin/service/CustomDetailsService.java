package org.tikcoin.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.tikcoin.repository.AdminRepository;
import org.tikcoin.repository.BuyerRepository;

@Service
@RequiredArgsConstructor
public class CustomDetailsService implements UserDetailsService  {
    private final BuyerRepository userRepository;
    private final AdminRepository adminRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return userRepository.findByTiktokOpenId(username)
                .<UserDetails>map(u -> u)
                .or(() -> adminRepository.findByTiktokOpenId(username).map(u -> u))
                .or(() -> userRepository.findByEmailAddress(username).map(u -> u))
                .or(() -> adminRepository.findByEmailAddress(username).map(u -> u))
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
    }
}
