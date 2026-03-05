// ===== FILE 1: DonationService.java (FIXED BACKEND) =====
package com.example.fooddonation.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.fooddonation.entity.DonationDTO;
import com.example.fooddonation.entity.DonationStatus;
import com.example.fooddonation.entity.DonorDTO;
import com.example.fooddonation.entity.NgoDTO;
import com.example.fooddonation.repository.DonationRepository;
import com.example.fooddonation.repository.DonorRepository;
import com.example.fooddonation.repository.NgoRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class DonationService {

    @Autowired
    private DonationRepository donationRepo;

    @Autowired
    private DonorRepository donorRepo;

    @Autowired
    private NgoRepository ngoRepo;

    @Autowired
    private EmailService emailService;

    public DonationDTO addDonation(int donorId, DonationDTO payload) {
        
        System.out.println("=".repeat(60));
        System.out.println("🔥 ADD DONATION START");
        System.out.println("=".repeat(60));
        System.out.println("📊 Donation Type RAW: [" + payload.getDonationType() + "]");
        System.out.println("💰 Amount: " + payload.getAmount());
        System.out.println("👤 Donor ID: " + donorId);

        Optional<DonorDTO> dOpt = donorRepo.findById(donorId);
        if (!dOpt.isPresent()) {
            throw new RuntimeException("Donor not found");
        }

        DonorDTO donor = dOpt.get();
        payload.setDonor(donor);

        // Attach NGO if exists
        if (payload.getNgo() != null && payload.getNgo().getId() != 0) {
            NgoDTO ngo = ngoRepo.findById(payload.getNgo().getId())
                    .orElseThrow(() -> new RuntimeException("NGO not found"));
            payload.setNgo(ngo);
            System.out.println("🏢 NGO attached: " + ngo.getNgoName());
        } else {
            payload.setNgo(null);
            System.out.println("⚠️ No NGO attached");
        }

        // ✅ CRITICAL FIX: Normalize donation type
        String donationType = payload.getDonationType();
        if (donationType != null) {
            donationType = donationType.trim().toUpperCase();
            payload.setDonationType(donationType); // ← Set it back!
            System.out.println("📝 Normalized donation type: [" + donationType + "]");
        } else {
            System.out.println("❌ ERROR: Donation type is NULL!");
        }

        // ✅ MONEY DONATION AUTOMATIC COMPLETION
        if ("MONEY".equals(donationType)) {
            
            System.out.println("💰💰💰 MONEY DONATION DETECTED - AUTO-COMPLETING");

            // Auto-quantity for money donations
            if (payload.getAmount() != null &&
                (payload.getQuantity() == null || payload.getQuantity().isEmpty())) {
                payload.setQuantity(payload.getAmount());
                System.out.println("✅ Set quantity to amount: " + payload.getAmount());
            }

            // ✅ Auto-set donation status timeline
            LocalDateTime now = LocalDateTime.now();
            
            payload.setStatus(DonationStatus.COMPLETED);
            payload.setConfirmedAt(now);
            payload.setScheduledAt(now);
            payload.setPickedUpAt(now);
            payload.setInTransitAt(now);
            payload.setDeliveredAt(now);
            payload.setCompletedAt(now);
            payload.setStatusMessage("Money donation received successfully. Thank you for your contribution!");
            payload.setUpdatedBy("SYSTEM");
            
            System.out.println("✅ Status set to: " + payload.getStatus());
            System.out.println("✅ All timestamps set to: " + now);
            System.out.println("✅ Status message: " + payload.getStatusMessage());

            // SAVE donation first
            DonationDTO savedDonation = donationRepo.save(payload);
            
            System.out.println("=".repeat(60));
            System.out.println("✅✅✅ MONEY DONATION SAVED SUCCESSFULLY");
            System.out.println("ID: " + savedDonation.getId());
            System.out.println("Status: " + savedDonation.getStatus());
            System.out.println("Completed At: " + savedDonation.getCompletedAt());
            System.out.println("=".repeat(60));

            // 👉 SEND EMAIL (after save)
            try {
                String ngoName = payload.getNgo() != null ? payload.getNgo().getNgoName() : "our organization";
                
                String subject = "✅ Donation Receipt - Thank You!";
                String message =
                        "Dear " + donor.getName() + ",\n\n" +
                        "Thank you for your generous donation of ₹" + payload.getAmount() + ".\n" +
                        "Your support helps us serve the community better.\n\n" +
                        "Donation Details:\n" +
                        "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n" +
                        "Amount: ₹" + payload.getAmount() + "\n" +
                        "NGO: " + ngoName + "\n" +
                        "Status: COMPLETED ✅\n" +
                        "Date: " + now.toString() + "\n" +
                        "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n\n" +
                        "Your donation has been immediately processed and recorded.\n" +
                        "This contribution will directly help those in need.\n\n" +
                        "Thank you for making a difference!\n\n" +
                        "With gratitude,\n" +
                        "Food Donation Team";

                emailService.sendEmail(donor.getEmail(), subject, message);
                System.out.println("📧 Email sent to: " + donor.getEmail());
                
            } catch (Exception emailEx) {
                System.err.println("⚠️ Email send failed: " + emailEx.getMessage());
            }

            System.out.println("=== MONEY DONATION COMPLETE ===\n");
            return savedDonation;
        }

        // ✅ NON-MONEY DONATIONS → Default: PENDING
        System.out.println("📦 Non-money donation - setting to PENDING");
        
        if (payload.getStatus() == null) {
            payload.setStatus(DonationStatus.PENDING);
            payload.setStatusMessage("Donation added to cart. Waiting for confirmation.");
            System.out.println("✅ Status set to PENDING");
        }

        DonationDTO saved = donationRepo.save(payload);
        System.out.println("✅ Donation saved with ID: " + saved.getId());
        System.out.println("=== ADD DONATION END ===\n");
        
        return saved;
    }

    public DonationDTO updateDonation(int id, int donorId, DonationDTO payload) {
        DonationDTO existing = donationRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Donation not found"));

        if (existing.getDonor() == null || existing.getDonor().getId() != donorId) {
            throw new RuntimeException("Not allowed");
        }

        existing.setDonationType(payload.getDonationType());
        existing.setFoodName(payload.getFoodName());
        existing.setMealType(payload.getMealType());
        existing.setCategory(payload.getCategory());
        existing.setQuantity(payload.getQuantity());
        existing.setCity(payload.getCity());
        existing.setExpiryDateTime(payload.getExpiryDateTime());
        existing.setAmount(payload.getAmount());
        existing.setClothesType(payload.getClothesType());
        existing.setItemName(payload.getItemName());

        if (payload.getNgo() != null && payload.getNgo().getId() != 0) {
            NgoDTO ngo = ngoRepo.findById(payload.getNgo().getId())
                    .orElseThrow(() -> new RuntimeException("NGO not found"));
            existing.setNgo(ngo);
        }

        return donationRepo.save(existing);
    }

    public List<DonationDTO> getAllDonations() {
        return donationRepo.findAll();
    }

    public List<DonationDTO> getByDonor(int donorId) {
        return donationRepo.findByDonorId(donorId);
    }

    public List<DonationDTO> getByNgo(int ngoId) {
        return donationRepo.findByNgoId(ngoId);
    }
}


// ===== FILE 2: DonationDTO.java - Make sure @PrePersist doesn't override =====
// Add this to your @PrePersist method in DonationDTO.java:

/*
@PrePersist
public void setTimestamp() {
    this.donatedDate = LocalDateTime.now();
    
    // ✅ FIX: Don't override status if it's already set (for MONEY donations)
    if (this.status == null) {
        this.status = DonationStatus.PENDING;
    }
    
    System.out.println("@PrePersist called - Status is: " + this.status);
}
*/