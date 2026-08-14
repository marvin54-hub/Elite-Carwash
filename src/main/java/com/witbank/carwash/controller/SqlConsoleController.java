package com.witbank.carwash.controller;

import com.witbank.carwash.model.Staff;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/admin/sql")
public class SqlConsoleController {

    @Autowired
    private JdbcTemplate jdbcTemplate;

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
    public String runQuery(@RequestParam String sql, HttpSession session, Model model) {
        if (notAdmin(session)) return "redirect:/staff/login";
        model.addAttribute("tables", List.of(
            "BOOKINGS","CUSTOMERS","STAFF","VEHICLES","FEEDBACK",
            "PAYMENTS","SERVICE_PACKAGES","INVENTORY_ITEMS",
            "NOTIFICATION_LOGS","STAFF_SCHEDULE"));
        model.addAttribute("executedSql", sql);
        try {
            String upper = sql.trim().toUpperCase();
            if (upper.startsWith("SELECT") || upper.startsWith("SHOW") || upper.startsWith("EXPLAIN") || upper.startsWith("WITH")) {
                List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql);
                List<String> columns = new ArrayList<>();
                List<List<String>> table = new ArrayList<>();
                if (!rows.isEmpty()) {
                    columns = new ArrayList<>(rows.get(0).keySet());
                    for (Map<String, Object> row : rows) {
                        List<String> cells = new ArrayList<>();
                        for (String col : columns) {
                            Object val = row.get(col);
                            cells.add(val != null ? val.toString() : "NULL");
                        }
                        table.add(cells);
                    }
                }
                model.addAttribute("columns", columns);
                model.addAttribute("resultRows", table);
                model.addAttribute("rowCount", rows.size());
            } else {
                int affected = jdbcTemplate.update(sql);
                model.addAttribute("updateMessage", "Query executed. Rows affected: " + affected);
            }
        } catch (Exception e) {
            model.addAttribute("sqlError", e.getMessage());
        }
        return "sql_console";
    }
}
