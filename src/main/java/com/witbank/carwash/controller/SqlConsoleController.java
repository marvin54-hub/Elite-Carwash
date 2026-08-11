package com.witbank.carwash.controller;

import com.witbank.carwash.model.Staff;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@Controller
@RequestMapping("/admin/sql")
public class SqlConsoleController {

    @PersistenceContext
    private EntityManager em;

    private boolean notAdmin(HttpSession s) {
        var staff = (Staff) s.getAttribute("staffUser");
        return staff == null || !"ADMIN".equalsIgnoreCase(staff.getRole());
    }

    @GetMapping
    public String consolePage(HttpSession session, Model model) {
        if (notAdmin(session)) return "redirect:/staff/login";
        model.addAttribute("tables", List.of(
            "BOOKINGS","CUSTOMERS","STAFF","VEHICLES","FEEDBACK",
            "PAYMENTS","SERVICE_PACKAGES","INVENTORY_ITEMS",
            "NOTIFICATION_LOGS","STAFF_SCHEDULE"));
        return "sql_console";
    }

    @PostMapping
    @Transactional
    public String runQuery(@RequestParam String sql, HttpSession session, Model model) {
        if (notAdmin(session)) return "redirect:/staff/login";
        model.addAttribute("tables", List.of(
            "BOOKINGS","CUSTOMERS","STAFF","VEHICLES","FEEDBACK",
            "PAYMENTS","SERVICE_PACKAGES","INVENTORY_ITEMS",
            "NOTIFICATION_LOGS","STAFF_SCHEDULE"));
        model.addAttribute("executedSql", sql);
        try {
            String upper = sql.trim().toUpperCase();
            if (upper.startsWith("SELECT") || upper.startsWith("SHOW")) {
                var q = em.createNativeQuery(sql);
                List<?> rows = q.getResultList();
                List<List<String>> table = new ArrayList<>();
                for (Object row : rows) {
                    List<String> cells = new ArrayList<>();
                    if (row instanceof Object[] arr) {
                        for (Object cell : arr) cells.add(cell != null ? cell.toString() : "NULL");
                    } else {
                        cells.add(row != null ? row.toString() : "NULL");
                    }
                    table.add(cells);
                }
                model.addAttribute("resultRows", table);
                model.addAttribute("rowCount", rows.size());
            } else {
                int affected = em.createNativeQuery(sql).executeUpdate();
                model.addAttribute("updateMessage", "Query executed. Rows affected: " + affected);
            }
        } catch (Exception e) {
            model.addAttribute("sqlError", e.getMessage());
        }
        return "sql_console";
    }
}
