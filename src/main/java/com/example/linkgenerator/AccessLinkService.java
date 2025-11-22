package com.example.linkgenerator;

import java.net.MalformedURLException;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class AccessLinkService {

    private static final Logger logger = LoggerFactory.getLogger(AccessLinkService.class);

    @Autowired
    AccessLinkRepository accessLinkRepository;

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd-MM-yyyy hh:mm a")
            .withZone(ZoneId.of("Asia/Kolkata")); // Indian Standard Time (IST)

    /**
     * Generates a new access link with a proper URL format.
     * @throws IllegalArgumentException if the target resource URL is invalid
     */
    public AccessLink generateAccessLink(String targetResource, LocalDateTime expiryTime) {
        // Validate and format the target resource URL
        String formattedUrl = formatAndValidateUrl(targetResource);
        
        AccessLink accessLink = new AccessLink();
        String id = UUID.randomUUID().toString();
        accessLink.setId(id);
        accessLink.setUrl("http://timelybackend.raniasmit.me/api/access-links/" + id);
        accessLink.setTargetResource(formattedUrl);
        accessLink.setExpiryTime(expiryTime);
        accessLink.setUsed(false);


        logger.info("Generating new access link for target resource: {}, expires at: {}", formattedUrl, expiryTime);
        return accessLinkRepository.save(accessLink);

    }

    /**
     * Validates the access link by its ID.
     */
    public Optional<AccessLink> validateAccessLink(String id) {
        Optional<AccessLink> accessLink = accessLinkRepository.findById(id);
        
        if (accessLink.isPresent()) {
            AccessLink link = accessLink.get();
            LocalDateTime now = LocalDateTime.now();
            
            if (link.isUsed()) {
                logger.warn("Access link {} has already been used", id);
                return Optional.empty();
            } else if (link.getExpiryTime().isBefore(now)) {
                logger.warn("Access link {} has expired at {}", id, link.getExpiryTime());
                return Optional.empty();
            } else {
                logger.info("Access link {} is valid", id);
                return Optional.of(link);
            }
        }
        
        logger.warn("Access link {} not found", id);
        return Optional.empty();
    }

    /**
     * Marks the access link as used.
     */
    public void markAsUsed(String id) {
        accessLinkRepository.findById(id).ifPresent(link -> {
            link.setUsed(true);
            accessLinkRepository.save(link);
            logger.info("Access link {} has been marked as used", id);
        });
    }

    /**
     * Formats the expiry time in IST with 24-hour clock and AM/PM.
     */
    public String formatExpiryTime(LocalDateTime expiryTime) {
        return expiryTime.atZone(ZoneId.systemDefault())
                .withZoneSameInstant(ZoneId.of("Asia/Kolkata"))
                .format(DATE_TIME_FORMATTER);
    }

    /**
     * Validates and formats the target resource URL.
     * @throws IllegalArgumentException if the URL is invalid
     */
    private String formatAndValidateUrl(String targetResource) {
        try {
            if (!targetResource.startsWith("http://") && !targetResource.startsWith("https://")) {
                targetResource = "https://" + targetResource;
            }
            
            // Validate URL format
            new URL(targetResource);
            
            return targetResource;
        } catch (MalformedURLException e) {
            logger.error("Invalid target resource URL: {}", targetResource);
            throw new IllegalArgumentException("Invalid target resource URL: " + targetResource);
        }
    }
}