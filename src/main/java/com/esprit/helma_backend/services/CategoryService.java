package com.esprit.helma_backend.services;

import com.esprit.helma_backend.entities.UserCategoryUsage;
import com.esprit.helma_backend.repositories.UserCategoryUsageRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class CategoryService {

    private final UserCategoryUsageRepository usageRepo;

    public CategoryService(UserCategoryUsageRepository usageRepo) {
        this.usageRepo = usageRepo;
    }

    // ── Master list ──────────────────────────────────────────────────────────
    public Map<String, List<String>> getMasterList() {
        Map<String, List<String>> map = new LinkedHashMap<>();

        map.put("🛒 Food & Groceries", List.of(
            "Groceries", "Supermarket", "Farmers Market", "Organic Food",
            "Bakery", "Butcher", "Fish Market", "Meal Kit Delivery"
        ));
        map.put("🍽️ Dining & Drinks", List.of(
            "Restaurant", "Fast Food", "Café & Coffee", "Bar & Nightlife",
            "Food Delivery", "Street Food", "Catering", "Work Lunch"
        ));
        map.put("🚗 Transport", List.of(
            "Fuel & Gas", "Public Transit", "Taxi & Rideshare", "Parking",
            "Car Maintenance", "Car Insurance", "Car Loan", "Tolls",
            "Bike & Scooter", "Flight", "Train", "Ferry"
        ));
        map.put("🏠 Housing", List.of(
            "Rent", "Mortgage", "Property Tax", "Home Insurance",
            "HOA Fees", "Repairs & Maintenance", "Furniture", "Appliances",
            "Cleaning Service", "Security System", "Storage"
        ));
        map.put("💡 Utilities", List.of(
            "Electricity", "Water", "Gas & Heating", "Internet",
            "Mobile Phone", "Landline", "Waste Collection", "Cable TV"
        ));
        map.put("🏥 Health & Medical", List.of(
            "Doctor Visit", "Dentist", "Pharmacy", "Hospital",
            "Health Insurance", "Therapy & Counseling", "Optician",
            "Lab Tests", "Gym & Fitness", "Supplements", "Spa & Wellness"
        ));
        map.put("🎓 Education", List.of(
            "Tuition", "Books & Supplies", "Online Courses", "Tutoring",
            "School Fees", "Student Loan", "Certification", "Workshop"
        ));
        map.put("👗 Shopping & Clothing", List.of(
            "Clothing", "Shoes", "Accessories", "Jewelry",
            "Electronics", "Gadgets", "Home Decor", "Sports Equipment",
            "Toys & Games", "Books", "Beauty & Cosmetics", "Gifts"
        ));
        map.put("🎬 Entertainment", List.of(
            "Streaming Services", "Cinema", "Concerts & Events", "Sports Events",
            "Gaming", "Hobbies", "Amusement Parks", "Museums & Culture",
            "Lottery & Gambling", "Photography"
        ));
        map.put("💼 Work & Business", List.of(
            "Office Supplies", "Software & SaaS", "Professional Services",
            "Business Travel", "Client Entertainment", "Advertising",
            "Equipment", "Freelance Expense", "Co-working Space"
        ));
        map.put("💰 Income", List.of(
            "Salary", "Freelance Income", "Business Revenue", "Rental Income",
            "Investment Return", "Dividends", "Bonus", "Side Hustle",
            "Gift Received", "Tax Refund", "Government Benefits", "Pension"
        ));
        map.put("💳 Finance & Banking", List.of(
            "Credit Card Payment", "Loan Repayment", "Bank Fees",
            "ATM Withdrawal", "Wire Transfer", "Currency Exchange",
            "Interest Paid", "Investment", "Crypto", "Savings Deposit"
        ));
        map.put("👨‍👩‍👧 Family & Pets", List.of(
            "Childcare", "Baby Supplies", "Pet Food", "Vet Bills",
            "Pet Insurance", "School Supplies", "Allowance", "Elder Care"
        ));
        map.put("✈️ Travel & Holidays", List.of(
            "Hotel & Accommodation", "Vacation Package", "Travel Insurance",
            "Visa & Passport", "Luggage", "Airport Transfers", "Activities Abroad"
        ));
        map.put("🤝 Giving", List.of(
            "Charity Donation", "Religious Tithe", "Crowdfunding",
            "Gift Given", "Family Support"
        ));
        map.put("📦 Other", List.of(
            "Miscellaneous", "Cash Withdrawal", "Uncategorized"
        ));

        return map;
    }

    // ── Frecency helpers ─────────────────────────────────────────────────────
    @Transactional(readOnly = true)
    public List<String> getFrequent(Long userId) {
        return usageRepo.findByUserIdOrderByUsageCountDesc(userId)
                .stream()
                .limit(8)
                .map(UserCategoryUsage::getCategory)
                .collect(Collectors.toList());
    }

    public void track(Long userId, String category) {
        usageRepo.findByUserIdAndCategory(userId, category)
                .ifPresentOrElse(
                    u -> u.setUsageCount(u.getUsageCount() + 1),
                    () -> usageRepo.save(
                        UserCategoryUsage.builder()
                            .userId(userId)
                            .category(category)
                            .usageCount(1)
                            .build()
                    )
                );
    }
}