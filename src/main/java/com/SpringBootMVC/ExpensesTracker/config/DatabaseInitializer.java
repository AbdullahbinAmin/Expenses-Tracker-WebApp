package com.SpringBootMVC.ExpensesTracker.config;

import com.SpringBootMVC.ExpensesTracker.entity.Category;
import com.SpringBootMVC.ExpensesTracker.entity.Role;
import com.SpringBootMVC.ExpensesTracker.repository.CategoryRepository;
import com.SpringBootMVC.ExpensesTracker.repository.RoleRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

@Component
public class DatabaseInitializer implements CommandLineRunner {

    private final RoleRepository roleRepository;
    private final CategoryRepository categoryRepository;

    public DatabaseInitializer(RoleRepository roleRepository, CategoryRepository categoryRepository) {
        this.roleRepository = roleRepository;
        this.categoryRepository = categoryRepository;
    }

    @Override
    public void run(String... args) throws Exception {
        // Initialize Default Role
        if (roleRepository.findByName("ROLE_STANDARD") == null) {
            Role role = new Role();
            role.setId(1);
            role.setName("ROLE_STANDARD");
            roleRepository.save(role);
            System.out.println("Initialized default role: ROLE_STANDARD");
        }

        // Initialize Default Categories
        List<String> defaultCategories = Arrays.asList(
                "groceries",
                "Utilities(bills)",
                "transportation",
                "dining out",
                "entertainment",
                "shopping",
                "travel",
                "education"
        );

        for (int i = 0; i < defaultCategories.size(); i++) {
            String catName = defaultCategories.get(i);
            if (categoryRepository.findByName(catName) == null) {
                Category category = new Category();
                category.setId(i + 1);
                category.setName(catName);
                categoryRepository.save(category);
                System.out.println("Initialized default category: " + catName);
            }
        }
    }
}
